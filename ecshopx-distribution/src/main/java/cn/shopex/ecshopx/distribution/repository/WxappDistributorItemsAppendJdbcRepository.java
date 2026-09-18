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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class WxappDistributorItemsAppendJdbcRepository {

	private final NamedParameterJdbcTemplate jdbc;
	private final ObjectMapper objectMapper;

	public WxappDistributorItemsAppendJdbcRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
		this.jdbc = jdbc;
		this.objectMapper = objectMapper;
	}

	public List<Map<String, Object>> listTopItemsForDistributor(
			long companyId, long distributorId, List<Long> filterItemIds, int limit, int offset) {
		StringBuilder sql = new StringBuilder();
		sql.append(
				"""
				SELECT i.item_id, i.item_name, i.price, i.market_price, i.store, i.pics, d.distributor_id
				FROM distribution_distributor_items d
				INNER JOIN items i ON d.item_id = i.item_id
				WHERE d.company_id = :companyId AND i.company_id = :companyId
				AND d.distributor_id = :distributorId
				AND i.item_type = 'normal' AND i.type = 0
				AND (i.is_gift = 0 OR i.is_gift IS NULL)
				AND i.audit_status = 'approved'
				AND i.approve_status IN ('onsale','offline_sale','only_show')
				AND d.is_can_sale = 1
				""");
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("companyId", companyId);
		p.addValue("distributorId", distributorId);
		if (filterItemIds != null && !filterItemIds.isEmpty()) {
			sql.append("AND i.item_id IN (:filterItemIds) ");
			p.addValue("filterItemIds", filterItemIds);
		}
		sql.append("ORDER BY i.created DESC, i.item_id DESC LIMIT :lim OFFSET :off");
		p.addValue("lim", limit);
		p.addValue("off", offset);
		List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), p);
		List<Map<String, Object>> out = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("item_id", row.get("item_id"));
			m.put("item_name", row.get("item_name"));
			m.put("price", row.get("price"));
			m.put("market_price", row.get("market_price"));
			m.put("store", row.get("store"));
			m.put("distributor_id", row.get("distributor_id"));
			m.put("pics", firstPicString(row.get("pics")));
			out.add(m);
		}
		return out;
	}

	private String firstPicString(Object picsRaw) {
		if (picsRaw == null) {
			return "";
		}
		String s = String.valueOf(picsRaw).trim();
		if (!StringUtils.hasText(s)) {
			return "";
		}
		try {
			JsonNode n = objectMapper.readTree(s);
			if (n != null && n.isArray() && n.size() > 0) {
				JsonNode first = n.get(0);
				if (first != null && first.isTextual()) {
					return first.asText();
				}
				return first == null ? "" : first.toString();
			}
		} catch (Exception ignored) {
			return s;
		}
		return s;
	}
}
