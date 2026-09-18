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

package cn.shopex.ecshopx.members.service.trustlogin;

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 信任登录 Redis 配置 merge / 脱敏 / Apple extra_config 保存语义（对齐 PHP TrustLoginService + SocialTrustLoginService）。
 */
public final class TrustLoginConfigSupport {

	public static final List<String> VERSION_KEYS = List.of("standard", "touch");

	private static final Set<String> EXTRA_CONFIG_PRESERVE_FIELDS =
			Set.of("private_key", "team_id", "key_id");

	private TrustLoginConfigSupport() {}

	public static boolean isEnabled(Object status) {
		if (status == null) {
			return false;
		}
		if (status instanceof Boolean b) {
			return b;
		}
		if (status instanceof Number n) {
			return n.intValue() == 1;
		}
		String s = String.valueOf(status).trim();
		return "true".equalsIgnoreCase(s) || "1".equals(s);
	}

	public static boolean normalizeStatus(Object status) {
		return isEnabled(status);
	}

	/** 缺 type 行从默认补入；缺 extra_config 补 ''；返回新 map（不修改入参）。 */
	public static Map<String, Object> mergeTrustLoginConfig(
			Map<String, Object> stored, Map<String, Object> defaults) {
		Map<String, Object> out = deepCopyRoot(stored);
		for (String version : VERSION_KEYS) {
			Object defaultSection = defaults.get(version);
			if (!(defaultSection instanceof List<?> defaultList)) {
				continue;
			}
			List<Map<String, Object>> storedRows = rowsOf(out.get(version));
			List<String> storedTypes = new ArrayList<>();
			for (Map<String, Object> row : storedRows) {
				Object type = row.get("type");
				if (type != null) {
					storedTypes.add(String.valueOf(type));
				}
			}
			for (Object defaultRowObj : defaultList) {
				if (!(defaultRowObj instanceof Map<?, ?> defaultRowMap)) {
					continue;
				}
				Object typeObj = defaultRowMap.get("type");
				if (typeObj == null) {
					continue;
				}
				String type = String.valueOf(typeObj);
				if (!storedTypes.contains(type)) {
					storedRows.add(copyRow(defaultRowMap));
					storedTypes.add(type);
				}
			}
			for (Map<String, Object> row : storedRows) {
				if (!row.containsKey("extra_config") || row.get("extra_config") == null) {
					row.put("extra_config", "");
				}
			}
			out.put(version, storedRows);
		}
		return out;
	}

	public static Map<String, Object> sanitizeConfigRow(Map<String, Object> row, boolean forFront) {
		Map<String, Object> out = new LinkedHashMap<>(row);
		if (forFront) {
			out.remove("secret");
		}
		Object extraRaw = out.get("extra_config");
		if (extraRaw == null || "".equals(String.valueOf(extraRaw).trim())) {
			out.put("extra_config", "");
			return out;
		}
		Map<String, Object> extra = parseExtraConfig(extraRaw);
		if (forFront) {
			extra.remove("private_key");
			extra.remove("team_id");
			extra.remove("key_id");
		} else if (extra.containsKey("private_key")) {
			extra.put("private_key", "***");
		}
		out.put("extra_config", extra.isEmpty() ? "" : toJson(extra));
		return out;
	}

	public static Map<String, Object> sanitizeRoot(Map<String, Object> root, boolean forFront) {
		Map<String, Object> out = new LinkedHashMap<>();
		for (String version : VERSION_KEYS) {
			List<Map<String, Object>> rows = rowsOf(root.get(version));
			List<Map<String, Object>> sanitized = new ArrayList<>(rows.size());
			for (Map<String, Object> row : rows) {
				sanitized.add(sanitizeConfigRow(row, forFront));
			}
			out.put(version, sanitized);
		}
		return out;
	}

	public static String normalizeExtraConfig(Object extraConfig, ObjectMapper objectMapper) {
		if (extraConfig == null || "".equals(String.valueOf(extraConfig).trim())) {
			return "";
		}
		if (extraConfig instanceof Map<?, ?> map) {
			try {
				return objectMapper.writeValueAsString(map);
			} catch (Exception e) {
				return String.valueOf(extraConfig).trim();
			}
		}
		return String.valueOf(extraConfig).trim();
	}

