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

package cn.shopex.ecshopx.wsugc.service.badge;

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
import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;

@Service
public class BadgeOutsideLangReadService {

	private static final String TABLE_NAME = "wsugc_badge";
	private static final String MODULE_NAME = "wsugc_badge";
	private static final List<String> FIELDS =
			List.of("badge_name", "badge_memo", "ai_refuse_reason", "manual_refuse_reason");

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public BadgeOutsideLangReadService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	/**
	 * Resolves {@code data_id} values from the per-locale outside lang table where {@code attribute_value} matches
	 * the substring. Dimensions match {@link #loadFromOutsideTable}: same tenant, table, and module. When non-empty,
	 * {@code BadgeListService} filters by {@code badge_id IN} only (多语言 {@code filterByLang} 逻辑); otherwise main-table
	 * {@code LIKE} is used alone.
	 */
	public List<Long> findDataIdsByFieldContains(
			long companyId, String requestLangTag, String fieldName, String contains) {
		if (!StringUtils.hasText(fieldName) || !StringUtils.hasText(contains)) {
			return List.of();
		}
		String suffix = normalizeLangTableSuffix(requestLangTag);
		String outsideTable = "outside_item_multi_lang_mod_lang_" + suffix;
		String likePattern = "%" + escapeLike(contains.trim()) + "%";
		String sql =
				"SELECT DISTINCT data_id FROM "
						+ outsideTable
						+ " WHERE company_id = :company_id AND table_name = :table_name AND module_name = :module_name "
						+ "AND `field` = :field AND attribute_value LIKE :like_pattern";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", companyId);
		p.addValue("table_name", TABLE_NAME);
		p.addValue("module_name", MODULE_NAME);
		p.addValue("field", fieldName.trim());
		p.addValue("like_pattern", likePattern);
		Set<Long> ids = new LinkedHashSet<>();
		try {
			namedParameterJdbcTemplate.query(
					sql,
					p,
					rs -> {
						long id = rs.getLong("data_id");
						if (!rs.wasNull() && id > 0L) {
							ids.add(id);
						}
					});
		} catch (DataAccessException ignored) {
			return List.of();
		}
		return new ArrayList<>(ids);
	}

	private static String escapeLike(String s) {
		return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	public void applyToRowMap(long companyId, String requestLangTag, Map<String, Object> rowMap) {
		if (rowMap == null || rowMap.isEmpty()) {
			return;
		}
		Long badgeId = longOrNull(rowMap.get("badge_id"));
		if (badgeId == null || badgeId <= 0L) {
			return;
		}
		String suffix = normalizeLangTableSuffix(requestLangTag);
		String outsideTable = "outside_item_multi_lang_mod_lang_" + suffix;
		Map<String, String> perField = new LinkedHashMap<>();
		loadFromOutsideTable(companyId, badgeId, outsideTable, perField);
		String langKey =
				StringUtils.hasText(requestLangTag) ? requestLangTag.trim() : "zh-CN";
		for (String field : FIELDS) {
			String val = perField.get(field);
			if (StringUtils.hasText(val)) {
				rowMap.put(field, val);
				LinkedHashMap<String, String> langMap = new LinkedHashMap<>();
				langMap.put(langKey, val);
				rowMap.put(field + "_lang", langMap);
			}
		}
	}

	private void loadFromOutsideTable(
			long companyId, long badgeId, String outsideTable, Map<String, String> perField) {
		String sql =
				"SELECT data_id, `field`, attribute_value FROM "
						+ outsideTable
						+ " WHERE company_id = :company_id AND table_name = :table_name AND module_name = :module_name "
						+ "AND data_id = :data_id AND `field` IN (:fields)";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", companyId);
		p.addValue("table_name", TABLE_NAME);
		p.addValue("module_name", MODULE_NAME);
		p.addValue("data_id", badgeId);
		p.addValue("fields", FIELDS);
		try {
			namedParameterJdbcTemplate.query(
					sql,
					p,
					rs -> {
						String field = rs.getString("field");
						String av = rs.getString("attribute_value");
						if (!StringUtils.hasText(field)) {
							return;
						}
						perField.put(field, av != null ? av : "");
					});
		} catch (DataAccessException ignored) {
		}
	}

	private static String normalizeLangTableSuffix(String langTag) {
		return OutsideMultiLangTableSupport.normalizeLangTableSuffix(langTag);
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
