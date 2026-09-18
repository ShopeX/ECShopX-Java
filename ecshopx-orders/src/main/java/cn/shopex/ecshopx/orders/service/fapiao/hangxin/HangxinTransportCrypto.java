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

package cn.shopex.ecshopx.orders.service.fapiao.hangxin;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

final class HangxinTransportCrypto {

	private HangxinTransportCrypto() {}

	static String encryptToTransport(String plainText, String desKey) {
		try {
			byte[] raw = plainText.getBytes(StandardCharsets.UTF_8);
			Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(normalizeDesedeKey(desKey), "DESede"));
			byte[] enc = cipher.doFinal(raw);
			return base64EncodeTransport(enc);
		} catch (Exception e) {
			throw new ResourceException("发票报文加密失败");
		}
	}

	static String decryptFromTransport(String transport, String desKey) {
		try {
			byte[] enc = base64DecodeTransport(transport);
			Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(normalizeDesedeKey(desKey), "DESede"));
			byte[] dec = cipher.doFinal(enc);
			return new String(dec, StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new ResourceException("发票报文解密失败");
		}
	}

	static String base64EncodeTransport(byte[] raw) {
		String b64 = Base64.getEncoder().encodeToString(raw);
		return b64.replace("/", "_a").replace("+", "_b").replace("=", "_c");
	}

	static byte[] base64DecodeTransport(String transport) {
		if (transport == null) {
			return new byte[0];
		}
		String b64 = transport.replace("_a", "/").replace("_b", "+").replace("_c", "=");
		return Base64.getDecoder().decode(b64);
	}

	private static byte[] normalizeDesedeKey(String key) {
		if (key == null || key.isEmpty()) {
			throw new ResourceException("发票接口密钥未配置");
		}
		byte[] b = key.getBytes(StandardCharsets.UTF_8);
		byte[] out = new byte[24];
		if (b.length >= 24) {
			System.arraycopy(b, 0, out, 0, 24);
			return out;
		}
		if (b.length == 16) {
			System.arraycopy(b, 0, out, 0, 16);
			System.arraycopy(b, 0, out, 16, 8);
			return out;
		}
		for (int i = 0; i < 24; i++) {
			out[i] = b[i % b.length];
		}
		return out;
	}
}
