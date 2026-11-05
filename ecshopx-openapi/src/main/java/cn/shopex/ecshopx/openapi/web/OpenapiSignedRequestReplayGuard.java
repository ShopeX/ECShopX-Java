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

package cn.shopex.ecshopx.openapi.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Locale;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * ECX-10869：OpenAPI 签名请求 Redis 幂等防重放（键 {@code
 * openapi:auth:replay:{sha256(app_key|sign|timestamp)}}）。
 */
@Service
public class OpenapiSignedRequestReplayGuard {

	private static final String KEY_PREFIX = "openapi:auth:replay:";

	private final StringRedisTemplate stringRedisTemplate;

	public OpenapiSignedRequestReplayGuard(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	/**
	 * @return true if this app_key+sign+timestamp is first sighting within TTL
	 */
	public boolean assertOnce(String appKey, String sign, String timestamp, int ttlSeconds) {
		if (!StringUtils.hasText(appKey)
				|| !StringUtils.hasText(sign)
				|| !StringUtils.hasText(timestamp)) {
			return false;
		}
		int ttl = Math.max(1, ttlSeconds);
		String key =
				KEY_PREFIX
						+ sha256Hex(
								appKey.trim() + "|" + sign.trim() + "|" + timestamp.trim());
		try {
			Boolean ok =
					stringRedisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(ttl));
			return Boolean.TRUE.equals(ok);
		} catch (RuntimeException ex) {
			return false;
		}
	}

	private static String sha256Hex(String raw) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder(digest.length * 2);
			for (byte b : digest) {
				hex.append(String.format(Locale.ROOT, "%02x", b));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}
}
