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
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorListOutsideLangReadService {

	private static final List<String> LANG_FIELDS =
			List.of("name", "contact", "logo", "province", "city", "area", "address", "introduce");

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public DistributorListOutsideLangReadService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	public void overlayNameLogoFromOutsideLang(long companyId, String requestLangTag, List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		List<Long> ids = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			Long did = longOrNull(row.get("distributor_id"));
			if (did != null && did > 0L) {
				ids.add(did);
			}
		}
		if (ids.isEmpty()) {
			return;
		}
		Map<Long, Map<String, String>> byDid = loadNameLogoByDistributorIds(companyId, requestLangTag, ids);
		for (Map<String, Object> row : rows) {
			Long did = longOrNull(row.get("distributor_id"));
			if (did == null || did <= 0L) {
				continue;
			}
			Map<String, String> perField = byDid.get(did);
			if (perField == null || perField.isEmpty()) {
				continue;
			}
			String name = perField.get("name");
			if (StringUtils.hasText(name)) {
				row.put("name", name);
			}
			String logo = perField.get("logo");
			if (StringUtils.hasText(logo)) {
				row.put("logo", logo);
			}
		}
	}

	public void overlayOpenapiListFields(long companyId, String requestLangTag, List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		List<Long> ids = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			Long did = longOrNull(row.get("distributor_id"));
			if (did != null && did > 0L) {
				ids.add(did);
			}
		}
		if (ids.isEmpty()) {
			return;
		}
		String tableSuffix = DistributorMultiLangWriteService.normalizeLangTableSuffix(requestLangTag);
		String outsideTable = "outside_item_multi_lang_mod_lang_" + tableSuffix;
		Map<Long, Map<String, String>> byDid = new LinkedHashMap<>();
		loadFromOutsideTable(companyId, ids, outsideTable, byDid);
		for (Map<String, Object> row : rows) {
			Long did = longOrNull(row.get("distributor_id"));
			if (did == null || did <= 0L) {
				continue;
			}
			Map<String, String> perField = byDid.get(did);
			if (perField == null || perField.isEmpty()) {
				continue;
			}
			for (String field : LANG_FIELDS) {
				String val = perField.get(field);
				if (StringUtils.hasText(val)) {
					row.put(field, val);
				}
			}
		}
	}

	public void applyLangMaps(long companyId, String requestLangTag, List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		List<Long> ids = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			Long did = longOrNull(row.get("distributor_id"));
			if (did != null && did > 0L) {
				ids.add(did);
			}
		}
		if (ids.isEmpty()) {
			return;
		}
		String langKey = displayLangKey(requestLangTag);
		String tableSuffix = DistributorMultiLangWriteService.normalizeLangTableSuffix(requestLangTag);
		String outsideTable = "outside_item_multi_lang_mod_lang_" + tableSuffix;

		Map<Long, Map<String, String>> byDid = new LinkedHashMap<>();
		loadFromOutsideTable(companyId, ids, outsideTable, byDid);

		for (Map<String, Object> row : rows) {
			Long did = longOrNull(row.get("distributor_id"));
			if (did == null || did <= 0L) {
				continue;
			}
			Map<String, String> perField = byDid.get(did);
			if (perField == null || perField.isEmpty()) {
				continue;
			}
			for (String field : LANG_FIELDS) {
				String val = perField.get(field);
				if (!StringUtils.hasText(val)) {
					continue;
				}
				row.put(field, val);
				Map<String, Object> langMap = new LinkedHashMap<>();
				langMap.put(langKey, val);
				row.put(field + "_lang", langMap);
			}
		}
	}

	private Map<Long, Map<String, String>> loadNameLogoByDistributorIds(
			long companyId, String requestLangTag, List<Long> distributorIds) {
		Map<Long, Map<String, String>> byDid = new LinkedHashMap<>();
		if (distributorIds == null || distributorIds.isEmpty()) {
			return byDid;
		}
		List<String> fields = List.of("name", "logo");
		String tableSuffix = DistributorMultiLangWriteService.normalizeLangTableSuffix(requestLangTag);
		String outsideTable = "outside_item_multi_lang_mod_lang_" + tableSuffix;
		String sqlOutside =
				"SELECT data_id, `field`, attribute_value FROM "
						+ outsideTable
						+ " WHERE company_id = :company_id AND table_name = :table_name AND module_name = :module_name "
						+ "AND data_id IN (:ids) AND `field` IN (:fields)";
		MapSqlParameterSource pOut = new MapSqlParameterSource();
		pOut.addValue("company_id", companyId);
		pOut.addValue("table_name", "distribution_distributor");
		pOut.addValue("module_name", "distribution_distributor");
		pOut.addValue("ids", distributorIds);
		pOut.addValue("fields", fields);
		try {
			namedParameterJdbcTemplate.query(
					sqlOutside,
					pOut,
					rs -> {
						long dataId = rs.getLong("data_id");
						String field = rs.getString("field");
						String av = rs.getString("attribute_value");
						if (!StringUtils.hasText(field)) {
							return;
						}
						byDid.computeIfAbsent(dataId, k -> new LinkedHashMap<>()).put(field, av != null ? av : "");
					});
		} catch (DataAccessException ignored) {
		}
		return byDid;
	}

	private void loadFromOutsideTable(
			long companyId, List<Long> ids, String outsideTable, Map<Long, Map<String, String>> byDid) {
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
		p.addValue("fields", LANG_FIELDS);
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
						byDid.computeIfAbsent(dataId, k -> new LinkedHashMap<>()).put(field, av != null ? av : "");
					});
		} catch (DataAccessException ignored) {
			// Per-locale table may be absent in some environments.
		}
	}

	private static String displayLangKey(String requestLangTag) {
		if (!StringUtils.hasText(requestLangTag)) {
			return "zh-CN";
		}
		return requestLangTag.trim();
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
