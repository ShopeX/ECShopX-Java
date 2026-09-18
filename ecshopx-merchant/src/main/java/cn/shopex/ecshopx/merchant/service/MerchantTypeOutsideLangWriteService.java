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

package cn.shopex.ecshopx.merchant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class MerchantTypeOutsideLangWriteService {

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	@SuppressWarnings("unused")
	private final ObjectMapper objectMapper;

	public MerchantTypeOutsideLangWriteService(
			NamedParameterJdbcTemplate namedParameterJdbcTemplate,
			ObjectMapper objectMapper) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
		this.objectMapper = objectMapper;
	}

	public void applyAfterInsert(long merchantTypeId, long companyId, String name, String requestLangTag) {
		String normalizedLang = MerchantOutsideLangWriteService.normalizeLangTableSuffix(requestLangTag);
		String table = "outside_item_multi_lang_mod_lang_" + normalizedLang;
		int now = (int) (System.currentTimeMillis() / 1000L);
		String attrStr = name == null ? "" : name;
		String sql = "INSERT INTO " + table + " (company_id, field, attribute_value, table_name, module_name, data_id, created, updated) "
				+ "VALUES (:company_id, :field, :attribute_value, :table_name, :module_name, :data_id, :created, :updated)";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", companyId);
		p.addValue("field", "name");
		p.addValue("attribute_value", attrStr);
		p.addValue("table_name", "merchant_type");
		p.addValue("module_name", "merchant_type");
		p.addValue("data_id", merchantTypeId);
		p.addValue("created", now);
		p.addValue("updated", now);
		namedParameterJdbcTemplate.update(sql, p);
	}

	public void applyAfterUpdateMerchantTypeName(
			long merchantTypeId, long companyId, String name, String requestLangTag) {
		String normalizedLang = MerchantOutsideLangWriteService.normalizeLangTableSuffix(requestLangTag);
		String table = "outside_item_multi_lang_mod_lang_" + normalizedLang;
		int now = (int) (System.currentTimeMillis() / 1000L);
		String attrStr = name == null ? "" : name;
		String deleteSql = "DELETE FROM " + table
				+ " WHERE company_id = :company_id AND table_name = :table_name AND module_name = :module_name AND data_id = :data_id AND field = :field";
		MapSqlParameterSource del = new MapSqlParameterSource();
		del.addValue("company_id", companyId);
		del.addValue("table_name", "merchant_type");
		del.addValue("module_name", "merchant_type");
		del.addValue("data_id", merchantTypeId);
		del.addValue("field", "name");
		namedParameterJdbcTemplate.update(deleteSql, del);
		String insertSql = "INSERT INTO " + table
				+ " (company_id, field, attribute_value, table_name, module_name, data_id, created, updated) "
				+ "VALUES (:company_id, :field, :attribute_value, :table_name, :module_name, :data_id, :created, :updated)";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", companyId);
		p.addValue("field", "name");
		p.addValue("attribute_value", attrStr);
		p.addValue("table_name", "merchant_type");
		p.addValue("module_name", "merchant_type");
		p.addValue("data_id", merchantTypeId);
		p.addValue("created", now);
		p.addValue("updated", now);
		namedParameterJdbcTemplate.update(insertSql, p);
	}
}
