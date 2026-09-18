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

package cn.shopex.ecshopx.companys.service.activation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * XOR + MD5 payload encryption for operator license storage (same {@code codetoken} chain as JWT inner id).
 */
@Component
public class CompanysLicensePayloadEncryptor {

	public String encryptActiveCodePlain(String plain) {
		if (plain == null) {
			plain = "";
		}
		return encryptSerialized(DiscuzAuthPayloadCodec.serializeWireScalarString(plain));
	}

	public String encryptParamsMap(Map<String, Object> params) {
		return encryptSerialized(DiscuzAuthPayloadCodec.serializeAssocStringMap(params));
	}

	private static String encryptSerialized(String arrayString) {
		String encryptKey = md5Hex(String.valueOf(ThreadLocalRandom.current().nextInt(10001)));
		StringBuilder pairBuilder = new StringBuilder();
		int ctr = 0;
		byte[] serializedBytes = arrayString.getBytes(StandardCharsets.ISO_8859_1);
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

	private static byte[] keyTransform(byte[] arrayString) {
		String codetoken = "YW53dWx1eWFudGFuZw";
		byte[] encryptKey = md5AsciiHexBytes(codetoken);
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

	private static byte[] md5AsciiHexBytes(String input) {
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
}
