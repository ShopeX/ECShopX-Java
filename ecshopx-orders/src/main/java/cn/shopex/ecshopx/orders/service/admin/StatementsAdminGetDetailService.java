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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.companys.mapper.DistributionDistributorSelfReadMapper;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.StatementDetails;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.StatementDetailsMapper;
import cn.shopex.ecshopx.orders.service.statement.export.StatementDetailsExportQuerySupport;
import cn.shopex.ecshopx.supplier.domain.Supplier;
import cn.shopex.ecshopx.supplier.mapper.SupplierMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class StatementsAdminGetDetailService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter EPOCH_FORMAT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final StatementDetailsMapper statementDetailsMapper;
	private final SupplierMapper supplierMapper;
	private final DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper;
	private final NormalOrdersMapper normalOrdersMapper;

	public StatementsAdminGetDetailService(
			StatementDetailsMapper statementDetailsMapper,
			SupplierMapper supplierMapper,
			DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper,
			NormalOrdersMapper normalOrdersMapper) {
		this.statementDetailsMapper = statementDetailsMapper;
		this.supplierMapper = supplierMapper;
		this.distributionDistributorSelfReadMapper = distributionDistributorSelfReadMapper;
		this.normalOrdersMapper = normalOrdersMapper;
	}

	public Map<String, Object> getDetail(
			long companyId,
			String statementId,
			String operatorType,
			Long distributorIdOrNull,
			Long merchantIdOrNull,
			long operatorId,
			String startTimeRaw,
			String endTimeRaw,
			String orderIdRaw,
			String pageRaw,
			String pageSizeRaw) {
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("statement_id", statementId);
		filter.put("company_id", companyId);

		if (filterParamLooksPresent(startTimeRaw)) {
			Integer st = parseEpochSecondsOrNull(startTimeRaw);
			if (st != null) {
				filter.put("created|gt", st);
			}
		}
		if (filterParamLooksPresent(endTimeRaw)) {
			Integer et = parseEpochSecondsOrNull(endTimeRaw);
			if (et != null) {
				filter.put("created|lt", et);
			}
		}
		if (filterParamLooksPresent(orderIdRaw)) {
			filter.put("order_id|contains", orderIdRaw.trim());
		}

		String op = operatorType == null ? "" : operatorType.trim();
		if ("distributor".equals(op)) {
			filter.put("distributor_id", distributorIdOrNull != null ? distributorIdOrNull : 0L);
		} else if ("merchant".equals(op)) {
			filter.put("merchant_id", merchantIdOrNull != null ? merchantIdOrNull : 0L);
		} else if ("supplier".equals(op)) {
			filter.put("supplier_id", resolveSupplierIdByOperator(companyId, operatorId));
		}

		int page = parsePageOneBased(pageRaw, 1);
		int pageSize = parsePageSize(pageSizeRaw, 20);

		LambdaQueryWrapper<StatementDetails> w = StatementDetailsExportQuerySupport.toWrapper(filter);

		long totalLong = statementDetailsMapper.selectCount(w);
		int totalCount = totalLong > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalLong;

		if (totalCount == 0) {
			return Map.of("total_count", totalCount, "list", List.of());
		}

		Page<StatementDetails> p = new Page<>(page, pageSize, false);
		Page<StatementDetails> pageResult = statementDetailsMapper.selectPage(p, w);
		List<StatementDetails> records = pageResult.getRecords();

		Set<Long> distributorIds = new LinkedHashSet<>();
		Set<Long> merchantIds = new LinkedHashSet<>();
		Set<Long> supplierIds = new LinkedHashSet<>();
		Set<Long> orderIds = new LinkedHashSet<>();
		for (StatementDetails d : records) {
			if (d.getDistributorId() != null) {
				distributorIds.add(d.getDistributorId());
			}
			if (d.getMerchantId() != null) {
				merchantIds.add(d.getMerchantId());
			}
			if (d.getSupplierId() != null && d.getSupplierId() > 0) {
				supplierIds.add(d.getSupplierId());
			}
			if (d.getOrderId() != null) {
				orderIds.add(d.getOrderId());
			}
		}

		Map<Long, String> distributorNameById = loadDistributorNames(companyId, distributorIds);
		Map<Long, String> merchantNameById = loadMerchantNames(companyId, merchantIds);
		Map<Long, String> supplierNameById = loadSupplierNames(supplierIds);

		List<Long> orderIdList = new ArrayList<>(orderIds);
		List<NormalOrders> orderRows =
				orderIdList.isEmpty()
						? List.of()
						: normalOrdersMapper.selectList(
								new LambdaQueryWrapper<NormalOrders>().in(NormalOrders::getOrderId, orderIdList));
		Map<Long, NormalOrders> orderById =
				orderRows.stream()
						.filter(o -> o.getOrderId() != null)
						.collect(Collectors.toMap(NormalOrders::getOrderId, Function.identity(), (a, b) -> a));

		Set<Long> orderDistributorIds = new LinkedHashSet<>();
		for (NormalOrders o : orderRows) {
			if (o.getDistributorId() != null) {
				orderDistributorIds.add(o.getDistributorId());
			}
		}
		Map<Long, String> distributorOrderNameById = loadDistributorNames(companyId, orderDistributorIds);
		if (distributorNameById.isEmpty()) {
			distributorNameById = new HashMap<>(distributorOrderNameById);
		}

		List<Map<String, Object>> listRows = new ArrayList<>();
		for (StatementDetails d : records) {
			Map<String, Object> row = statementDetailsToRow(d);
			Long oid = d.getOrderId();
			NormalOrders ord = oid == null ? null : orderById.get(oid);

			long effDistId =
					(d.getDistributorId() != null && d.getDistributorId() > 0)
							? d.getDistributorId()
							: (ord != null && ord.getDistributorId() != null ? ord.getDistributorId() : 0L);
			String distName = effDistId > 0 ? distributorNameById.getOrDefault(effDistId, "") : "";
			row.put("distributor_name", distName);

			row.put(
					"merchant_name",
					merchantNameById.getOrDefault(d.getMerchantId() == null ? 0L : d.getMerchantId(), ""));
			row.put(
					"supplier_name",
					supplierNameById.getOrDefault(d.getSupplierId() == null ? 0L : d.getSupplierId(), ""));

			if (ord != null && ord.getEndTime() != null && ord.getEndTime() > 0) {
				row.put("end_time", formatEpochSecondsShanghai(ord.getEndTime()));
			} else {
				row.put("end_time", Integer.valueOf(0));
			}

			listRows.add(row);
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", listRows);
		return out;
	}

	private static boolean filterParamLooksPresent(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof Boolean b) {
			return b;
		}
		if (o instanceof Number n) {
			return n.longValue() != 0L;
		}
		if (o instanceof String s) {
			String t = s.trim();
			return !t.isEmpty() && !"0".equals(t);
		}
		return true;
	}

	private static Integer parseEpochSecondsOrNull(Object raw) {
		try {
			String s = String.valueOf(raw).trim();
			long v = Long.parseLong(s);
			if (v > Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			}
			if (v < Integer.MIN_VALUE) {
				return Integer.MIN_VALUE;
			}
			return (int) v;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private long resolveSupplierIdByOperator(long companyId, long operatorId) {
		if (operatorId == 0L) {
			return 0L;
		}
		Supplier row =
				supplierMapper.selectOne(
						new LambdaQueryWrapper<Supplier>()
								.eq(Supplier::getCompanyId, companyId)
								.eq(Supplier::getOperatorId, operatorId)
								.last("LIMIT 1"));
		if (row == null || row.getId() == null) {
			return 0L;
		}
		return row.getId();
	}

	private static int parsePageOneBased(String raw, int defaultValue) {
		if (raw == null || raw.trim().isEmpty()) {
			return defaultValue;
		}
		try {
			int v = Integer.parseInt(raw.trim());
			return v < 1 ? 1 : v;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private static int parsePageSize(String raw, int defaultValue) {
		if (raw == null || raw.trim().isEmpty()) {
			return defaultValue;
		}
		try {
			int v = Integer.parseInt(raw.trim());
			return v < 1 ? defaultValue : v;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private static Map<String, Object> statementDetailsToRow(StatementDetails d) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("id", d.getId());
		row.put("company_id", d.getCompanyId());
		row.put("merchant_id", d.getMerchantId());
		row.put("supplier_id", d.getSupplierId());
		row.put("distributor_id", d.getDistributorId());
		row.put("statement_id", d.getStatementId());
		row.put("statement_no", d.getStatementNo());
		row.put("order_id", d.getOrderId());
		row.put("total_fee", d.getTotalFee());
		row.put("freight_fee", d.getFreightFee());
		row.put("intra_city_freight_fee", d.getIntraCityFreightFee());
		row.put("rebate_fee", d.getRebateFee());
		row.put("refund_fee", d.getRefundFee());
		row.put("statement_fee", d.getStatementFee());
		row.put("pay_type", d.getPayType());
		row.put("created", d.getCreated());
		row.put("updated", d.getUpdated());
		row.put("num", d.getNum());
		row.put("item_fee", d.getItemFee());
		row.put("commission_fee", d.getCommissionFee());
		row.put("cost_fee", d.getCostFee());
		row.put("point_fee", d.getPointFee());
		row.put("refund_num", d.getRefundNum());
		row.put("refund_point", d.getRefundPoint());
		row.put("refund_cost_fee", d.getRefundCostFee());
		return row;
	}

	private Map<Long, String> loadDistributorNames(long companyId, Set<Long> distributorIds) {
		Map<Long, String> out = new HashMap<>();
		if (distributorIds.isEmpty()) {
			return out;
		}
		List<Long> idList = new ArrayList<>(distributorIds);
		List<Map<String, Object>> rows =
				distributionDistributorSelfReadMapper.listDistributorNamesByCompanyAndIds(companyId, idList);
		for (Map<String, Object> dbRow : rows) {
			Long did = longFromCell(dbRow.get("distributor_id"));
			if (did != null) {
				Object nameObj = dbRow.get("name");
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
		for (Map<String, Object> dbRow : dbRows) {
			Long mid = longFromCell(dbRow.get("merchant_id"));
			if (mid != null) {
				Object nameObj = dbRow.get("merchant_name");
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
		for (Supplier s : list) {
			if (s.getId() != null) {
				out.put(s.getId(), s.getSupplierName() == null ? "" : s.getSupplierName());
			}
		}
		return out;
	}

	private static String formatEpochSecondsShanghai(long epochSeconds) {
		return ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), SHANGHAI).format(EPOCH_FORMAT);
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
