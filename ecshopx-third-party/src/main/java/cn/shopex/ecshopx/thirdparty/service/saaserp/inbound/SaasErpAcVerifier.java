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

package cn.shopex.ecshopx.thirdparty.service.saaserp.inbound;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SaasErpAcVerifier {

	public boolean matches(Map<String, Object> rawParams, String token) {
		Map<String, Object> copy = new HashMap<>(rawParams);
		Object acObj = copy.remove("ac");
		String ac = acObj == null ? "" : String.valueOf(acObj);
		String acNormalized = ac.trim().toLowerCase(Locale.ROOT);

		List<String> keys = new ArrayList<>(copy.keySet());
		keys.sort(String::compareTo);

		StringBuilder concatenated = new StringBuilder();
		for (String key : keys) {
			Object value = copy.get(key);
			if (value == null) {
				continue;
			}
			if (value instanceof List || value instanceof Map) {
				concatenated.append("Array");
			} else {
				concatenated.append(String.valueOf(value));
			}
		}

		String tokenSuffix = token == null ? "" : token;
		String digest = md5LowerHex((concatenated.toString() + tokenSuffix).trim());

		return !acNormalized.isEmpty() && acNormalized.equals(digest);
	}

	private static String md5LowerHex(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder(hash.length * 2);
			for (byte b : hash) {
				hex.append(String.format(Locale.ROOT, "%02x", b));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("MD5 not available", e);
		}
	}
}
