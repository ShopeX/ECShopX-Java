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

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;

@Service
public class PagesTemplateSetOutsideTabBarReadService {

	private static final String OUTSIDE_LANG_TABLE_PREFIX = "outside_item_multi_lang_mod_lang_";

	private static final String TABLE_PAGES_TEMPLATE_SET = "pages_template_set";
	private static final String MODULE_PAGES_TEMPLATE_SET = "pages_template_set";
	private static final String FIELD_TAB_BAR = "tab_bar";

	private static final String LANG_ZH_CN = "zh-CN";
	private static final String LANG_EN_CN = "en-CN";
	private static final String LANG_AR_SA = "ar-SA";

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public PagesTemplateSetOutsideTabBarReadService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	/**
	 * Reads {@code tab_bar} overlay from the per-locale outside mod table. Rows are ordered by {@code id ASC}; the
	 * last non-empty {@code attribute_value} wins.
	 */
	public String findTabBarOverlay(int companyId, long dataId, String requestLang) {
		if (requestLang == null) {
			return null;
		}
		String lang = requestLang.trim();
		if (!LANG_ZH_CN.equalsIgnoreCase(lang)
				&& !LANG_EN_CN.equalsIgnoreCase(lang)
				&& !LANG_AR_SA.equalsIgnoreCase(lang)) {
			return null;
		}
		String canonicalLang = canonicalCommonLangLocaleTag(lang);
		if (canonicalLang == null) {
			return null;
		}
		String physicalTable = OUTSIDE_LANG_TABLE_PREFIX + normalizeLangTableSuffix(canonicalLang);
		String sql =
				"SELECT attribute_value FROM "
						+ physicalTable
						+ " WHERE company_id = :company_id AND table_name = :table_name "
						+ "AND module_name = :module_name AND `field` = :field AND data_id = :data_id "
						+ "ORDER BY id ASC";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", companyId);
		p.addValue("table_name", TABLE_PAGES_TEMPLATE_SET);
		p.addValue("module_name", MODULE_PAGES_TEMPLATE_SET);
		p.addValue("field", FIELD_TAB_BAR);
		p.addValue("data_id", dataId);
		final String[] lastNonEmpty = new String[1];
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
			return null;
		}
		return lastNonEmpty[0];
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
}
