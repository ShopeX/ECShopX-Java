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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.popularize.support.DistributorIdParamParser;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Aggregated brokerage rows for salesperson statistics.
 *
 * <p>column allowlist {@link #ALLOWED_OO_GROUP_BY} synced from NormalOrders {@code @TableField} values as of
 * 2026-04-11.</p>
 */
@Service
public class SalesmanBrokerageCountListQueryService {

	private static final Pattern DIGITS_ONLY = Pattern.compile("^[0-9]+$");

	// column allowlist synced from NormalOrders @TableField values as of 2026-04-11
	private static final Set<String> ALLOWED_OO_GROUP_BY = Set.of(
			"order_id", "title", "company_id", "shop_id", "cost_fee", "commission_fee", "user_id", "act_id", "mobile",
			"order_class", "freight_fee", "freight_point", "freight_point_fee", "freight_type", "item_fee", "total_fee",
			"market_fee", "step_paid_fee", "total_rebate", "distributor_id", "is_rate", "receipt_type", "ziti_code",
			"ziti_status", "self_delivery_status", "order_status", "pay_status", "order_source", "order_holder",
			"order_type", "auto_cancel_time", "auto_finish_time", "is_distribution", "source_id", "monitor_id",
			"salesman_id", "delivery_corp", "delivery_corp_source", "delivery_code", "delivery_img", "delivery_time",
			"end_time", "delivery_status", "cancel_status", "receiver_name", "receiver_mobile", "receiver_zip",
			"receiver_state", "receiver_city", "receiver_district", "receiver_address", "member_discount",
			"coupon_discount", "discount_fee", "discount_info", "coupon_discount_desc", "member_discount_desc",
			"create_time", "update_time", "fee_type", "fee_rate", "fee_symbol", "item_point", "point", "pay_type",
			"pay_channel", "remark", "third_params", "invoice", "invoice_number", "is_invoiced", "send_point",
			"is_online_order", "is_profitsharing", "profitsharing_status", "profitsharing_rate",
			"order_auto_close_aftersales_time", "type", "taxable_fee", "identity_id", "identity_name", "total_tax",
			"audit_status", "audit_msg", "point_fee", "point_use", "uppoint_use", "point_up_use", "get_point_type",
			"pack", "is_shopscreen", "is_logistics", "get_points", "extra_points", "bonus_points", "bind_auth_code",
			"sale_salesman_distributor_id", "bind_salesman_id", "bind_salesman_distributor_id", "chat_id",
			"is_consumption", "app_pay_type", "distributor_remark", "merchant_id", "self_delivery_operator_id",
			"self_delivery_fee", "self_delivery_time", "self_delivery_end_time", "subdistrict_parent_id", "subdistrict_id",
			"building_number", "house_number", "operator_id", "left_aftersales_num", "source_from", "supplier_id",
			"original_order_id", "offline_payment_status", "prescription_status", "invoice_status", "dm_point_preid");

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public SalesmanBrokerageCountListQueryService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	public long countGroupedSalesmanBrokerageRows(Map<String, Object> params) {
		MapSqlParameterSource p = new MapSqlParameterSource();
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(*) FROM (");
		sql.append("SELECT 1 AS x ");
		sql.append("FROM popularize_brokerage bb ");
		sql.append("LEFT JOIN orders_normal_orders oo ON bb.order_id = oo.order_id ");
		sql.append("LEFT JOIN aftersales aa ON bb.order_id = aa.order_id ");
		sql.append("LEFT JOIN distribution_distributor dd ON oo.distributor_id = dd.distributor_id ");
		sql.append("LEFT JOIN shop_salesperson ss ON bb.user_id = ss.user_id AND oo.distributor_id = ss.shop_id AND ss.user_id > 0 ");
		sql.append("WHERE 1 AND oo.distributor_id > 0 AND ss.salesperson_id IS NOT NULL AND bb.source = 'order' ");
		sql.append("AND ss.salesperson_id IS NOT NULL ");

		appendDateYearMonthDay(sql, p, params);
		appendUserIdConditions(sql, p, params);
		appendDistributorIn(sql, p, params);
		appendIsClose(sql, p, params);
		appendMobile(sql, p, params);
		appendOrderId(sql, p, params);
		appendDateStartEnd(sql, p, params);
		appendGroupBy(sql, params);

		sql.append(") AS t");

		Long cnt = namedParameterJdbcTemplate.queryForObject(sql.toString(), p, Long.class);
		return cnt == null ? 0L : cnt;
	}

	public List<Map<String, Object>> getSalesmanBrokerageCountList(Map<String, Object> params, int limit, int page) {
		int offset = limit * (page - 1);
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("offset", offset);
		p.addValue("limit", limit);

		StringBuilder sql = new StringBuilder();
		sql.append(
				"SELECT ss.salesperson_id, SUM(IF(bb.rebate > 0, 1, 0)) AS order_num, SUM(IF(bb.rebate < 0, 1, 0)) AS order_num_refund, ");
		sql.append("bb.order_id, bb.user_id, SUM(IF(bb.is_close = 0, bb.rebate, 0)) AS rebate_sum_noclose, ");
		sql.append("SUM(bb.rebate) AS rebate_sum, SUM(bb.price) AS price_sum, oo.total_fee, ss.name AS username, ");
		sql.append("ss.mobile, dd.name AS store_name, dd.distributor_id ");
		sql.append("FROM popularize_brokerage bb ");
		sql.append("LEFT JOIN orders_normal_orders oo ON bb.order_id = oo.order_id ");
		sql.append("LEFT JOIN aftersales aa ON bb.order_id = aa.order_id ");
		sql.append("LEFT JOIN distribution_distributor dd ON oo.distributor_id = dd.distributor_id ");
		sql.append("LEFT JOIN shop_salesperson ss ON bb.user_id = ss.user_id AND oo.distributor_id = ss.shop_id AND ss.user_id > 0 ");
		sql.append("WHERE 1 AND oo.distributor_id > 0 AND ss.salesperson_id IS NOT NULL AND bb.source = 'order' ");
		sql.append("AND ss.salesperson_id IS NOT NULL ");

		appendDateYearMonthDay(sql, p, params);
		appendUserIdConditions(sql, p, params);
		appendDistributorIn(sql, p, params);
		appendIsClose(sql, p, params);
		appendMobile(sql, p, params);
		appendOrderId(sql, p, params);
		appendDateStartEnd(sql, p, params);
		appendGroupBy(sql, params);

		sql.append(" ORDER BY rebate_sum DESC ");
		sql.append(" LIMIT :offset, :limit ");

		List<Map<String, Object>> rows = namedParameterJdbcTemplate.queryForList(sql.toString(), p);
		if (rows.isEmpty()) {
			return Collections.emptyList();
		}
		List<Map<String, Object>> out = new ArrayList<>(rows.size());
		for (Map<String, Object> row : rows) {
			out.add(new LinkedHashMap<>(row));
		}
		return out;
	}

	/**
	 * Single-row aggregate for salesperson brokerage totals (no {@code oo.distributor_id > 0} filter).
	 */
	public Map<String, Object> getSalesmanBrokerageCountAggregate(Map<String, Object> params) {
		MapSqlParameterSource p = new MapSqlParameterSource();
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ");
		sql.append("SUM(IF(bb.is_close = 0, bb.rebate, 0)) AS rebate_sum_noclose, ");
		sql.append("SUM(IF(bb.is_close = 1, bb.rebate, 0)) AS rebate_sum_close, ");
		sql.append("SUM(bb.rebate) AS rebate_sum, ");
		sql.append("SUM(bb.price) AS price_sum, ");
		sql.append("MAX(oo.total_fee) AS total_fee, ");
		sql.append("MAX(ss.name) AS name, ");
		sql.append("MAX(ss.mobile) AS mobile ");
		sql.append("FROM popularize_brokerage bb ");
		sql.append("LEFT JOIN orders_normal_orders oo ON bb.order_id = oo.order_id ");
		sql.append("LEFT JOIN aftersales aa ON bb.order_id = aa.order_id ");
		sql.append("LEFT JOIN shop_salesperson ss ON bb.user_id = ss.user_id AND oo.distributor_id = ss.shop_id ");
		sql.append("WHERE bb.source = 'order' AND ss.salesperson_id IS NOT NULL ");

		appendUserIdConditions(sql, p, params);
		appendDistributorIn(sql, p, params);
		appendIsClose(sql, p, params);
		appendMobile(sql, p, params);
		appendOrderId(sql, p, params);
		appendDateStartEnd(sql, p, params);
		appendDateYearMonthDay(sql, p, params);

		List<Map<String, Object>> rows = namedParameterJdbcTemplate.queryForList(sql.toString(), p);
		if (rows == null || rows.isEmpty()) {
			return emptyAggregateRow();
		}
		return new LinkedHashMap<>(rows.get(0));
	}

	private static Map<String, Object> emptyAggregateRow() {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("rebate_sum_noclose", null);
		m.put("rebate_sum_close", null);
		m.put("rebate_sum", null);
		m.put("price_sum", null);
		m.put("total_fee", null);
		m.put("name", null);
		m.put("mobile", null);
		return m;
	}

	private static void appendDateYearMonthDay(StringBuilder sql, MapSqlParameterSource p, Map<String, Object> params) {
		String ymd = null;
		int len = 0;
		Object year = params.get("year");
		if (year != null && StringUtils.hasText(year.toString().trim())) {
			ymd = year.toString().trim();
			len = 4;
		}
		Object month = params.get("month");
		if (month != null && StringUtils.hasText(month.toString().trim())) {
			ymd = month.toString().trim();
			len = 7;
		}
		Object day = params.get("day");
		if (day != null && StringUtils.hasText(day.toString().trim())) {
			ymd = day.toString().trim();
			len = 10;
		}
		if (ymd != null && len > 0) {
			p.addValue("ymdFragment", ymd);
			sql.append(" AND SUBSTR(FROM_UNIXTIME(bb.created), 1, ").append(len).append(") = :ymdFragment ");
		}
	}

	private static void appendUserIdConditions(StringBuilder sql, MapSqlParameterSource p, Map<String, Object> params) {
		Object userId = params.get("user_id");
		List<Long> inList = tryParseUserIdInList(userId);
		if (inList != null && !inList.isEmpty()) {
			p.addValue("bbUserIds", inList);
			sql.append(" AND bb.user_id IN (:bbUserIds) ");
			return;
		}
		long single = parseSingleUserIdOrZero(userId);
		if (single > 0L) {
			p.addValue("bbUserId", single);
			sql.append(" AND bb.user_id = :bbUserId ");
		}
	}

	private static List<Long> tryParseUserIdInList(Object userId) {
		if (userId == null) {
			return null;
		}
		if (userId instanceof Iterable<?> it) {
			List<Long> out = new ArrayList<>();
			for (Object el : it) {
				if (el == null) {
					continue;
				}
				Long v = parseLongElement(el);
				if (v != null && v > 0L) {
					out.add(v);
				}
			}
			return out.isEmpty() ? null : out;
		}
		if (userId instanceof Object[] arr) {
			List<Long> out = new ArrayList<>();
			for (Object el : arr) {
				if (el == null) {
					continue;
				}
				Long v = parseLongElement(el);
				if (v != null && v > 0L) {
					out.add(v);
				}
			}
			return out.isEmpty() ? null : out;
		}
		if (userId instanceof long[] arr) {
			List<Long> out = new ArrayList<>();
			for (long v : arr) {
				if (v > 0L) {
					out.add(v);
				}
			}
			return out.isEmpty() ? null : out;
		}
		if (userId instanceof int[] arr) {
			List<Long> out = new ArrayList<>();
			for (int v : arr) {
				if (v > 0) {
					out.add((long) v);
				}
			}
			return out.isEmpty() ? null : out;
		}
		return null;
	}

	private static Long parseLongElement(Object el) {
		if (el instanceof Number n) {
			return n.longValue();
		}
		if (el instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t) || !DIGITS_ONLY.matcher(t).matches()) {
				return null;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	private static long parseSingleUserIdOrZero(Object userId) {
		if (userId == null) {
			return 0L;
		}
		if (userId instanceof Number n) {
			return n.longValue();
		}
		if (userId instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t) || !DIGITS_ONLY.matcher(t).matches()) {
				return 0L;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
	}

	private static void appendDistributorIn(StringBuilder sql, MapSqlParameterSource p, Map<String, Object> params) {
		List<Long> ids = DistributorIdParamParser.resolveDistributorIdsForOrderInFilter(params);
		if (ids != null && !ids.isEmpty()) {
			p.addValue("distributorIds", ids);
			sql.append(" AND oo.distributor_id IN (:distributorIds) ");
		}
	}

	private static void appendIsClose(StringBuilder sql, MapSqlParameterSource p, Map<String, Object> params) {
		if (!params.containsKey("is_close")) {
			return;
		}
		int v = normalizeIsCloseToBit(params.get("is_close"));
		p.addValue("bbIsClose", v);
		sql.append(" AND bb.is_close = :bbIsClose ");
	}

	private static int normalizeIsCloseToBit(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Boolean b) {
			return b ? 1 : 0;
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L ? 1 : 0;
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t) || "0".equals(t)) {
				return 0;
			}
			if ("false".equalsIgnoreCase(t) || "no".equalsIgnoreCase(t)) {
				return 0;
			}
			return 1;
		}
		return 1;
	}

	private static void appendMobile(StringBuilder sql, MapSqlParameterSource p, Map<String, Object> params) {
		if (!params.containsKey("mobile")) {
			return;
		}
		Object m = params.get("mobile");
		if (m == null) {
			return;
		}
		String t = m.toString().trim();
		if (!StringUtils.hasText(t)) {
			return;
		}
		p.addValue("ooMobile", t);
		sql.append(" AND oo.mobile = :ooMobile ");
	}

	private static void appendOrderId(StringBuilder sql, MapSqlParameterSource p, Map<String, Object> params) {
		if (!params.containsKey("order_id")) {
			return;
		}
		Object oid = params.get("order_id");
		if (oid == null) {
			return;
		}
		String t = oid.toString().trim();
		if (!StringUtils.hasText(t)) {
			return;
		}
		p.addValue("bbOrderId", t);
		sql.append(" AND bb.order_id = :bbOrderId ");
	}

	private static void appendDateStartEnd(StringBuilder sql, MapSqlParameterSource p, Map<String, Object> params) {
		ZoneId z = ZoneId.systemDefault();
		DateTimeFormatter isoDate = DateTimeFormatter.ISO_LOCAL_DATE;
		if (params.containsKey("date_start") && params.get("date_start") != null) {
			String ds = params.get("date_start").toString().trim();
			if (StringUtils.hasText(ds)) {
				try {
					LocalDate d = LocalDate.parse(ds, isoDate);
					long sec = d.atStartOfDay(z).toEpochSecond();
					p.addValue("createdStart", sec);
					sql.append(" AND bb.created >= :createdStart ");
				} catch (DateTimeParseException e) {
					throw new BadRequestException("date_start 格式错误");
				}
			}
		}
		if (params.containsKey("date_end") && params.get("date_end") != null) {
			String de = params.get("date_end").toString().trim();
			if (StringUtils.hasText(de)) {
				try {
					LocalDate d = LocalDate.parse(de, isoDate);
					long sec = d.atTime(23, 59, 59).atZone(z).toEpochSecond();
					p.addValue("createdEnd", sec);
					sql.append(" AND bb.created <= :createdEnd ");
				} catch (DateTimeParseException e) {
					throw new BadRequestException("date_end 格式错误");
				}
			}
		}
	}

	private static void appendGroupBy(StringBuilder sql, Map<String, Object> params) {
		Object gb = params.get("groupby");
		String extraCol = null;
		if (gb != null) {
			String t = gb.toString().trim();
			if (StringUtils.hasText(t)) {
				if (!ALLOWED_OO_GROUP_BY.contains(t)) {
					throw new BadRequestException("非法 groupby 字段");
				}
				extraCol = t;
			}
		}
		sql.append(" GROUP BY bb.user_id, oo.distributor_id ");
		if (extraCol != null) {
			sql.append(", oo.").append(extraCol).append(" ");
		}
	}
}
