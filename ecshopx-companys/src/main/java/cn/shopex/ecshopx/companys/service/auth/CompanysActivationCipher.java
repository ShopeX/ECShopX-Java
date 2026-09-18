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

package cn.shopex.ecshopx.companys.service.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 运营商激活载荷：XOR、密钥派生与既有 checkUserAuth 兼容的序列化子集。
 */
public final class CompanysActivationCipher {

	private CompanysActivationCipher() {}

	/**
	 * 与解密链路对称：serialize → 随机 md5 成对 XOR → key() → Base64。
	 */
	public static String encryptUserPayload(long operatorId, String token, long time) {
		String serialized = serializeAuthTriplet(operatorId, token, time);
		String encryptKey = md5Hex(String.valueOf(ThreadLocalRandom.current().nextInt(10001)));
		StringBuilder pairBuilder = new StringBuilder();
		int ctr = 0;
		byte[] serializedBytes = serialized.getBytes(StandardCharsets.ISO_8859_1);
		for (byte b : serializedBytes) {
			if (ctr == encryptKey.length()) {
				ctr = 0;
			}
			char k = encryptKey.charAt(ctr);
			pairBuilder.append(k).append((char) ((b & 0xFF) ^ k));
			ctr++;
		}
		byte[] afterPairs = pairBuilder.toString().getBytes(StandardCharsets.ISO_8859_1);
		byte[] afterKey = keyTransform(afterPairs);
		return Base64.getEncoder().encodeToString(afterKey);
	}

	private static String serializeAuthTriplet(long operatorId, String token, long time) {
		String idStr = String.valueOf(operatorId);
		return "a:3:{s:2:\"id\";s:" + idStr.length() + ":\"" + idStr + "\";s:5:\"token\";s:" + token.length()
				+ ":\"" + token + "\";s:4:\"time\";i:" + time + ";}";
	}

	private static String md5Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] d = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : d) {
				hex.append(String.format("%02x", b));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	public static Map<String, String> decryptUserPayload(String encoded) {
		byte[] raw = java.util.Base64.getDecoder().decode(encoded);
		byte[] xored = keyTransform(raw);
		byte[] decoded = xorDecode(xored);
		String serialized = new String(decoded, StandardCharsets.ISO_8859_1);
		return parseSerializedStringMap(stripSerializedArrayWrapper(serialized));
	}

	private static String stripSerializedArrayWrapper(String s) {
		if (s.startsWith("a:")) {
			int start = s.indexOf('{');
			int end = s.lastIndexOf('}');
			if (start >= 0 && end > start) {
				return s.substring(start + 1, end);
			}
		}
		return s;
	}

	private static byte[] keyTransform(byte[] arrayString) {
		String codetoken = "YW53dWx1eWFudGFuZw";
		byte[] encryptKey = md5Bytes(codetoken);
		int ctr = 0;
		byte[] out = new byte[arrayString.length];
		for (int i = 0; i < arrayString.length; i++) {
			if (ctr == encryptKey.length) {
				ctr = 0;
			}
			out[i] = (byte) (arrayString[i] ^ encryptKey[ctr++]);
		}
		return out;
	}

	private static byte[] xorDecode(byte[] encodeString) {
		StringBuilder resultString = new StringBuilder();
		for (int i = 0; i < encodeString.length; i++) {
			int md5 = encodeString[i] & 0xFF;
			if (i + 1 >= encodeString.length) {
				break;
			}
			i++;
			int ch = encodeString[i] & 0xFF;
			resultString.append((char) (ch ^ md5));
		}
		return resultString.toString().getBytes(StandardCharsets.ISO_8859_1);
	}

	private static Map<String, String> parseSerializedStringMap(String s) {
		Map<String, String> map = new HashMap<>();
		int i = 0;
		while (i < s.length()) {
			if (!s.startsWith("s:", i)) {
				break;
			}
			i += 2;
			int colon = s.indexOf(':', i);
			int keyLen = Integer.parseInt(s.substring(i, colon));
			i = colon + 2;
			String key = s.substring(i, i + keyLen);
			i += keyLen + 2;
			if (i >= s.length()) {
				break;
			}
			char type = s.charAt(i);
			if (type == 's') {
				i += 2;
				colon = s.indexOf(':', i);
				int valLen = Integer.parseInt(s.substring(i, colon));
				i = colon + 2;
				String val = s.substring(i, i + valLen);
				i += valLen + 2;
				map.put(key, val);
			} else if (type == 'i') {
				i += 2;
				int semi = s.indexOf(';', i);
				map.put(key, s.substring(i, semi));
				i = semi + 1;
			} else {
				break;
			}
		}
		return map;
	}

	private static byte[] md5Bytes(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] d = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : d) {
				hex.append(String.format("%02x", b));
			}
			return hex.toString().getBytes(StandardCharsets.US_ASCII);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
