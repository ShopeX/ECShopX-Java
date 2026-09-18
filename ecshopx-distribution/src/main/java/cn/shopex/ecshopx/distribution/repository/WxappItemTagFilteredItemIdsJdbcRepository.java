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

import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WxappItemTagFilteredItemIdsJdbcRepository {

	private final NamedParameterJdbcTemplate jdbc;

	public WxappItemTagFilteredItemIdsJdbcRepository(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public List<Long> listItemIdsByCompanyAndTagIds(long companyId, List<Long> tagIds) {
		if (tagIds == null || tagIds.isEmpty()) {
			return List.of();
		}
		String sql =
				"""
				SELECT item_id FROM items_rel_tags
				WHERE company_id = :companyId AND tag_id IN (:tagIds)
				ORDER BY item_id
				""";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("companyId", companyId);
		p.addValue("tagIds", tagIds);
		return jdbc.query(sql, p, (rs, i) -> rs.getLong("item_id"));
	}
}
