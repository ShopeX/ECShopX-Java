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

package cn.shopex.ecshopx.thirdparty.service.dmcrm;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DmCrmSettingAdminService {

	private static final String SETTING_PREFIX = "DmCrmSetting:";

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public DmCrmSettingAdminService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public void setSetting(long companyId, Map<String, Object> postdata) {
		Map<String, Object> src = postdata != null ? postdata : Collections.emptyMap();

		if (!src.containsKey("ent_sign") || src.get("ent_sign") == null) {
			throw new BadRequestException("缺少参数 ent_sign");
		}
		if (!src.containsKey("app_key") || src.get("app_key") == null) {
			throw new BadRequestException("缺少参数 app_key");
		}
		if (!src.containsKey("app_secret") || src.get("app_secret") == null) {
			throw new BadRequestException("缺少参数 app_secret");
		}

		boolean isOpen;
		if (!src.containsKey("is_open") || src.get("is_open") == null) {
			isOpen = false;
		} else {
			Object v = src.get("is_open");
			if (v instanceof Boolean b) {
				isOpen = b.booleanValue();
			} else {
				isOpen = "true".equals(String.valueOf(v));
			}
		}

		String entSign = String.valueOf(src.get("ent_sign")).trim();
		String appKey = String.valueOf(src.get("app_key")).trim();
		String appSecret = String.valueOf(src.get("app_secret")).trim();
		String encodeAesKey = src.containsKey("app_secret") && src.get("app_secret") != null
				? String.valueOf(src.get("app_secret"))
				: "";
		String url = src.containsKey("url") && src.get("url") != null
				? String.valueOf(src.get("url")).trim()
				: "";

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("is_open", isOpen);
		data.put("ent_sign", entSign);
		data.put("app_key", appKey);
		data.put("app_secret", appSecret);
		data.put("encodeAESKey", encodeAesKey);
		data.put("url", url);
		data.put("company_id", companyId);

		String json;
		try {
			json = objectMapper.writeValueAsString(data);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("配置序列化失败");
		}

		String key = redisKey(companyId);
		try {
			stringRedisTemplate.opsForValue().set(key, json);
		} catch (DataAccessException e) {
			throw new ResourceException("Redis 不可用，请稍后重试");
		}
	}

	public Object getSetting(long companyId) {
		String key = redisKey(companyId);
		String raw;
		try {
			raw = stringRedisTemplate.opsForValue().get(key);
		} catch (DataAccessException e) {
			throw new ResourceException("Redis 不可用，请稍后重试");
		}

		if (raw == null || "0".equals(raw) || !StringUtils.hasText(raw)) {
			LinkedHashMap<String, Object> closed = new LinkedHashMap<>();
			closed.put("is_open", Boolean.FALSE);
			return closed;
		}

		try {
			return objectMapper.readValue(raw, Object.class);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	private static String redisKey(long companyId) {
		return SETTING_PREFIX + sha1Hex(String.valueOf(companyId));
	}

	private static String sha1Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] d = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(d.length * 2);
			for (byte b : d) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
