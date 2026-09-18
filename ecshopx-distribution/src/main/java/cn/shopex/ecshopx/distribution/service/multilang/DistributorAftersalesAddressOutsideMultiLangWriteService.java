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

package cn.shopex.ecshopx.distribution.service.multilang;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;

@Service
public class DistributorAftersalesAddressOutsideMultiLangWriteService {

	private static final String TABLE_AND_MODULE = "distributor_aftersales_address";
	private static final List<String> FIELDS =
			List.of("name", "contact", "logo", "province", "city", "area", "address", "introduce");

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	public DistributorAftersalesAddressOutsideMultiLangWriteService(
			JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
	}

	public void updateLangData(long companyId, long dataId, Map<String, Object> langBag, String requestLangTag) {
		if (dataId <= 0L || langBag == null || langBag.isEmpty()) {
			return;
		}
		String table = "outside_item_multi_lang_mod_lang_" + normalizeLangTableSuffix(requestLangTag);
		int now = (int) (System.currentTimeMillis() / 1000L);
		for (String field : FIELDS) {
			if (!langBag.containsKey(field)) {
				continue;
			}
			Object val = langBag.get(field);
			if (val == null) {
				continue;
			}
			String attrStr = toAttributeString(val);
			String sql = "UPDATE `"
					+ table
					+ "` SET attribute_value = ?, updated = ? WHERE company_id = ? AND table_name = ? AND module_name = ? AND data_id = ? AND field = ?";
			jdbcTemplate.update(
					sql, attrStr, now, companyId, TABLE_AND_MODULE, TABLE_AND_MODULE, dataId, field);
		}
	}

	public void addMultiLangAfterInsert(long addressId, long companyId, Map<String, Object> sourceRow, String requestLangTag) {
		if (addressId <= 0L) {
			return;
		}
		Map<String, Object> langBag = new LinkedHashMap<>();
		for (String f : FIELDS) {
			Object v = sourceRow == null ? null : sourceRow.get(f);
			langBag.put(f, v == null ? "" : v);
		}
		String normalizedLang = normalizeLangTableSuffix(requestLangTag);
		String table = "outside_item_multi_lang_mod_lang_" + normalizedLang;
		int now = (int) (System.currentTimeMillis() / 1000L);
		for (Map.Entry<String, Object> e : langBag.entrySet()) {
			String field = e.getKey();
			String attrStr = toAttributeString(e.getValue());
			String sql = "INSERT INTO `"
					+ table
					+ "` (company_id, field, attribute_value, table_name, module_name, data_id, created, updated) "
					+ "VALUES (?,?,?,?,?,?,?,?)";
			jdbcTemplate.update(
					sql,
					companyId,
					field,
					attrStr,
					TABLE_AND_MODULE,
					TABLE_AND_MODULE,
					addressId,
					now,
					now);
		}
	}

	private String toAttributeString(Object attributeValue) {
		if (attributeValue == null) {
			return "";
		}
		if (attributeValue instanceof List<?> || attributeValue instanceof Map<?, ?>) {
			try {
				return objectMapper.writeValueAsString(attributeValue);
			} catch (Exception ex) {
				return attributeValue.toString();
			}
		}
		return attributeValue.toString();
	}

	public static String normalizeLangTableSuffix(String langTag) {
		return OutsideMultiLangTableSupport.normalizeLangTableSuffix(langTag);
	}
}
