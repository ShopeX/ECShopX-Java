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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Reads {@code DmCrmSetting:{sha1(companyId)}} from Redis to determine whether point integration is
 * enabled. When {@code ecshopx.thirdparty.dm-crm.setting-read.redis-enabled} is false, this bean is
 * not registered and {@link DmCrmSettingReadNoOp} applies (integration treated as off).
 */
@Service("dmCrmSettingReadRedis")
@Primary
@ConditionalOnProperty(
		prefix = "ecshopx.thirdparty.dm-crm.setting-read",
		name = "redis-enabled",
		havingValue = "true")
public class DmCrmSettingReadRedisService implements DmCrmSettingReadPort {

	private static final String SETTING_PREFIX = "DmCrmSetting:";

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public DmCrmSettingReadRedisService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	@Override
	public boolean isPointIntegrationOpen(long companyId) {
		try {
			String key = SETTING_PREFIX + sha1Hex(String.valueOf(companyId));
			String raw = stringRedisTemplate.opsForValue().get(key);
			if (!StringUtils.hasText(raw)) {
				return false;
			}
			JsonNode root = objectMapper.readTree(raw);
			return parseIsOpen(root);
		} catch (Exception ignored) {
			return false;
		}
	}

	private static boolean parseIsOpen(JsonNode root) {
		if (root == null || !root.has("is_open") || root.get("is_open").isNull()) {
			return false;
		}
		JsonNode n = root.get("is_open");
		if (n.isBoolean()) {
			return n.booleanValue();
		}
		if (n.isNumber()) {
			return n.asDouble() != 0.0;
		}
		if (n.isTextual()) {
			String t = n.asText("").trim();
			if (t.isEmpty() || "0".equals(t) || "false".equalsIgnoreCase(t)) {
				return false;
			}
			return true;
		}
		return false;
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
