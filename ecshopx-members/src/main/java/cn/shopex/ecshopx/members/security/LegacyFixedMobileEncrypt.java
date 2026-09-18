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

package cn.shopex.ecshopx.members.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Deterministic storage transform for member mobile columns, matching legacy app-key AES
 * payload layout (fixed IV, legacy string wire format, HMAC) without feature switches or
 * plaintext fallback.
 */
public final class LegacyFixedMobileEncrypt {

	private static final byte[] FIXED_IV_RAW = "X3xMx3dnoCtSprda".getBytes(StandardCharsets.UTF_8);

	private static final ObjectMapper JSON = new ObjectMapper();

	private static volatile byte[] cipherKey;

	/**
	 * When {@code false}, stored mobile remains plaintext with {@code encrypt_sensitive_data}
	 * disabled: plaintext in {@code members.mobile} and related queries.
	 */
	private static volatile boolean storageEncryptEnabled;

	private LegacyFixedMobileEncrypt() {
	}

	public static void configure(String appKeyConfig, String cipher, boolean encryptSensitiveData) {
		storageEncryptEnabled = encryptSensitiveData;
		if (!encryptSensitiveData) {
			cipherKey = null;
			return;
		}
		String c = cipher == null || cipher.isBlank() ? "AES-128-CBC" : cipher.trim();
		cipherKey = resolveKey(appKeyConfig, c);
	}

	public static String fixedEncryptMobile(String plainMobile) {
		if (plainMobile == null) {
			return null;
		}
		if (!storageEncryptEnabled) {
			return plainMobile;
		}
		byte[] key = cipherKey;
		if (key == null) {
			throw new IllegalStateException("缺少应用密钥配置，无法加密手机号存储");
		}
		try {
			doDecrypt(plainMobile, key);
			return plainMobile;
		} catch (Exception ignored) {
			try {
				return doEncrypt(plainMobile, key);
			} catch (Exception e) {
				throw new IllegalStateException("手机号存储加密失败", e);
			}
		}
	}

	private static byte[] resolveKey(String appKeyConfig, String cipherName) {
		if (appKeyConfig == null || appKeyConfig.isEmpty()) {
			return null;
		}
		String k = appKeyConfig.trim();
		if (k.startsWith("base64:")) {
			k = k.substring(7);
		}
		byte[] raw = Base64.getDecoder().decode(k);
		int need = cipherName.contains("256") ? 32 : 16;
		if (raw.length == need) {
			return raw;
		}
		if (need == 16 && raw.length == 32) {
			return Arrays.copyOf(raw, 16);
		}
		return null;
	}

	private static String doEncrypt(String plain, byte[] key) throws Exception {
		byte[] serialized = serializeString(plain).getBytes(StandardCharsets.UTF_8);
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(FIXED_IV_RAW));
		byte[] enc = cipher.doFinal(serialized);
		String valueB64 = Base64.getEncoder().encodeToString(enc);
		String ivB64 = Base64.getEncoder().encodeToString(FIXED_IV_RAW);
		String mac = hmacSha256(ivB64 + valueB64, key);
		String json = JSON.createObjectNode().put("iv", ivB64).put("value", valueB64).put("mac", mac).toString();
		return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
	}

	private static void doDecrypt(String payloadB64, byte[] key) throws Exception {
		decryptStoredToPlain(payloadB64, key);
	}

	/**
	 * Decrypts a stored legacy AES+HMAC payload to the inner plaintext string (legacy wire string decoded).
	 */
	private static String decryptStoredToPlain(String payloadB64, byte[] key) throws Exception {
		byte[] jsonBytes = Base64.getDecoder().decode(payloadB64);
		JsonNode root = JSON.readTree(jsonBytes);
		String ivB64 = root.get("iv").asText();
		String valueB64 = root.get("value").asText();
		String mac = root.get("mac").asText();
		String expectMac = hmacSha256(ivB64 + valueB64, key);
		if (!constantTimeEquals(mac, expectMac)) {
			throw new IllegalArgumentException("mac");
		}
		byte[] iv = Base64.getDecoder().decode(ivB64);
		byte[] enc = Base64.getDecoder().decode(valueB64);
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
		byte[] decrypted = cipher.doFinal(enc);
		String s = new String(decrypted, StandardCharsets.UTF_8);
		return unserializeString(s);
	}

	/**
	 * Decrypts a column value produced by {@link #fixedEncryptMobile(String)} when storage encryption
	 * is enabled; otherwise returns the value unchanged.
	 */
	public static String fixedDecryptLegacyPayload(String stored) {
		if (stored == null) {
			return null;
		}
		if (!storageEncryptEnabled) {
			return stored;
		}
		byte[] key = cipherKey;
		if (key == null) {
			throw new IllegalStateException("legacy fixed decrypt failed");
		}
		try {
			return decryptStoredToPlain(stored, key);
		} catch (Exception e) {
			throw new IllegalStateException("legacy fixed decrypt failed", e);
		}
	}

	private static boolean constantTimeEquals(String a, String b) {
		if (a == null || b == null || a.length() != b.length()) {
			return false;
		}
		int r = 0;
		for (int i = 0; i < a.length(); i++) {
			r |= a.charAt(i) ^ b.charAt(i);
		}
		return r == 0;
	}

	private static String hmacSha256(String data, byte[] key) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(key, "HmacSHA256"));
		byte[] out = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
		return bytesToHex(out);
	}

	private static String bytesToHex(byte[] raw) {
		StringBuilder sb = new StringBuilder(raw.length * 2);
		for (byte b : raw) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	private static String serializeString(String s) {
		byte[] utf8 = s.getBytes(StandardCharsets.UTF_8);
		String escaped = s.replace("\\", "\\\\").replace("\"", "\\\"");
		return "s:" + utf8.length + ":\"" + escaped + "\";";
	}

	private static String unserializeString(String raw) {
		if (!raw.startsWith("s:")) {
			throw new IllegalArgumentException("expected string");
		}
		int colon1 = raw.indexOf(':', 2);
		int len = Integer.parseInt(raw.substring(2, colon1));
		int q = raw.indexOf('"', colon1);
		if (q < 0) {
			throw new IllegalArgumentException("quote");
		}
		int start = q + 1;
		String inner = raw.substring(start, start + len);
		return inner.replace("\\\\", "\\").replace("\\\"", "\"");
	}
}
