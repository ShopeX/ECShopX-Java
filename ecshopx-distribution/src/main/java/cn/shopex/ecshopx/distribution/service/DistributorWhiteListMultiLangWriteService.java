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
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DistributorWhiteListMultiLangWriteService {

	private static final String TABLE_AND_MODULE = "distribution_distributor_white_list";

	private static final List<String> MULTI_LANG_FIELDS = List.of("username");

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private final ObjectMapper objectMapper;

	public DistributorWhiteListMultiLangWriteService(
			NamedParameterJdbcTemplate namedParameterJdbcTemplate, ObjectMapper objectMapper) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
		this.objectMapper = objectMapper;
	}

	public void applyAfterInsert(long whiteListRowId, long companyId, String username, String requestLangTag) {
		if (whiteListRowId <= 0L) {
			return;
		}
		String normalizedLang = DistributorMultiLangWriteService.normalizeLangTableSuffix(requestLangTag);
		String table = "outside_item_multi_lang_mod_lang_" + normalizedLang;
		int now = (int) (System.currentTimeMillis() / 1000L);
		for (String field : MULTI_LANG_FIELDS) {
			Object attributeValue = username == null ? "" : username;
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
			p.addValue("table_name", TABLE_AND_MODULE);
			p.addValue("module_name", TABLE_AND_MODULE);
			p.addValue("data_id", whiteListRowId);
			p.addValue("created", now);
			p.addValue("updated", now);
			namedParameterJdbcTemplate.update(sql, p);
		}
	}
}
