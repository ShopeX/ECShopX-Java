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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SalesmanBrokerageLogsQueryService {

	private static final Pattern DIGITS_ONLY = Pattern.compile("^[0-9]+$");

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public SalesmanBrokerageLogsQueryService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	public long countByFilter(Map<String, Object> filter) {
		MapSqlParameterSource p = new MapSqlParameterSource();
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(1) ");
		appendFromJoinWhere(sql, p, filter);
		Long n = namedParameterJdbcTemplate.queryForObject(sql.toString(), p, Long.class);
		return n == null ? 0L : n;
	}

	public List<Map<String, Object>> selectPageByFilter(Map<String, Object> filter, int pageSize, int page) {
		int offset = pageSize * (page - 1);
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("offset", offset);
		p.addValue("limit", pageSize);

		StringBuilder sql = new StringBuilder();
		sql.append(
				"SELECT bb.id, bb.brokerage_type, bb.order_id, bb.user_id, bb.buy_user_id, bb.source, bb.order_type, ");
		sql.append("bb.company_id, bb.price, bb.is_close, bb.plan_close_time, bb.commission_type, bb.rebate, ");
		sql.append("bb.rebate_point, bb.detail, bb.created, bb.updated, ");
		sql.append("oo.distributor_id, oo.title, oo.total_fee, dd.name AS store_name, ");
		sql.append("ss.salesperson_id, ss.name, ss.mobile, ss.number ");
		appendFromJoinWhere(sql, p, filter);
		sql.append(" ORDER BY bb.created DESC ");
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

	public List<Map<String, Object>> enrichListWithSalesperson(long companyId, List<Map<String, Object>> list) {
		if (list == null || list.isEmpty()) {
			return Collections.emptyList();
		}
		Set<Long> idSet = new LinkedHashSet<>();
		for (Map<String, Object> row : list) {
			Long sid = extractLong(row.get("salesperson_id"));
			if (sid != null && sid > 0L) {
				idSet.add(sid);
			}
		}
		if (idSet.isEmpty()) {
			return Collections.emptyList();
		}
		List<Long> ids = new ArrayList<>(idSet);
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("companyId", companyId);
		p.addValue("ids", ids);
		String sql =
				"SELECT salesperson_id, name, mobile, number FROM shop_salesperson WHERE company_id = :companyId AND salesperson_id IN (:ids)";
		List<Map<String, Object>> rawRows = namedParameterJdbcTemplate.queryForList(sql, p);
		List<Map<String, Object>> rawLinked = new ArrayList<>(rawRows.size());
		for (Map<String, Object> r : rawRows) {
			rawLinked.add(new LinkedHashMap<>(r));
		}
		Map<Long, Map<String, Object>> index = new LinkedHashMap<>();
		for (Map<String, Object> r : rawLinked) {
			Long sid = extractLong(r.get("salesperson_id"));
			if (sid != null) {
				index.put(sid, r);
			}
		}
		for (Map<String, Object> row : list) {
			Long sid = extractLong(row.get("salesperson_id"));
			if (sid == null || !index.containsKey(sid)) {
				continue;
			}
			Map<String, Object> sp = index.get(sid);
			row.put("mobile", sp.get("mobile"));
			row.put("name", sp.get("name"));
			String nameStr = sp.get("name") == null ? "" : String.valueOf(sp.get("name")).trim();
			String numberStr = sp.get("number") == null ? "" : String.valueOf(sp.get("number")).trim();
			row.put("username", StringUtils.hasText(nameStr) ? nameStr : numberStr);
		}
		return rawLinked;
	}

	private static Long extractLong(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return null;
			}
			if (!DIGITS_ONLY.matcher(t).matches()) {
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

	private void appendFromJoinWhere(StringBuilder sql, MapSqlParameterSource p, Map<String, Object> filter) {
		Object cid = filter.get("company_id");
		p.addValue("companyId", cid);
		sql.append("FROM popularize_brokerage bb ");
		sql.append("LEFT JOIN orders_normal_orders oo ON bb.order_id = oo.order_id ");
		sql.append("LEFT JOIN aftersales aa ON bb.order_id = aa.order_id ");
		sql.append("LEFT JOIN distribution_distributor dd ON oo.distributor_id = dd.distributor_id ");
		sql.append("LEFT JOIN shop_salesperson ss ON bb.user_id = ss.user_id AND oo.distributor_id = ss.shop_id AND ss.user_id > 0 ");
		sql.append("WHERE bb.company_id = :companyId ");
		sql.append("AND bb.source = 'order' ");
		sql.append("AND ss.salesperson_id IS NOT NULL ");
		appendUserIdConditions(sql, p, filter);
		appendDistributorIn(sql, p, filter);
		appendIsClose(sql, p, filter);
		appendMobileForOrderExport(sql, p, filter);
		appendDateStartEndForOrderExport(sql, p, filter);
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

	private static void appendMobileForOrderExport(
			StringBuilder sql, MapSqlParameterSource p, Map<String, Object> params) {
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

	private static void appendDateStartEndForOrderExport(
			StringBuilder sql, MapSqlParameterSource p, Map<String, Object> params) {
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

	public long countH5PromoterSalesmanBrokerageLogs(Map<String, Object> filter) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COUNT(1) ");
		MapSqlParameterSource p = new MapSqlParameterSource();
		appendFromJoinWhere(sql, p, filter);
		appendH5OrderIdEquals(sql, p, filter);
		Long n = namedParameterJdbcTemplate.queryForObject(sql.toString(), p, Long.class);
		return n == null ? 0L : n;
	}

	public List<Map<String, Object>> selectPageH5PromoterSalesmanBrokerageLogs(
			Map<String, Object> filter, int pageSize, int page) {
		int offset = pageSize * (page - 1);
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("offset", offset);
		p.addValue("limit", pageSize);
		StringBuilder sql = new StringBuilder();
		sql.append(
				"SELECT bb.id, bb.brokerage_type, bb.order_id, bb.user_id, bb.buy_user_id, bb.source, bb.order_type, ");
		sql.append("bb.company_id, bb.price, bb.is_close, bb.plan_close_time, bb.commission_type, bb.rebate, ");
		sql.append("bb.rebate_point, bb.detail, bb.created, bb.updated, ");
		sql.append("oo.distributor_id, oo.title, oo.total_fee, ss.name, ss.mobile, dd.name AS store_name, ");
		sql.append("ss.salesperson_id, ss.number, ");
		sql.append("CASE WHEN ss.user_id > 0 THEN '业务员' ELSE '推广员' END AS promote_type ");
		appendFromJoinWhere(sql, p, filter);
		appendH5OrderIdEquals(sql, p, filter);
		sql.append(" ORDER BY bb.created DESC ");
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

	private void appendH5OrderIdEquals(StringBuilder sql, MapSqlParameterSource p, Map<String, Object> filter) {
		if (!filter.containsKey("order_id")) {
			return;
		}
		Object v = filter.get("order_id");
		if (!(v instanceof String s)) {
			return;
		}
		String trimmed = s.trim();
		if (trimmed.isEmpty() || "0".equalsIgnoreCase(trimmed)) {
			return;
		}
		sql.append(" AND bb.order_id = :h5BbOrderId ");
		p.addValue("h5BbOrderId", trimmed);
	}
}
