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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TradeFinishPushMarketingCenterProcessor {

	private static final DateTimeFormatter PAY_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private static final String SQL_ORDER_FOR_MARKETING =
			"SELECT order_id, company_id, salesman_id, bind_salesman_id, chat_id,"
					+ " sale_salesman_distributor_id, bind_salesman_distributor_id"
					+ " FROM orders_normal_orders WHERE company_id = ? AND order_id = ? LIMIT 1";

	private static final String SQL_TRADE_FALLBACK =
			"SELECT trade_id, order_id, company_id, shop_id, distributor_id, trade_source_type, user_id, mobile,"
					+ " discount_info, mch_id, total_fee, discount_fee, pay_fee, pay_type, transaction_id,"
					+ " time_expire, coupon_fee, coupon_info"
					+ " FROM trade WHERE trade_id = ? LIMIT 1";

	private final JdbcTemplate jdbcTemplate;
	private final MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient;

	public TradeFinishPushMarketingCenterProcessor(
			JdbcTemplate jdbcTemplate, MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient) {
		this.jdbcTemplate = jdbcTemplate;
		this.marketingCenterOpenApiSignedFormClient = marketingCenterOpenApiSignedFormClient;
	}

	public void handle(Map<String, Object> tradeRowPayload) {
		if (tradeRowPayload == null || tradeRowPayload.isEmpty()) {
			return;
		}
		Optional<Map<String, Object>> tradeOpt = resolveTradeRow(tradeRowPayload);
		if (tradeOpt.isEmpty()) {
			return;
		}
		Map<String, Object> trade = stringifyIntsInTradeMap(tradeOpt.get());

		long companyId = longVal(trade.get("company_id"));
		long orderId = longVal(trade.get("order_id"));
		if (companyId <= 0L || orderId <= 0L) {
			return;
		}

		Optional<Map<String, Object>> orderOpt = queryOrderRow(companyId, orderId);
		if (orderOpt.isEmpty()) {
			return;
		}
		Map<String, Object> orderRow = orderOpt.get();
		if (!hasSalesman(orderRow)) {
			return;
		}

		LinkedHashMap<String, Object> input = buildBaseMarketingInput(trade, orderRow);
		Optional<Map<String, Object>> mergedOpt = mergeSalesFormatting(companyId, orderRow, input);
		if (mergedOpt.isEmpty()) {
			return;
		}
		Map<String, Object> out = stringifyValuesForOutbound(mergedOpt.get());
		marketingCenterOpenApiSignedFormClient.basicsOrderPay(companyId, out);
	}

	private Optional<Map<String, Object>> resolveTradeRow(Map<String, Object> tradeRowPayload) {
		Object tradeIdRaw = tradeRowPayload.get("trade_id");
		if (!StringUtils.hasText(str(tradeIdRaw))) {
			return Optional.empty();
		}
		boolean needsReload =
				!hasMeaningfulLong(tradeRowPayload.get("company_id"))
						|| !hasMeaningfulLong(tradeRowPayload.get("order_id"));
		Map<String, Object> tradeRow = tradeRowPayload;
		if (needsReload) {
			Optional<Map<String, Object>> db = queryTradeSnakeRow(str(tradeIdRaw));
			if (db.isEmpty()) {
				return Optional.empty();
			}
			tradeRow = db.get();
		}
		return Optional.of(tradeRow);
	}

	private Optional<Map<String, Object>> queryTradeSnakeRow(String tradeId) {
		try {
			Map<String, Object> row =
					jdbcTemplate.queryForObject(
							SQL_TRADE_FALLBACK,
							(rs, rn) -> {
								LinkedHashMap<String, Object> m = new LinkedHashMap<>();
								m.put("trade_id", rs.getObject("trade_id"));
								m.put("order_id", rs.getObject("order_id"));
								m.put("company_id", rs.getObject("company_id"));
								m.put("shop_id", rs.getObject("shop_id"));
								m.put("distributor_id", rs.getObject("distributor_id"));
								m.put("trade_source_type", rs.getObject("trade_source_type"));
								m.put("user_id", rs.getObject("user_id"));
								m.put("mobile", rs.getObject("mobile"));
								m.put("discount_info", rs.getObject("discount_info"));
								m.put("mch_id", rs.getObject("mch_id"));
								m.put("total_fee", rs.getObject("total_fee"));
								m.put("discount_fee", rs.getObject("discount_fee"));
								m.put("pay_fee", rs.getObject("pay_fee"));
								m.put("pay_type", rs.getObject("pay_type"));
								m.put("transaction_id", rs.getObject("transaction_id"));
								m.put("time_expire", rs.getObject("time_expire"));
								m.put("coupon_fee", rs.getObject("coupon_fee"));
								m.put("coupon_info", rs.getObject("coupon_info"));
								return m;
							},
							tradeId);
			return Optional.ofNullable(row);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	private static Map<String, Object> stringifyIntsInTradeMap(Map<String, Object> trade) {
		Map<String, Object> out = new LinkedHashMap<>(trade);
		for (Map.Entry<String, Object> e : out.entrySet()) {
			Object v = e.getValue();
			if (v instanceof Integer || v instanceof Long || v instanceof Short || v instanceof Byte) {
				out.put(e.getKey(), String.valueOf(((Number) v).longValue()));
			}
		}
		return out;
	}

	private Optional<Map<String, Object>> queryOrderRow(long companyId, long orderId) {
		try {
			Map<String, Object> row =
					jdbcTemplate.queryForObject(
							SQL_ORDER_FOR_MARKETING,
							(rs, rn) -> {
								Map<String, Object> m = new LinkedHashMap<>();
								m.put("order_id", rs.getObject("order_id"));
								m.put("company_id", rs.getObject("company_id"));
								m.put("salesman_id", rs.getObject("salesman_id"));
								m.put("bind_salesman_id", rs.getObject("bind_salesman_id"));
								m.put("chat_id", rs.getObject("chat_id"));
								m.put("sale_salesman_distributor_id", rs.getObject("sale_salesman_distributor_id"));
								m.put("bind_salesman_distributor_id", rs.getObject("bind_salesman_distributor_id"));
								return m;
							},
							companyId,
							orderId);
			return Optional.ofNullable(row);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	private static boolean hasSalesman(Map<String, Object> orderRow) {
		return longVal(orderRow.get("salesman_id")) != 0L;
	}

	private static boolean hasMeaningfulLong(Object o) {
		return longVal(o) != 0L;
	}

	private LinkedHashMap<String, Object> buildBaseMarketingInput(Map<String, Object> trade, Map<String, Object> orderRow) {
		String payTypeToken = str(trade.get("pay_type"));
		String payTypeCode =
				switch (payTypeToken) {
					case "wxpay" -> "1";
					case "deposit" -> "2";
					case "pos" -> "3";
					case "point" -> "4";
					default -> "1";
				};
		long epochSec = parseTimeExpireEpochSeconds(trade.get("time_expire"));
		String payTime = PAY_TIME_FMT.format(Instant.ofEpochSecond(epochSec));

		LinkedHashMap<String, Object> input = new LinkedHashMap<>();
		input.put("trade_id", emptyIfNull(trade.get("trade_id")));
		input.put("chat_id", emptyIfNull(orderRow.get("chat_id")));
		input.put("order_id", emptyIfNull(trade.get("order_id")));
		input.put("company_id", emptyIfNull(trade.get("company_id")));
		input.put("shop_id", emptyIfNull(trade.get("shop_id")));
		input.put("distributor_id", emptyIfNull(trade.get("distributor_id")));
		input.put("external_member_id", emptyIfNull(trade.get("user_id")));
		input.put("mobile", emptyIfNull(trade.get("mobile")));
		input.put("discount_info", emptyIfNull(trade.get("discount_info")));
		input.put("mch_id", emptyIfNull(trade.get("mch_id")));
		input.put("total_fee", emptyIfNull(trade.get("total_fee")));
		input.put("discount_fee", emptyIfNull(trade.get("discount_fee")));
		input.put("pay_fee", emptyIfNull(trade.get("pay_fee")));
		input.put("pay_type", payTypeCode);
		input.put("transaction_id", emptyIfNull(trade.get("transaction_id")));
		input.put("pay_time", payTime);
		input.put("coupon_fee", emptyIfNull(trade.get("coupon_fee")));
		input.put("coupon_info", emptyIfNull(trade.get("coupon_info")));
		input.put("order_source", "1");
		return input;
	}

	private static long parseTimeExpireEpochSeconds(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	/**
	 * Port of salesperson / distributor enrichment used before basics.order.pay: shop_salesperson,
	 * shop_rel_salesperson (distributor), distribution_distributor, optional orders_normal_orders patch.
	 */
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

	private static Map<String, Object> stringifyValuesForOutbound(Map<String, Object> src) {
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : src.entrySet()) {
			Object v = e.getValue();
			if (v instanceof Integer || v instanceof Long || v instanceof Short || v instanceof Byte) {
				out.put(e.getKey(), String.valueOf(((Number) v).longValue()));
			} else if (v == null) {
				out.put(e.getKey(), "");
			} else if (v instanceof List<?> list && list.isEmpty()) {
				out.put(e.getKey(), "");
			} else {
				out.put(e.getKey(), v);
			}
		}
		return out;
	}

	private static String emptyIfNull(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static String orZeroString(String s) {
		return StringUtils.hasText(s) ? s : "0";
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
