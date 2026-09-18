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

package cn.shopex.ecshopx.shuyun.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 入站回调验签。对齐 PHP {@code ShuyunCallbackSignatureVerifier::verifyHttpCallback}。
 */
@Component
public class CallbackSignatureVerifier {

	public boolean verifyHttpCallback(String secret, HttpServletRequest request, String sign) {
		if (!StringUtils.hasText(sign)) {
			return false;
		}
		Map<String, String> params = new TreeMap<>();
		Map<String, String[]> query = request.getParameterMap();
		if (query != null) {
			for (Map.Entry<String, String[]> e : query.entrySet()) {
				String key = e.getKey();
				if ("sign".equals(key)) {
					continue;
				}
				String[] vals = e.getValue();
				params.put(key, vals != null && vals.length > 0 ? vals[0] : "");
			}
		}
		String syTime = firstHeader(request, "SY-Request-Time", "Sy-Request-Time");
		if (StringUtils.hasText(syTime)) {
			params.put("SY-Request-Time", syTime);
		} else if (!params.containsKey("SY-Request-Time")) {
			String legacy = request.getParameter("callBackTime");
			if (!StringUtils.hasText(legacy)) {
				legacy = firstHeader(request, "callBackTime", "Callbacktime");
			}
			if (StringUtils.hasText(legacy)) {
				params.put("callBackTime", legacy);
			}
		}
		if (params.isEmpty()) {
			return false;
		}
		StringBuilder sb = new StringBuilder(secret == null ? "" : secret);
		for (Map.Entry<String, String> e : params.entrySet()) {
			sb.append(e.getKey()).append(e.getValue() == null ? "" : e.getValue());
		}
		sb.append(secret == null ? "" : secret);
		String expected = md5Hex(sb.toString());
		return expected.equalsIgnoreCase(sign.trim());
	}

	/** 出站 Gateway-Sign 同源算法。 */
	public String signParams(String appSecret, Map<String, String> params) {
		if (!StringUtils.hasText(appSecret)) {
			throw new IllegalArgumentException("appSecret must not be empty.");
		}
		Map<String, String> sorted = new TreeMap<>(params);
		StringBuilder concat = new StringBuilder();
		for (Map.Entry<String, String> e : sorted.entrySet()) {
			concat.append(e.getKey()).append(e.getValue() == null ? "" : e.getValue());
		}
		return md5Hex(appSecret + concat + appSecret);
	}

	private static String firstHeader(HttpServletRequest request, String... names) {
		for (String name : names) {
			String v = request.getHeader(name);
			if (StringUtils.hasText(v)) {
				return v;
			}
		}
		return null;
	}

	static String md5Hex(String raw) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder(digest.length * 2);
			for (byte b : digest) {
				hex.append(String.format(Locale.ROOT, "%02x", b));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("MD5 not available", e);
		}
	}
}
