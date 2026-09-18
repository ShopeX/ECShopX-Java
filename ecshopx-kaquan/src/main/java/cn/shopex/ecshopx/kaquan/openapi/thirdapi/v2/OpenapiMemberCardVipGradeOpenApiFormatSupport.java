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

import cn.shopex.ecshopx.kaquan.domain.VipGrade;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class OpenapiMemberCardVipGradeOpenApiFormatSupport {

	private static final DateTimeFormatter DATETIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private OpenapiMemberCardVipGradeOpenApiFormatSupport() {}

	static Map<String, Object> formatOpenApiVipGradeRow(VipGrade entity, ObjectMapper objectMapper) {
		if (entity == null) {
			return defaultEmptyOpenApiVipGradeRow();
		}
		Object monthlyFee = null;
		Object quarterFee = null;
		Object yearFee = null;

		if (entity.getPriceList() != null) {
			List<Map<String, Object>> priceItems = parseJsonArray(entity.getPriceList(), objectMapper);
			for (Map<String, Object> item : priceItems) {
				Object name = item.get("name");
				Object price = item.get("price");
				if ("monthly".equals(String.valueOf(name))) {
					monthlyFee = price;
				} else if ("quarter".equals(String.valueOf(name))) {
					quarterFee = price;
				} else if ("year".equals(String.valueOf(name))) {
					yearFee = price;
				}
			}
		}

		Map<String, Object> privileges = entity.getPrivileges() != null
				? parseJsonObject(entity.getPrivileges(), objectMapper)
				: new LinkedHashMap<>();

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("vip_grade_id", intOrZero(entity.getVipGradeId()));
		out.put("type", stringOrEmpty(entity.getLvType()));
		out.put("grade_name", stringOrEmpty(entity.getGradeName()));
		out.put("monthly_fee", monthlyFee);
		out.put("quarter_fee", quarterFee);
		out.put("year_fee", yearFee);
		out.put("discount", stringOrDefault(privileges.get("discount_desc"), "0"));
		out.put("guide_title", stringOrEmpty(entity.getGuideTitle()));
		out.put("description", stringOrEmpty(entity.getDescription()));
		out.put("background_pic_url", stringOrEmpty(entity.getBackgroundPicUrl()));
		out.put("is_default", booleanToInt(entity.getIsDefault()));
		out.put("is_disabled", booleanToInt(entity.getIsDisabled()));
		out.put("external_id", stringOrEmpty(entity.getExternalId()));
		out.put("created", formatEpochSeconds(entity.getCreated()));
		out.put("updated", formatEpochSeconds(entity.getUpdated()));
		return out;
	}

	static Map<String, Object> defaultEmptyOpenApiVipGradeRow() {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("vip_grade_id", 0);
		out.put("type", "");
		out.put("grade_name", "");
		out.put("monthly_fee", null);
		out.put("quarter_fee", null);
		out.put("year_fee", null);
		out.put("discount", "0");
		out.put("guide_title", "");
		out.put("description", "");
		out.put("background_pic_url", "");
		out.put("is_default", 0);
		out.put("is_disabled", 0);
		out.put("external_id", "");
		out.put("created", "");
		out.put("updated", "");
		return out;
	}

	static Map<String, Object> parseJsonObject(String raw, ObjectMapper objectMapper) {
		if (raw == null || raw.isEmpty()) {
			return new LinkedHashMap<>();
		}
		try {
			return objectMapper.readValue(raw, new TypeReference<LinkedHashMap<String, Object>>() {});
		} catch (Exception e) {
			return new LinkedHashMap<>();
		}
	}

	static List<Map<String, Object>> parseJsonArray(String raw, ObjectMapper objectMapper) {
		if (raw == null || raw.isEmpty()) {
			return List.of();
		}
		try {
			return objectMapper.readValue(raw, new TypeReference<List<Map<String, Object>>>() {});
		} catch (Exception e) {
			return List.of();
		}
	}

	static int booleanToInt(Boolean value) {
		return Boolean.TRUE.equals(value) ? 1 : 0;
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

	static int intOrZero(Long value) {
		if (value == null) {
			return 0;
		}
		return value.intValue();
	}

	static String formatEpochSeconds(Integer value) {
		if (value == null) {
			return "";
		}
		return DATETIME_FMT.format(Instant.ofEpochSecond(value.longValue()));
	}
}
