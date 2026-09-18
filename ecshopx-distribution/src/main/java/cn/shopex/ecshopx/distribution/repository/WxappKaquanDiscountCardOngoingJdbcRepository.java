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

package cn.shopex.ecshopx.distribution.repository;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WxappKaquanDiscountCardOngoingJdbcRepository {

	private static final String DATE_TYPE_FIX_TERM = "DATE_TYPE_FIX_TERM";
	private static final String DATE_TYPE_FIX_TIME_RANGE = "DATE_TYPE_FIX_TIME_RANGE";

	private final NamedParameterJdbcTemplate jdbc;

	public WxappKaquanDiscountCardOngoingJdbcRepository(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public List<Map<String, Object>> listOngoingShopCards(long companyId, List<Long> distributorIds) {
		if (distributorIds == null || distributorIds.isEmpty()) {
			return List.of();
		}
		MapSqlParameterSource params = new MapSqlParameterSource();
		StringBuilder sql = new StringBuilder();
		sql.append(
				"""
				SELECT card_id, card_type, title, color, date_type, begin_date, end_date, fixed_term, quantity, discount,
					least_cost, most_cost, reduce_cost, get_limit, receive, source_id
				FROM kaquan_discount_cards
				WHERE company_id = :companyId
				AND source_type = 'distributor'
				AND source_id IN (:distributorIds)
				AND kq_status = 0
				""");
		params.addValue("companyId", companyId);
		params.addValue("distributorIds", distributorIds);
		appendOngoingTimeWhere(params, sql, ZonedDateTime.now());
		sql.append(" ORDER BY created DESC, card_id DESC ");
		return queryShopCards(params, sql);
	}

	/**
	 * Ongoing window aligned with {@code DiscountCardsRepository::getOngoingList}: FIX_TERM rows (fixed-term
	 * semantics selected in projection, not an extra SQL predicate), or FIX_TIME_RANGE with begin/end
	 * straddling {@code now}.
	 */
	private void appendOngoingTimeWhere(MapSqlParameterSource params, StringBuilder whereSql, ZonedDateTime nowZoned) {
		int nowTs = (int) Math.min(nowZoned.toEpochSecond(), Integer.MAX_VALUE);
		params.addValue("ongoingNowTs", nowTs);
		params.addValue("ongoingDateTypeFixTerm", DATE_TYPE_FIX_TERM);
		params.addValue("ongoingDateTypeFixTimeRange", DATE_TYPE_FIX_TIME_RANGE);
		whereSql.append(
				"""
				AND (
					date_type = :ongoingDateTypeFixTerm
					OR (
						date_type = :ongoingDateTypeFixTimeRange
						AND begin_date <= :ongoingNowTs
						AND end_date >= :ongoingNowTs
					)
				)
				""");
	}

	private List<Map<String, Object>> queryShopCards(MapSqlParameterSource params, StringBuilder sql) {
		List<Map<String, Object>> raw = jdbc.queryForList(sql.toString(), params);
		List<Map<String, Object>> out = new ArrayList<>();
		for (Map<String, Object> row : raw) {
			Map<String, Object> m = new LinkedHashMap<>(row);
			Object recv = m.get("receive");
			int receive = 0;
			if (recv != null) {
				String r = String.valueOf(recv).trim();
				if ("1".equals(r) || "true".equalsIgnoreCase(r)) {
					receive = 1;
				}
			}
			m.put("receive", receive);
			out.add(m);
		}
		return out;
	}
}
