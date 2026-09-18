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

package cn.shopex.ecshopx.bspay.support;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class BsPayCallbackSignatureVerifier {

	private BsPayCallbackSignatureVerifier() {}

	public static boolean verifyRsaSha256(
			String signatureBase64, String dataUtf8, String rsaPublicKeyMaterial) {
		String sigB64 = signatureBase64 == null ? "" : signatureBase64;
		String data = dataUtf8 == null ? "" : dataUtf8;
		final byte[] signatureBytes;
		try {
			signatureBytes = Base64.getDecoder().decode(sigB64);
		} catch (IllegalArgumentException e) {
			return false;
		}
		String pem = toPemPublicKey(rsaPublicKeyMaterial);
		try {
			byte[] keyBytes = decodePublicKeyBytes(pem);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
			PublicKey pub = kf.generatePublic(spec);
			Signature signature = Signature.getInstance("SHA256withRSA");
			signature.initVerify(pub);
			signature.update(data.getBytes(StandardCharsets.UTF_8));
			return signature.verify(signatureBytes);
		} catch (GeneralSecurityException e) {
			return false;
		}
	}

	private static String toPemPublicKey(String rsaPublicKeyMaterial) {
		String material = rsaPublicKeyMaterial == null ? "" : rsaPublicKeyMaterial.trim();
		if (material.contains("BEGIN PUBLIC KEY")) {
			return material;
		}
		StringBuilder body = new StringBuilder();
		for (int i = 0; i < material.length(); i += 64) {
			int end = Math.min(i + 64, material.length());
			body.append(material, i, end).append('\n');
		}
		return "-----BEGIN PUBLIC KEY-----\n"
				+ body
				+ "-----END PUBLIC KEY-----";
	}

	private static byte[] decodePublicKeyBytes(String pemOrBase64) throws GeneralSecurityException {
		String s = pemOrBase64.trim();
		if (s.contains("BEGIN PUBLIC KEY")) {
			int start = s.indexOf("-----BEGIN PUBLIC KEY-----");
			int end = s.indexOf("-----END PUBLIC KEY-----");
			if (start < 0 || end < 0 || end <= start) {
				throw new GeneralSecurityException("invalid PEM");
			}
			String b64 =
					s.substring(start + "-----BEGIN PUBLIC KEY-----".length(), end).replaceAll("\\s", "");
			return Base64.getDecoder().decode(b64);
		}
		return Base64.getDecoder().decode(s.replaceAll("\\s", ""));
	}
}
