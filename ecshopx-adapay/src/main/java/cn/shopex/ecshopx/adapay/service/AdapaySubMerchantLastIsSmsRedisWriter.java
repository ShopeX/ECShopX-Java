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

package cn.shopex.ecshopx.adapay.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AdapaySubMerchantLastIsSmsRedisWriter {

	private static final String KEY_PREFIX = "last_is_sms";

	private final StringRedisTemplate sharedStringRedisTemplate;

	public AdapaySubMerchantLastIsSmsRedisWriter(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate) {
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
	}

	public void setLastIsSms(long companyId, Object isSms) {
		String key = KEY_PREFIX + sha1Hex(String.valueOf(companyId));
		String value = isSms == null ? "" : String.valueOf(isSms);
		sharedStringRedisTemplate.opsForValue().set(key, value);
	}

	public String getLastIsSms(long companyId) {
		String key = KEY_PREFIX + sha1Hex(String.valueOf(companyId));
		return sharedStringRedisTemplate.opsForValue().get(key);
	}

	private static String sha1Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-1 not available", e);
		}
	}
}
