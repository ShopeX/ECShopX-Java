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

package cn.shopex.ecshopx.distribution.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class DistributorQrCodeRedisService {

	private final StringRedisTemplate sharedStringRedisTemplate;

	public DistributorQrCodeRedisService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate) {
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
	}

	/** 缓存命中（含 uri=""）返回 {@link Optional#of}；field 不存在返回 {@link Optional#empty()} */
	public Optional<String> hashGet(long companyId, long distributorId) {
		Object raw =
				sharedStringRedisTemplate
						.opsForHash()
						.get(hashKey(companyId), String.valueOf(distributorId));
		if (raw == null) {
			return Optional.empty();
		}
		return Optional.of(raw.toString());
	}

	/** 写入 field→uri；不设 expire */
	public void hashSet(long companyId, long distributorId, String uri) {
		sharedStringRedisTemplate
				.opsForHash()
				.put(hashKey(companyId), String.valueOf(distributorId), uri == null ? "" : uri);
	}

	private static String hashKey(long companyId) {
		return "hash:distributor_qr_code:" + sha1Hex(String.valueOf(companyId));
	}

	private static String sha1Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : digest) {
				hex.append(String.format("%02x", b));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-1 algorithm unavailable", e);
		}
	}
}
