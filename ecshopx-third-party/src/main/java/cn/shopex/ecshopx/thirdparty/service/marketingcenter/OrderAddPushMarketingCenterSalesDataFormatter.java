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

package cn.shopex.ecshopx.thirdparty.service.marketingcenter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Resolves salesperson / distributor shop linkage for normal-order-add marketing payloads. Empty result
 * means the outbound marketing call must be skipped (no valid shop binding for the guide).
 */
@Component
public class OrderAddPushMarketingCenterSalesDataFormatter {

	private final JdbcTemplate jdbcTemplate;

	public OrderAddPushMarketingCenterSalesDataFormatter(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<Map<String, Object>> format(long companyId, Map<String, Object> draftInput) {
		if (draftInput == null || draftInput.isEmpty()) {
			return Optional.empty();
		}
		long salesmanId = longVal(draftInput.get("salesman_id"));
		if (salesmanId == 0L) {
			return Optional.empty();
		}
		Map<String, Object> orderRow = extractOrderRow(draftInput);
		LinkedHashMap<String, Object> input = new LinkedHashMap<>(draftInput);
		return mergeSalesFormatting(companyId, orderRow, input);
	}

	private static Map<String, Object> extractOrderRow(Map<String, Object> draft) {
		Map<String, Object> orderRow = new LinkedHashMap<>();
		for (String k : List.of(
				"order_id",
				"company_id",
				"salesman_id",
				"bind_salesman_id",
				"chat_id",
				"sale_salesman_distributor_id",
				"bind_salesman_distributor_id")) {
			orderRow.put(k, draft.get(k));
		}
		return orderRow;
	}

	private Optional<Map<String, Object>> mergeSalesFormatting(
			long companyId, Map<String, Object> orderRow, LinkedHashMap<String, Object> input) {
		long salesmanId = longVal(orderRow.get("salesman_id"));
		long bindSalesmanId = longVal(orderRow.get("bind_salesman_id"));
		long orderPk = longVal(orderRow.get("order_id"));

		Set<Long> guideIds = new LinkedHashSet<>();
		guideIds.add(salesmanId);
		if (bindSalesmanId != 0L) {
			guideIds.add(bindSalesmanId);
		}
		List<Long> guideIdList = new ArrayList<>(guideIds);
		Map<Long, String> salespersonToWorkUser = querySalespersonWorkUserids(companyId, guideIdList);
		input.put(
				"sale_salesperson_id",
				orZeroString(salespersonToWorkUser.get(salesmanId)));
		input.put(
				"bind_salesperson_id",
				orZeroString(salespersonToWorkUser.get(bindSalesmanId)));

		Map<Long, List<Long>> relBySalesperson =
				queryDistributorShopIdsBySalesperson(companyId, guideIdList);
		List<Long> saleRel = relBySalesperson.get(salesmanId);
		if (saleRel == null || saleRel.isEmpty()) {
			return Optional.empty();
		}

		MutableLong saleDist = mutableLong(orderRow.get("sale_salesman_distributor_id"));
		MutableLong bindDist = mutableLong(orderRow.get("bind_salesman_distributor_id"));
		Map<String, Long> pendingUpdate = new LinkedHashMap<>();

		if (!containsLong(saleRel, saleDist.value)) {
			long firstShop = saleRel.get(0);
			pendingUpdate.put("sale_salesman_distributor_id", firstShop);
			saleDist.value = firstShop;
		}
		List<Long> bindRel = relBySalesperson.get(bindSalesmanId);
		if (bindRel != null
				&& !bindRel.isEmpty()
				&& !containsLong(bindRel, bindDist.value)) {
			long firstBindShop = bindRel.get(0);
			pendingUpdate.put("bind_salesman_distributor_id", firstBindShop);
			bindDist.value = firstBindShop;
		}

		List<Long> selDistIds = new ArrayList<>(2);
		if (saleDist.value != 0L) {
			selDistIds.add(saleDist.value);
		}
		if (bindDist.value != 0L) {
			selDistIds.add(bindDist.value);
		}
		Map<Long, String> distToShopCode = queryDistributorShopCodes(companyId, selDistIds);
		input.put("sale_store_bn", distToShopCode.getOrDefault(saleDist.value, ""));
		input.put("bind_store_bn", distToShopCode.getOrDefault(bindDist.value, ""));

		if (pendingUpdate.isEmpty()) {
			return Optional.of(input);
		}
		if (applyOrderDistributorPatch(companyId, orderPk, pendingUpdate)) {
			return Optional.of(input);
		}
		return Optional.empty();
	}

	private Map<Long, String> querySalespersonWorkUserids(long companyId, List<Long> salespersonIds) {
		if (salespersonIds.isEmpty()) {
			return Map.of();
		}
		String placeholders = salespersonIds.stream().map(id -> "?").collect(Collectors.joining(","));
		String sql =
				"SELECT salesperson_id, work_userid FROM shop_salesperson WHERE company_id = ? AND salesperson_id IN ("
						+ placeholders
						+ ")";
		Object[] args = new Object[salespersonIds.size() + 1];
		args[0] = companyId;
		for (int i = 0; i < salespersonIds.size(); i++) {
			args[i + 1] = salespersonIds.get(i);
		}
		return jdbcTemplate.query(
				sql,
				rs -> {
					Map<Long, String> map = new LinkedHashMap<>();
					while (rs.next()) {
						map.put(rs.getLong("salesperson_id"), str(rs.getObject("work_userid")));
					}
					return map;
				},
				args);
	}

	private Map<Long, List<Long>> queryDistributorShopIdsBySalesperson(long companyId, List<Long> salespersonIds) {
		if (salespersonIds.isEmpty()) {
			return Map.of();
		}
		String placeholders = salespersonIds.stream().map(id -> "?").collect(Collectors.joining(","));
		String sql =
				"SELECT shop_id, salesperson_id FROM shop_rel_salesperson WHERE company_id = ?"
						+ " AND store_type = 'distributor' AND salesperson_id IN ("
						+ placeholders
						+ ")";
		Object[] args = new Object[salespersonIds.size() + 1];
		args[0] = companyId;
		for (int i = 0; i < salespersonIds.size(); i++) {
			args[i + 1] = salespersonIds.get(i);
		}
		return jdbcTemplate.query(
				sql,
				rs -> {
					Map<Long, List<Long>> map = new LinkedHashMap<>();
					while (rs.next()) {
						long sid = rs.getLong("salesperson_id");
						long shopId = rs.getLong("shop_id");
						map.computeIfAbsent(sid, k -> new ArrayList<>()).add(shopId);
					}
					return map;
				},
				args);
	}

	private Map<Long, String> queryDistributorShopCodes(long companyId, List<Long> distributorIds) {
		distributorIds = distributorIds.stream().filter(id -> id != 0L).distinct().toList();
		if (distributorIds.isEmpty()) {
			return Map.of();
		}
		String placeholders = distributorIds.stream().map(id -> "?").collect(Collectors.joining(","));
		String sql =
				"SELECT distributor_id, shop_code FROM distribution_distributor WHERE company_id = ? AND distributor_id IN ("
						+ placeholders
						+ ")";
		Object[] args = new Object[distributorIds.size() + 1];
		args[0] = companyId;
		for (int i = 0; i < distributorIds.size(); i++) {
			args[i + 1] = distributorIds.get(i);
		}
		return jdbcTemplate.query(
				sql,
				rs -> {
					Map<Long, String> map = new LinkedHashMap<>();
					while (rs.next()) {
						map.put(rs.getLong("distributor_id"), str(rs.getObject("shop_code")));
					}
					return map;
				},
				args);
	}

	private boolean applyOrderDistributorPatch(long companyId, long orderId, Map<String, Long> updates) {
		StringBuilder sb = new StringBuilder("UPDATE orders_normal_orders SET ");
		List<Object> vals = new ArrayList<>();
		boolean first = true;
		for (Map.Entry<String, Long> e : updates.entrySet()) {
			if (!first) {
				sb.append(", ");
			}
			first = false;
			sb.append(e.getKey()).append(" = ?");
			vals.add(e.getValue());
		}
		sb.append(" WHERE company_id = ? AND order_id = ?");
		vals.add(companyId);
		vals.add(orderId);
		return jdbcTemplate.update(sb.toString(), vals.toArray()) > 0;
	}

	private static boolean containsLong(List<Long> list, long v) {
		for (Long x : list) {
			if (x != null && x.longValue() == v) {
				return true;
			}
		}
		return false;
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}

	private static String orZeroString(String s) {
		return StringUtils.hasText(s) ? s : "0";
	}

	private static MutableLong mutableLong(Object o) {
		return new MutableLong(longVal(o));
	}

	private static final class MutableLong {
		private long value;

		private MutableLong(long value) {
			this.value = value;
		}
	}
}
