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

package cn.shopex.ecshopx.orders.service.statement.export;

import cn.shopex.ecshopx.companys.mapper.DistributionDistributorSelfReadMapper;
import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.StatementDetails;
import cn.shopex.ecshopx.orders.domain.Statements;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.StatementDetailsMapper;
import cn.shopex.ecshopx.orders.mapper.StatementsMapper;
import cn.shopex.ecshopx.supplier.domain.Supplier;
import cn.shopex.ecshopx.supplier.mapper.SupplierMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class StatementDetailsCsvExportService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
	private static final int PAGE_SIZE = 500;

	private static final Map<String, String> PAY_TYPE_LABELS = new LinkedHashMap<>();

	static {
		PAY_TYPE_LABELS.put("wxpay", "微信支付");
		PAY_TYPE_LABELS.put("wxpayh5", "微信支付");
		PAY_TYPE_LABELS.put("wxpayjsapi", "微信支付");
		PAY_TYPE_LABELS.put("wxpayapp", "微信支付");
		PAY_TYPE_LABELS.put("wxpaymini", "微信支付");
		PAY_TYPE_LABELS.put("wxpaypos", "微信支付");
		PAY_TYPE_LABELS.put("alipay", "支付宝");
		PAY_TYPE_LABELS.put("alipayapp", "支付宝");
		PAY_TYPE_LABELS.put("alipaymini", "支付宝");
		PAY_TYPE_LABELS.put("alipaypos", "支付宝");
		PAY_TYPE_LABELS.put("alipayh5", "支付宝");
		PAY_TYPE_LABELS.put("offline_pay", "线下支付");
		PAY_TYPE_LABELS.put("point", "积分支付");
		PAY_TYPE_LABELS.put("deposit", "预存款");
		PAY_TYPE_LABELS.put("bspay", "汇付支付");
		PAY_TYPE_LABELS.put("adapay", "汇付支付");
		PAY_TYPE_LABELS.put("paypal", "PayPal");
		PAY_TYPE_LABELS.put("chinaums", "银联商务");
	}

	private final StatementDetailsMapper statementDetailsMapper;
	private final DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper;
	private final SupplierMapper supplierMapper;
	private final StatementsMapper statementsMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final ExportCsvFileService exportCsvFileService;

	public StatementDetailsCsvExportService(
			StatementDetailsMapper statementDetailsMapper,
			DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper,
			SupplierMapper supplierMapper,
			StatementsMapper statementsMapper,
			NormalOrdersMapper normalOrdersMapper,
			ExportCsvFileService exportCsvFileService) {
		this.statementDetailsMapper = statementDetailsMapper;
		this.distributionDistributorSelfReadMapper = distributionDistributorSelfReadMapper;
		this.supplierMapper = supplierMapper;
		this.statementsMapper = statementsMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.exportCsvFileService = exportCsvFileService;
	}

	public Optional<Map<String, String>> runExport(LinkedHashMap<String, Object> filter, long operatorId) {
		long companyId = readCompanyId(filter);
		LambdaQueryWrapper<StatementDetails> baseWrapper = StatementDetailsExportQuerySupport.toWrapper(filter);
		long total = statementDetailsMapper.selectCount(baseWrapper);
		if (total == 0) {
			return Optional.empty();
		}

		Object mtObj = filter.get("merchant_type");
		String merchantType = mtObj == null ? "" : String.valueOf(mtObj).trim();
		boolean supplierExport = "supplier".equals(merchantType);

		LinkedHashMap<String, String> title = supplierExport ? buildSupplierTitle() : buildDefaultTitle();

		LambdaQueryWrapper<StatementDetails> pageWrapper = StatementDetailsExportQuerySupport.toWrapper(filter);
		pageWrapper.orderByDesc(StatementDetails::getCreated);

		List<Map<String, String>> rows = new ArrayList<>();
		int totalPages = (int) Math.ceil(total / (double) PAGE_SIZE);
		for (int page = 1; page <= totalPages; page++) {
			Page<StatementDetails> p = new Page<>(page, PAGE_SIZE, false);
			Page<StatementDetails> result = statementDetailsMapper.selectPage(p, pageWrapper);
			List<StatementDetails> records = result.getRecords();
			if (records.isEmpty()) {
				continue;
			}

			Set<Long> distributorIds = new LinkedHashSet<>();
			Set<Long> supplierIds = new LinkedHashSet<>();
			Set<Long> orderIds = new LinkedHashSet<>();
			Set<Long> statementIds = new LinkedHashSet<>();
			for (StatementDetails d : records) {
				if (d.getDistributorId() != null) {
					distributorIds.add(d.getDistributorId());
				}
				if (d.getSupplierId() != null && d.getSupplierId() > 0) {
					supplierIds.add(d.getSupplierId());
				}
				if (d.getOrderId() != null) {
					orderIds.add(d.getOrderId());
				}
				if (d.getStatementId() != null) {
					statementIds.add(d.getStatementId());
				}
			}

			Map<Long, String> distributorNameById = loadDistributorNames(companyId, distributorIds);
			Map<Long, String> supplierNameById = loadSupplierNames(supplierIds);
			Map<Long, Integer> statementTimeById =
					supplierExport ? loadStatementTimes(companyId, statementIds) : Map.of();
			Map<Long, Long> orderEndTimeById =
					supplierExport ? loadOrderEndTimes(companyId, orderIds) : Map.of();

			for (StatementDetails d : records) {
				if (supplierExport) {
					rows.add(
							toSupplierRow(
									d,
									distributorNameById,
									supplierNameById,
									statementTimeById,
									orderEndTimeById));
				} else {
					rows.add(toDefaultRow(d, distributorNameById, supplierNameById));
				}
			}
		}

		String ts = ZonedDateTime.now(SHANGHAI).format(FILE_TS);
		String suffix = supplierExport ? "supplier_statement_details" : "statement_details";
		String fileBaseName = ts + companyId + suffix;

		Map<String, String> upload = exportCsvFileService.exportCsv(fileBaseName, title, rows);
		if (upload == null || upload.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(upload);
	}

	private static long readCompanyId(LinkedHashMap<String, Object> filter) {
		Object v = filter.get("company_id");
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}

	private static LinkedHashMap<String, String> buildDefaultTitle() {
		LinkedHashMap<String, String> title = new LinkedHashMap<>();
		title.put("order_id", "订单号");
		title.put("statement_no", "结算单号");
		title.put("distributor_name", "店铺名称");
		title.put("supplier_name", "供应商名称");
		title.put("num", "购买数量");
		title.put("total_fee", "实付金额");
		title.put("freight_fee", "运费");
		title.put("intra_city_freight_fee", "同城配");
		title.put("rebate_fee", "分销佣金");
		title.put("refund_fee", "退款金额");
		title.put("statement_fee", "结算金额");
		title.put("pay_type", "支付方式");
		title.put("item_fee", "销售总金额");
		title.put("commission_fee", "佣金金额");
		title.put("cost_fee", "成本结算");
		title.put("point_fee", "积分抵扣");
		title.put("refund_num", "退货数量");
		title.put("refund_point", "退款积分");
		title.put("refund_cost_fee", "退货成本");
		title.put("created", "创建时间");
		return title;
	}

	private static LinkedHashMap<String, String> buildSupplierTitle() {
		LinkedHashMap<String, String> title = new LinkedHashMap<>();
		title.put("order_id", "订单号");
		title.put("statement_no", "结算单号");
		title.put("statement_time", "结算时间");
		title.put("end_time", "订单完成时间");
		title.put("distributor_name", "店铺名称");
		title.put("supplier_name", "供应商名称");
		title.put("num", "购买数量");
		title.put("total_fee", "实付金额");
		title.put("freight_fee", "运费");
		title.put("intra_city_freight_fee", "同城配");
		title.put("rebate_fee", "分销佣金");
		title.put("refund_fee", "退款金额");
		title.put("statement_fee", "结算金额");
		title.put("pay_type", "支付方式");
		title.put("item_fee", "销售总金额");
		title.put("commission_fee", "佣金金额");
		title.put("cost_fee", "成本结算");
		title.put("point_fee", "积分抵扣");
		title.put("refund_num", "退货数量");
		title.put("refund_point", "退款积分");
		title.put("refund_cost_fee", "退货成本");
		title.put("created", "创建时间");
		return title;
	}

	private Map<Long, String> loadDistributorNames(long companyId, Set<Long> distributorIds) {
		Map<Long, String> out = new HashMap<>();
		if (distributorIds.isEmpty()) {
			return out;
		}
		List<Long> idList = new ArrayList<>(distributorIds);
		List<Map<String, Object>> rows =
				distributionDistributorSelfReadMapper.listDistributorNamesByCompanyAndIds(companyId, idList);
		for (Map<String, Object> row : rows) {
			Long did = longFromCell(row.get("distributor_id"));
			if (did != null) {
				Object nameObj = row.get("name");
				out.put(did, nameObj == null ? "" : String.valueOf(nameObj));
			}
		}
		return out;
	}

	private Map<Long, String> loadSupplierNames(Set<Long> supplierIds) {
		Map<Long, String> out = new HashMap<>();
		if (supplierIds.isEmpty()) {
			return out;
		}
		List<Supplier> list =
				supplierMapper.selectList(new LambdaQueryWrapper<Supplier>().in(Supplier::getId, supplierIds));
		for (Supplier s : list) {
			if (s.getId() != null) {
				out.put(s.getId(), s.getSupplierName() == null ? "" : s.getSupplierName());
			}
		}
		return out;
	}

	private Map<Long, Integer> loadStatementTimes(long companyId, Set<Long> statementIds) {
		Map<Long, Integer> out = new HashMap<>();
		if (statementIds.isEmpty()) {
			return out;
		}
		List<Statements> list =
				statementsMapper.selectList(
						new LambdaQueryWrapper<Statements>()
								.eq(Statements::getCompanyId, companyId)
								.in(Statements::getId, statementIds));
		for (Statements s : list) {
			if (s.getId() != null) {
				out.put(s.getId(), s.getStatementTime());
			}
		}
		return out;
	}

	private Map<Long, Long> loadOrderEndTimes(long companyId, Set<Long> orderIds) {
		Map<Long, Long> out = new HashMap<>();
		if (orderIds.isEmpty()) {
			return out;
		}
		List<NormalOrders> list =
				normalOrdersMapper.selectList(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.in(NormalOrders::getOrderId, orderIds));
		for (NormalOrders o : list) {
			if (o.getOrderId() != null) {
				out.put(o.getOrderId(), o.getEndTime());
			}
		}
		return out;
	}

	private Map<String, String> toDefaultRow(
			StatementDetails d,
			Map<Long, String> distributorNameById,
			Map<Long, String> supplierNameById) {
		Map<String, String> row = new LinkedHashMap<>();
		row.put("order_id", formatOrderId(d.getOrderId()));
		row.put("statement_no", nullToEmpty(d.getStatementNo()));
		row.put("distributor_name", nameOrEmpty(distributorNameById, d.getDistributorId()));
		row.put("supplier_name", nameOrEmpty(supplierNameById, d.getSupplierId()));
		row.put("num", intOrEmpty(d.getNum()));
		row.put("total_fee", centsToYuan(d.getTotalFee()));
		row.put("freight_fee", centsToYuan(d.getFreightFee()));
		row.put("intra_city_freight_fee", centsToYuan(d.getIntraCityFreightFee()));
		row.put("rebate_fee", centsToYuan(d.getRebateFee()));
		row.put("refund_fee", centsToYuan(d.getRefundFee()));
		row.put("statement_fee", centsToYuan(d.getStatementFee()));
		row.put("pay_type", payTypeLabel(d.getPayType()));
		row.put("item_fee", centsToYuan(d.getItemFee()));
		row.put("commission_fee", centsToYuan(d.getCommissionFee()));
		row.put("cost_fee", centsToYuan(d.getCostFee()));
		row.put("point_fee", centsToYuan(d.getPointFee()));
		row.put("refund_num", intOrEmpty(d.getRefundNum()));
		row.put("refund_point", intOrEmpty(d.getRefundPoint()));
		row.put("refund_cost_fee", centsToYuan(d.getRefundCostFee()));
		row.put("created", formatEpochSeconds(d.getCreated()));
		return row;
	}

	private Map<String, String> toSupplierRow(
			StatementDetails d,
			Map<Long, String> distributorNameById,
			Map<Long, String> supplierNameById,
			Map<Long, Integer> statementTimeById,
			Map<Long, Long> orderEndTimeById) {
		Map<String, String> row = new LinkedHashMap<>();
		row.put("order_id", formatOrderId(d.getOrderId()));
		row.put("statement_no", nullToEmpty(d.getStatementNo()));
		Integer st = d.getStatementId() == null ? null : statementTimeById.get(d.getStatementId());
		row.put("statement_time", formatEpochSeconds(st));
		Long end = d.getOrderId() == null ? null : orderEndTimeById.get(d.getOrderId());
		row.put("end_time", formatEndTime(end));
		row.put("distributor_name", nameOrEmpty(distributorNameById, d.getDistributorId()));
		row.put("supplier_name", nameOrEmpty(supplierNameById, d.getSupplierId()));
		row.put("num", intOrEmpty(d.getNum()));
		row.put("total_fee", centsToYuan(d.getTotalFee()));
		row.put("freight_fee", centsToYuan(d.getFreightFee()));
		row.put("intra_city_freight_fee", centsToYuan(d.getIntraCityFreightFee()));
		row.put("rebate_fee", centsToYuan(d.getRebateFee()));
		row.put("refund_fee", centsToYuan(d.getRefundFee()));
		row.put("statement_fee", centsToYuan(d.getStatementFee()));
		row.put("pay_type", payTypeLabel(d.getPayType()));
		row.put("item_fee", centsToYuan(d.getItemFee()));
		row.put("commission_fee", centsToYuan(d.getCommissionFee()));
		row.put("cost_fee", centsToYuan(d.getCostFee()));
		row.put("point_fee", centsToYuan(d.getPointFee()));
		row.put("refund_num", intOrEmpty(d.getRefundNum()));
		row.put("refund_point", intOrEmpty(d.getRefundPoint()));
		row.put("refund_cost_fee", centsToYuan(d.getRefundCostFee()));
		row.put("created", formatEpochSeconds(d.getCreated()));
		return row;
	}

	private static String formatOrderId(Long orderId) {
		if (orderId == null) {
			return "\t";
		}
		return orderId + "\t";
	}

	private static String nameOrEmpty(Map<Long, String> m, Long id) {
		if (id == null) {
			return "";
		}
		return m.getOrDefault(id, "");
	}

	private static String intOrEmpty(Integer v) {
		if (v == null) {
			return "";
		}
		return String.valueOf(v);
	}

	private String centsToYuan(Integer cents) {
		if (cents == null) {
			return "0.00";
		}
		return BigDecimal.valueOf(cents)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	private static String formatEpochSeconds(Integer epoch) {
		if (epoch == null || epoch <= 0) {
			return "";
		}
		return ZonedDateTime.ofInstant(Instant.ofEpochSecond(epoch.longValue()), SHANGHAI).format(CSV_TIME);
	}

	private static String formatEndTime(Long epoch) {
		if (epoch == null || epoch <= 0) {
			return "";
		}
		return ZonedDateTime.ofInstant(Instant.ofEpochSecond(epoch.longValue()), SHANGHAI).format(CSV_TIME);
	}

	private static String payTypeLabel(String code) {
		if (!StringUtils.hasText(code)) {
			return "";
		}
		String c = code.trim();
		String lc = c.toLowerCase(Locale.ROOT);
		return PAY_TYPE_LABELS.getOrDefault(lc, c);
	}

	private static Long longFromCell(Object cell) {
		if (cell == null) {
			return null;
		}
		if (cell instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(cell).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
