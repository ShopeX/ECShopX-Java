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

package cn.shopex.ecshopx.thirdparty.service.prism;

import cn.shopex.ecshopx.thirdparty.config.PrismIshopexProperties;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Signed POST client compatible with the upstream Prism OpenAPI form encoding and {@code PrismSign} rules
 * (app key/secret, MD5 sign, x-www-form-urlencoded).
 */
@Component
public class PrismIshopexHttpClient {

	private static final int CONNECT_TIMEOUT_SEC = 30;
	private static final int READ_TIMEOUT_SEC = 60;

	private final PrismIshopexProperties properties;
	private final RestTemplate restTemplate;

	public PrismIshopexHttpClient(PrismIshopexProperties properties) {
		this.properties = properties;
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout((int) Duration.ofSeconds(CONNECT_TIMEOUT_SEC).toMillis());
		factory.setReadTimeout((int) Duration.ofSeconds(READ_TIMEOUT_SEC).toMillis());
		this.restTemplate = new RestTemplate(factory);
	}

	public boolean isConfigured() {
		return StringUtils.hasText(properties.getBaseUrl())
				&& StringUtils.hasText(properties.getAppKey())
				&& StringUtils.hasText(properties.getAppSecret());
	}

	/**
	 * POST {@code path} with signed query string and {@code application/x-www-form-urlencoded} body.
	 *
	 * @return response body text, or {@code null} on transport/HTTP failure
	 */
	public String postSignedForm(String path, LinkedHashMap<String, Object> postData) {
		if (!isConfigured()) {
			return null;
		}
		String base = properties.getBaseUrl().replaceAll("/+$", "");
		String rel = path.startsWith("/") ? path.substring(1) : path;
		String fullUrl = base + "/" + rel;
		URI uri = URI.create(fullUrl);
		String pathForSign = uri.getRawPath() != null ? uri.getRawPath() : uri.getPath();
		if (pathForSign == null || pathForSign.isEmpty()) {
			pathForSign = "/";
		}

		Map<String, String> query = new LinkedHashMap<>();
		if (uri.getRawQuery() != null && !uri.getRawQuery().isEmpty()) {
			for (String pair : uri.getRawQuery().split("&")) {
				int eq = pair.indexOf('=');
				if (eq > 0) {
					query.put(urlDecode(pair.substring(0, eq)), urlDecode(pair.substring(eq + 1)));
				} else if (!pair.isEmpty()) {
					query.put(urlDecode(pair), "");
				}
			}
		}
		query.put("client_id", properties.getAppKey());
		query.put("sign_method", "md5");
		query.put("sign_time", Long.toString(System.currentTimeMillis() / 1000L));

		Map<String, String> headers = new LinkedHashMap<>();
		headers.put("Pragma", "no-cache");
		headers.put("Cache-Control", "no-cache");
		headers.put("Content-Type", "application/x-www-form-urlencoded");

		String sign = produceSign("POST", pathForSign, headers, query, postData, properties.getAppSecret());
		query.put("sign", sign);

		boolean https = "https".equalsIgnoreCase(uri.getScheme());
		Map<String, String> finalQuery;
		if (https) {
			finalQuery = new LinkedHashMap<>();
			finalQuery.put("client_id", properties.getAppKey());
			finalQuery.put("client_secret", properties.getAppSecret());
		} else {
			finalQuery = query;
		}

		String queryString = buildQueryString(finalQuery);
		String urlWithQuery = stripQuery(fullUrl) + "?" + queryString;

		String body = buildFormBody(postData);
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set("Pragma", "no-cache");
		httpHeaders.set("Cache-Control", "no-cache");
		httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		HttpEntity<String> entity = new HttpEntity<>(body, httpHeaders);
		try {
			ResponseEntity<String> resp = restTemplate.postForEntity(urlWithQuery, entity, String.class);
			if (!resp.getStatusCode().is2xxSuccessful()) {
				return null;
			}
			return resp.getBody();
		} catch (RestClientException e) {
			return null;
		}
	}

	private static String stripQuery(String fullUrl) {
		int q = fullUrl.indexOf('?');
		return q >= 0 ? fullUrl.substring(0, q) : fullUrl;
	}

