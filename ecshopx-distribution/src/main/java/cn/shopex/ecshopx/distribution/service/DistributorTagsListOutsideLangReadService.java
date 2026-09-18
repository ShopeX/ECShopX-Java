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

package cn.shopex.ecshopx.distribution.service;

import java.util.ArrayList;
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
public class DistributorTagsListOutsideLangReadService {

	private static final String TABLE_MODULE = "distributor_tags";
	private static final List<String> FIELDS = List.of("tag_name", "description");

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public DistributorTagsListOutsideLangReadService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	public void applyTagFields(long companyId, String requestLangTag, List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		Set<Long> idSet = new LinkedHashSet<>();
		for (Map<String, Object> row : rows) {
			Long id = longOrNull(row.get("tag_id"));
			if (id != null && id > 0L) {
				idSet.add(id);
			}
		}
		if (idSet.isEmpty()) {
			return;
		}
		List<Long> ids = new ArrayList<>(idSet);
		String tableSuffix = DistributorMultiLangWriteService.normalizeLangTableSuffix(requestLangTag);
		String outsideTable = "outside_item_multi_lang_mod_lang_" + tableSuffix;

		Map<Long, Map<String, String>> byTagId = new LinkedHashMap<>();
		loadFromOutsideTable(companyId, ids, outsideTable, byTagId);

		for (Map<String, Object> row : rows) {
			Long tagId = longOrNull(row.get("tag_id"));
			if (tagId == null || tagId <= 0L) {
				continue;
			}
			Map<String, String> perField = byTagId.get(tagId);
			if (perField == null || perField.isEmpty()) {
				continue;
			}
			for (String field : FIELDS) {
				String val = perField.get(field);
				if (!StringUtils.hasText(val)) {
					continue;
				}
				row.put(field, val);
			}
		}
	}

	private void loadFromOutsideTable(
			long companyId, List<Long> ids, String outsideTable, Map<Long, Map<String, String>> byTagId) {
		String sql =
				"SELECT data_id, `field`, attribute_value FROM "
						+ outsideTable
						+ " WHERE company_id = :company_id AND table_name = :table_name AND module_name = :module_name "
						+ "AND data_id IN (:ids) AND `field` IN (:fields)";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", companyId);
		p.addValue("table_name", TABLE_MODULE);
		p.addValue("module_name", TABLE_MODULE);
		p.addValue("ids", ids);
		p.addValue("fields", FIELDS);
		try {
			namedParameterJdbcTemplate.query(
					sql,
					p,
					rs -> {
						long dataId = rs.getLong("data_id");
						String field = rs.getString("field");
						String av = rs.getString("attribute_value");
						if (!StringUtils.hasText(field)) {
							return;
						}
						byTagId.computeIfAbsent(dataId, k -> new LinkedHashMap<>()).put(field, av != null ? av : "");
					});
		} catch (DataAccessException ignored) {
		}
	}

	private static Long longOrNull(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
