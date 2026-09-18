/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.orders.statement.generate;

import cn.shopex.ecshopx.common.cron.statement.StatementSettlementCursorRedisPort;
import cn.shopex.ecshopx.common.distribution.DistributorGetInfoSimpleByDistributorIdPort;
import cn.shopex.ecshopx.orders.domain.StatementDetails;
import cn.shopex.ecshopx.orders.domain.Statements;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.StatementDetailsMapper;
import cn.shopex.ecshopx.orders.mapper.StatementGenerationQueryMapper;
import cn.shopex.ecshopx.orders.mapper.StatementsMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.statement.StatementPeriodValue;
import cn.shopex.ecshopx.orders.statement.StatementTimeWindowCalculator;
import cn.shopex.ecshopx.supplier.domain.Supplier;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierMapper;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 内联 {@code GenerateStatementsJob}：单窗口内 {@code forSupplier}/{@code forDistributor} 各一事务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatementGenerateJobInliner {

	private static final int ORDER_PAGE = 500;
	private static final String MERCHANT_SUPPLIER = "supplier";

	private final StatementSettlementCursorRedisPort cursorRedisPort;
	private final DistributorGetInfoSimpleByDistributorIdPort distributorGetInfoSimpleByDistributorIdPort;
	private final StatementsMapper statementsMapper;
	private final StatementDetailsMapper statementDetailsMapper;
	private final StatementGenerationQueryMapper statementGenerationQueryMapper;
	private final TradeMapper tradeMapper;
	private final SupplierMapper supplierMapper;
	private final SupplierOrderMapper supplierOrderMapper;
	private final PlatformTransactionManager platformTransactionManager;

	public int runJob(
			Clock clock,
			long companyId,
			long distributorOrSupplierTableId,
			StatementPeriodValue period,
			long lastEndTime,
			String merchantType) {
		long now = clock.instant().getEpochSecond();
		String u = period.unit();
		int n = period.n();
		int windows = 0;
		long startTime = lastEndTime + 1;
		long endTime =
				switch (u) {
					case "day" -> StatementTimeWindowCalculator.jobFirstEndDay(lastEndTime, n);
					case "week" -> StatementTimeWindowCalculator.jobFirstEndWeek(lastEndTime, n);
					case "month" -> StatementTimeWindowCalculator.jobFirstEndMonth(lastEndTime, n);
					default -> Long.MAX_VALUE;
				};
		if (endTime == Long.MAX_VALUE) {
			return 0;
		}
		while (endTime < now) {
			boolean ok =
					doGenerate(
							clock,
							companyId,
							distributorOrSupplierTableId,
							period,
							startTime,
							endTime,
							merchantType);
			if (!ok) {
				break;
			}
			windows++;
			startTime = endTime + 1;
			endTime =
					switch (u) {
						case "day" -> StatementTimeWindowCalculator.jobAdvanceEndDay(endTime, n);
						case "week" -> StatementTimeWindowCalculator.jobAdvanceEndWeek(endTime, n);
						case "month" -> StatementTimeWindowCalculator.jobAdvanceEndMonth(endTime, n);
						default -> Long.MAX_VALUE;
					};
		}
		return windows;
	}

	private boolean doGenerate(
			Clock clock,
			long companyId,
			long distributorOrSupplierTableId,
			StatementPeriodValue period,
			long startTime,
			long endTime,
			String merchantType) {
		if (MERCHANT_SUPPLIER.equals(merchantType)) {
			return forSupplier(clock, companyId, distributorOrSupplierTableId, period, startTime, endTime);
		}
		return forDistributor(clock, companyId, distributorOrSupplierTableId, period, startTime, endTime);
	}

	private boolean forDistributor(
			Clock clock,
			long companyId,
			long distributorId,
			StatementPeriodValue period,
			long startTime,
			long endTime) {
		TransactionTemplate tt = new TransactionTemplate(platformTransactionManager);
		try {
			tt.executeWithoutResult(
					status -> {
						Map<String, Object> dist =
								distributorGetInfoSimpleByDistributorIdPort.getInfoSimpleByDistributorId(
										companyId, distributorId);
						long merchantId = 0L;
						Object mid = dist.get("merchant_id");
						if (mid instanceof Number num) {
							merchantId = num.longValue();
						}
						String statementNo = genIdDistributor(distributorId);
						Statements head = new Statements();
						head.setCompanyId(companyId);
						head.setMerchantId(merchantId);
						head.setDistributorId(distributorId);
						head.setMerchantType("distributor");
						head.setStatementNo(statementNo);
						head.setOrderNum(0);
						head.setTotalFee(0);
						head.setFreightFee(0);
						head.setIntraCityFreightFee(0);
						head.setRebateFee(0);
						head.setRefundFee(0);
						head.setStatementFee(0);
						head.setStartTime((int) startTime);
						head.setEndTime((int) endTime);
						head.setSupplierId(0L);
						int t0 = (int) (clock.millis() / 1000L);
						head.setCreated(t0);
						head.setUpdated(t0);
						statementsMapper.insert(head);

						int off = 0;
						Statements acc = head;
						while (true) {
							List<StatementDistributorOrderRow> list =
									statementGenerationQueryMapper.selectDistributorOrdersPage(
											companyId, distributorId, endTime, off, ORDER_PAGE);
							if (list == null) {
								list = List.of();
							}
							List<StatementDetails> batch = new ArrayList<>();
							for (StatementDistributorOrderRow row : list) {
								int costFee =
										statementGenerationQueryMapper.sumItemCostForDistributorOrder(
												companyId, row.getOrderId());
								int total = nz(row.getTotalFee());
								int refund = nz(row.getRefundFee());
								int intra = 0;
								int freightMain = 0;
								if ("dada".equals(row.getReceiptType())) {
									intra = nz(row.getFreightFee());
								} else {
									freightMain = nz(row.getFreightFee());
								}
								int statementFee = total - refund - intra - costFee;
								StatementDetails d = new StatementDetails();
								d.setCompanyId(companyId);
								d.setMerchantId(merchantId);
								d.setDistributorId(distributorId);
								d.setSupplierId(0L);
								d.setStatementId(head.getId());
								d.setStatementNo(statementNo);
								d.setOrderId(row.getOrderId());
								d.setTotalFee(total);
								d.setRebateFee(0);
								d.setRefundFee(refund);
								d.setPayType(row.getPayType());
								d.setFreightFee(freightMain);
								d.setIntraCityFreightFee(intra);
								d.setStatementFee(statementFee);
								d.setCreated(t0);
								d.setUpdated(t0);
								batch.add(d);
								acc.setOrderNum(nz(acc.getOrderNum()) + 1);
								acc.setTotalFee(nz(acc.getTotalFee()) + total);
								acc.setFreightFee(nz(acc.getFreightFee()) + freightMain);
								acc.setIntraCityFreightFee(nz(acc.getIntraCityFreightFee()) + intra);
								acc.setRefundFee(nz(acc.getRefundFee()) + refund);
								acc.setStatementFee(nz(acc.getStatementFee()) + statementFee);
							}
							if (!list.isEmpty()) {
								statementDetailsMapper.insertBatch(batch);
								List<String> tradeIds = new ArrayList<>();
								for (StatementDistributorOrderRow r : list) {
									tradeIds.add(r.getTradeId());
								}
								LambdaUpdateWrapper<Trade> uw = new LambdaUpdateWrapper<>();
								uw.in(Trade::getTradeId, tradeIds).set(Trade::getIsSettled, true);
								tradeMapper.update(null, uw);
							}
							off += ORDER_PAGE;
							if (list.size() < ORDER_PAGE) {
								break;
							}
						}
						statementsMapper.updateById(acc);
						cursorRedisPort.setDistributorLastEnd(companyId, distributorId, String.valueOf(endTime));
					});
			return true;
		} catch (Exception e) {
			log.debug(
					"生成结算单失败：company_id_{} distributor_id_{} {}",
					companyId,
					distributorId,
					e.getMessage());
			return false;
		}
	}

	private boolean forSupplier(
			Clock clock,
			long companyId,
			long supplierTableId,
			StatementPeriodValue period,
			long startTime,
			long endTime) {
		Supplier row = supplierMapper.selectById(supplierTableId);
		if (row == null || row.getOperatorId() == null) {
			return false;
		}
		long operatorId = row.getOperatorId();
		TransactionTemplate tt = new TransactionTemplate(platformTransactionManager);
		try {
			tt.executeWithoutResult(
					status -> {
						String statementNo = genIdSupplier(supplierTableId);
						Statements head = new Statements();
						head.setCompanyId(companyId);
						head.setMerchantId(0L);
						head.setSupplierId(supplierTableId);
						head.setDistributorId(0L);
						head.setMerchantType("supplier");
						head.setStatementNo(statementNo);
						head.setOrderNum(0);
						head.setTotalFee(0);
						head.setFreightFee(0);
						head.setIntraCityFreightFee(0);
						head.setRebateFee(0);
						head.setRefundFee(0);
						head.setStatementFee(0);
						head.setPointFee(0);
						head.setRefundNum(0);
						head.setRefundPoint(0);
						head.setRefundCostFee(0);
						head.setStartTime((int) startTime);
						head.setEndTime((int) endTime);
						int t0 = (int) (clock.millis() / 1000L);
						head.setCreated(t0);
						head.setUpdated(t0);
						statementsMapper.insert(head);

						int off = 0;
						Statements acc = head;
						while (true) {
							List<StatementSupplierOrderRow> list =
									statementGenerationQueryMapper.selectSupplierOrdersPage(
											companyId, operatorId, endTime, off, ORDER_PAGE);
							if (list == null) {
								list = List.of();
							}
							if (list.isEmpty()) {
								throw new IllegalStateException("数据为空");
							}
							List<Long> orderIds = new ArrayList<>();
							for (StatementSupplierOrderRow r : list) {
								orderIds.add(r.getOrderId());
							}
							Map<Long, Integer> numMap = new HashMap<>();
							List<StatementOrderItemNumRow> nums =
									statementGenerationQueryMapper.sumOrderSupplierItemNums(
											companyId, orderIds, operatorId);
							if (nums != null) {
								for (StatementOrderItemNumRow n : nums) {
									numMap.put(n.getOrderId(), nz(n.getNum()));
								}
							}
							List<StatementDetails> batch = new ArrayList<>();
							for (StatementSupplierOrderRow r : list) {
								StatementRefundInfoRow ref =
										statementGenerationQueryMapper.selectSupplierRefundInfo(
												companyId, r.getOrderId(), operatorId);
								int refundedFee = ref == null || ref.getRefundedFee() == null ? 0 : ref.getRefundedFee();
								int refundedPoint = ref == null || ref.getRefundedPoint() == null ? 0 : ref.getRefundedPoint();
								int refundCostFee = ref == null || ref.getRefundCostFee() == null ? 0 : ref.getRefundCostFee();
								int joinRefund = nz(r.getRefundFee());
								long asNum =
										statementGenerationQueryMapper.sumAftersalesDetailNumForOrder(
												companyId, r.getOrderId());
								int orderNum = numMap.getOrDefault(r.getOrderId(), 0);
								StatementDetails d = new StatementDetails();
								d.setCompanyId(companyId);
								d.setMerchantId(0L);
								d.setSupplierId(supplierTableId);
								d.setDistributorId(0L);
								d.setStatementId(head.getId());
								d.setStatementNo(statementNo);
								d.setOrderId(r.getOrderId());
								int totalI = intFromDecimalish(r.getTotalFee());
								d.setTotalFee(totalI);
								d.setRebateFee(0);
								d.setPayType(r.getPayType());
								int intra = 0;
								int freightMain = 0;
								if ("dada".equals(r.getReceiptType())) {
									intra = nz(r.getFreightFee());
								} else {
									freightMain = nz(r.getFreightFee());
								}
								int costF = nz(r.getCostFee());
								d.setItemFee(intFromString(r.getItemFee()));
								d.setCommissionFee(nz(r.getCommissionFee()));
								d.setCostFee(costF);
								d.setPointFee(nz(r.getPointFee()));
								d.setNum(orderNum);
								if ("CANCEL".equals(r.getOrderStatus()) && "PAYED".equals(r.getPayStatus())) {
									d.setRefundFee(totalI);
									d.setRefundNum(orderNum);
									d.setRefundPoint(nz(r.getPointFee()));
									d.setRefundCostFee(costF);
								} else {
									d.setRefundFee(refundedFee);
									d.setRefundNum((int) asNum);
									d.setRefundPoint(refundedPoint);
									d.setRefundCostFee(refundCostFee);
								}
								d.setFreightFee(freightMain);
								d.setIntraCityFreightFee(intra);
								int stFee = costF - joinRefund - intra;
								d.setStatementFee(stFee);
								d.setCreated(t0);
								d.setUpdated(t0);
								batch.add(d);
								acc.setOrderNum(nz(acc.getOrderNum()) + 1);
								acc.setTotalFee(nz(acc.getTotalFee()) + d.getTotalFee());
								acc.setFreightFee(nz(acc.getFreightFee()) + freightMain);
								acc.setIntraCityFreightFee(nz(acc.getIntraCityFreightFee()) + intra);
								acc.setRebateFee(nz(acc.getRebateFee()));
								acc.setRefundFee(nz(acc.getRefundFee()) + d.getRefundFee());
								acc.setStatementFee(nz(acc.getStatementFee()) + stFee);
								acc.setPointFee(nz(acc.getPointFee()) + nz(r.getPointFee()));
								acc.setRefundNum(nz(acc.getRefundNum()) + d.getRefundNum());
								acc.setRefundPoint(nz(acc.getRefundPoint()) + d.getRefundPoint());
								acc.setRefundCostFee(nz(acc.getRefundCostFee()) + d.getRefundCostFee());
							}
							statementDetailsMapper.insertBatch(batch);
							LambdaUpdateWrapper<SupplierOrder> uw = new LambdaUpdateWrapper<>();
							uw.eq(SupplierOrder::getCompanyId, companyId)
									.eq(SupplierOrder::getSupplierId, operatorId)
									.in(SupplierOrder::getOrderId, orderIds)
									.set(SupplierOrder::getIsSettled, true);
							supplierOrderMapper.update(null, uw);
							off += ORDER_PAGE;
							if (list.size() < ORDER_PAGE) {
								break;
							}
						}
						statementsMapper.updateById(acc);
						cursorRedisPort.setSupplierLastEnd(companyId, supplierTableId, String.valueOf(endTime));
					});
			return true;
		} catch (Exception e) {
			log.debug(
					"生成结算单失败：company_id_{} supplier_id_{} {}",
					companyId,
					supplierTableId,
					e.getMessage());
			return false;
		}
	}

	private static int nz(Integer v) {
		return v == null ? 0 : v;
	}

	private static int intFromString(String s) {
		if (s == null || s.isEmpty()) {
			return 0;
		}
		try {
			return (int) Double.parseDouble(s);
		} catch (Exception e) {
			return 0;
		}
	}

	private static int intFromDecimalish(Integer o) {
		return o == null ? 0 : o;
	}

	private static String genIdDistributor(long distributorId) {
		String ymd =
				java.time.LocalDate.now(StatementTimeWindowCalculator.SHANGHAI)
						.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
		int r = 1000 + ThreadLocalRandom.current().nextInt(9000);
		return ymd + r + leftPad4(Math.floorMod(distributorId, 10_000L));
	}

	private static String genIdSupplier(long supplierId) {
		return genIdDistributor(supplierId);
	}

	private static String leftPad4(long n) {
		return String.format("%04d", n);
	}
}
