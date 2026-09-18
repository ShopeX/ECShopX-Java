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

package cn.shopex.ecshopx.adapay.service.callback;

import cn.shopex.ecshopx.adapay.config.AdapayCallbackProperties;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdapaySignatureVerifier {

	private static final Logger log = LoggerFactory.getLogger(AdapaySignatureVerifier.class);

	private final AdapayCallbackProperties properties;

	public AdapaySignatureVerifier(AdapayCallbackProperties properties) {
		this.properties = properties;
	}

	public void verifyOrThrow(String signBase64, String dataUtf8) {
		if (!StringUtils.hasText(properties.getRsaPublicKey())) {
			throw new BadRequestException("签名验证失败");
		}
		if (!StringUtils.hasText(signBase64) || dataUtf8 == null) {
			throw new BadRequestException("签名验证失败");
		}
		try {
			PublicKey publicKey = parseRsaPublicKey(properties.getRsaPublicKey());
			Signature signature = Signature.getInstance("SHA1withRSA");
			signature.initVerify(publicKey);
			signature.update(dataUtf8.getBytes(StandardCharsets.UTF_8));
			byte[] signBytes = Base64.getDecoder().decode(signBase64);
			if (!signature.verify(signBytes)) {
				throw new BadRequestException("签名验证失败");
			}
		} catch (BadRequestException e) {
			throw e;
		} catch (Exception e) {
			log.warn("AdaPay signature verification error: {}", e.toString());
			throw new BadRequestException("签名验证失败");
		}
	}

	private static PublicKey parseRsaPublicKey(String raw) throws Exception {
		String normalized = raw.trim();
		if (normalized.contains("BEGIN PUBLIC KEY")) {
			normalized =
					normalized
							.replace("-----BEGIN PUBLIC KEY-----", "")
							.replace("-----END PUBLIC KEY-----", "")
							.replaceAll("\\s", "");
		}
		byte[] decoded = Base64.getDecoder().decode(normalized);
		X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
		return KeyFactory.getInstance("RSA").generatePublic(spec);
	}
}
