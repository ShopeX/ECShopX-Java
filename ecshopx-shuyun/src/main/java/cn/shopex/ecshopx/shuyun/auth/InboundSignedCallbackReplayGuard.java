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

package cn.shopex.ecshopx.shuyun.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Locale;
import java.util.function.LongSupplier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * ECX-10841：入站回调时间窗 + Redis 幂等防重放（键 {@code shuyun:cb:replay:{sha256(sign|time)}}）。
 */
@Service
public class InboundSignedCallbackReplayGuard {

	private static final String KEY_PREFIX = "shuyun:cb:replay:";

	private final StringRedisTemplate stringRedisTemplate;

	public InboundSignedCallbackReplayGuard(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	/** Production entry: uses wall clock. */
	public boolean assertFreshAndOnce(String sign, String requestTimeRaw, int skewSeconds, int ttlSeconds) {
		return assertFreshAndOnce(sign, requestTimeRaw, skewSeconds, ttlSeconds, System::currentTimeMillis);
	}

	/**
	 * @param nowMsSupplier injectable clock for tests
	 * @return true if within skew and first sighting of this sign+time
	 */
	public boolean assertFreshAndOnce(
			String sign,
			String requestTimeRaw,
			int skewSeconds,
			int ttlSeconds,
			LongSupplier nowMsSupplier) {
		if (!StringUtils.hasText(sign) || !StringUtils.hasText(requestTimeRaw)) {
			return false;
		}
		long requestMs;
		try {
			requestMs = Long.parseLong(requestTimeRaw.trim());
		} catch (NumberFormatException e) {
			return false;
		}
		if (requestMs <= 0L) {
			return false;
		}
		// Accept seconds-epoch (10 digits) by scaling to ms
		if (requestMs < 100_000_000_000L) {
			requestMs = requestMs * 1000L;
		}
		long nowMs = nowMsSupplier.getAsLong();
		long skewMs = Math.max(0, skewSeconds) * 1000L;
		if (Math.abs(nowMs - requestMs) > skewMs) {
			return false;
		}
		int ttl = Math.max(1, ttlSeconds);
		String key = KEY_PREFIX + sha256Hex(sign.trim() + "|" + requestTimeRaw.trim());
		Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(ttl));
		return Boolean.TRUE.equals(ok);
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
