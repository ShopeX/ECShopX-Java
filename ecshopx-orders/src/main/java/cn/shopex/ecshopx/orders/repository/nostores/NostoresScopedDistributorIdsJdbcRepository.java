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

package cn.shopex.ecshopx.orders.repository.nostores;

import cn.shopex.ecshopx.orders.service.nostores.dto.NostoresScopedDistributorFilter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class NostoresScopedDistributorIdsJdbcRepository {

	private final NamedParameterJdbcTemplate jdbc;

	public NostoresScopedDistributorIdsJdbcRepository(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public List<Long> listDistributorIdsByScope(NostoresScopedDistributorFilter f) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT d.distributor_id FROM distribution_distributor d WHERE d.company_id = :companyId ");
		sql.append("AND d.is_valid = 'true' ");
		if (f.excludeDistributorSelf()) {
			sql.append("AND d.distributor_self = 0 ");
		}
		MapSqlParameterSource p = new MapSqlParameterSource("companyId", f.companyId());
		if (f.isZiti() != null) {
			sql.append("AND d.is_ziti = :isZiti ");
			p.addValue("isZiti", f.isZiti());
		}
		if (f.isDelivery() != null) {
			sql.append("AND d.is_delivery = :isDelivery ");
			p.addValue("isDelivery", f.isDelivery());
		}
		if (f.isDada() != null) {
			sql.append("AND d.is_dada = :isDada ");
			p.addValue("isDada", f.isDada());
		}
		if (f.requireShopIdNonEmpty()) {
			sql.append("AND (d.shop_id IS NOT NULL AND d.shop_id <> 0 AND d.shop_id <> '') ");
		}
		if (StringUtils.hasText(f.provinceLikeEscaped())) {
			sql.append("AND d.province LIKE CONCAT('%', :provinceEsc, '%') ESCAPE '\\\\' ");
			p.addValue("provinceEsc", f.provinceLikeEscaped());
		}
		if (StringUtils.hasText(f.cityLikeEscaped())) {
			sql.append("AND d.city LIKE CONCAT('%', :cityEsc, '%') ESCAPE '\\\\' ");
			p.addValue("cityEsc", f.cityLikeEscaped());
		}
		if (StringUtils.hasText(f.areaLikeEscaped())) {
			sql.append("AND d.area LIKE CONCAT('%', :areaEsc, '%') ESCAPE '\\\\' ");
			p.addValue("areaEsc", f.areaLikeEscaped());
		}
		List<Long> inList = f.distributorIdInList();
		if (inList == null) {
			// no IN
		} else if (inList.isEmpty()) {
			sql.append("AND 1 = 0 ");
		} else {
			sql.append("AND d.distributor_id IN (:ids) ");
			p.addValue("ids", inList);
		}
		Integer st = f.searchType();
		String nameEsc = f.nameLikeEscaped();
		if (st != null && StringUtils.hasText(nameEsc)) {
			if (st.intValue() == 1) {
				sql.append("AND ( d.name LIKE CONCAT('%', :nameEsc, '%') ESCAPE '\\\\' ");
				p.addValue("nameEsc", nameEsc);
				List<Long> itemDids = f.orDistributorIdsFromItems();
				if (itemDids != null && !itemDids.isEmpty()) {
					sql.append("OR d.distributor_id IN (:itemDids) ");
					p.addValue("itemDids", itemDids);
				} else {
					sql.append("OR 1 = 0 ");
				}
				sql.append(") ");
			} else if (st.intValue() == 2) {
				sql.append(
						"AND ( d.name LIKE CONCAT('%', :nameEsc2, '%') ESCAPE '\\\\' OR d.address LIKE CONCAT('%', :nameEsc2, '%') ESCAPE '\\\\' ) ");
				p.addValue("nameEsc2", nameEsc);
			} else {
				sql.append("AND d.name LIKE CONCAT('%', :nameEsc3, '%') ESCAPE '\\\\' ");
				p.addValue("nameEsc3", nameEsc);
			}
		}
		sql.append("ORDER BY d.distributor_id ASC");
		return jdbc.query(
				sql.toString(),
				p,
				(rs, i) -> rs.getLong("distributor_id"));
	}
}
