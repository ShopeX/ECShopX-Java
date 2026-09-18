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

import cn.shopex.ecshopx.common.auth.H5JwtBlacklistPort;
import cn.shopex.ecshopx.members.config.H5JwtProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class H5JwtBlacklistService implements H5JwtBlacklistPort {

	private static final String KEY_PREFIX = "h5_jwt_invalidate:";

	private final StringRedisTemplate prismRedisTemplate;

	private final H5JwtProperties h5JwtProperties;

	public H5JwtBlacklistService(
			@Qualifier("prismRedisTemplate") StringRedisTemplate prismRedisTemplate,
			H5JwtProperties h5JwtProperties) {
		this.prismRedisTemplate = prismRedisTemplate;
		this.h5JwtProperties = h5JwtProperties;
	}

	@Override
	public void addToBlacklist(String compactJwt, long expirationEpochSeconds) {
		if (compactJwt == null || compactJwt.isBlank()) {
			return;
		}
		String key = redisKeyFor(compactJwt);
		if (Boolean.TRUE.equals(prismRedisTemplate.hasKey(key))) {
			return;
		}
		long nowSec = System.currentTimeMillis() / 1000L;
		int grace = Math.max(0, h5JwtProperties.getBlacklistGracePeriodSeconds());
		long validUntilSec = nowSec + grace;
		long ttlSec = expirationEpochSeconds - nowSec;
		if (ttlSec < 1L) {
			ttlSec = 1L;
		}
		long minTtl = validUntilSec - nowSec + 1L;
		if (ttlSec < minTtl) {
			ttlSec = minTtl;
		}
		prismRedisTemplate.opsForValue().set(key, String.valueOf(validUntilSec), Duration.ofSeconds(ttlSec));
	}

	@Override
	public boolean isBlacklisted(String compactJwt) {
		if (compactJwt == null || compactJwt.isBlank()) {
			return false;
		}
		String key = redisKeyFor(compactJwt);
		String stored = prismRedisTemplate.opsForValue().get(key);
		if (stored == null || stored.isBlank()) {
			return false;
		}
		long validUntilSec;
		try {
			validUntilSec = Long.parseLong(stored.trim());
		} catch (NumberFormatException e) {
			return true;
		}
		long nowSec = System.currentTimeMillis() / 1000L;
		return nowSec >= validUntilSec;
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
