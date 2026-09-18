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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class WxappDiscountCardDistributorIdsJdbcRepository {

	private final NamedParameterJdbcTemplate jdbc;

	public WxappDiscountCardDistributorIdsJdbcRepository(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public List<Long> listDistributorIdsByCompanyAndCardId(long companyId, long cardId) {
		String sql = "SELECT distributor_id FROM kaquan_discount_cards WHERE company_id = :companyId AND card_id = :cardId LIMIT 1";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("companyId", companyId);
		p.addValue("cardId", cardId);
		List<Map<String, Object>> rows = jdbc.queryForList(sql, p);
		if (rows.isEmpty()) {
			return List.of();
		}
		Object v = rows.get(0).get("distributor_id");
		if (v == null) {
			return List.of();
		}
		String raw = String.valueOf(v).trim();
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		String trimmed = raw.replaceAll("^,+|,+$", "");
		if (!StringUtils.hasText(trimmed)) {
			return List.of();
		}
		Set<Long> out = new LinkedHashSet<>();
		for (String part : trimmed.split(",")) {
			String t = part.trim();
			if (!StringUtils.hasText(t)) {
				continue;
			}
			try {
				out.add(Long.parseLong(t));
			} catch (NumberFormatException ignored) {
				// skip invalid token
			}
		}
		return new ArrayList<>(out);
	}
}
