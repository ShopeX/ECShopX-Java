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

/**
 * Writes {@code pages_template_set.tab_bar} overlays to {@code outside_item_multi_lang_mod_lang_*} —
 * same physical tables as {@link PagesTemplateSetOutsideTabBarReadService} (PHP {@code MultiLangItem('other')}).
 */
@Service
public class PagesTemplateSetOutsideTabBarWriteService {

	private static final String OUTSIDE_LANG_TABLE_PREFIX = "outside_item_multi_lang_mod_lang_";
	private static final String TABLE_PAGES_TEMPLATE_SET = "pages_template_set";
	private static final String MODULE_PAGES_TEMPLATE_SET = "pages_template_set";
	private static final String FIELD_TAB_BAR = "tab_bar";

	private static final String LANG_ZH_CN = "zh-CN";
	private static final String LANG_EN_CN = "en-CN";
	private static final String LANG_AR_SA = "ar-SA";

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public PagesTemplateSetOutsideTabBarWriteService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	/** Upsert {@code tab_bar} for one locale; no-op for unknown / blank lang. */
	public void upsertTabBar(int companyId, long dataId, String tabBar, String requestLang) {
		if (dataId <= 0L || !StringUtils.hasText(tabBar)) {
			return;
		}
		String canonical = canonicalCommonLangLocaleTag(requestLang);
		if (canonical == null) {
			return;
		}
		String physicalTable = OUTSIDE_LANG_TABLE_PREFIX + normalizeLangTableSuffix(canonical);
		int now = (int) (System.currentTimeMillis() / 1000L);
		String value = tabBar.trim();

		try {
			deleteField(physicalTable, companyId, dataId);
			insertRow(physicalTable, companyId, dataId, value, canonical, now);
		} catch (DataAccessException ignored) {
			// per-locale table may be absent
		}
	}

	private void deleteField(String physicalTable, int companyId, long dataId) {
		String sql =
				"DELETE FROM `"
						+ physicalTable
						+ "` WHERE company_id = :company_id AND table_name = :table_name AND module_name = :module_name "
						+ "AND data_id = :data_id AND `field` = :field";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", companyId);
		p.addValue("table_name", TABLE_PAGES_TEMPLATE_SET);
		p.addValue("module_name", MODULE_PAGES_TEMPLATE_SET);
		p.addValue("data_id", dataId);
		p.addValue("field", FIELD_TAB_BAR);
		namedParameterJdbcTemplate.update(sql, p);
	}

	private void insertRow(
			String physicalTable, int companyId, long dataId, String value, String langTag, int now) {
		String sql =
				"INSERT INTO `"
						+ physicalTable
						+ "` (company_id, field, attribute_value, table_name, module_name, data_id, lang, created, updated) "
						+ "VALUES (:company_id, :field, :attribute_value, :table_name, :module_name, :data_id, :lang, :created, :updated)";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", companyId);
		p.addValue("field", FIELD_TAB_BAR);
		p.addValue("attribute_value", value);
		p.addValue("table_name", TABLE_PAGES_TEMPLATE_SET);
		p.addValue("module_name", MODULE_PAGES_TEMPLATE_SET);
		p.addValue("data_id", dataId);
		p.addValue("lang", langTag);
		p.addValue("created", now);
		p.addValue("updated", now);
		namedParameterJdbcTemplate.update(sql, p);
	}

	private static String canonicalCommonLangLocaleTag(String langRaw) {
		if (langRaw == null) {
			return null;
		}
		String t = langRaw.trim();
		if (LANG_ZH_CN.equalsIgnoreCase(t)) {
			return LANG_ZH_CN;
		}
		if (LANG_EN_CN.equalsIgnoreCase(t)) {
			return LANG_EN_CN;
		}
		if (LANG_AR_SA.equalsIgnoreCase(t)) {
			return LANG_AR_SA;
		}
		return null;
	}

	private static String normalizeLangTableSuffix(String langTag) {
		return OutsideMultiLangTableSupport.normalizeLangTableSuffix(langTag);
	}
}