	public static String mergeExtraConfigOnSave(String existing, String incoming, ObjectMapper objectMapper) {
		Map<String, Object> incomingExtra = parseExtraConfigJson(incoming, objectMapper);
		if (incomingExtra == null) {
			return existing != null && !existing.isBlank() ? existing : incoming;
		}
		Map<String, Object> existingExtra = parseExtraConfigJson(existing, objectMapper);
		if (existingExtra == null) {
			existingExtra = new LinkedHashMap<>();
		}
		for (String field : EXTRA_CONFIG_PRESERVE_FIELDS) {
			if (!incomingExtra.containsKey(field)) {
				continue;
			}
			Object incomingVal = incomingExtra.get(field);
			if ("***".equals(incomingVal)) {
				Object existingVal = existingExtra.get(field);
				if (existingVal != null && !String.valueOf(existingVal).isBlank()) {
					incomingExtra.put(field, existingVal);
				}
			}
		}
		return toJson(incomingExtra, objectMapper);
	}

	public static void assertAppleExtraConfigValid(String extraConfig, ObjectMapper objectMapper) {
		Map<String, Object> extra = parseExtraConfigJson(extraConfig, objectMapper);
		if (extra == null) {
			throw new ResourceException("Apple extra_config 格式错误");
		}
		String privateKey = String.valueOf(extra.getOrDefault("private_key", "")).trim();
		if (privateKey.isEmpty() || "***".equals(privateKey)) {
			throw new ResourceException("Apple 保存失败：请填写完整 private_key（.p8 PEM）");
		}
		String teamId = String.valueOf(extra.getOrDefault("team_id", "")).trim();
		String keyId = String.valueOf(extra.getOrDefault("key_id", "")).trim();
		if (teamId.isEmpty() || keyId.isEmpty()) {
			throw new ResourceException("Apple 保存失败：extra_config 需包含 team_id 与 key_id");
		}
	}

	public static Map<String, Object> getConfigRow(Map<String, Object> root, String type, String version) {
		if (root == null || type == null || version == null) {
			return Map.of();
		}
		for (Map<String, Object> row : rowsOf(root.get(version))) {
			Object t = row.get("type");
			if (t != null && type.equals(String.valueOf(t))) {
				return row;
			}
		}
		return Map.of();
	}

	private static Map<String, Object> deepCopyRoot(Map<String, Object> stored) {
		Map<String, Object> out = new LinkedHashMap<>();
		if (stored != null) {
			for (Map.Entry<String, Object> e : stored.entrySet()) {
				if (VERSION_KEYS.contains(e.getKey())) {
					out.put(e.getKey(), new ArrayList<>(rowsOf(e.getValue())));
				} else {
					out.put(e.getKey(), e.getValue());
				}
			}
		}
		for (String version : VERSION_KEYS) {
			out.putIfAbsent(version, new ArrayList<>());
		}
		return out;
	}

	private static List<Map<String, Object>> rowsOf(Object sectionObj) {
		List<Map<String, Object>> rows = new ArrayList<>();
		if (!(sectionObj instanceof List<?> list)) {
			return rows;
		}
		for (Object item : list) {
			if (item instanceof Map<?, ?> map) {
				rows.add(copyRow(map));
			}
		}
		return rows;
	}

	private static Map<String, Object> copyRow(Map<?, ?> src) {
		Map<String, Object> copy = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : src.entrySet()) {
			if (e.getKey() != null) {
				copy.put(String.valueOf(e.getKey()), e.getValue());
			}
		}
		return copy;
	}

	private static Map<String, Object> parseExtraConfig(Object extraConfig) {
		if (extraConfig instanceof Map<?, ?> map) {
			Map<String, Object> out = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : map.entrySet()) {
				if (e.getKey() != null) {
					out.put(String.valueOf(e.getKey()), e.getValue());
				}
			}
			return out;
		}
		if (extraConfig == null) {
			return Map.of();
		}
		String raw = String.valueOf(extraConfig).trim();
		if (raw.isEmpty()) {
			return Map.of();
		}
		try {
			Map<String, Object> decoded =
					new ObjectMapper().readValue(raw, new TypeReference<Map<String, Object>>() {});
			return decoded != null ? decoded : Map.of();
		} catch (Exception e) {
			return Map.of();
		}
	}

	private static Map<String, Object> parseExtraConfigJson(String raw, ObjectMapper objectMapper) {
		if (raw == null || raw.isBlank()) {
			return new LinkedHashMap<>();
		}
		try {
			Map<String, Object> decoded =
					objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
			return decoded != null ? decoded : null;
		} catch (Exception e) {
			return null;
		}
	}

	private static String toJson(Map<String, Object> map) {
		return toJson(map, new ObjectMapper());
	}

	private static String toJson(Map<String, Object> map, ObjectMapper objectMapper) {
		try {
			return objectMapper.writeValueAsString(map);
		} catch (Exception e) {
			throw new ResourceException("Apple extra_config 格式错误");
		}
	}
}
