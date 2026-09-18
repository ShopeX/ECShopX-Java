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

package cn.shopex.ecshopx.orders.service;

import cn.shopex.ecshopx.common.dispatch.HfpayProfitSharingEventDispatchPublisher;
import cn.shopex.ecshopx.hfpay.domain.HfpayEnterapply;
import cn.shopex.ecshopx.hfpay.mapper.HfpayEnterapplyMapper;
import cn.shopex.ecshopx.common.hfpay.payment.HfPayPaymentSettingLoadPort;
import cn.shopex.ecshopx.popularize.domain.Brokerage;
import cn.shopex.ecshopx.popularize.mapper.BrokerageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderProfitSharing;
import cn.shopex.ecshopx.orders.domain.OrderProfitSharingDetails;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderProfitSharingDetailsMapper;
import cn.shopex.ecshopx.orders.mapper.OrderProfitSharingMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;

/**
 * 汇付分账跑批；每单独立事务，提交后经统一 Dispatch Bus 投递分账确认事件并驱动 pay006 与主表回写。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProfitSharingService {

	private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
	private static final int BATCH_LIMIT = 500;

	private final NormalOrdersMapper normalOrdersMapper;
	private final OrderProfitSharingMapper orderProfitSharingMapper;
	private final OrderProfitSharingDetailsMapper orderProfitSharingDetailsMapper;
	private final AftersalesRefundMapper aftersalesRefundMapper;
	private final BrokerageMapper brokerageMapper;
	private final HfpayEnterapplyMapper hfpayEnterapplyMapper;
	private final HfPayPaymentSettingLoadPort hfPayPaymentSettingLoadPort;
	private final TradeMapper tradeMapper;
	private final HfpayProfitSharingEventDispatchPublisher hfpayProfitSharingEventDispatchPublisher;
	private final PlatformTransactionManager transactionManager;

	public int scheduleShareOrderProfit() {
		TransactionTemplate reqNew = new TransactionTemplate(transactionManager);
		reqNew.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		long nowSec = Instant.now().getEpochSecond();
		LambdaQueryWrapper<NormalOrders> w = new LambdaQueryWrapper<NormalOrders>()
				.eq(NormalOrders::getOrderStatus, "DONE")
				.eq(NormalOrders::getPayType, "hfpay")
				.eq(NormalOrders::getIsProfitsharing, 2)
				.eq(NormalOrders::getProfitsharingStatus, 1)
				.le(NormalOrders::getOrderAutoCloseAftersalesTime, (int) nowSec)
				.last("LIMIT " + BATCH_LIMIT);
		List<NormalOrders> batch = normalOrdersMapper.selectList(w);
		if (batch == null || batch.isEmpty()) {
			return 0;
		}
		int committed = 0;
		for (NormalOrders val : batch) {
			try {
				HfAccount acc = getOrderHfAccount(val.getCompanyId(), val.getDistributorId(), val.getOrderId());
				if (acc == null || !acc.ok) {
					continue;
				}
				long companyId = val.getCompanyId() != null ? val.getCompanyId() : 0L;
				long orderId = val.getOrderId() != null ? val.getOrderId() : 0L;
				long distId = val.getDistributorId() != null ? val.getDistributorId() : 0L;
				int refundSum = sumRefundFeeFen(companyId, orderId);
				int rebate = sumBrokerageRebateFen(companyId, orderId, val.getIsDistribution());
				int baseTotalFen = parseOrderTotalFeeFen(val) - refundSum;
				if (baseTotalFen < 0) {
					baseTotalFen = 0;
				}
				Integer pr = val.getProfitsharingRate();
				BigDecimal feeRateBd =
						BigDecimal.valueOf(pr == null ? 0 : pr)
								.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
				BigDecimal totalBd = BigDecimal.valueOf(baseTotalFen);
				BigDecimal feeAmt = BigDecimal.ZERO;
				if (pr != null && pr > 0) {
					feeAmt = totalBd.multiply(feeRateBd).divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
				}
				int distributorFen = baseTotalFen;
				if (feeAmt.compareTo(BigDecimal.ONE) >= 0) {
					distributorFen -= feeAmt.intValue();
				}
				if (rebate > 0) {
					distributorFen -= rebate;
				}
				if (distributorFen < 0) {
					distributorFen = 0;
				}
				final int feeAmtInt = feeAmt.intValue();
				final int distributorFenFinal = distributorFen;
				final HfAccount accFinal = acc;
				final int baseTotalFenFinal = baseTotalFen;
				final long orderIdF = orderId;
				final long companyIdF = companyId;
				final long distIdF = distId;
				final String payType = val.getPayType() != null ? val.getPayType() : "hfpay";
				final String isOpen = acc.isOpen;
				final int rebateF = rebate;

				reqNew.executeWithoutResult(
						st -> {
							List<Long> idBox = new ArrayList<>();
							OrderProfitSharing main = new OrderProfitSharing();
							main.setCompanyId(companyIdF);
							main.setOrderId(orderIdF);
							main.setDistributorId(distIdF);
							main.setPayType(payType);
							main.setChannelId(accFinal.userCustId);
							main.setChannelAcctId(accFinal.acctId);
							main.setTotalFee(baseTotalFenFinal);
							main.setStatus(0);
							orderProfitSharingMapper.insert(main);
							Long sharingId = main.getOrderProfitSharingId();
							if (sharingId == null) {
								throw new ResourceException("分账主表插入后主键缺失");
							}
							idBox.add(sharingId);

							List<OrderProfitSharingDetails> details = new ArrayList<>();
							if ("true".equals(isOpen)
									&& StringUtils.hasText(accFinal.sysUserCustId)
									&& StringUtils.hasText(accFinal.sysAcctId)) {
								int platformFen = feeAmtInt + rebateF;
								OrderProfitSharingDetails p = new OrderProfitSharingDetails();
								p.setSharingId(sharingId);
								p.setCompanyId(companyIdF);
								p.setDistributorId(0L);
								p.setOrderId(orderIdF);
								p.setChannelId(accFinal.sysUserCustId);
								p.setChannelAcctId(accFinal.sysAcctId);
								p.setTotalFee(platformFen);
								details.add(p);
							}
							OrderProfitSharingDetails shop = new OrderProfitSharingDetails();
							shop.setSharingId(sharingId);
							shop.setCompanyId(companyIdF);
							shop.setDistributorId(distIdF);
							shop.setOrderId(orderIdF);
							shop.setChannelId(accFinal.userCustId);
							shop.setChannelAcctId(accFinal.acctId);
							shop.setTotalFee(distributorFenFinal);
							details.add(shop);
							for (OrderProfitSharingDetails d : details) {
								orderProfitSharingDetailsMapper.insert(d);
							}
							NormalOrders patch = new NormalOrders();
							patch.setOrderId(orderIdF);
							patch.setProfitsharingStatus(2);
							normalOrdersMapper.updateById(patch);

							List<Long> publishIds = new ArrayList<>(idBox);
							TransactionSynchronizationManager.registerSynchronization(
									new TransactionSynchronization() {
										@Override
										public void afterCommit() {
											hfpayProfitSharingEventDispatchPublisher.publishProfitSharingAfterCommit(
													orderIdF, List.copyOf(publishIds));
										}
									});
						});
				committed++;
			} catch (Throwable t) {
				log.debug("hf_profit_data => {}", t.getMessage() != null ? t.getMessage() : t.toString());
			}
		}
		return committed;
	}

	private HfAccount getOrderHfAccount(Long companyId, Long distributorId, Long orderId) {
		if (companyId == null || orderId == null) {
			return HfAccount.fail();
		}
		Map<String, Object> setting;
		try {
			setting = hfPayPaymentSettingLoadPort.loadForCompany(companyId);
		} catch (Exception e) {
			return HfAccount.fail();
		}
		String isOpen = setting.get("is_open") == null ? "false" : String.valueOf(setting.get("is_open"));
		String sysUserCustId = setting.get("mer_cust_id") == null ? "" : String.valueOf(setting.get("mer_cust_id")).trim();
		String sysAcctId = setting.get("acct_id") == null ? "" : String.valueOf(setting.get("acct_id")).trim();

		long dist = distributorId != null ? distributorId : 0L;
		HfpayEnterapply ent =
				hfpayEnterapplyMapper.selectOne(
						new LambdaQueryWrapper<HfpayEnterapply>()
								.eq(HfpayEnterapply::getCompanyId, companyId)
								.eq(HfpayEnterapply::getDistributorId, dist)
								.eq(HfpayEnterapply::getStatus, "3")
								.last("LIMIT 1"));
		if (ent == null) {
			if (!"true".equals(isOpen) || !StringUtils.hasText(sysUserCustId) || !StringUtils.hasText(sysAcctId)) {
				return HfAccount.fail();
			}
			LambdaQueryWrapper<Trade> tw = new LambdaQueryWrapper<>();
			tw.eq(Trade::getCompanyId, String.valueOf(companyId));
			tw.eq(Trade::getOrderId, String.valueOf(orderId));
			tw.last("LIMIT 5");
			List<Trade> trades = tradeMapper.selectList(tw);
			if (trades == null || trades.isEmpty()) {
				return HfAccount.fail();
			}
			Instant payInst = firstPayInstant(trades.get(0));
			if (payInst == null) {
				return HfAccount.fail();
			}
			LocalDate payDay = payInst.atZone(CN).toLocalDate();
			LocalDate deadline = payDay.plusDays(160);
			if (deadline.isAfter(LocalDate.now(CN))) {
				return HfAccount.fail();
			}
			return HfAccount.ok(isOpen, 1, sysUserCustId, sysAcctId, sysUserCustId, sysAcctId);
		}
		String u = ent.getUserCustId() == null ? "" : ent.getUserCustId().trim();
		String a = ent.getAcctId() == null ? "" : ent.getAcctId().trim();
		return HfAccount.ok(isOpen, 2, u, a, sysUserCustId, sysAcctId);
	}

	private static Instant firstPayInstant(Trade t) {
		if (t == null) {
			return null;
		}
		String te = t.getTimeExpire();
		if (!StringUtils.hasText(te)) {
			return null;
		}
		String s = te.trim();
		try {
			if (s.chars().allMatch(Character::isDigit)) {
				long n = Long.parseLong(s);
				if (n > 1_000_000_000_000L) {
					n = n / 1000;
				}
				return Instant.ofEpochSecond(n);
			}
			return java.time.LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
					.atZone(CN)
					.toInstant();
		} catch (DateTimeParseException e) {
			try {
				return java.time.LocalDateTime.parse(s, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
						.atZone(CN)
						.toInstant();
			} catch (Exception e2) {
				return null;
			}
		}
	}

	private int sumRefundFeeFen(long companyId, long orderId) {
		QueryWrapper<AftersalesRefund> q = new QueryWrapper<>();
		q.select("IFNULL(SUM(refund_fee),0) AS s");
		q.eq("company_id", companyId);
		q.eq("order_id", orderId);
		q.in("refund_status", "SUCCESS", "AUDIT_SUCCESS");
		List<Map<String, Object>> rows = aftersalesRefundMapper.selectMaps(q);
		if (rows == null || rows.isEmpty()) {
			return 0;
		}
		Object v = rows.get(0).get("s");
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return new BigDecimal(String.valueOf(v).trim()).intValue();
		} catch (Exception e) {
			return 0;
		}
	}

	private int sumBrokerageRebateFen(long companyId, long orderId, Boolean isDistribution) {
		if (!Boolean.TRUE.equals(isDistribution)) {
			return 0;
		}
		List<Brokerage> list =
				brokerageMapper.selectList(
						new LambdaQueryWrapper<Brokerage>()
								.eq(Brokerage::getCompanyId, companyId)
								.eq(Brokerage::getOrderId, String.valueOf(orderId))
								.eq(Brokerage::getIsClose, true));
		if (list == null || list.isEmpty()) {
			return 0;
		}
		int s = 0;
		for (Brokerage b : list) {
			if (b.getRebate() != null) {
				s += b.getRebate();
			}
		}
		return s;
	}

	private static int parseOrderTotalFeeFen(NormalOrders val) {
		String tf = val.getTotalFee();
		if (!StringUtils.hasText(tf)) {
			return 0;
		}
		try {
			return new BigDecimal(tf.trim()).intValue();
		} catch (Exception e) {
			return 0;
		}
	}

	private static final class HfAccount {
		final boolean ok;
		final String isOpen;
		final int isSys;
		final String userCustId;
		final String acctId;
		final String sysUserCustId;
		final String sysAcctId;

		private HfAccount(
				boolean ok,
				String isOpen,
				int isSys,
				String userCustId,
				String acctId,
				String sysUserCustId,
				String sysAcctId) {
			this.ok = ok;
			this.isOpen = isOpen;
			this.isSys = isSys;
			this.userCustId = userCustId;
			this.acctId = acctId;
			this.sysUserCustId = sysUserCustId;
			this.sysAcctId = sysAcctId;
		}

		static HfAccount fail() {
			return new HfAccount(false, "false", 0, "", "", "", "");
		}

		static HfAccount ok(
				String isOpen,
				int isSys,
				String userCustId,
				String acctId,
				String sysUserCustId,
				String sysAcctId) {
			return new HfAccount(true, isOpen, isSys, userCustId, acctId, sysUserCustId, sysAcctId);
		}
	}
}
