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
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Shopex AuthCode encoder/decoder: XOR stream with MD5-derived keys, Base64 outer layer, and
 * associative payloads encoded via {@link DiscuzAuthPayloadCodec} for gateway compatibility.
 */
@Component
public class ShopexAuthCodeClient {

	private static final int CKEY_LENGTH = 4;

	private final String shopexAuthKey;

	public ShopexAuthCodeClient() {
		this("11958e9cfa0a44d7be353637220ee4ac");
	}

	public ShopexAuthCodeClient(String shopexAuthKey) {
		this.shopexAuthKey = shopexAuthKey != null ? shopexAuthKey : "shopex_authKey";
	}

	public String encode(Map<String, ?> arr) {
		String str = DiscuzAuthPayloadCodec.serializeAssocStringMap(arr);
		return authcode(str, "ENCODE", shopexAuthKey, 3600);
	}

	public Map<String, Object> decode(String str) {
		String data = authcode(str, "DECODE", shopexAuthKey, 3600);
		if (data == null || data.isEmpty()) {
			return Map.of();
		}
		try {
			return DiscuzAuthPayloadCodec.readAssocMap(data);
		} catch (RuntimeException e) {
			return Map.of();
		}
	}

	private static String authcode(String string, String operation, String key, int expiry) {
		String cryptKeyMd5 = md5Hex(key != null && !key.isEmpty() ? key : "shopex_authKey");
		String keya = md5Hex(cryptKeyMd5.substring(0, 16));
		String keyb = md5Hex(cryptKeyMd5.substring(16, 32));
		String keyc;
		if (CKEY_LENGTH > 0) {
			if ("DECODE".equals(operation)) {
				keyc = string.substring(0, Math.min(CKEY_LENGTH, string.length()));
			} else {
				String mdMicro = md5Hex(discuzMicrotimeToken());
				keyc = mdMicro.substring(mdMicro.length() - CKEY_LENGTH);
			}
		} else {
			keyc = "";
		}
		String cryptkey = keya + md5Hex(keya + keyc);
		int keyLength = cryptkey.length();
		byte[] stringBytes;
		if ("DECODE".equals(operation)) {
			String b64 = string.substring(CKEY_LENGTH);
			byte[] decoded = Base64.getDecoder().decode(padBase64(b64));
			stringBytes = decoded;
		} else {
			long exp = expiry != 0 ? (long) expiry + System.currentTimeMillis() / 1000L : 0L;
			String head = String.format(Locale.ROOT, "%010d", exp);
			String check = md5Hex(string + keyb).substring(0, 16);
			String combined = head + check + string;
			stringBytes = combined.getBytes(StandardCharsets.ISO_8859_1);
		}
		int stringLength = stringBytes.length;
		int[] box = new int[256];
		for (int i = 0; i < 256; i++) {
			box[i] = i;
		}
		int[] rndkey = new int[256];
		byte[] cryptkeyBytes = cryptkey.getBytes(StandardCharsets.US_ASCII);
		for (int i = 0; i <= 255; i++) {
			rndkey[i] = cryptkeyBytes[i % keyLength] & 0xFF;
		}
		for (int j = 0, i = 0; i < 256; i++) {
			j = (j + box[i] + rndkey[i]) % 256;
			int tmp = box[i];
			box[i] = box[j];
			box[j] = tmp;
		}
		byte[] result = new byte[stringLength];
		for (int a = 0, j = 0, i = 0; i < stringLength; i++) {
			a = (a + 1) % 256;
			j = (j + box[a]) % 256;
			int tmp = box[a];
			box[a] = box[j];
			box[j] = tmp;
			int xor = box[(box[a] + box[j]) % 256];
			result[i] = (byte) ((stringBytes[i] & 0xFF) ^ xor);
		}
		if ("DECODE".equals(operation)) {
			String resultStr = new String(result, StandardCharsets.ISO_8859_1);
			if (resultStr.length() < 26) {
				return "";
			}
			String tsStr = resultStr.substring(0, 10);
			String checkPart = resultStr.substring(10, 26);
			String payload = resultStr.substring(26);
			long ts;
			try {
				ts = Long.parseLong(tsStr);
			} catch (NumberFormatException e) {
				return "";
			}
			long now = System.currentTimeMillis() / 1000L;
			boolean timeOk = ts == 0 || ts - now > 0;
			String expectCheck = md5Hex(payload + keyb).substring(0, 16);
			if (timeOk && checkPart.equals(expectCheck)) {
				return payload;
			}
			return "";
		}
		String inner = new String(result, StandardCharsets.ISO_8859_1);
		String b64 = Base64.getEncoder().encodeToString(inner.getBytes(StandardCharsets.ISO_8859_1))
				.replace("=", "");
		return keyc + b64;
	}

	private static String padBase64(String s) {
		int pad = (4 - (s.length() % 4)) % 4;
		return s + "=".repeat(pad);
	}

	private static String discuzMicrotimeToken() {
		Instant now = Instant.now();
		long epochSecond = now.getEpochSecond();
		double frac = now.getNano() / 1_000_000_000.0;
		return String.format(Locale.US, "%.8f %d", frac, epochSecond);
	}

	private static String md5Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] d = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : d) {
				hex.append(String.format(Locale.ROOT, "%02x", b));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
