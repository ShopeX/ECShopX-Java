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

package cn.shopex.ecshopx.espier.service.config;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 从 {@link RequestFieldProperties} 读取默认字段与「必须开启且必填」字段列表。
 *
 * <p>Locale 暂固定为 {@code zh_cn}，待与运营端语言源对齐。
 */
@Component
public class RequestFieldDefaultConfigProvider {

	private static final String FIXED_LOCALE = "zh_cn";

	private final RequestFieldProperties properties;

	public RequestFieldDefaultConfigProvider(RequestFieldProperties properties) {
		this.properties = properties;
	}

	public Map<String, Map<String, Object>> getDefaultFieldsFromConfig(int companyId, int moduleType) {
		String moduleKey = String.valueOf(moduleType);
		if (properties.isOemShuyun()) {
			Map<String, Map<String, Object>> m = properties.getShuyun().getDefaults().get(moduleKey);
			return m != null ? new LinkedHashMap<>(deepCopyOuter(m)) : new LinkedHashMap<>();
		}
		Map<String, Map<String, Map<String, Object>>> byModule =
				properties.getStandard().getDefaults().get(FIXED_LOCALE);
		if (byModule == null) {
			return new LinkedHashMap<>();
		}
		Map<String, Map<String, Object>> fields = byModule.get(moduleKey);
		return fields != null ? new LinkedHashMap<>(deepCopyOuter(fields)) : new LinkedHashMap<>();
	}

	private static Map<String, Map<String, Object>> deepCopyOuter(Map<String, Map<String, Object>> src) {
		Map<String, Map<String, Object>> out = new LinkedHashMap<>();
		for (Map.Entry<String, Map<String, Object>> e : src.entrySet()) {
			out.put(e.getKey(), e.getValue() != null ? new LinkedHashMap<>(e.getValue()) : new LinkedHashMap<>());
		}
		return out;
	}

	public List<String> getMustStartAndRequiredFieldsFromConfig(int companyId, int moduleType) {
		String moduleKey = String.valueOf(moduleType);
		String raw;
		if (properties.isOemShuyun()) {
			raw = properties.getShuyun().getMustStartRequired().get(moduleKey);
		} else {
			raw = properties.getStandard().getMustStartRequired().get(moduleKey);
		}
		if (raw == null || raw.isBlank()) {
			return List.of();
		}
		return Arrays.stream(raw.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toList());
	}
}
