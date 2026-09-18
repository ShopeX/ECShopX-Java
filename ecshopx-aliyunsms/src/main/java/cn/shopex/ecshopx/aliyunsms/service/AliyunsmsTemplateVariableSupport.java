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

package cn.shopex.ecshopx.aliyunsms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps scene template placeholders ({@code var_title} / {@code var_name}) for Aliyun SMS send and validation.
 * Synced cloud templates often use {@code ${code}} while admin scenes define {@code var_title=验证码}.
 */
final class AliyunsmsTemplateVariableSupport {

	private static final Pattern TEMPLATE_VAR_PATTERN = Pattern.compile("\\$\\{(.+?)}");

	private AliyunsmsTemplateVariableSupport() {}

	static List<String> extractPlaceholders(String templateContent) {
		List<String> out = new ArrayList<>();
		if (templateContent == null || templateContent.isEmpty()) {
			return out;
		}
		Matcher m = TEMPLATE_VAR_PATTERN.matcher(templateContent);
		while (m.find()) {
			out.add(m.group(1));
		}
		return out;
	}

	static Map<String, String> buildPlaceholderKeyToVarName(List<Map<String, Object>> varDefs) {
		LinkedHashMap<String, String> map = new LinkedHashMap<>();
		if (varDefs == null) {
			return map;
		}
		for (Map<String, Object> row : varDefs) {
			if (row == null) {
				continue;
			}
			Object title = row.get("var_title");
			Object name = row.get("var_name");
			if (name == null) {
				continue;
			}
			String varName = String.valueOf(name);
			if (title != null) {
				map.put(String.valueOf(title), varName);
			}
			map.putIfAbsent(varName, varName);
		}
		return map;
	}

	static Set<String> allowedPlaceholderKeys(List<Map<String, Object>> varDefs) {
		Set<String> allowed = new HashSet<>();
		if (varDefs == null) {
			return allowed;
		}
		for (Map<String, Object> row : varDefs) {
			if (row == null) {
				continue;
			}
			Object title = row.get("var_title");
			Object name = row.get("var_name");
			if (title != null) {
				allowed.add(String.valueOf(title));
			}
			if (name != null) {
				allowed.add(String.valueOf(name));
			}
		}
		return allowed;
	}

	static List<Map<String, Object>> decodeVariables(String variablesJson, ObjectMapper objectMapper) {
		if (variablesJson == null || variablesJson.isBlank() || "0".equals(variablesJson.trim())) {
			return List.of();
		}
		try {
			List<Map<String, Object>> list =
					objectMapper.readValue(variablesJson, new TypeReference<>() {});
			if (list == null || list.isEmpty()) {
				return List.of();
			}
			return list;
		} catch (Exception e) {
			return List.of();
		}
	}

	static Map<String, String> filterTemplateParam(
			String tmplContent, Map<String, String> data, String variablesJson, ObjectMapper objectMapper) {
		List<Map<String, Object>> varDefs = decodeVariables(variablesJson, objectMapper);
		if (varDefs.isEmpty()) {
			return new LinkedHashMap<>();
		}
		Map<String, String> placeholderToVarName = buildPlaceholderKeyToVarName(varDefs);
		Matcher m = TEMPLATE_VAR_PATTERN.matcher(tmplContent);
		List<String> allowedNames = new ArrayList<>();
		while (m.find()) {
			String placeholderKey = m.group(1);
			String varName = placeholderToVarName.get(placeholderKey);
			if (varName != null) {
				allowedNames.add(varName);
			}
		}
		LinkedHashMap<String, String> out = new LinkedHashMap<>();
		Map<String, String> safeData = data == null ? Map.of() : data;
		for (Map.Entry<String, String> e : safeData.entrySet()) {
			if (allowedNames.contains(e.getKey())) {
				out.put(e.getKey(), e.getValue());
			}
		}
		return out;
	}

	static String compileTemplateDisplay(
			String tmplContent, Map<String, String> data, String variablesJson, ObjectMapper objectMapper) {
		if (variablesJson == null || variablesJson.isBlank() || "0".equals(variablesJson.trim())) {
			return tmplContent;
		}
		List<Map<String, Object>> varDefs = decodeVariables(variablesJson, objectMapper);
		if (varDefs.isEmpty()) {
			return tmplContent;
		}
		Map<String, String> placeholderToVarName = buildPlaceholderKeyToVarName(varDefs);
		Matcher m = TEMPLATE_VAR_PATTERN.matcher(tmplContent);
		Map<String, String> patterns = new LinkedHashMap<>();
		while (m.find()) {
			String placeholderKey = m.group(1);
			String varName = placeholderToVarName.get(placeholderKey);
			if (varName != null) {
				patterns.put(placeholderKey, data.getOrDefault(varName, ""));
			} else {
				patterns.put(placeholderKey, "");
			}
		}
		String content = tmplContent;
		for (Map.Entry<String, String> e : patterns.entrySet()) {
			content = content.replace("${" + e.getKey() + "}", e.getValue() != null ? e.getValue() : "");
		}
		return content;
	}
}
