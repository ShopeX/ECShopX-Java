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

package cn.shopex.ecshopx.companys.service;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CommonLangModWriteService {

	private static final String OUTSIDE_LANG_TABLE_PREFIX = "outside_item_multi_lang_mod_lang_";

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private final LangueProperties langueProperties;

	public CommonLangModWriteService(
			NamedParameterJdbcTemplate namedParameterJdbcTemplate, LangueProperties langueProperties) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
		this.langueProperties = langueProperties;
	}

	public void deleteLang(int companyId, String tableName, long dataId, String module, String langue) {
		requireKnownLangue(langue);
		String table = OUTSIDE_LANG_TABLE_PREFIX + OutsideMultiLangTableSupport.normalizeLangTableSuffix(langue);
		String sql =
				"DELETE FROM "
						+ table
						+ " WHERE company_id = :company_id AND table_name = :table_name AND module_name = :module_name "
						+ "AND data_id = :data_id";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", (long) companyId);
		p.addValue("table_name", tableName);
		p.addValue("module_name", module);
		p.addValue("data_id", dataId);
		try {
			namedParameterJdbcTemplate.update(sql, p);
		} catch (DataAccessException ignored) {
			// per-locale table may be absent
		}
	}

	public void saveLang(
			int companyId, Map<String, String> langMap, String table, int id, String module, String langue) {
		if (langMap == null || langMap.isEmpty()) {
			return;
		}
		requireKnownLangue(langue);
		int now = (int) (System.currentTimeMillis() / 1000L);
		String outsideTable =
				OUTSIDE_LANG_TABLE_PREFIX + OutsideMultiLangTableSupport.normalizeLangTableSuffix(langue);
		for (Map.Entry<String, String> e : langMap.entrySet()) {
			String field = e.getKey();
			String value = e.getValue();
			if (field == null || value == null) {
				continue;
			}
			insertRow(outsideTable, companyId, table, module, id, field, value, now);
		}
	}

	public void updateLangData(
			int companyId,
			Map<String, String> langMap,
			String table,
			int dataId,
			String module,
			String langue) {
		if (langMap == null || langMap.isEmpty()) {
			return;
		}
		requireKnownLangue(langue);
		int now = (int) (System.currentTimeMillis() / 1000L);
		String outsideTable =
				OUTSIDE_LANG_TABLE_PREFIX + OutsideMultiLangTableSupport.normalizeLangTableSuffix(langue);
		for (Map.Entry<String, String> e : langMap.entrySet()) {
			String field = e.getKey();
			String value = e.getValue();
			if (field == null || value == null) {
				continue;
			}
			deleteField(outsideTable, companyId, table, module, dataId, field);
			insertRow(outsideTable, companyId, table, module, dataId, field, value, now);
		}
	}

	private void deleteField(
			String outsideTable, int companyId, String table, String module, int dataId, String field) {
		String sql =
				"DELETE FROM "
						+ outsideTable
						+ " WHERE company_id = :company_id AND table_name = :table_name AND module_name = :module_name "
						+ "AND data_id = :data_id AND `field` = :field";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", (long) companyId);
		p.addValue("table_name", table);
		p.addValue("module_name", module);
		p.addValue("data_id", (long) dataId);
		p.addValue("field", field);
		try {
			namedParameterJdbcTemplate.update(sql, p);
		} catch (DataAccessException ignored) {
		}
	}

	private void insertRow(
			String outsideTable,
			int companyId,
			String table,
			String module,
			int dataId,
			String field,
			String value,
			int now) {
		String sql =
				"INSERT INTO "
						+ outsideTable
						+ " (company_id, field, attribute_value, table_name, module_name, data_id, created, updated) "
						+ "VALUES (:company_id, :field, :attribute_value, :table_name, :module_name, :data_id, :created, :updated)";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", (long) companyId);
		p.addValue("field", field);
		p.addValue("attribute_value", value);
		p.addValue("table_name", table);
		p.addValue("module_name", module);
		p.addValue("data_id", (long) dataId);
		p.addValue("created", now);
		p.addValue("updated", now);
		namedParameterJdbcTemplate.update(sql, p);
	}

	private void requireKnownLangue(String langue) {
		if (!StringUtils.hasText(langue)) {
			throw new ResourceException("多语言配置与实现不一致，未知语种: " + langue);
		}
		String tag = langue.trim();
		List<String> supported = langueProperties.getList();
		if (supported != null) {
			for (String lang : supported) {
				if (lang != null && lang.equalsIgnoreCase(tag)) {
					return;
				}
			}
		}
		throw new ResourceException("多语言配置与实现不一致，未知语种: " + langue);
	}
}
