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

package cn.shopex.ecshopx.common.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * PHP Laravel 兼容的邮件配置密码编解码器。
 *
 * <p>对齐 {@code fixedencrypt->default()->encrypt($plain, false)} / {@code decrypt($input, false)} 语义：
 * 固定 IV、AES-CBC、HMAC-SHA256 over {@code base64(iv) + base64(ciphertext)}、JSON + Base64 载荷，且
 * <b>不对明文做 PHP 序列化</b>（raw 模式）。密钥按 Laravel 语义解析：以 {@code base64:} 前缀则先解码，
 * 否则使用原始字符串字节（如 PHP {@code APP_KEY}）。</p>
 */
@Component
public class MailSettingPasswordCodec {

	private static final byte[] FIXED_IV_RAW = "X3xMx3dnoCtSprda".getBytes(StandardCharsets.UTF_8);

	private static final ObjectMapper JSON = new ObjectMapper();

	private final byte[] cipherKey;

	public MailSettingPasswordCodec(
			@Value("${ecshopx.mail.password.app-key:}") String appKeyConfig,
			@Value("${ecshopx.mail.password.cipher:AES-256-CBC}") String cipherName) {
		this.cipherKey = resolveKey(appKeyConfig, cipherName);
	}

	/**
	 * 明文为空返回空串；否则返回加密值（与 PHP GET 语义一致）。密钥缺失或加密失败时降级返回明文。
	 */
	public String encryptMailPasswordForApiResponse(String plain) {
		if (plain == null || plain.isEmpty()) {
			return "";
		}
		if (cipherKey == null) {
			return plain;
		}
		try {
			return encryptRaw(plain);
		} catch (Exception e) {
			return plain;
		}
	}

	/**
	 * 提交值为空沿用已有明文；非空先尝试解密，解密失败视为新明文直接使用（与 PHP POST 语义一致）。
	 */
	public String resolveMailPasswordFromSaveInput(String input, String existingPlain) {
		if (input == null || input.isEmpty()) {
			return existingPlain == null ? "" : existingPlain;
		}
		try {
			return decryptRaw(input);
		} catch (Exception e) {
			return input;
		}
	}

	/**
	 * 发信前取明文：存储为空返回空串；否则尝试解密，失败按明文使用（与
	 * {@code MailSettingStoredPasswordResolver::plainForSmtp} 一致）。
	 */
	public String plainForSmtp(String stored) {
		if (stored == null || stored.isEmpty()) {
			return "";
		}
		try {
			return decryptRaw(stored);
		} catch (Exception e) {
			return stored;
		}
	}

	private static byte[] resolveKey(String appKeyConfig, String cipherName) {
		if (appKeyConfig == null || appKeyConfig.isEmpty()) {
			return null;
		}
		String k = appKeyConfig.trim();
		final byte[] raw;
		if (k.startsWith("base64:")) {
			try {
				raw = Base64.getDecoder().decode(k.substring(7));
			} catch (IllegalArgumentException e) {
				return null;
			}
		} else {
			raw = k.getBytes(StandardCharsets.UTF_8);
		}
		int need = cipherName != null && cipherName.contains("256") ? 32 : 16;
		if (raw.length != need) {
			return null;
		}
		return raw;
	}

	private String encryptRaw(String plain) throws Exception {
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cipherKey, "AES"), new IvParameterSpec(FIXED_IV_RAW));
		byte[] enc = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
		String valueB64 = Base64.getEncoder().encodeToString(enc);
		String ivB64 = Base64.getEncoder().encodeToString(FIXED_IV_RAW);
		String mac = hmacSha256(ivB64 + valueB64, cipherKey);
		String json = JSON.createObjectNode().put("iv", ivB64).put("value", valueB64).put("mac", mac).toString();
		return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
	}

	private String decryptRaw(String payloadB64) throws Exception {
		byte[] jsonBytes = Base64.getDecoder().decode(payloadB64);
		JsonNode root = JSON.readTree(jsonBytes);
		String ivB64 = root.get("iv").asText();
		String valueB64 = root.get("value").asText();
		String mac = root.get("mac").asText();
		String expectMac = hmacSha256(ivB64 + valueB64, cipherKey);
		if (!constantTimeEquals(mac, expectMac)) {
			throw new IllegalArgumentException("mac");
		}
		byte[] iv = Base64.getDecoder().decode(ivB64);
		byte[] enc = Base64.getDecoder().decode(valueB64);
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cipherKey, "AES"), new IvParameterSpec(iv));
		byte[] decrypted = cipher.doFinal(enc);
		return new String(decrypted, StandardCharsets.UTF_8);
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
			sb.append(String.format("%02x", b & 0xff));
		}
		return sb.toString();
	}
}
