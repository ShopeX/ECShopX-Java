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

package cn.shopex.ecshopx.espier.service.printer;

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PrinterCompanyConfigApplicationService {

	private static final List<String> SUPPORTED_TYPES = List.of("yilianyun");

	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper;

	public PrinterCompanyConfigApplicationService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate redis,
			ObjectMapper objectMapper) {
		this.redis = redis;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> info(long companyId, String type) {
		if (type == null || !StringUtils.hasText(type.trim()) || !SUPPORTED_TYPES.contains(type.trim())) {
			throw new ResourceException("打印机配置类型错误");
		}
		String normalizedType = type.trim();
		return readStoredConfigOrDefault(companyId, normalizedType);
	}

	private Map<String, Object> readStoredConfigOrDefault(long companyId, String normalizedType) {
		String key = "printer:" + companyId + ":config:" + normalizedType;
		String raw = redis.opsForValue().get(key);
		if (raw == null || !StringUtils.hasText(raw)) {
			return defaultConfigMap(normalizedType);
		}
		try {
			LinkedHashMap<String, Object> parsed =
					objectMapper.readValue(raw, new TypeReference<LinkedHashMap<String, Object>>() {});
			if (parsed == null) {
				return defaultConfigMap(normalizedType);
			}
			return parsed;
		} catch (JsonProcessingException e) {
			return defaultConfigMap(normalizedType);
		}
	}

	public Map<String, Object> update(long companyId, Map<String, Object> payload) {
		Object isOpen = payload.get("is_open");
		if (!"true".equals(isOpen) && !"false".equals(isOpen)) {
			throw new ResourceException("开启状态必填");
		}
		requireNonBlank(payload, "person_id", "请填用户ID");
		requireNonBlank(payload, "app_id", "请填写应用ID");
		requireNonBlank(payload, "app_key", "请填写应用密钥");

		Object typeObj = payload.get("type");
		if (typeObj == null || !StringUtils.hasText(String.valueOf(typeObj).trim())) {
			throw new ResourceException("打印机配置类型错误");
		}
		String typeTrim = String.valueOf(typeObj).trim();
		if (!SUPPORTED_TYPES.contains(typeTrim)) {
			throw new ResourceException("打印机配置类型错误");
		}

		String type = typeTrim;

		String key = "printer:" + companyId + ":config:" + type;
		String json;
		try {
			json = objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			throw new ResourceException("保存配置失败");
		}
		redis.opsForValue().set(key, json);

		String raw = redis.opsForValue().get(key);
		Map<String, Object> result;
		if (raw == null || !StringUtils.hasText(raw)) {
			result = defaultConfigMap(type);
		} else {
			try {
				LinkedHashMap<String, Object> parsed =
						objectMapper.readValue(raw, new TypeReference<LinkedHashMap<String, Object>>() {});
				if (parsed == null) {
					result = defaultConfigMap(type);
				} else {
					result = parsed;
				}
			} catch (JsonProcessingException e) {
				result = defaultConfigMap(type);
			}
		}

		Object resultType = result.get("type");
		String resultTypeStr = resultType == null ? "" : String.valueOf(resultType).trim();
		if (!SUPPORTED_TYPES.contains(resultTypeStr)) {
			throw new ResourceException("打印机配置类型错误");
		}
		return result;
	}

	private static LinkedHashMap<String, Object> defaultConfigMap(String type) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("is_open", Boolean.FALSE);
		m.put("person_id", "");
		m.put("app_id", "");
		m.put("app_key", "");
		m.put("is_hide", Boolean.FALSE);
		m.put("type", type);
		return m;
	}

	private static void requireNonBlank(Map<String, Object> payload, String key, String message) {
		if (!payload.containsKey(key)) {
			throw new ResourceException(message);
		}
		Object v = payload.get(key);
		if (v == null || !StringUtils.hasText(String.valueOf(v).trim())) {
			throw new ResourceException(message);
		}
	}
}
