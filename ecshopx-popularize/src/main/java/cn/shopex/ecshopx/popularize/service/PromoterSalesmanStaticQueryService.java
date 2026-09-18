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

package cn.shopex.ecshopx.popularize.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromoterSalesmanStaticQueryService {

	private static final Logger log = LoggerFactory.getLogger(PromoterSalesmanStaticQueryService.class);

	private static final Pattern DIGITS_ONLY = Pattern.compile("^[0-9]+$");

	private static final Set<String> FIXED_BROKERAGE_SELECT_ALIASES = Set.of(
			"distributor_id",
			"date_brokerage",
			"order_num",
			"total_Fee",
			"refund_Fee",
			"total_rebate",
			"aftersales_num",
			"aftersale_Fee",
			"price_fee",
			"buy_member_num");

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Value("${ecshopx.popularize.debug-salesman-user-id:}")
	private String debugSalesmanUserId;

	public PromoterSalesmanStaticQueryService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	public List<LinkedHashMap<String, Object>> getSalesmanStatic(
			long userId, String tab, String datetype, String date, String distributorIdRaw) {
		log.debug(
				"getSalesmanStatic params userId={} tab={} datetype={} date={} distributorIdRaw={}",
				userId,
				tab,
				datetype,
				date,
				distributorIdRaw);
		long effectiveUserId = resolveEffectiveUserId(userId);
		DateContext ctx = resolveDateContext(datetype, date);

		StringBuilder sqlBrokerage = new StringBuilder();
		sqlBrokerage.append("SELECT oo.distributor_id,\n")
				.append("substr(from_unixtime(bb.created),1,")
				.append(ctx.dateLen())
				.append(") AS date_brokerage,\n")
				.append("sum(if(price > 0,1 ,0) ) AS order_num,\n")
				.append("SUM(if(price > 0,total_fee,0)) AS total_Fee,\n")
				.append("SUM(if(price < 0,total_fee,0)) AS refund_Fee,\n")
				.append("if(count(1)>0,sum(bb.rebate),0 ) AS total_rebate,\n")
				.append("if(count(1)>0 ,sum(if(aftersales_bn > 0, 1, 0)),0) AS aftersales_num,\n")
				.append("if(count(1)>0 ,sum(refund_fee),0) AS aftersale_Fee,\n")
				.append("if(count(1)>0,sum(total_fee) /count(1),0) AS price_fee,\n")
				.append("count(distinct oo.user_id) AS buy_member_num,\n")
				.append("concat(oo.user_id)\n")
				.append("FROM popularize_brokerage AS bb\n")
				.append("LEFT JOIN orders_normal_orders oo ON bb.order_id = oo.order_id\n")
				.append("LEFT JOIN aftersales aa ON bb.order_id = aa.order_id\n")
				.append("WHERE bb.user_id = :bbUserId\n");
		MapSqlParameterSource paramsBrokerage = new MapSqlParameterSource();
		paramsBrokerage.addValue("bbUserId", effectiveUserId);
		paramsBrokerage.addValues(ctx.fragmentParams().getValues());
		sqlBrokerage.append(ctx.datePredicateSqlFragment());
		appendDistributorOnOrders(sqlBrokerage, paramsBrokerage, distributorIdRaw);
		appendBrokerageTypeInClause(sqlBrokerage, tab);
		sqlBrokerage.append(" GROUP BY substr(from_unixtime(created),1,")
				.append(ctx.dateLen())
				.append(") ")
				.append("ORDER BY created DESC");

		log.debug("getSalesmanStatic brokerage sql: {} params: {}", sqlBrokerage, paramsBrokerage.getValues());

		List<Map<String, Object>> listBrokerageRaw =
				namedParameterJdbcTemplate.queryForList(sqlBrokerage.toString(), paramsBrokerage);
		List<Map<String, Object>> listBrokerage = new ArrayList<>();
		if (listBrokerageRaw != null) {
			for (Map<String, Object> row : listBrokerageRaw) {
				listBrokerage.add(normalizeBrokerageRow(row));
			}
		}

		StringBuilder sqlPromoter = new StringBuilder();
		sqlPromoter.append("SELECT substr(from_unixtime(created),1,")
				.append(ctx.dateLen())
				.append(") AS date_brokerage,\n")
				.append("       count(1) AS member_num\n")
				.append("FROM popularize_promoter bb\n")
				.append("WHERE bb.pid = :pidUserId\n")
				.append(ctx.datePredicateSqlFragment());
		sqlPromoter.append(" GROUP BY substr(from_unixtime(created),1,")
				.append(ctx.dateLen())
				.append(") ")
				.append("ORDER BY created DESC");
		MapSqlParameterSource paramsPromoter = new MapSqlParameterSource();
		paramsPromoter.addValue("pidUserId", effectiveUserId);
		paramsPromoter.addValues(ctx.fragmentParams().getValues());

		log.debug("getSalesmanStatic promoter sql: {} params: {}", sqlPromoter, paramsPromoter.getValues());

		List<Map<String, Object>> listPromoter =
				namedParameterJdbcTemplate.queryForList(sqlPromoter.toString(), paramsPromoter);
		if (listPromoter == null) {
			listPromoter = List.of();
		} else {
			listPromoter = new ArrayList<>(listPromoter);
			listPromoter.removeIf(Objects::isNull);
		}

		log.debug(
				"getSalesmanStatic listPromoter={} listBrokerage={}",
				listPromoter,
				listBrokerage);

		List<LinkedHashMap<String, Object>> merged = mergeSaleStaticByDate(listPromoter, listBrokerage);
		log.debug("getSalesmanStatic merged={}", merged);
		return merged;
	}

	private long resolveEffectiveUserId(long userId) {
		if (!StringUtils.hasText(debugSalesmanUserId)) {
			return userId;
		}
		String t = debugSalesmanUserId.trim();
		if (!DIGITS_ONLY.matcher(t).matches()) {
			return userId;
		}
		try {
			long v = Long.parseLong(t);
			return v > 0L ? v : userId;
		} catch (NumberFormatException e) {
			return userId;
		}
	}

	private DateContext resolveDateContext(String datetype, String date) {
		int dateLen = 1;
		String datePredicateSqlFragment = "";
		MapSqlParameterSource fragmentParams = new MapSqlParameterSource();
		String dt = datetype == null ? "" : datetype.trim();
		switch (dt) {
			case "y" -> {
				dateLen = 7;
				datePredicateSqlFragment = " and substr(from_unixtime(bb.created),1,4) = :dateFragment ";
				fragmentParams.addValue("dateFragment", date == null ? "" : date.trim());
			}
			case "m" -> {
				dateLen = 10;
				datePredicateSqlFragment = " and substr(from_unixtime(bb.created),1,7) = :dateFragment ";
				fragmentParams.addValue("dateFragment", date == null ? "" : date.trim());
			}
			case "d" -> {
				dateLen = 10;
				datePredicateSqlFragment = " and substr(from_unixtime(bb.created),1,10) = :dateFragment ";
				fragmentParams.addValue("dateFragment", date == null ? "" : date.trim());
			}
			default -> {
				// keep dateLen=1 and empty predicate
			}
		}
		return new DateContext(dateLen, datePredicateSqlFragment, fragmentParams);
	}

	private record DateContext(int dateLen, String datePredicateSqlFragment, MapSqlParameterSource fragmentParams) {}

	private static void appendBrokerageTypeInClause(StringBuilder sql, String tab) {
		if (tab == null) {
			return;
		}
		switch (tab.trim()) {
			case "all" -> sql.append(" and bb.brokerage_type in ( 'first_level', 'second_level' ) ");
			case "lv1" -> sql.append(" and bb.brokerage_type in ( 'first_level') ");
			case "lv2" -> sql.append(" and bb.brokerage_type in ( 'second_level') ");
			default -> {
				// no fragment
			}
		}
	}

	private static void appendDistributorOnOrders(
			StringBuilder sql, MapSqlParameterSource params, String distributorIdRaw) {
		if (distributorIdRaw == null) {
			return;
		}
		String t = distributorIdRaw.trim();
		if (!StringUtils.hasText(t) || !DIGITS_ONLY.matcher(t).matches()) {
			return;
		}
		try {
			long id = Long.parseLong(t);
			if (id == 0L) {
				return;
			}
			sql.append(" and oo.distributor_id = :distributorId ");
			params.addValue("distributorId", id);
		} catch (NumberFormatException e) {
			// ignore invalid filter
		}
	}

	private List<LinkedHashMap<String, Object>> mergeSaleStaticByDate(
			List<Map<String, Object>> listPromoter, List<Map<String, Object>> listBrokerage) {
		if (listPromoter == null) {
			listPromoter = List.of();
		}
		if (listBrokerage == null) {
			listBrokerage = List.of();
		}
		if (listPromoter.isEmpty() && listBrokerage.isEmpty()) {
			return new ArrayList<>();
		}
		LinkedHashMap<String, LinkedHashMap<String, Object>> byDate = new LinkedHashMap<>();
		for (Map<String, Object> row : listPromoter) {
			if (row == null) {
				continue;
			}
			String dateKey = Objects.toString(row.get("date_brokerage"), "");
			byDate.put(dateKey, new LinkedHashMap<>(row));
		}
		for (Map<String, Object> row : listBrokerage) {
			if (row == null) {
				continue;
			}
			String dateKey = Objects.toString(row.get("date_brokerage"), "");
			if (byDate.containsKey(dateKey)) {
				LinkedHashMap<String, Object> existing = byDate.get(dateKey);
				for (Map.Entry<String, Object> e : row.entrySet()) {
					existing.put(e.getKey(), e.getValue());
				}
			} else {
				LinkedHashMap<String, Object> copy = new LinkedHashMap<>(row);
				copy.put("member_num", Integer.valueOf(0));
				byDate.put(dateKey, copy);
			}
		}
		List<LinkedHashMap<String, Object>> ret = new ArrayList<>();
		for (LinkedHashMap<String, Object> itemStatic : byDate.values()) {
			if (!keySetAndNonNull(itemStatic, "order_num")) {
				LinkedHashMap<String, Object> defaults = new LinkedHashMap<>();
				defaults.put("order_num", "0");
				defaults.put("distributor_id", "0");
				defaults.put("total_Fee", "0");
				defaults.put("aftersales_num", "0");
				defaults.put("refund_Fee", "0");
				defaults.put("price_fee", "0");
				defaults.put("buy_member_num", "0");
				defaults.put("total_rebate", "0");
				LinkedHashMap<String, Object> line = new LinkedHashMap<>(itemStatic);
				line.putAll(defaults);
				line.put("salesName", "推广员tobe");
				ret.add(line);
			} else {
				LinkedHashMap<String, Object> line = new LinkedHashMap<>(itemStatic);
				line.put("salesName", "推广员tobe");
				ret.add(line);
			}
		}
		return ret;
	}

	private static boolean keySetAndNonNull(Map<String, Object> map, String key) {
		if (!map.containsKey(key)) {
			return false;
		}
		return map.get(key) != null;
	}

	private LinkedHashMap<String, Object> normalizeBrokerageRow(Map<String, Object> row) {
		Map<String, Object> safe = row;
		if (safe == null) {
			safe = Map.of();
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : safe.entrySet()) {
			out.put(e.getKey(), e.getValue());
		}
		String concatDriverKey = null;
		for (String k : out.keySet()) {
			if (!FIXED_BROKERAGE_SELECT_ALIASES.contains(k)) {
				concatDriverKey = k;
				break;
			}
		}
		if (concatDriverKey != null) {
			out.put("user_id_concat", Objects.toString(out.get(concatDriverKey), ""));
		} else {
			out.put("user_id_concat", "");
		}
		if (out.containsKey("price_fee")) {
			out.put("price_fee", normalizePriceFeeScalar(out.get("price_fee")));
		}
		return out;
	}

	private static String normalizePriceFeeScalar(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof String s) {
			return s;
		}
		if (raw instanceof Number n) {
			return new BigDecimal(n.toString()).stripTrailingZeros().toPlainString();
		}
		return Objects.toString(raw, null);
	}
}
