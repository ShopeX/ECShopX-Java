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

package cn.shopex.ecshopx.promotions.service.multilang;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;

@Service
public class LuckyDrawActivityOutsideMultiLangWriteService {

	private static final String TABLE_NAME = "lucky_draw_activity";
	private static final String MODULE_NAME = "lucky_draw_activity";
	private static final List<String> FIELDS = List.of("activity_name", "intro");

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	public LuckyDrawActivityOutsideMultiLangWriteService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
	}

	public static String normalizeLangTableSuffix(String langTag) {
		return OutsideMultiLangTableSupport.normalizeLangTableSuffix(langTag);
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

	public void applyAfterInsert(long activityId, long companyId, Map<String, Object> requestData, String requestLangTag) {
		if (activityId <= 0L) {
			return;
		}
		String normalizedLang = normalizeLangTableSuffix(requestLangTag);
		String table = "outside_item_multi_lang_mod_lang_" + normalizedLang;
		int now = (int) (System.currentTimeMillis() / 1000L);
		for (String field : FIELDS) {
			Object v = requestData == null ? null : requestData.get(field);
			String attrStr = toAttributeString(v);
			String sql = "INSERT INTO `"
					+ table
					+ "` (company_id, field, attribute_value, table_name, module_name, data_id, created, updated) "
					+ "VALUES (?,?,?,?,?,?,?,?)";
			jdbcTemplate.update(
					sql, companyId, field, attrStr, TABLE_NAME, MODULE_NAME, activityId, now, now);
		}
	}

	public void applyAfterUpdate(long activityId, long companyId, Map<String, Object> merged, String requestLangTag) {
		if (activityId <= 0L) {
			return;
		}
		String normalizedLang = normalizeLangTableSuffix(requestLangTag);
		String table = "outside_item_multi_lang_mod_lang_" + normalizedLang;
		int now = (int) (System.currentTimeMillis() / 1000L);
		for (String field : FIELDS) {
			if (merged == null || !merged.containsKey(field)) {
				continue;
			}
			Object attributeValue = merged.get(field);
			String attrStr = toAttributeString(attributeValue);
			String deleteSql = "DELETE FROM `"
					+ table
					+ "` WHERE company_id = ? AND table_name = ? AND module_name = ? AND data_id = ? AND field = ?";
			jdbcTemplate.update(deleteSql, companyId, TABLE_NAME, MODULE_NAME, activityId, field);
			String insertSql = "INSERT INTO `"
					+ table
					+ "` (company_id, field, attribute_value, table_name, module_name, data_id, created, updated) "
					+ "VALUES (?,?,?,?,?,?,?,?)";
			jdbcTemplate.update(
					insertSql, companyId, field, attrStr, TABLE_NAME, MODULE_NAME, activityId, now, now);
		}
	}
}
