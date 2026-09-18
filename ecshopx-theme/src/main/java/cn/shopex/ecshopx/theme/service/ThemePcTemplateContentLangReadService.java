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

package cn.shopex.ecshopx.theme.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;

/**
 * Read-side multi-language helpers for {@code theme_pc_template_content}: outside-item LIKE resolution and per-row
 * field overlays backed by local {@code outside_item_multi_lang_mod_lang_*} rows.
 */
@Service
@RequiredArgsConstructor
public class ThemePcTemplateContentLangReadService {

	private static final String TABLE_THEME_PC_TEMPLATE_CONTENT = "theme_pc_template_content";
	private static final String MODULE_THEME_PC_TEMPLATE_CONTENT = "theme_pc_template_content";
	private static final String FIELD_THEME_NAME = "name";
	private static final List<String> THEME_CONTENT_DETAIL_LANG_FIELDS = List.of("name", "params");
	private static final String OUTSIDE_LANG_TABLE_PREFIX = "outside_item_multi_lang_mod_lang_";

	private static final String LANG_ZH_CN = "zh-CN";
	private static final String LANG_EN_CN = "en-CN";
	private static final String LANG_AR_SA = "ar-SA";

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	/**
	 * Resolves {@code theme_pc_template_content} row ids whose {@code name} translation in the outside mod table for
	 * the request locale contains {@code nameNeedle}. When {@code nameNeedle} is the empty string, the match pattern
	 * is {@code %%}, consistent with attribute_value contains on an empty needle.
	 *
	 * @param pageName raw query value; {@code null} means the parameter was absent — caller must not apply this path
	 *     and should use a main-table {@code name IS NULL} filter instead.
	 */
	public List<Long> filterThemePcTemplateContentIdsByNameLangContains(
			int companyId, String requestLocaleTag, String pageName) {
		if (pageName == null) {
			return List.of();
		}
		String primary = extractPrimaryLanguageTag(requestLocaleTag == null ? "" : requestLocaleTag);
		String canonical = canonicalCommonLangLocaleTag(primary);
		if (canonical == null) {
			if (pageName.isEmpty()) {
				canonical = LANG_ZH_CN;
			} else {
				return List.of();
			}
		}
		String likePat = "%" + escapeLike(pageName) + "%";
		String outsideTable = OUTSIDE_LANG_TABLE_PREFIX + normalizeLangTableSuffix(canonical);
		String sql =
				"SELECT id, data_id FROM "
						+ outsideTable
						+ " WHERE company_id = :company_id AND table_name = :table_name AND `field` = :field_name "
						+ "AND attribute_value LIKE :like_pat ORDER BY id ASC";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", (long) companyId);
		p.addValue("table_name", TABLE_THEME_PC_TEMPLATE_CONTENT);
		p.addValue("field_name", FIELD_THEME_NAME);
		p.addValue("like_pat", likePat);
		LinkedHashSet<Long> out = new LinkedHashSet<>();
		try {
			namedParameterJdbcTemplate.query(
					sql,
					p,
					rs -> {
						long dataId = rs.getLong("data_id");
						if (dataId > 0L) {
							out.add(dataId);
						}
					});
		} catch (DataAccessException ignored) {
			return List.of();
		}
		return new ArrayList<>(out);
	}

	public void applyThemePcTemplateContentDetailLangOverlay(
			long companyId, Map<String, Object> row, String requestLocaleTag) {
		if (row == null || row.isEmpty()) {
			return;
		}
		long dataId = parsePositiveLong(row.get("theme_pc_template_content_id"));
		if (dataId <= 0L) {
			return;
		}
		String langRaw = requestLocaleTag == null ? "" : requestLocaleTag.trim();
		langRaw = extractPrimaryLanguageTag(langRaw);
		String canonicalTag = canonicalCommonLangLocaleTag(langRaw);
		if (canonicalTag == null) {
			canonicalTag = LANG_ZH_CN;
		}
		for (String field : THEME_CONTENT_DETAIL_LANG_FIELDS) {
			String base = row.get(field) == null ? "" : String.valueOf(row.get(field));
			String mod = loadFieldModLastNonEmpty(companyId, dataId, canonicalTag, field);
			if (StringUtils.hasText(mod)) {
				row.put(field, mod);
			}
			String effective = StringUtils.hasText(mod) ? mod : base;
			Map<String, Object> langMap = new LinkedHashMap<>();
			langMap.put(canonicalTag, effective);
			row.put(field + "_lang", langMap);
		}
	}

	private String loadFieldModLastNonEmpty(long companyId, long dataId, String canonicalTag, String fieldName) {
		String table = OUTSIDE_LANG_TABLE_PREFIX + normalizeLangTableSuffix(canonicalTag);
		String sql =
				"SELECT attribute_value FROM "
						+ table
						+ " WHERE company_id = :company_id AND table_name = :table_name AND module_name = :module_name "
						+ "AND data_id = :data_id AND `field` = :field ORDER BY id ASC";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", companyId);
		p.addValue("table_name", TABLE_THEME_PC_TEMPLATE_CONTENT);
		p.addValue("module_name", MODULE_THEME_PC_TEMPLATE_CONTENT);
		p.addValue("data_id", dataId);
		p.addValue("field", fieldName);
		String[] lastNonEmpty = {""};
		try {
			namedParameterJdbcTemplate.query(
					sql,
					p,
					rs -> {
						String av = rs.getString("attribute_value");
						if (StringUtils.hasText(av)) {
							lastNonEmpty[0] = av;
						}
					});
		} catch (DataAccessException ignored) {
			return "";
		}
		return lastNonEmpty[0];
	}

	private static long parsePositiveLong(Object idObj) {
		if (idObj == null) {
			return 0L;
		}
		if (idObj instanceof Number idNum) {
			return idNum.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(idObj).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String extractPrimaryLanguageTag(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "";
		}
		String s = raw.trim();
		int comma = s.indexOf(',');
		if (comma >= 0) {
			s = s.substring(0, comma).trim();
		}
		int semi = s.indexOf(';');
		if (semi >= 0) {
			s = s.substring(0, semi).trim();
		}
		return s;
	}

	private static String canonicalCommonLangLocaleTag(String langRaw) {
		if (LANG_ZH_CN.equalsIgnoreCase(langRaw)) {
			return LANG_ZH_CN;
		}
		if (LANG_EN_CN.equalsIgnoreCase(langRaw)) {
			return LANG_EN_CN;
		}
		if (LANG_AR_SA.equalsIgnoreCase(langRaw)) {
			return LANG_AR_SA;
		}
		return null;
	}

	private static String normalizeLangTableSuffix(String langTag) {
		return OutsideMultiLangTableSupport.normalizeLangTableSuffix(langTag);
	}

	private static String escapeLike(String needle) {
		if (needle == null) {
			return "";
		}
		return needle
				.replace("\\", "\\\\")
				.replace("%", "\\%")
				.replace("_", "\\_");
	}
}
