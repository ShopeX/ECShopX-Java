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

package cn.shopex.ecshopx.promotions.service.multilang;

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.distribution.service.DistributorMultiLangWriteService;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LimitPromotionListDistributorNameReadService {

	private static final List<String> NAME_FIELD = List.of("name");

	private final DistributorListQueryService distributorListQueryService;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public LimitPromotionListDistributorNameReadService(
			DistributorListQueryService distributorListQueryService,
			NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.distributorListQueryService = distributorListQueryService;
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	public Map<Long, String> mapDistributorIdToDisplayName(
			long companyId, List<Long> distributorIds, String requestLangTag) {
		LinkedHashMap<Long, String> out = new LinkedHashMap<>();
		if (distributorIds == null || distributorIds.isEmpty()) {
			return out;
		}
		Set<Long> idSet = new LinkedHashSet<>();
		for (Long id : distributorIds) {
			if (id != null && id > 0L) {
				idSet.add(id);
			}
		}
		if (idSet.isEmpty()) {
			return out;
		}
		List<Long> ids = List.copyOf(idSet);
		List<Distributor> distributors = distributorListQueryService.listByIdsAndCompany(companyId, ids);
		for (Distributor d : distributors) {
			if (d.getDistributorId() == null) {
				continue;
			}
			String n = d.getName();
			out.put(d.getDistributorId(), n != null ? n : "");
		}
		for (Long id : ids) {
			out.putIfAbsent(id, "");
		}

		String tableSuffix = DistributorMultiLangWriteService.normalizeLangTableSuffix(requestLangTag);
		String outsideTable = "outside_item_multi_lang_mod_lang_" + tableSuffix;
		loadFromOutsideTable(companyId, ids, outsideTable, out);
		return out;
	}

	private void loadFromOutsideTable(
			long companyId, List<Long> ids, String outsideTable, Map<Long, String> byId) {
		String sql =
				"SELECT data_id, `field`, attribute_value FROM "
						+ outsideTable
						+ " WHERE company_id = :company_id AND table_name = :table_name AND module_name = :module_name "
						+ "AND data_id IN (:ids) AND `field` IN (:fields)";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", companyId);
		p.addValue("table_name", "distribution_distributor");
		p.addValue("module_name", "distribution_distributor");
		p.addValue("ids", ids);
		p.addValue("fields", NAME_FIELD);
		try {
			namedParameterJdbcTemplate.query(
					sql,
					p,
					rs -> {
						if (!"name".equals(rs.getString("field"))) {
							return;
						}
						long dataId = rs.getLong("data_id");
						String av = rs.getString("attribute_value");
						if (StringUtils.hasText(av)) {
							byId.put(dataId, av);
						}
					});
		} catch (DataAccessException ignored) {
		}
	}
}
