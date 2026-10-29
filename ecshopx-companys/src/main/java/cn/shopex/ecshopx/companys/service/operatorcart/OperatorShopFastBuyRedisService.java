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

package cn.shopex.ecshopx.companys.service.operatorcart;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** Aligns with PHP {@code OperatorShopFastBuyRedisService}. */
@Service
public class OperatorShopFastBuyRedisService {

	private static final String KEY_PREFIX = "shop_fastbuy:";
	private static final Duration TTL = Duration.ofSeconds(600);

	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper;

	public OperatorShopFastBuyRedisService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate redis, ObjectMapper objectMapper) {
		this.redis = redis;
		this.objectMapper = objectMapper;
	}

	public String buildKey(long companyId, long operatorId, long distributorId) {
		return KEY_PREFIX + DigestUtils.sha1Hex(companyId + ":" + operatorId + ":" + distributorId);
	}

	public void set(long companyId, long operatorId, long distributorId, Map<String, Object> payload) {
		Map<String, Object> copy = payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
		copy.put("cart_id", 0);
		try {
			redis.opsForValue().set(buildKey(companyId, operatorId, distributorId), objectMapper.writeValueAsString(copy), TTL);
		} catch (Exception e) {
			throw new IllegalStateException("shop fastbuy redis serialize failed", e);
		}
	}

	public Map<String, Object> get(long companyId, long operatorId, long distributorId) {
		String raw = redis.opsForValue().get(buildKey(companyId, operatorId, distributorId));
		if (raw == null || raw.isBlank()) {
			return Collections.emptyMap();
		}
		try {
			Map<String, Object> decoded = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
			return decoded != null ? decoded : Collections.emptyMap();
		} catch (Exception e) {
			return Collections.emptyMap();
		}
	}

	public void clear(long companyId, long operatorId, long distributorId) {
		set(companyId, operatorId, distributorId, Collections.emptyMap());
	}
}
