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

package cn.shopex.ecshopx.systemlink.wdterp;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class WdtErpOpenApiClient {

	private static final Logger log = LoggerFactory.getLogger(WdtErpOpenApiClient.class);

	private static final long WDT_LEGACY_EPOCH_OFFSET_SECONDS = 1325347200L;

	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate = new RestTemplate();

	@Value("${ecshopx.wdterp.api-base-url:}")
	private String apiBaseUrl;

	@Value("${ecshopx.wdterp.sid:}")
	private String defaultSid;

	@Value("${ecshopx.wdterp.app-key:}")
	private String defaultAppKey;

	@Value("${ecshopx.wdterp.app-secret:}")
	private String defaultAppSecret;

	public WdtErpOpenApiClient(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public Object call(
			long companyId,
			String method,
			List<Object> bodyArgs,
			String credentialSid,
			String credentialKey,
			String credentialSecret) {
		String base = trimSlash(apiBaseUrl != null ? apiBaseUrl : "");
		if (!StringUtils.hasText(base)) {
			log.debug("wdterp api skipped (missing api-base-url) companyId={} method={}", companyId, method);
			return Map.of("fail_msg", "wdterp client not configured");
		}
		String sid = StringUtils.hasText(credentialSid) ? credentialSid : defaultSid;
		String key = StringUtils.hasText(credentialKey) ? credentialKey : defaultAppKey;
		String fullSecret = StringUtils.hasText(credentialSecret) ? credentialSecret : defaultAppSecret;
		if (!StringUtils.hasText(sid) || !StringUtils.hasText(key) || !StringUtils.hasText(fullSecret)) {
			log.debug("wdterp api skipped (missing sid/app-key/app-secret) companyId={} method={}", companyId, method);
			return Map.of("fail_msg", "wdterp client not configured");
		}
		int colon = fullSecret.indexOf(':');
		String secretPart = colon >= 0 ? fullSecret.substring(0, colon) : fullSecret;
		String saltPart = colon >= 0 && colon + 1 < fullSecret.length() ? fullSecret.substring(colon + 1) : "";

		try {
			String bodyJson = objectMapper.writeValueAsString(bodyArgs != null ? bodyArgs : List.of());
			TreeMap<String, String> req = new TreeMap<>();
			req.put("sid", sid);
			req.put("key", key);
			req.put("salt", saltPart);
			req.put("method", method);
			req.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000L - WDT_LEGACY_EPOCH_OFFSET_SECONDS));
			req.put("v", "1.0");
			req.put("body", bodyJson);
			String sign = makeSign(req, secretPart);
			req.put("sign", sign);

			String bodyOnly = req.remove("body");
			UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(base);
			for (Map.Entry<String, String> e : req.entrySet()) {
				ub.queryParam(e.getKey(), e.getValue());
			}
			String url = ub.encode(StandardCharsets.UTF_8).build().toUriString();

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.add("X-Version-SDK", "java-ecshopx");
			log.debug("wdterp request companyId={} method={} url={}", companyId, method, url);
			String response = restTemplate.postForObject(url, new HttpEntity<>(bodyOnly, headers), String.class);
			if (!StringUtils.hasText(response)) {
				return Map.of();
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> root = objectMapper.readValue(response, Map.class);
			Object st = root.get("status");
			if (st instanceof Number n && n.intValue() > 0) {
				log.debug("wdterp api error companyId={} method={} status={} message={}", companyId, method, n, root.get("message"));
				return Map.of("fail_msg", root.get("message"));
			}
			return root.get("data") != null ? root.get("data") : root;
		} catch (Exception e) {
			log.debug("wdterp request failed companyId={} method={} msg={}", companyId, method, e.getMessage());
			return Map.of("fail_msg", e.getMessage());
		}
	}

	/**
	 * 与 PHP {@code WdtErpClient::pageCall} 一致：query 上携带 {@code page_size} / {@code page_no} / {@code calc_total}，body
	 * 为对参数列表的 JSON 编码。
	 */
	public Object pageCall(
			long companyId,
			String method,
			int pageSize,
			int pageNo,
			boolean calcTotal,
			List<Object> bodyArgs,
			String credentialSid,
			String credentialKey,
			String credentialSecret) {
		String base = trimSlash(apiBaseUrl != null ? apiBaseUrl : "");
		if (!StringUtils.hasText(base)) {
			log.debug("wdterp pageCall skipped (missing api-base-url) companyId={} method={}", companyId, method);
			return Map.of("fail_msg", "wdterp client not configured");
		}
		String sid = StringUtils.hasText(credentialSid) ? credentialSid : defaultSid;
		String key = StringUtils.hasText(credentialKey) ? credentialKey : defaultAppKey;
		String fullSecret = StringUtils.hasText(credentialSecret) ? credentialSecret : defaultAppSecret;
		if (!StringUtils.hasText(sid) || !StringUtils.hasText(key) || !StringUtils.hasText(fullSecret)) {
			log.debug("wdterp pageCall skipped (missing sid/app-key/app-secret) companyId={} method={}", companyId, method);
			return Map.of("fail_msg", "wdterp client not configured");
		}
		int colon = fullSecret.indexOf(':');
		String secretPart = colon >= 0 ? fullSecret.substring(0, colon) : fullSecret;
		String saltPart = colon >= 0 && colon + 1 < fullSecret.length() ? fullSecret.substring(colon + 1) : "";

		try {
			String bodyJson = objectMapper.writeValueAsString(bodyArgs != null ? bodyArgs : List.of());
			TreeMap<String, String> req = new TreeMap<>();
			req.put("sid", sid);
			req.put("key", key);
			req.put("salt", saltPart);
			req.put("method", method);
			req.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000L - WDT_LEGACY_EPOCH_OFFSET_SECONDS));
			req.put("v", "1.0");
			req.put("page_size", String.valueOf(pageSize));
			req.put("page_no", String.valueOf(pageNo));
			req.put("calc_total", calcTotal ? "1" : "0");
			req.put("body", bodyJson);
			String sign = makeSign(req, secretPart);
			req.put("sign", sign);

			String bodyOnly = req.remove("body");
			UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(base);
			for (Map.Entry<String, String> e : req.entrySet()) {
				ub.queryParam(e.getKey(), e.getValue());
			}
			String url = ub.encode(StandardCharsets.UTF_8).build().toUriString();

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.add("X-Version-SDK", "java-ecshopx");
			log.debug("wdterp pageCall companyId={} method={} pageNo={} url={}", companyId, method, pageNo, url);
			String response = restTemplate.postForObject(url, new HttpEntity<>(bodyOnly, headers), String.class);
			if (!StringUtils.hasText(response)) {
				return Map.of();
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> root = objectMapper.readValue(response, Map.class);
			Object st = root.get("status");
			if (st instanceof Number n && n.intValue() > 0) {
				log.debug("wdterp pageCall error companyId={} method={} status={} message={}", companyId, method, n, root.get("message"));
				return Map.of("fail_msg", root.get("message"));
			}
			if (calcTotal) {
				return root;
			}
			return root.get("data") != null ? root.get("data") : root;
		} catch (Exception e) {
			log.debug("wdterp pageCall failed companyId={} method={} msg={}", companyId, method, e.getMessage());
			return Map.of("fail_msg", e.getMessage());
		}
	}

	private static String makeSign(TreeMap<String, String> req, String secretPart) {
		StringBuilder sb = new StringBuilder();
		sb.append(secretPart);
		for (Map.Entry<String, String> e : req.entrySet()) {
			if ("sign".equals(e.getKey())) {
				continue;
			}
			sb.append(e.getKey()).append(e.getValue());
		}
		sb.append(secretPart);
		return md5Hex(sb.toString());
	}

	private static String md5Hex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] raw = md.digest(s.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(raw);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String trimSlash(String u) {
		if (u.endsWith("/")) {
			return u.substring(0, u.length() - 1);
		}
		return u;
	}
}
