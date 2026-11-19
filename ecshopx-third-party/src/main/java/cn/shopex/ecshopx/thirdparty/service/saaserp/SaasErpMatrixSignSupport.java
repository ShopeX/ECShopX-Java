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

package cn.shopex.ecshopx.thirdparty.service.saaserp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/** Matrix sign helpers matching PHP {@code SaasErpCentre\Request::get_matrix_sign/gen_sign/assemble}. */
public final class SaasErpMatrixSignSupport {

	private SaasErpMatrixSignSupport() {}

	/**
	 * PHP json-encodes array/object values before signing, then {@code
	 * strtoupper(md5(strtoupper(md5(assemble(params))) . token))}.
	 */
	public static String matrixSign(Map<String, Object> params, String token, ObjectMapper objectMapper) {
		Map<String, Object> normalized = normalizeForSign(params, objectMapper);
		return md5Upper(md5Upper(assemble(normalized)) + (token == null ? "" : token));
	}

	static Map<String, Object> normalizeForSign(Map<String, Object> params, ObjectMapper objectMapper) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		if (params == null) {
			return out;
		}
		for (Map.Entry<String, Object> e : params.entrySet()) {
			Object v = e.getValue();
			if (v instanceof Map<?, ?> || v instanceof Iterable<?>) {
				try {
					out.put(e.getKey(), objectMapper.writeValueAsString(v));
				} catch (JsonProcessingException ex) {
					out.put(e.getKey(), String.valueOf(v));
				}
			} else {
				out.put(e.getKey(), v);
			}
		}
		return out;
	}

	static String assemble(Map<String, Object> params) {
		if (params == null) {
			return "";
		}
		TreeMap<String, Object> sorted = new TreeMap<>(String::compareTo);
		sorted.putAll(params);
		StringBuilder sign = new StringBuilder();
		for (Map.Entry<String, Object> e : sorted.entrySet()) {
			Object val = e.getValue();
			if (val == null) {
				continue;
			}
			if (val instanceof Boolean b) {
				val = b ? 1 : 0;
			}
			sign.append(e.getKey());
			if (val instanceof Map<?, ?> m) {
				LinkedHashMap<String, Object> nested = new LinkedHashMap<>();
				for (Map.Entry<?, ?> ne : m.entrySet()) {
					nested.put(String.valueOf(ne.getKey()), ne.getValue());
				}
				sign.append(assemble(nested));
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
}
