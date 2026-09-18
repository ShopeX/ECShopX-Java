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
public class WxappItemsSearchDistributorIdsJdbcRepository {

	private final NamedParameterJdbcTemplate jdbc;

	public WxappItemsSearchDistributorIdsJdbcRepository(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public List<Long> listDistributorIdsByCompanyAndItemNameLike(long companyId, String itemNameLikeEscaped) {
		String sql =
				"""
				SELECT DISTINCT i.distributor_id
				FROM items i
				WHERE i.company_id = :companyId
				AND i.item_id = 'default_item_id'
				AND i.item_name LIKE CONCAT('%', :itemName, '%') ESCAPE '\\\\'
				AND i.audit_status = 'approved'
				AND i.approve_status = 'onsale'
				""";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("companyId", companyId);
		p.addValue("itemName", itemNameLikeEscaped);
		return jdbc.query(sql, p, (rs, i) -> rs.getLong("distributor_id"));
	}
}
