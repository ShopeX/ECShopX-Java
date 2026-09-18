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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DistributorTagsTagMultiLangWriteService {

	private static final String TABLE_MODULE = "distributor_tags";
	private static final List<String> FIELDS = List.of("tag_name", "description");

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private final ObjectMapper objectMapper;

	public DistributorTagsTagMultiLangWriteService(
			NamedParameterJdbcTemplate namedParameterJdbcTemplate,
			ObjectMapper objectMapper) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
		this.objectMapper = objectMapper;
	}

	public void applyAfterInsert(long tagId, long companyId, Map<String, Object> params, String requestLangTag) {
		Map<String, Object> langBag = new LinkedHashMap<>();
		for (String f : FIELDS) {
			Object v = params.get(f);
			langBag.put(f, v == null ? "" : v);
		}
		String table =
				"outside_item_multi_lang_mod_lang_" + DistributorMultiLangWriteService.normalizeLangTableSuffix(requestLangTag);
		int now = (int) (System.currentTimeMillis() / 1000L);
		for (Map.Entry<String, Object> e : langBag.entrySet()) {
			String field = e.getKey();
			Object attributeValue = e.getValue();
			String attrStr;
			if (attributeValue == null) {
				attrStr = "";
			} else if (attributeValue instanceof List<?> || attributeValue instanceof Map<?, ?>) {
				try {
					attrStr = objectMapper.writeValueAsString(attributeValue);
				} catch (Exception ex) {
					attrStr = attributeValue.toString();
				}
			} else {
				attrStr = attributeValue.toString();
			}
			String sql = "INSERT INTO " + table + " (company_id, field, attribute_value, table_name, module_name, data_id, created, updated) "
					+ "VALUES (:company_id, :field, :attribute_value, :table_name, :module_name, :data_id, :created, :updated)";
			MapSqlParameterSource p = new MapSqlParameterSource();
			p.addValue("company_id", companyId);
			p.addValue("field", field);
			p.addValue("attribute_value", attrStr);
			p.addValue("table_name", TABLE_MODULE);
			p.addValue("module_name", TABLE_MODULE);
			p.addValue("data_id", tagId);
			p.addValue("created", now);
			p.addValue("updated", now);
			namedParameterJdbcTemplate.update(sql, p);
		}
	}

	public void applyAfterUpdate(long tagId, long companyId, Map<String, Object> merged, String requestLangTag) {
		String table =
				"outside_item_multi_lang_mod_lang_" + DistributorMultiLangWriteService.normalizeLangTableSuffix(requestLangTag);
		int now = (int) (System.currentTimeMillis() / 1000L);
		for (String f : FIELDS) {
			if (!merged.containsKey(f)) {
				continue;
			}
			if ("description".equals(f) && merged.get("description") == null) {
				continue;
			}
			Object attributeValue = merged.get(f);
			String attrStr;
			if (attributeValue == null) {
				attrStr = "";
			} else if (attributeValue instanceof List<?> || attributeValue instanceof Map<?, ?>) {
				try {
					attrStr = objectMapper.writeValueAsString(attributeValue);
				} catch (Exception ex) {
					attrStr = attributeValue.toString();
				}
			} else {
				attrStr = attributeValue.toString();
			}
			String deleteSql = "DELETE FROM " + table + " WHERE company_id = :company_id AND table_name = :table_name AND module_name = :module_name AND data_id = :data_id AND field = :field";
			MapSqlParameterSource del = new MapSqlParameterSource();
			del.addValue("company_id", companyId);
			del.addValue("table_name", TABLE_MODULE);
			del.addValue("module_name", TABLE_MODULE);
			del.addValue("data_id", tagId);
			del.addValue("field", f);
			namedParameterJdbcTemplate.update(deleteSql, del);
			String insertSql = "INSERT INTO " + table + " (company_id, field, attribute_value, table_name, module_name, data_id, created, updated) "
					+ "VALUES (:company_id, :field, :attribute_value, :table_name, :module_name, :data_id, :created, :updated)";
			MapSqlParameterSource p = new MapSqlParameterSource();
			p.addValue("company_id", companyId);
			p.addValue("field", f);
			p.addValue("attribute_value", attrStr);
			p.addValue("table_name", TABLE_MODULE);
			p.addValue("module_name", TABLE_MODULE);
			p.addValue("data_id", tagId);
			p.addValue("created", now);
			p.addValue("updated", now);
			namedParameterJdbcTemplate.update(insertSql, p);
		}
	}
}