	private static String urlDecode(String s) {
		try {
			return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8);
		} catch (Exception e) {
			return s;
		}
	}

	/** Build the signature string (Prism sign contract, MD5). */
	public static String produceSign(
			String method,
			String path,
			Map<String, String> headers,
			Map<String, String> query,
			Map<String, Object> postData,
			String secret) {
		String h = signHeaders(headers);
		String q = signParamsFromStringMap(query);
		String p = signParamsFromObjectMap(postData);
		String sign =
				secret
						+ "&"
						+ method
						+ "&"
						+ rawUrlEncode(path)
						+ "&"
						+ rawUrlEncode(h)
						+ "&"
						+ rawUrlEncode(q)
						+ "&"
						+ rawUrlEncode(p)
						+ "&"
						+ secret;
		return md5Upper(sign);
	}

	private static String signHeaders(Map<String, String> headers) {
		TreeMap<String, String> sorted = new TreeMap<>();
		for (Map.Entry<String, String> e : headers.entrySet()) {
			if (e.getKey() == null || e.getValue() == null) {
				continue;
			}
			sorted.put(e.getKey(), e.getValue());
		}
		List<String> parts = new ArrayList<>();
		for (Map.Entry<String, String> e : sorted.entrySet()) {
			String k = e.getKey();
			if ("Authorization".equalsIgnoreCase(k) || k.regionMatches(true, 0, "X-Api-", 0, 6)) {
				parts.add(k + "=" + e.getValue());
			}
		}
		return String.join("&", parts);
	}

	private static String signParamsFromStringMap(Map<String, String> params) {
		if (params == null || params.isEmpty()) {
			return "";
		}
		TreeMap<String, String> sorted = new TreeMap<>(params);
		List<String> parts = new ArrayList<>();
		for (Map.Entry<String, String> e : sorted.entrySet()) {
			String key = e.getKey();
			String value = e.getValue();
			if (value == null) {
				continue;
			}
			parts.add(key + "=" + value);
		}
		return String.join("&", parts);
	}

	/** Flat key=value string for signed POST fields. */
	private static String signParamsFromObjectMap(Map<String, Object> params) {
		if (params == null || params.isEmpty()) {
			return "";
		}
		TreeMap<String, Object> sorted = new TreeMap<>();
		for (Map.Entry<String, Object> e : params.entrySet()) {
			sorted.put(e.getKey(), e.getValue());
		}
		List<String> parts = new ArrayList<>();
		for (Map.Entry<String, Object> e : sorted.entrySet()) {
			Object v = e.getValue();
			if (v == null) {
				continue;
			}
			if (Boolean.FALSE.equals(v)) {
				v = 0;
			}
			parts.add(e.getKey() + "=" + v);
		}
		return String.join("&", parts);
	}

	/** RFC 3986 percent-encoding for UTF-8 bytes (unreserved: {@code A-Za-z0-9-._~}). */
	static String rawUrlEncode(String input) {
		if (input == null || input.isEmpty()) {
			return "";
		}
		byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			int c = b & 0xff;
			if ((c >= 'A' && c <= 'Z')
					|| (c >= 'a' && c <= 'z')
					|| (c >= '0' && c <= '9')
					|| c == '-'
					|| c == '.'
					|| c == '_'
					|| c == '~') {
				sb.append((char) c);
			} else {
				sb.append(String.format("%%%02X", c));
			}
		}
		return sb.toString();
	}

	private static String md5Upper(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : dig) {
				hex.append(String.format("%02x", b));
			}
			return hex.toString().toUpperCase();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	/** Form query string: flat map, {@code +} for space, null as empty value. */
	private static String buildQueryString(Map<String, String> query) {
		StringJoiner joiner = new StringJoiner("&");
		for (Map.Entry<String, String> e : query.entrySet()) {
			String v = e.getValue() != null ? e.getValue() : "";
			joiner.add(formEncode(e.getKey()) + "=" + formEncode(v));
		}
		return joiner.toString();
	}

	private static String buildFormBody(LinkedHashMap<String, Object> postData) {
		StringJoiner joiner = new StringJoiner("&");
		for (Map.Entry<String, Object> e : postData.entrySet()) {
			String v;
			if (e.getValue() == null) {
				v = "";
			} else {
				v = e.getValue().toString();
			}
			joiner.add(formEncode(e.getKey()) + "=" + formEncode(v));
		}
		return joiner.toString();
	}

	private static String formEncode(String s) {
		return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
	}
}
