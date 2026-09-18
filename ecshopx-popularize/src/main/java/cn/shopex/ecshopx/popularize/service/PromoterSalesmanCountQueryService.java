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
public class PromoterSalesmanCountQueryService {

	private static final Logger log = LoggerFactory.getLogger(PromoterSalesmanCountQueryService.class);

	private static final Pattern DIGITS_ONLY = Pattern.compile("^[0-9]+$");

	/**
	 * Column names that match explicit AS aliases in {@link #COUNT_SQL_BODY}. Any other key in the JDBC row map is
	 * treated as the unaliased {@code concat(oo.user_id)} projection (label varies by driver), so {@link #normalizeRow}
	 * writes a stable {@code user_id_concat} entry.
	 */
	private static final Set<String> FIXED_SELECT_ALIASES = Set.of(
			"order_num",
			"total_Fee",
			"refund_Fee",
			"aftersales_num",
			"aftersale_Fee",
			"price_fee",
			"member_num");

	private static final String COUNT_SQL_BODY =
			"SELECT    if(count(1)>0 ,  sum(if(price > 0,1 ,0) ),0) AS order_num,\n"
					+ "        SUM(if(price > 0,total_fee,0)) AS total_Fee,\n"
					+ "        SUM(if(price < 0,total_fee,0)) AS refund_Fee,\n"
					+ "                        if(count(1)>0 ,sum(if(aftersales_bn > 0, 1, 0)),0) as aftersales_num,\n"
					+ "                        if(count(1)>0 ,sum(refund_fee),0) as aftersale_Fee,\n"
					+ "                        if(count(1)>0,sum(total_fee) /count(1),0) as price_fee,\n"
					+ "                        count(distinct oo.user_id) as member_num ,\n"
					+ "                        concat( oo.user_id)\n"
					+ "                        FROM popularize_brokerage as bb \n"
					+ "                        left join orders_normal_orders oo ON bb.order_id = oo.order_id \n"
					+ "                        left join aftersales as aa ON bb.order_id = aa.order_id \n"
					+ "                        WHERE bb.user_id = :bbUserId ";

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Value("${ecshopx.popularize.debug-salesman-user-id:}")
	private String debugSalesmanUserId;

	public PromoterSalesmanCountQueryService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	public LinkedHashMap<String, Object> getSalesmanCount(
			long userId, String date, String datetype, String distributorIdRaw) {
		long effectiveUserId = resolveEffectiveUserId(userId);

		StringBuilder sql = new StringBuilder(COUNT_SQL_BODY);
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("bbUserId", effectiveUserId);

		appendDatePredicate(sql, params, date, datetype);
		appendDistributorPredicate(sql, params, distributorIdRaw);

		log.debug("getSalesmanCount sql: {} params: {}", sql, params.getValues());

		List<Map<String, Object>> list = namedParameterJdbcTemplate.queryForList(sql.toString(), params);
		Map<String, Object> row;
		if (list == null || list.isEmpty()) {
			row = defaultAggregateRow();
		} else {
			row = list.get(0);
		}
		return normalizeRow(row);
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

	private static void appendDatePredicate(
			StringBuilder sql, MapSqlParameterSource params, String date, String datetype) {
		if (date == null || date.trim().isEmpty()) {
			return;
		}
		String dt = datetype == null ? "" : datetype.trim();
		switch (dt) {
			case "y" -> {
				sql.append(" and   substr(from_unixtime(bb.created),1,4) = :dateFragment ");
				params.addValue("dateFragment", date.trim());
			}
			case "m" -> {
				sql.append(" and   substr(from_unixtime(bb.created),1,7) = :dateFragment ");
				params.addValue("dateFragment", date.trim());
			}
			case "d" -> {
				sql.append(" and   substr(from_unixtime(bb.created),1,10) = :dateFragment ");
				params.addValue("dateFragment", date.trim());
			}
			default -> {
				// silent: no date filter
			}
		}
	}

	private static void appendDistributorPredicate(
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

	private static Map<String, Object> defaultAggregateRow() {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("order_num", 0L);
		m.put("total_Fee", 0L);
		m.put("refund_Fee", 0L);
		m.put("aftersales_num", 0L);
		m.put("aftersale_Fee", 0L);
		m.put("price_fee", "0");
		m.put("member_num", 0L);
		return m;
	}

	private static LinkedHashMap<String, Object> normalizeRow(Map<String, Object> row) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		if (row != null) {
			for (Map.Entry<String, Object> e : row.entrySet()) {
				out.put(e.getKey(), e.getValue());
			}
		}
		String concatDriverKey = null;
		for (String k : out.keySet()) {
			if (!FIXED_SELECT_ALIASES.contains(k)) {
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

	/** Normalizes {@code sum(total_fee)/count(1)} driver values to a plain decimal string without a redundant {@code .0} suffix. */
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
