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

import cn.shopex.ecshopx.thirdparty.config.PrismCoreProperties;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
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
 * Signed POST client for the main Prism gateway ({@code common.prism_url} / OAuth / license APIs).
 */
@Component
public class PrismCoreHttpClient {

	private static final int CONNECT_TIMEOUT_SEC = 30;
	private static final int READ_TIMEOUT_SEC = 60;

	private final PrismCoreProperties properties;
	private final RestTemplate restTemplate;

	public PrismCoreHttpClient(PrismCoreProperties properties) {
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

		String sign = PrismIshopexHttpClient.produceSign(
				"POST", pathForSign, headers, query, postData, properties.getAppSecret());
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
