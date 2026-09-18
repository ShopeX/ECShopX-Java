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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import cn.shopex.ecshopx.common.marketing.support.OutsideMultiLangTableSupport;

@Slf4j
@Service
public class RegistrationActivityOutsideMultiLangWriteService {

	private static final String TABLE_AND_MODULE = "selfservice_registration_activity";
	private static final List<String> FIELDS = List.of(
			"activity_name",
			"place",
			"area",
			"address",
			"intro",
			"join_tips",
			"submit_form_tips",
			"content",
			"pics");

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	public RegistrationActivityOutsideMultiLangWriteService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
	}

	public void updateLangDataForRegistrationActivity(
			long companyId, long dataId, Map<String, Object> langBag, String requestLangTag) {
		if (dataId <= 0L) {
			return;
		}
		String table = "outside_item_multi_lang_mod_lang_" + normalizeLangTableSuffix(requestLangTag);
		int now = (int) (System.currentTimeMillis() / 1000L);
		for (String field : FIELDS) {
			if (langBag == null || !langBag.containsKey(field) || langBag.get(field) == null) {
				continue;
			}
			String attrStr = toAttributeString(langBag.get(field));
			String updateSql = "UPDATE `"
					+ table
					+ "` SET attribute_value = ?, updated = ? WHERE table_name = ? AND module_name = ? AND field = ? AND data_id = ?";
			int n = jdbcTemplate.update(updateSql, attrStr, now, TABLE_AND_MODULE, TABLE_AND_MODULE, field, dataId);
			if (n == 0) {
				String insertSql = "INSERT INTO `"
						+ table
						+ "` (company_id, field, attribute_value, table_name, module_name, data_id, created, updated) "
						+ "VALUES (?,?,?,?,?,?,?,?)";
				jdbcTemplate.update(
						insertSql, companyId, field, attrStr, TABLE_AND_MODULE, TABLE_AND_MODULE, dataId, now, now);
			}
		}
	}

	public void addForNewRegistrationActivity(
			long dataId, long companyId, Map<String, Object> requestData, String requestLangTag) {
		if (dataId <= 0L) {
			return;
		}
		String normalizedLang = normalizeLangTableSuffix(requestLangTag);
		LinkedHashMap<String, Object> langBag = new LinkedHashMap<>();
		for (String field : FIELDS) {
			Object v = requestData == null ? null : requestData.get(field);
			langBag.put(field, v == null ? "" : v);
		}
		String fmt =
				"addForNewRegistrationActivity多语言:=>multiLang:{}|langBag:{}|dataId={}|companyId={}|requestLangTag={}|langSuffix={}|tableModule={}";
		try {
			log.debug(
					fmt,
					objectMapper.writeValueAsString(FIELDS),
					objectMapper.writeValueAsString(langBag),
					dataId,
					companyId,
					requestLangTag != null ? requestLangTag : "",
					normalizedLang,
					TABLE_AND_MODULE);
		} catch (JsonProcessingException ex) {
			log.debug(
					fmt,
					"<multiLang序列化失败>",
					"<langBag序列化失败>",
					dataId,
					companyId,
					requestLangTag != null ? requestLangTag : "",
					normalizedLang,
					TABLE_AND_MODULE);
		}
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
					sql, companyId, field, attrStr, TABLE_AND_MODULE, TABLE_AND_MODULE, dataId, now, now);
		}
	}

	private String toAttributeString(Object attributeValue) {
		if (attributeValue == null) {
			return "";
		}
		if (attributeValue instanceof JsonNode jn) {
			try {
				return objectMapper.writeValueAsString(jn);
			} catch (JsonProcessingException ex) {
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

	private static String normalizeLangTableSuffix(String langTag) {
		return OutsideMultiLangTableSupport.normalizeLangTableSuffix(langTag);
	}
}
