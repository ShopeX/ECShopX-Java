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

package cn.shopex.ecshopx.distribution.openapi;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class OpenapiDistributorOpenApiRowFormatSupport {

	private static final DateTimeFormatter DATE_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private OpenapiDistributorOpenApiRowFormatSupport() {}

	public static Map<String, Object> toInternalRowMap(
			Distributor entity, SensitiveFieldEncryptor encryptor, ObjectMapper objectMapper) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("distributor_id", entity.getDistributorId());
		row.put("shop_code", entity.getShopCode());

		String contact = entity.getContact();
		if (contact != null) {
			contact = encryptor.decrypt(contact);
		}
		row.put("contact", contact);

		String mobile = entity.getMobile();
		if (mobile != null) {
			mobile = encryptor.decrypt(mobile);
		}
		row.put("mobile", mobile);

		row.put("name", entity.getName());
		row.put("province", entity.getProvince());
		row.put("city", entity.getCity());
		row.put("area", entity.getArea());
		row.put("address", entity.getAddress());
		row.put("lng", entity.getLng());
		row.put("lat", entity.getLat());
		row.put("hour", entity.getHour());
		row.put("logo", entity.getLogo());
		row.put("is_valid", entity.getIsValid());
		row.put("is_ziti", entity.getIsZiti());
		row.put("is_delivery", entity.getIsDelivery());
		row.put("auto_sync_goods", entity.getAutoSyncGoods());
		row.put("is_dada", entity.getIsDada());
		row.put("is_default", entity.getIsDefault());
		row.put("created", entity.getCreated());
		row.put("updated", entity.getUpdated());
		row.put("regions_id", decodeJsonList(entity.getRegionsId(), objectMapper));
		row.put("regions", decodeJsonList(entity.getRegions(), objectMapper));
		return row;
	}

	public static Map<String, Object> toOpenapiRow(Map<String, Object> item) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("distributor_id", toInt(item.get("distributor_id"), 0));
		out.put("shop_code", nullToEmpty(item.get("shop_code")));
		out.put("status", statusByIsValid(item.get("is_valid")));
		out.put("distributor_name", nullToEmpty(item.get("name")));
		out.put("contact_username", nullToEmpty(item.get("contact")));
		out.put("contact_mobile", nullToEmpty(item.get("mobile")));
		out.put("province", nullToEmpty(item.get("province")));
		out.put("city", nullToEmpty(item.get("city")));
		out.put("area", nullToEmpty(item.get("area")));
		out.put("region_codes", toStringList(item.get("regions_id")));
		out.put("region_names", toStringList(item.get("regions")));
		out.put("address", nullToEmpty(item.get("address")));
		out.put("lng", nullToEmpty(item.get("lng")));
		out.put("lat", nullToEmpty(item.get("lat")));
		out.put("hour", nullToEmpty(item.get("hour")));
		out.put("logo", nullToEmpty(item.get("logo")));
		out.put("is_ziti", intFlag(item, "is_ziti", 0));
		out.put("is_delivery", intFlag(item, "is_delivery", 1));
		out.put("is_auto_sync_goods", intFlag(item, "auto_sync_goods", 0));
		out.put("is_dada", intFlag(item, "is_dada", 0));
		out.put("is_default", intFlag(item, "is_default", 0));
		out.put("created", formatEpochSeconds(item.get("created")));
		out.put("updated", formatEpochSeconds(item.get("updated")));
		return out;
	}

	public static int statusByIsValid(Object isValid) {
		if (isValid == null) {
			return -1;
		}
		return switch (String.valueOf(isValid)) {
			case "delete" -> 0;
			case "true" -> 1;
			case "false" -> 2;
			default -> -1;
		};
	}

	public static int intFlag(Map<String, Object> item, String key, int defaultVal) {
		if (!item.containsKey(key) || item.get(key) == null) {
			return defaultVal;
		}
		Object v = item.get(key);
		if (v instanceof Boolean b) {
			return b ? 1 : 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return (int) Double.parseDouble(String.valueOf(v));
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	public static String formatEpochSeconds(Object v) {
		if (v == null) {
			return "";
		}
		long sec;
		if (v instanceof Number n) {
			sec = n.longValue();
		} else {
			try {
				sec = Long.parseLong(String.valueOf(v));
			} catch (NumberFormatException e) {
				return "";
			}
		}
		return DATE_TIME_FMT.format(
				Instant.ofEpochSecond(sec).atZone(ZoneId.systemDefault()));
	}

	private static Object decodeJsonList(String json, ObjectMapper objectMapper) {
		if (!StringUtils.hasText(json)) {
			return null;
		}
		String t = json.trim();
		if (t.startsWith("[")) {
			try {
				return objectMapper.readValue(t, new TypeReference<List<Object>>() {});
			} catch (Exception e) {
				return json;
			}
		}
		if (t.contains(",")) {
			String[] parts = t.split(",");
			List<String> list = new ArrayList<>();
			for (String p : parts) {
				if (StringUtils.hasText(p)) {
					list.add(p.trim());
				}
			}
			return list;
		}
		return List.of(t);
	}

	private static int toInt(Object v, int defaultVal) {
		if (v == null) {
			return defaultVal;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(v));
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static String nullToEmpty(Object value) {
		return value != null ? String.valueOf(value) : "";
	}

	@SuppressWarnings("unchecked")
	private static List<String> toStringList(Object value) {
		if (value == null) {
			return List.of();
		}
		if (value instanceof List<?> list) {
			List<String> out = new ArrayList<>(list.size());
			for (Object item : list) {
				out.add(item != null ? String.valueOf(item) : "");
			}
			return out;
		}
		return List.of(String.valueOf(value));
	}
}
