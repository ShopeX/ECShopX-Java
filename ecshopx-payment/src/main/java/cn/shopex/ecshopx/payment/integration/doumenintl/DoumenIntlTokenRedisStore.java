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

package cn.shopex.ecshopx.payment.integration.doumenintl;

import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Token Redis：JSON {@code {token, expires_at}}，逻辑过期，不设 EXPIRE。 */
@Component
public class DoumenIntlTokenRedisStore {

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public DoumenIntlTokenRedisStore(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public String getValidToken(String accessCode) {
		String raw =
				companysRedisTemplate
						.opsForValue()
						.get(PaymentSettingRedisKeys.doumenIntlTokenKey(accessCode));
		Map<String, Object> decoded = PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
		Object token = decoded.get("token");
		Object expiresAt = decoded.get("expires_at");
		if (!(token instanceof String t) || !StringUtils.hasText(t) || expiresAt == null) {
			return null;
		}
		long exp;
		if (expiresAt instanceof Number n) {
			exp = n.longValue();
		} else {
			try {
				exp = Long.parseLong(String.valueOf(expiresAt));
			} catch (NumberFormatException e) {
				return null;
			}
		}
		if (exp <= System.currentTimeMillis() / 1000L) {
			return null;
		}
		return t;
	}

	public void setToken(String accessCode, String token, int ttlSeconds) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("token", token);
		payload.put("expires_at", System.currentTimeMillis() / 1000L + ttlSeconds);
		try {
			companysRedisTemplate
					.opsForValue()
					.set(
							PaymentSettingRedisKeys.doumenIntlTokenKey(accessCode),
							objectMapper.writeValueAsString(payload));
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("failed to encode doumen token", e);
		}
	}
}
