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

package cn.shopex.ecshopx.kaquan.openapi.thirdapi.v2;

import cn.shopex.ecshopx.kaquan.domain.MemberCardGrade;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

class OpenapiMemberCardGradeOpenApiFormatSupport {

	private static final DateTimeFormatter DATETIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private OpenapiMemberCardGradeOpenApiFormatSupport() {}

	static Map<String, Object> toOverlayRow(MemberCardGrade entity) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("grade_id", entity.getGradeId());
		row.put("grade_name", entity.getGradeName());
		row.put("background_pic_url", entity.getBackgroundPicUrl());
		row.put("grade_background", entity.getGradeBackground());
		row.put("description", entity.getDescription());
		row.put("default_grade", entity.getDefaultGrade());
		row.put("privileges", entity.getPrivileges());
		row.put("promotion_condition", entity.getPromotionCondition());
		row.put("external_id", entity.getExternalId());
		row.put("created", entity.getCreated());
		row.put("updated", entity.getUpdated());
		return row;
	}

	static Map<String, Object> formatShuyunGradeRow(Map<String, Object> row, ObjectMapper objectMapper) {
		Map<String, Object> promotionCondition = parseJsonObject(row.get("promotion_condition"), objectMapper);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("grade_id", stringOrEmpty(row.get("external_id")));
		out.put("grade_name", stringOrEmpty(row.get("grade_name")));
		out.put("grade_level", stringOrDefault(promotionCondition.get("total_consumption"), "0"));
		out.put("created", formatEpochSeconds(row.get("created")));
		out.put("updated", formatEpochSeconds(row.get("updated")));
		return out;
	}

	static Map<String, Object> formatOpenApiGradeRow(Map<String, Object> row, ObjectMapper objectMapper) {
		Map<String, Object> privileges = parseJsonObject(row.get("privileges"), objectMapper);
		Map<String, Object> promotionCondition = parseJsonObject(row.get("promotion_condition"), objectMapper);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("grade_id", intOrZero(row.get("grade_id")));
		out.put("is_default", booleanToInt(row.get("default_grade")));
		out.put("grade_name", stringOrEmpty(row.get("grade_name")));
		out.put("discount", stringOrDefault(privileges.get("discount_desc"), "0"));
		out.put("total_consumption", stringOrDefault(promotionCondition.get("total_consumption"), "0"));
		out.put("background_pic_url", stringOrEmpty(row.get("background_pic_url")));
		out.put("external_id", stringOrEmpty(row.get("external_id")));
		out.put("created", formatEpochSeconds(row.get("created")));
		out.put("updated", formatEpochSeconds(row.get("updated")));
		return out;
	}

	static Map<String, Object> parseJsonObject(Object raw, ObjectMapper objectMapper) {
		if (raw == null) {
			return new LinkedHashMap<>();
		}
		if (raw instanceof Map<?, ?> map) {
			LinkedHashMap<String, Object> out = new LinkedHashMap<>();
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				if (entry.getKey() != null) {
					out.put(String.valueOf(entry.getKey()), entry.getValue());
				}
			}
			return out;
		}
		String json = String.valueOf(raw);
		if (json.isEmpty()) {
			return new LinkedHashMap<>();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
		} catch (Exception e) {
			return new LinkedHashMap<>();
		}
	}

	static int booleanToInt(Object value) {
		if (Boolean.TRUE.equals(value)) {
			return 1;
		}
		return 0;
	}

	static String stringOrDefault(Object value, String defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		return String.valueOf(value);
	}

	static String stringOrEmpty(Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	static int intOrZero(Object value) {
		if (value instanceof Number number) {
			return number.intValue();
		}
		if (value == null) {
			return 0;
		}
		try {
			return Integer.parseInt(String.valueOf(value).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	static String formatEpochSeconds(Object value) {
		if (!(value instanceof Number number)) {
			return "";
		}
		return DATETIME_FMT.format(Instant.ofEpochSecond(number.longValue()));
	}
}
