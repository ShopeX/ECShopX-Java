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

import java.util.Map;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MerchantTypeOutsideLangReadService {

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public MerchantTypeOutsideLangReadService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	public String resolveName(long companyId, long typeId, String baseName, String requestLangTag) {
		String normalizedLang = MerchantOutsideLangWriteService.normalizeLangTableSuffix(requestLangTag);
		String table = "outside_item_multi_lang_mod_lang_" + normalizedLang;
		String sql = "SELECT attribute_value FROM " + table
				+ " WHERE company_id = :company_id AND table_name = 'merchant_type' AND module_name = 'merchant_type'"
				+ " AND data_id = :data_id AND field = 'name' LIMIT 1";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("company_id", companyId);
		p.addValue("data_id", typeId);
		try {
			String v = namedParameterJdbcTemplate.queryForObject(sql, p, String.class);
			if (StringUtils.hasText(v)) {
				return v;
			}
		} catch (EmptyResultDataAccessException ignored) {
		}
		return baseName;
	}
}
