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

package cn.shopex.ecshopx.companys.service.shopex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ShopexSmsClientFacade {

	private final StringRedisTemplate prismRedisTemplate;

	public ShopexSmsClientFacade(@Qualifier("prismRedisTemplate") StringRedisTemplate prismRedisTemplate) {
		this.prismRedisTemplate = prismRedisTemplate;
	}

	public void setAccessToken(long companyId, String shopexUid, String accessToken, long expiresAtEpochSeconds) {
		String key = accessTokenRedisKey(companyId, shopexUid);
		prismRedisTemplate.opsForValue().set(key, accessToken);
		if (expiresAtEpochSeconds > 0) {
			prismRedisTemplate.expireAt(key, java.time.Instant.ofEpochSecond(expiresAtEpochSeconds));
		}
	}

	public void setRefreshToken(long companyId, String shopexUid, String refreshToken, long refreshExpiresAtEpochSeconds) {
		String key = refreshTokenRedisKey(companyId, shopexUid);
		prismRedisTemplate.opsForValue().set(key, refreshToken);
		if (refreshExpiresAtEpochSeconds > 0) {
			prismRedisTemplate.expireAt(key, java.time.Instant.ofEpochSecond(refreshExpiresAtEpochSeconds));
		}
	}

	private static String accessTokenRedisKey(long companyId, String shopexUid) {
		return "prism:" + sha1Hex(shopexUid + "_" + companyId + "_AccessToken");
	}

	private static String refreshTokenRedisKey(long companyId, String shopexUid) {
		return "prism:" + sha1Hex(shopexUid + "_" + companyId + "_RefreshToken");
	}

	private static String sha1Hex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : d) {
				hex.append(String.format("%02x", b & 0xFF));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
