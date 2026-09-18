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

package cn.shopex.ecshopx.selfservice.service.multilang;

import cn.shopex.ecshopx.promotions.service.multilang.SeckillActivityOutsideMultiLangWriteService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RegistrationRecordOutsideMultiLangWriteService {

	private static final String TABLE_AND_MODULE = "selfservice_registration_record";
	private static final List<String> MULTI_LANG_FIELDS = List.of("remark", "reason");

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	public RegistrationRecordOutsideMultiLangWriteService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
	}

	public void updateLangFields(long recordId, long companyId, Map<String, Object> data, String requestLangTag) {
		if (recordId <= 0L) {
			return;
		}
		String normalizedLang = SeckillActivityOutsideMultiLangWriteService.normalizeLangTableSuffix(requestLangTag);
		String table = "outside_item_multi_lang_mod_lang_" + normalizedLang;
		int now = (int) (System.currentTimeMillis() / 1000L);
		for (String field : MULTI_LANG_FIELDS) {
			if (data == null || !data.containsKey(field)) {
				continue;
			}
			Object v = data.get(field);
			String attrStr = toAttributeString(v);
			Integer exists =
					jdbcTemplate.queryForObject(
							"SELECT COUNT(1) FROM `"
									+ table
									+ "` WHERE company_id=? AND field=? AND table_name=? AND module_name=? AND data_id=? LIMIT 1",
							Integer.class,
							companyId,
							field,
							TABLE_AND_MODULE,
							TABLE_AND_MODULE,
							recordId);
			int cnt = exists == null ? 0 : exists;
			if (cnt > 0) {
				jdbcTemplate.update(
						"UPDATE `"
								+ table
								+ "` SET attribute_value=?, updated=? WHERE company_id=? AND field=? AND table_name=? AND module_name=? AND data_id=?",
						attrStr,
						now,
						companyId,
						field,
						TABLE_AND_MODULE,
						TABLE_AND_MODULE,
						recordId);
			} else {
				String sql =
						"INSERT INTO `"
								+ table
								+ "` (company_id, field, attribute_value, table_name, module_name, data_id, created, updated) "
								+ "VALUES (?,?,?,?,?,?,?,?)";
				jdbcTemplate.update(
						sql, companyId, field, attrStr, TABLE_AND_MODULE, TABLE_AND_MODULE, recordId, now, now);
			}
		}
	}

	private String toAttributeString(Object attributeValue) {
		if (attributeValue == null) {
			return "";
		}
		if (attributeValue instanceof JsonNode jn) {
			try {
				return objectMapper.writeValueAsString(jn);
			} catch (Exception ex) {
				return jn.toString();
			}
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
}
