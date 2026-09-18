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

import java.util.List;
import java.util.Map;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MerchantOutsideLangReadService {

	private static final List<String> MULTI_LANG_FIELDS =
			List.of("merchant_name", "province", "city", "area", "address", "legal_name");

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public MerchantOutsideLangReadService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	public void applyMerchantRow(long companyId, long merchantId, String requestLangTag, Map<String, Object> row) {
		String normalizedLang = MerchantOutsideLangWriteService.normalizeLangTableSuffix(requestLangTag);
		String table = "outside_item_multi_lang_mod_lang_" + normalizedLang;
		for (String field : MULTI_LANG_FIELDS) {
			String sql = "SELECT attribute_value FROM " + table
					+ " WHERE company_id = :company_id AND table_name = 'merchant' AND module_name = 'merchant'"
					+ " AND data_id = :data_id AND field = :field LIMIT 1";
			MapSqlParameterSource p = new MapSqlParameterSource();
			p.addValue("company_id", companyId);
			p.addValue("data_id", merchantId);
			p.addValue("field", field);
			try {
				String v = namedParameterJdbcTemplate.queryForObject(sql, p, String.class);
				if (StringUtils.hasText(v)) {
					row.put(field, v);
				}
			} catch (EmptyResultDataAccessException ignored) {
			}
		}
	}
}
