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

package cn.shopex.ecshopx.orders.service.supplier;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Reads Shopex ERP open flag from shared Redis (same key scheme as company settings) for list
 * status messaging without adding a Maven dependency on ecshopx-companys.
 */
@Component
public class OrdersShopexErpSettingRedisReader {

	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper;

	public OrdersShopexErpSettingRedisReader(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis, ObjectMapper objectMapper) {
		this.redis = redis;
		this.objectMapper = objectMapper;
	}

	public boolean isErpOpen(long companyId) {
		Optional<Map<String, Object>> parsed = readParsed(companyId);
		if (parsed.isEmpty()) {
			return false;
		}
		Object v = parsed.get().get("is_open");
		if (v instanceof Boolean b) {
			return Boolean.TRUE.equals(b);
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		return false;
	}

	private Optional<Map<String, Object>> readParsed(long companyId) {
		String key = "ShopexerpSetting:" + sha1Hex(String.valueOf(companyId));
		String raw = redis.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			return Optional.empty();
		}
		try {
			return Optional.of(objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {}));
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	private static String sha1Hex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] raw = md.digest(s.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(raw);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
