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

package cn.shopex.ecshopx.thirdparty.service.shuyun;

import cn.shopex.ecshopx.thirdparty.config.ShuyunGatewayProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Signed HTTP client for Shuyun gateway ({@code u_appId} / {@code u_timestamp} / {@code u_signature} query params).
 */
@Component
public class ShuyunSignedGatewayClient {

	private static final int CONNECT_TIMEOUT_SEC = 5;
	private static final int READ_TIMEOUT_SEC = 30;

	private final ShuyunGatewayProperties properties;
	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;

	public ShuyunSignedGatewayClient(ShuyunGatewayProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
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
	 * GET {@code path} with merged query params plus signature (e.g. shopex userInfo by code).
	 */
	public JsonNode getSignedJson(String path, Map<String, String> queryParams) {
		if (!isConfigured()) {
			return null;
		}
		String base = properties.getBaseUrl().replaceAll("/+$", "");
		String rel = path.startsWith("/") ? path : "/" + path;
		LinkedHashMap<String, String> all = new LinkedHashMap<>();
		if (queryParams != null) {
			all.putAll(queryParams);
		}
		URI uri = UriComponentsBuilder.fromUriString(base + rel).queryParams(signQuery(all)).build().encode().toUri();
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
		ResponseEntity<String> resp =
				restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
		if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
			return null;
		}
		try {
			return objectMapper.readTree(resp.getBody());
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * POST JSON body; query string carries only signature params (matches legacy client {@code json()}).
	 */
	public JsonNode postSignedJson(String path, String jsonBody, Map<String, String> extraSignedQuery) {
		if (!isConfigured()) {
			return null;
		}
		String base = properties.getBaseUrl().replaceAll("/+$", "");
		String rel = path.startsWith("/") ? path : "/" + path;
		LinkedHashMap<String, String> signBase = new LinkedHashMap<>();
		if (extraSignedQuery != null) {
			signBase.putAll(extraSignedQuery);
		}
		URI uri = UriComponentsBuilder.fromUriString(base + rel).queryParams(signQuery(signBase)).build().encode().toUri();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
		String body = jsonBody != null ? jsonBody : "{}";
		ResponseEntity<String> resp =
				restTemplate.exchange(uri, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
		if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
			return null;
		}
		try {
			return objectMapper.readTree(resp.getBody());
		} catch (Exception e) {
			return null;
		}
	}

	private org.springframework.util.MultiValueMap<String, String> signQuery(LinkedHashMap<String, String> requestParams) {
		TreeMap<String, String> sorted = new TreeMap<>();
		sorted.putAll(requestParams);
		sorted.put("u_appId", properties.getAppKey());
		sorted.put("u_sign_method", "md5");
		sorted.put("u_timestamp", String.valueOf(System.currentTimeMillis()));
		String signature = sign(sorted);
		sorted.put("u_signature", signature);
		org.springframework.util.LinkedMultiValueMap<String, String> out = new org.springframework.util.LinkedMultiValueMap<>();
		for (Map.Entry<String, String> e : sorted.entrySet()) {
			out.add(e.getKey(), e.getValue());
		}
		return out;
	}

	private String sign(TreeMap<String, String> data) {
		StringBuilder args = new StringBuilder();
		for (Map.Entry<String, String> e : data.entrySet()) {
			args.append(e.getKey()).append(e.getValue());
		}
		String raw = properties.getAppSecret() + args + properties.getAppSecret();
		return md5Hex(raw);
	}

	private static String md5Hex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : d) {
				hex.append(String.format("%02x", b & 0xFF));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
