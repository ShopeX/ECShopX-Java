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

package cn.shopex.ecshopx.wechat.support;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;
import java.util.Collection;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Keeps {@code outside_item_multi_lang_mod_lang_*} {@code params} in sync when admin saves
 * {@code wechat_weapp_setting}. H5/C端 reads overlay via {@link WeappSettingOutsideLangParamsLoader}.
 */
@Component
public class WeappSettingOutsideLangParamsWriter {

	private static final String OUTSIDE_LANG_TABLE_PREFIX = "outside_item_multi_lang_mod_lang_";

	private static final String TABLE_WECHAT_WEAPP_SETTING = "wechat_weapp_setting";

	private static final String FIELD_PARAMS = "params";

	private static final String MODULE_WECHAT_WEAPP_SETTING = "wechat_weapp_setting";

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	private final LangueProperties langueProperties;

	public WeappSettingOutsideLangParamsWriter(
			NamedParameterJdbcTemplate namedParameterJdbcTemplate, LangueProperties langueProperties) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
		this.langueProperties = langueProperties;
	}

	/** Sync default-locale overlay so C端 {@code country_code=zh-CN} reads the same params as admin save. */
	public void syncDefaultLangParams(long companyId, long weappSettingId, String serializedParams) {
		if (!StringUtils.hasText(serializedParams)) {
			return;
		}
		syncLangParams(companyId, weappSettingId, serializedParams, langueProperties.getDefaultLang());
	}

	public void deleteLangParamsForWeappSettingIds(long companyId, Collection<Long> weappSettingIds) {
		if (weappSettingIds == null || weappSettingIds.isEmpty()) {
			return;
		}
		List<String> langs = langueProperties.getList();
		if (langs == null || langs.isEmpty()) {
			return;
		}
		for (String lang : langs) {
			if (!StringUtils.hasText(lang)) {
				continue;
			}
			String table = OUTSIDE_LANG_TABLE_PREFIX + normalizeLangTableSuffix(lang.trim());
			String sql =
					"DELETE FROM "
							+ table
							+ " WHERE company_id = :company_id AND table_name = :table_name AND `field` = :field "
							+ "AND data_id IN (:ids)";
			MapSqlParameterSource p = new MapSqlParameterSource();
			p.addValue("company_id", companyId);
			p.addValue("table_name", TABLE_WECHAT_WEAPP_SETTING);
			p.addValue("field", FIELD_PARAMS);
			p.addValue("ids", weappSettingIds);
			try {
				namedParameterJdbcTemplate.update(sql, p);
			} catch (DataAccessException ignored) {
				// per-locale table may be absent
			}
		}
	}

	private void syncLangParams(long companyId, long weappSettingId, String serializedParams, String langTag) {
		if (!StringUtils.hasText(langTag)) {
			return;
		}
		String table = OUTSIDE_LANG_TABLE_PREFIX + normalizeLangTableSuffix(langTag.trim());
		deleteParamsOverlay(table, companyId, weappSettingId);
		insertParamsOverlay(table, companyId, weappSettingId, serializedParams);
	}

	private void deleteParamsOverlay(String outsideTable, long companyId, long weappSettingId) {
		String sql =
				"DELETE FROM "
						+ outsideTable
						+ " WHERE company_id = :company_id AND table_name = :table_name AND `field` = :field "
						+ "AND data_id = :data_id";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", companyId);
		p.addValue("table_name", TABLE_WECHAT_WEAPP_SETTING);
		p.addValue("field", FIELD_PARAMS);
		p.addValue("data_id", weappSettingId);
		try {
			namedParameterJdbcTemplate.update(sql, p);
		} catch (DataAccessException ignored) {
			// per-locale table may be absent
		}
	}

	private void insertParamsOverlay(
			String outsideTable, long companyId, long weappSettingId, String serializedParams) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		String sql =
				"INSERT INTO "
						+ outsideTable
						+ " (company_id, field, attribute_value, table_name, module_name, data_id, created, updated) "
						+ "VALUES (:company_id, :field, :attribute_value, :table_name, :module_name, :data_id, :created, :updated)";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", companyId);
		p.addValue("field", FIELD_PARAMS);
		p.addValue("attribute_value", serializedParams);
		p.addValue("table_name", TABLE_WECHAT_WEAPP_SETTING);
		p.addValue("module_name", MODULE_WECHAT_WEAPP_SETTING);
		p.addValue("data_id", weappSettingId);
		p.addValue("created", now);
		p.addValue("updated", now);
		try {
			namedParameterJdbcTemplate.update(sql, p);
		} catch (DataAccessException ignored) {
			// per-locale table may be absent
		}
	}

	private static String normalizeLangTableSuffix(String langTag) {
		return OutsideMultiLangTableSupport.normalizeLangTableSuffix(langTag);
	}
}
