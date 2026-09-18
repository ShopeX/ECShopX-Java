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
import cn.shopex.ecshopx.orders.domain.Statements;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class StatementsSummarizedCsvExportService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
	private static final int PAGE_SIZE = 500;

	private final StatementsMapper statementsMapper;
	private final DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper;
	private final SupplierMapper supplierMapper;
	private final ExportCsvFileService exportCsvFileService;

	public StatementsSummarizedCsvExportService(
			StatementsMapper statementsMapper,
			DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper,
			SupplierMapper supplierMapper,
			ExportCsvFileService exportCsvFileService) {
		this.statementsMapper = statementsMapper;
		this.distributionDistributorSelfReadMapper = distributionDistributorSelfReadMapper;
		this.supplierMapper = supplierMapper;
		this.exportCsvFileService = exportCsvFileService;
	}

	public Optional<Map<String, String>> runExport(LinkedHashMap<String, Object> filter, long operatorId) {
		long companyId = readCompanyId(filter);
		LambdaQueryWrapper<Statements> baseWrapper = StatementsSummarizedExportQuerySupport.toWrapper(filter);
		long total = statementsMapper.selectCount(baseWrapper);
		if (total == 0) {
			return Optional.empty();
		}

		Object mtObj = filter.get("merchant_type");
		boolean supplierExport = "supplier".equals(mtObj == null ? "" : String.valueOf(mtObj).trim());

		LinkedHashMap<String, String> title = supplierExport ? buildSupplierTitle() : buildDefaultTitle();

		LambdaQueryWrapper<Statements> pageWrapper = StatementsSummarizedExportQuerySupport.toWrapper(filter);
		pageWrapper.orderByDesc(Statements::getCreated);

		List<Map<String, String>> rows = new ArrayList<>();
		int totalPages = (int) Math.ceil(total / (double) PAGE_SIZE);
		for (int page = 1; page <= totalPages; page++) {
			Page<Statements> p = new Page<>(page, PAGE_SIZE, false);
			Page<Statements> result = statementsMapper.selectPage(p, pageWrapper);
			List<Statements> records = result.getRecords();
			if (records.isEmpty()) {
				continue;
			}

			if (supplierExport) {
				Set<Long> supplierIds = new LinkedHashSet<>();
				for (Statements s : records) {
					if (s.getSupplierId() != null) {
						supplierIds.add(s.getSupplierId());
					}
				}
				Map<Long, String> supplierNameById = loadSupplierNames(supplierIds);
				for (Statements s : records) {
					rows.add(toSupplierRow(s, title, supplierNameById));
				}
			} else {
				Set<Long> distributorIds = new LinkedHashSet<>();
				Set<Long> merchantIds = new LinkedHashSet<>();
				Set<Long> supplierIds = new LinkedHashSet<>();
				for (Statements s : records) {
					if (s.getDistributorId() != null) {
						distributorIds.add(s.getDistributorId());
					}
					if (s.getMerchantId() != null) {
						merchantIds.add(s.getMerchantId());
					}
					if (s.getSupplierId() != null && s.getSupplierId() > 0) {
						supplierIds.add(s.getSupplierId());
					}
				}
				Map<Long, String> distributorNameById = loadDistributorNames(companyId, distributorIds);
				Map<Long, String> merchantNameById = loadMerchantNames(companyId, merchantIds);
				Map<Long, String> supplierNameById = loadSupplierNames(supplierIds);
				for (Statements s : records) {
					rows.add(toDefaultRow(s, title, distributorNameById, merchantNameById, supplierNameById));
				}
			}
		}

		String ts = ZonedDateTime.now(SHANGHAI).format(FILE_TS);
		String suffix = supplierExport ? "supplier_statements" : "statements";
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
		title.put("statement_no", "结算单号");
		title.put("merchant_name", "商家");
		title.put("distributor_name", "店铺");
		title.put("order_num", "订单数量");
		title.put("total_fee", "订单实付");
		title.put("freight_fee", "运费");
		title.put("intra_city_freight_fee", "同城配");
		title.put("refund_fee", "退款金额");
		title.put("statement_fee", "结算金额");
		title.put("statement_period", "结算周期");
		title.put("confirm_time", "确认时间");
		title.put("statement_time", "结算时间");
		title.put("statement_status", "结算状态");
		return title;
	}

	private static LinkedHashMap<String, String> buildSupplierTitle() {
		LinkedHashMap<String, String> title = new LinkedHashMap<>();
		title.put("statement_no", "结算单号");
		title.put("supplier_name", "供应商");
		title.put("order_num", "订单数量");
		title.put("total_total_fee", "订单实付总金额(￥)");
		title.put("total_fee", "现金实付（￥）");
		title.put("point_fee", "积分抵扣");
		title.put("freight_fee", "运费（总）");
		title.put("refund_num", "退货数量");
		title.put("refund_fee", "退款金额");
		title.put("refund_point", "退款积分");
		title.put("refund_cost_fee", "退货成本");
		title.put("statement_fee", "结算金额（￥）");
		title.put("statement_period", "结算周期");
		title.put("confirm_time", "确认时间");
		title.put("statement_time", "结算时间");
		title.put("statement_status", "结算状态");
		return title;
	}

	private Map<String, String> toDefaultRow(
			Statements s,
			LinkedHashMap<String, String> title,
			Map<Long, String> distributorNameById,
			Map<Long, String> merchantNameById,
			Map<Long, String> supplierNameById) {
		Map<String, String> row = new LinkedHashMap<>();
		for (String k : title.keySet()) {
			row.put(k, cellDefaultExport(k, s, distributorNameById, merchantNameById, supplierNameById));
		}
		return row;
	}

	private String cellDefaultExport(
			String k,
			Statements s,
			Map<Long, String> distributorNameById,
			Map<Long, String> merchantNameById,
			Map<Long, String> supplierNameById) {
		return switch (k) {
			case "statement_no" -> nullToEmpty(s.getStatementNo());
			case "total_fee",
					"freight_fee",
					"intra_city_freight_fee",
					"refund_fee",
					"statement_fee" -> centsToYuan(fieldIntByName(s, k));
			case "merchant_name" -> {
				boolean supTruth = s.getSupplierId() != null && s.getSupplierId() != 0L;
				if (supTruth) {
					yield dashOrName(supplierNameById, s.getSupplierId());
				}
				yield dashOrName(merchantNameById, s.getMerchantId());
			}
			case "distributor_name" -> dashOrName(distributorNameById, s.getDistributorId());
			case "statement_period" -> formatStatementPeriod(s.getStartTime(), s.getEndTime());
			case "confirm_time" -> formatConfirmOrStatementTime(s, true);
			case "statement_time" -> formatConfirmOrStatementTime(s, false);
			case "statement_status" -> statementStatusLabel(s.getStatementStatus());
			case "order_num" -> intOrRaw(s.getOrderNum());
			default -> defaultEntityString(s, k);
		};
	}

	private Map<String, String> toSupplierRow(
			Statements s, LinkedHashMap<String, String> title, Map<Long, String> supplierNameById) {
		Map<String, String> row = new LinkedHashMap<>();
		for (String k : title.keySet()) {
			row.put(k, cellSupplierExport(k, s, supplierNameById));
		}
		return row;
	}

	private String cellSupplierExport(String k, Statements s, Map<Long, String> supplierNameById) {
		return switch (k) {
			case "statement_no" -> nullToEmpty(s.getStatementNo());
			case "supplier_name" -> dashOrName(supplierNameById, s.getSupplierId());
			case "order_num" -> intOrZero(s.getOrderNum());
			case "total_total_fee" -> {
				int tf = s.getTotalFee() == null ? 0 : s.getTotalFee();
				int pf = s.getPointFee() == null ? 0 : s.getPointFee();
				yield centsToYuan(tf + pf);
			}
			case "total_fee", "point_fee", "freight_fee" -> centsToYuan(fieldIntByName(s, k));
			case "refund_num" -> intOrZero(s.getRefundNum());
			case "refund_fee" -> centsToYuan(s.getRefundFee());
			case "refund_point" -> centsToYuan(s.getRefundPoint());
			case "refund_cost_fee" -> centsToYuan(s.getRefundCostFee());
			case "statement_fee" -> centsToYuan(s.getStatementFee());
			case "statement_period" -> formatStatementPeriod(s.getStartTime(), s.getEndTime());
			case "confirm_time" -> formatConfirmOrStatementTime(s, true);
			case "statement_time" -> formatConfirmOrStatementTime(s, false);
			case "statement_status" -> statementStatusLabel(s.getStatementStatus());
			default -> supplierDefaultField(s, k);
		};
	}

	private static String supplierDefaultField(Statements s, String k) {
		Object v = fieldValue(s, k);
		if (v == null) {
			return "-";
		}
		return String.valueOf(v);
	}

	private static Object fieldValue(Statements s, String k) {
		return switch (k) {
			case "order_num" -> s.getOrderNum();
			case "total_fee" -> s.getTotalFee();
			case "point_fee" -> s.getPointFee();
			case "freight_fee" -> s.getFreightFee();
			case "refund_num" -> s.getRefundNum();
			case "refund_fee" -> s.getRefundFee();
			case "refund_point" -> s.getRefundPoint();
			case "refund_cost_fee" -> s.getRefundCostFee();
			case "statement_fee" -> s.getStatementFee();
			case "statement_no" -> s.getStatementNo();
			case "supplier_name" -> null;
			default -> null;
		};
	}

	private static Integer fieldIntByName(Statements s, String k) {
		return switch (k) {
			case "total_fee" -> s.getTotalFee();
			case "freight_fee" -> s.getFreightFee();
			case "intra_city_freight_fee" -> s.getIntraCityFreightFee();
			case "refund_fee" -> s.getRefundFee();
			case "statement_fee" -> s.getStatementFee();
			default -> null;
		};
	}

	private static String defaultEntityString(Statements s, String k) {
		Object v = fieldValue(s, k);
		if (v == null) {
			return "";
		}
		return String.valueOf(v);
	}

	private static boolean statementTimeTruthy(Integer st) {
		return st != null && st != 0;
	}

	private String formatConfirmOrStatementTime(Statements s, boolean confirmColumn) {
		if (!statementTimeTruthy(s.getStatementTime())) {
			return "-";
		}
		Integer epoch = confirmColumn ? s.getConfirmTime() : s.getStatementTime();
		return formatEpochSeconds(epoch);
	}

	private static String formatStatementPeriod(Integer start, Integer end) {
		String a = formatEpochSeconds(start);
		String b = formatEpochSeconds(end);
		return a + "~" + b;
	}

	private static String formatEpochSeconds(Integer epoch) {
		if (epoch == null || epoch <= 0) {
			return "";
		}
		return ZonedDateTime.ofInstant(Instant.ofEpochSecond(epoch.longValue()), SHANGHAI).format(CSV_TIME);
	}

	private static String statementStatusLabel(String raw) {
		if ("done".equals(raw)) {
			return "已结算";
		}
		if ("confirmed".equals(raw)) {
			return "待平台结算";
		}
		return "待商家确认";
	}

	private static String dashOrName(Map<Long, String> m, Long id) {
		if (id == null) {
			return "-";
		}
		String n = m.get(id);
		return StringUtils.hasText(n) ? n : "-";
	}

	private Map<Long, String> loadDistributorNames(long companyId, Set<Long> distributorIds) {
		Map<Long, String> out = new HashMap<>();
		if (distributorIds.isEmpty()) {
			return out;
		}
		List<Long> idList = new ArrayList<>(distributorIds);
		List<Map<String, Object>> dbRows =
				distributionDistributorSelfReadMapper.listDistributorNamesByCompanyAndIds(companyId, idList);
		for (Map<String, Object> row : dbRows) {
			Long did = longFromCell(row.get("distributor_id"));
			if (did != null) {
				Object nameObj = row.get("name");
				out.put(did, nameObj == null ? "" : String.valueOf(nameObj));
			}
		}
		return out;
	}

	private Map<Long, String> loadMerchantNames(long companyId, Set<Long> merchantIds) {
		Map<Long, String> out = new HashMap<>();
		if (merchantIds.isEmpty()) {
			return out;
		}
		List<Long> idList = new ArrayList<>(merchantIds);
		List<Map<String, Object>> dbRows =
				distributionDistributorSelfReadMapper.listMerchantNamesByCompanyAndIds(companyId, idList);
		for (Map<String, Object> row : dbRows) {
			Long mid = longFromCell(row.get("merchant_id"));
			if (mid != null) {
				Object nameObj = row.get("merchant_name");
				out.put(mid, nameObj == null ? "" : String.valueOf(nameObj));
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
		for (Supplier sup : list) {
			if (sup.getId() != null) {
				out.put(sup.getId(), sup.getSupplierName() == null ? "" : sup.getSupplierName());
			}
		}
		return out;
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	private static String intOrRaw(Integer v) {
		if (v == null) {
			return "";
		}
		return String.valueOf(v);
	}

	private static String intOrZero(Integer v) {
		if (v == null) {
			return "0";
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

	private String centsToYuan(int cents) {
		return BigDecimal.valueOf(cents)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
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
