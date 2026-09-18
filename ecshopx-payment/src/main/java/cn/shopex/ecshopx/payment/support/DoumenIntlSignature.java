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

package cn.shopex.ecshopx.payment.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.util.StringUtils;

/** 斗门国际网关签名：{@code md5(rawBody + X-SecretKey)}，验签大小写不敏感。 */
public final class DoumenIntlSignature {

	private DoumenIntlSignature() {}

	public static String signPostBody(String jsonOrRawBody, String secretKey) {
		String body = jsonOrRawBody == null ? "" : jsonOrRawBody;
		String secret = secretKey == null ? "" : secretKey;
		return md5Hex(body + secret);
	}

	public static boolean verifyNotify(String rawBody, String secretKey, String signature) {
		String expected = signPostBody(rawBody, secretKey);
		String actual = signature == null ? "" : signature.trim();
		return expected.toLowerCase(Locale.ROOT).equals(actual.toLowerCase(Locale.ROOT));
	}

	private static String md5Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("MD5 not available", e);
		}
	}

	public static boolean hasText(String s) {
		return StringUtils.hasText(s);
	}
}
