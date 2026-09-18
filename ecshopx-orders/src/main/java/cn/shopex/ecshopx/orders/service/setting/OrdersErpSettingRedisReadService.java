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

package cn.shopex.ecshopx.orders.service.setting;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Reads ERP open flags from companys Redis using the same keys as the integration admin layer. */
@Service
public class OrdersErpSettingRedisReadService {

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public OrdersErpSettingRedisReadService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> readJushuitanSetting(long companyId) {
		return readJsonByKeyPrefix("JushuitanSetting:", companyId);
	}

	public Map<String, Object> readWdtErpSetting(long companyId) {
		return readJsonByKeyPrefix("WdtErpSetting:", companyId);
	}

	private Map<String, Object> readJsonByKeyPrefix(String keyPrefix, long companyId) {
		String key = keyPrefix + sha1HexUtf8(String.valueOf(companyId));
		String raw = companysRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			LinkedHashMap<String, Object> data = new LinkedHashMap<>();
			data.put("is_open", Boolean.FALSE);
			return data;
		}
		try {
			LinkedHashMap<String, Object> parsed =
					objectMapper.readValue(raw, new TypeReference<LinkedHashMap<String, Object>>() {});
			if (parsed == null || parsed.isEmpty()) {
				LinkedHashMap<String, Object> data = new LinkedHashMap<>();
				data.put("is_open", Boolean.FALSE);
				return data;
			}
			return parsed;
		} catch (Exception e) {
			LinkedHashMap<String, Object> data = new LinkedHashMap<>();
			data.put("is_open", Boolean.FALSE);
			return data;
		}
	}

	private static String sha1HexUtf8(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(dig);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
