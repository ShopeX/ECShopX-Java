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

package cn.shopex.ecshopx.distribution.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SelfDeliverySettingReadService {

	private static final String KEY_PREFIX = "SelfDeliverySetting:";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public SelfDeliverySettingReadService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate, ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public void setSelfDeliverySetting(long companyId, String distributorRedisSuffix, Map<String, Object> params) {
		String key = buildRedisKey(companyId, distributorRedisSuffix);
		String json;
		if (params == null || params.isEmpty()) {
			json = "[]";
		} else {
			try {
				json = objectMapper.writeValueAsString(params);
			} catch (JsonProcessingException e) {
				throw new IllegalStateException(e);
			}
		}
		companysRedisTemplate.opsForValue().set(key, json);
	}

	/**
	 * 按公司 ID 与 Redis 键后缀读取自配送配置（与 POST 写入使用相同键规则）。
	 *
	 * @param distributorRedisSuffix 已解析的 Redis 键第三段（可来自 Query 覆盖或 JWT）
	 * @return 解码后的 Map；无键 / 空 / 字面 JSON null 时返回默认结构；解析失败或解码为 null 时返回 null
	 */
	public Map<String, Object> getSelfDeliverySetting(long companyId, String distributorRedisSuffix) {
		String key = buildRedisKey(companyId, distributorRedisSuffix);
		String raw = companysRedisTemplate.opsForValue().get(key);
		if (raw == null || !StringUtils.hasText(raw) || "null".equals(raw.trim())) {
			return defaultTemplate();
		}
		try {
			Map<String, Object> parsed = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
			return parsed;
		} catch (Exception e) {
			return null;
		}
	}

	public Map<String, Object> getSetting(long companyId, long distributorId, int distributorSelf) {
		long effectiveDistributorId = distributorSelf != 0 ? 0L : distributorId;
		String key = buildRedisKey(companyId, String.valueOf(effectiveDistributorId));
		String raw = companysRedisTemplate.opsForValue().get(key);
		if (StringUtils.hasText(raw) && !"null".equals(raw.trim())) {
			try {
				Map<String, Object> parsed = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
				if (parsed != null && !parsed.isEmpty()) {
					return parsed;
				}
			} catch (Exception ignored) {
				// fall through to default structure
			}
		}
		return defaultTemplate();
	}

	private String buildRedisKey(long companyId, String distributorRedisSuffix) {
		return KEY_PREFIX + sha1Hex(String.valueOf(companyId)) + ":" + distributorRedisSuffix;
	}

	private static Map<String, Object> defaultTemplate() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("is_open", Boolean.FALSE);
		m.put("min_amount", 0);
		m.put("freight_fee", 0);
		List<Map<String, Object>> rules = new ArrayList<>();
		for (int i = 0; i < 2; i++) {
			Map<String, Object> r = new LinkedHashMap<>();
			r.put("selected", Boolean.FALSE);
			r.put("full", 0);
			r.put("freight_fee", 0);
			rules.add(r);
		}
		m.put("rule", rules);
		return m;
	}

	private static String sha1Hex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(dig.length * 2);
			for (byte b : dig) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
