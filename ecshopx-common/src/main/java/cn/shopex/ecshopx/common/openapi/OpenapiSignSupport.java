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

package cn.shopex.ecshopx.common.openapi;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.util.StringUtils;

/** 复刻 PHP {@code OpenapiCheck::gen_sign} / {@code assemble}。 */
public final class OpenapiSignSupport {

	private OpenapiSignSupport() {}

	public static String genSign(Map<String, ?> params, String token) {
		return md5Upper(token + assemble(params) + token);
	}

	public static String assemble(Map<String, ?> params) {
		if (params == null) {
			return "";
		}
		TreeMap<String, Object> sorted = new TreeMap<>(params);
		StringBuilder sign = new StringBuilder();
		for (Map.Entry<String, Object> entry : sorted.entrySet()) {
			Object val = entry.getValue();
			if (val == null) {
				continue;
			}
			if (val instanceof Boolean b) {
				val = b ? 1 : 0;
			}
			sign.append(entry.getKey());
			if (val instanceof Map<?, ?> nested) {
				@SuppressWarnings("unchecked")
				Map<String, ?> nestedMap = (Map<String, ?>) nested;
				sign.append(assemble(nestedMap));
			} else if (val instanceof Iterable<?> iterable) {
				for (Object element : iterable) {
					if (element instanceof Map<?, ?> nestedEl) {
						@SuppressWarnings("unchecked")
						Map<String, ?> nestedMap = (Map<String, ?>) nestedEl;
						sign.append(assemble(nestedMap));
					} else if (element != null) {
						sign.append(element);
					}
				}
			} else {
				sign.append(val);
			}
		}
		return sign.toString();
	}

	private static String md5Upper(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder(digest.length * 2);
			for (byte b : digest) {
				hex.append(String.format("%02X", b));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("MD5 not available", e);
		}
	}

	public static boolean signMatches(Map<String, ?> params, String token, String providedSign) {
		if (!StringUtils.hasText(providedSign)) {
			return false;
		}
		return genSign(params, token).equals(providedSign.trim().toUpperCase());
	}
}
