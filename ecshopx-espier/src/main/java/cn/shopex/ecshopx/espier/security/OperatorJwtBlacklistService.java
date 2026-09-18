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

package cn.shopex.ecshopx.espier.security;

import cn.shopex.ecshopx.common.auth.OperatorJwtBlacklistPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class OperatorJwtBlacklistService implements OperatorJwtBlacklistPort {

	private static final String KEY_PREFIX = "operator_jwt_invalidate:";

	private final StringRedisTemplate prismRedisTemplate;

	public OperatorJwtBlacklistService(
			@Qualifier("prismRedisTemplate") StringRedisTemplate prismRedisTemplate) {
		this.prismRedisTemplate = prismRedisTemplate;
	}

	@Override
	public void addToBlacklist(String compactJwt, long expirationEpochSeconds) {
		if (compactJwt == null || compactJwt.isBlank()) {
			return;
		}
		long nowSec = System.currentTimeMillis() / 1000L;
		long ttlSec = expirationEpochSeconds - nowSec;
		if (ttlSec < 1L) {
			ttlSec = 1L;
		}
		String key = redisKeyFor(compactJwt);
		prismRedisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(ttlSec));
	}

	@Override
	public boolean isBlacklisted(String compactJwt) {
		if (compactJwt == null || compactJwt.isBlank()) {
			return false;
		}
		return Boolean.TRUE.equals(prismRedisTemplate.hasKey(redisKeyFor(compactJwt)));
	}

	private static String redisKeyFor(String compactJwt) {
		return KEY_PREFIX + sha256HexUtf8(compactJwt);
	}

	private static String sha256HexUtf8(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}
}
