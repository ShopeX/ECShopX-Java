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

package cn.shopex.ecshopx.youshu.integration;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class YoushuOpenApiSignSupport {

	private static final SecureRandom RANDOM = new SecureRandom();

	private YoushuOpenApiSignSupport() {}

	static Map<String, String> buildSignedQueryParams(String appId, String appSecret) {
		String nonce = randomNonce(32);
		long timestamp = Instant.now().getEpochSecond();
		String signType = "sha256";
		String str = "app_id=" + appId + "&nonce=" + nonce + "&sign=" + signType + "&timestamp=" + timestamp;
		String signature = hmacSha256Hex(str, appSecret);
		Map<String, String> m = new LinkedHashMap<>();
		m.put("app_id", appId);
		m.put("nonce", nonce);
		m.put("sign", signType);
		m.put("timestamp", Long.toString(timestamp));
		m.put("signature", signature);
		return m;
	}

	private static String randomNonce(int len) {
		String chars = "abcdefghijklmnopqrstuvwxyz123456789";
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
		}
		return sb.toString();
	}

	private static String hmacSha256Hex(String data, String secret) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
			return hexLower(raw);
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String hexLower(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}
