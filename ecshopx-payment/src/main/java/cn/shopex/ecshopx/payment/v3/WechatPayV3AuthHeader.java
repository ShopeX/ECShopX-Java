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

package cn.shopex.ecshopx.payment.v3;

import cn.shopex.ecshopx.common.port.weixin.WechatMerchantV3ApiMaterial;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import org.springframework.util.StringUtils;

/**
 * 构造 WeChat 商户 API v3 请求 <code>Authorization: WECHATPAY2-SHA256-RSA2048 ...</code> 头（与
 * PHP <code>WechatBundle\Services\Payment\BatchTransfer\Client::getToken</code> 对齐）。
 */
public final class WechatPayV3AuthHeader {

	private static final String SCHEMA = "WECHATPAY2-SHA256-RSA2048";

	private WechatPayV3AuthHeader() {
	}

	public static String buildAuthorization(
			WechatMerchantV3ApiMaterial material,
			String method,
			String urlPathWithLeadingSlash,
			String body) {
		long timestamp = System.currentTimeMillis() / 1000L;
		String nonce = randomNonce();
		String message =
				(method + "\n" + urlPathWithLeadingSlash + "\n" + timestamp + "\n" + nonce + "\n" + (body == null ? "" : body) + "\n");
		PrivateKey pk = material.getApiClientPrivateKey();
		String signature = signRsaSha256Base64(message, pk);
		return SCHEMA
				+ " mchid=\""
				+ escapeQuote(material.getMchId())
				+ "\","
				+ "nonce_str=\""
				+ escapeQuote(nonce)
				+ "\","
				+ "timestamp=\""
				+ timestamp
				+ "\","
				+ "serial_no=\""
				+ escapeQuote(material.getCertSerialNoHex())
				+ "\","
				+ "signature=\""
				+ escapeQuote(signature)
				+ "\"";
	}

	private static String escapeQuote(String s) {
		if (!StringUtils.hasText(s)) {
			return "";
		}
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private static String randomNonce() {
		byte[] b = new byte[16];
		new SecureRandom().nextBytes(b);
		StringBuilder sb = new StringBuilder(32);
		for (byte value : b) {
			sb.append(String.format("%02x", value));
		}
		return sb.toString() + (System.nanoTime() % 90000L + 10000L);
	}

	private static String signRsaSha256Base64(String message, PrivateKey privateKey) {
		try {
			java.security.Signature sign = java.security.Signature.getInstance("SHA256withRSA");
			sign.initSign(privateKey);
			sign.update(message.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(sign.sign());
		} catch (Exception e) {
			throw new IllegalStateException("wechat v3 sign failed", e);
		}
	}
}
