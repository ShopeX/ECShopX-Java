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

package cn.shopex.ecshopx.systemlink.jushuitan;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
public class JushuitanOpenApiClient {

	private static final Logger log = LoggerFactory.getLogger(JushuitanOpenApiClient.class);

	private static final String PATH_ITEM_STORE_QUERY = "/open/jushuitan/inventory/query";

	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate = new RestTemplate();

	@Value("${ecshopx.jushuitan.api-base-url:}")
	private String apiBaseUrl;

	@Value("${ecshopx.jushuitan.app-key:}")
	private String appKey;

	@Value("${ecshopx.jushuitan.app-secret:}")
	private String appSecret;

	public JushuitanOpenApiClient(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> call(long companyId, String method, Map<String, Object> bizPayload, String accessToken) {
		if (!StringUtils.hasText(apiBaseUrl) || !StringUtils.hasText(appSecret) || !StringUtils.hasText(appKey)) {
			log.debug("jushuitan api skipped (missing base-url/app-key/app-secret) companyId={} method={}", companyId, method);
			return Map.of("fail_msg", "jushuitan client not configured");
		}
		String path = methodPath(method);
		if (!StringUtils.hasText(path)) {
			return Map.of("fail_msg", "unknown method: " + method);
		}
		try {
			Object cleaned = filterDeep(bizPayload);
			String bizJson = objectMapper.writeValueAsString(cleaned);
			Map<String, String> queryParams = new TreeMap<>();
			queryParams.put("biz", bizJson);
			queryParams.put("app_key", appKey);
			queryParams.put("access_token", accessToken != null ? accessToken : "");
			queryParams.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000L));
			queryParams.put("charset", "utf-8");
			queryParams.put("version", "2");
			String assembled = assembleSignString(queryParams);
			String sign = md5HexBinaryAsHex(appSecret + assembled);
			queryParams.put("sign", sign);

			MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
			for (Map.Entry<String, String> e : queryParams.entrySet()) {
				form.add(e.getKey(), e.getValue());
			}
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			String url = trimSlash(apiBaseUrl) + path;
			log.debug("jushuitan request companyId={} method={} url={}", companyId, method, url);
			String body = restTemplate.postForObject(url, new HttpEntity<>(form, headers), String.class);
			if (!StringUtils.hasText(body)) {
				return Map.of();
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> parsed = objectMapper.readValue(body, Map.class);
			return parsed;
		} catch (Exception e) {
			log.debug("jushuitan request failed companyId={} method={} msg={}", companyId, method, e.getMessage());
			return Map.of("fail_msg", e.getMessage());
		}
	}

	private static String trimSlash(String u) {
		if (u.endsWith("/")) {
			return u.substring(0, u.length() - 1);
		}
		return u;
	}

	private static String methodPath(String method) {
		return switch (method) {
			case "item_add" -> "/open/jushuitan/itemsku/upload";
			case "shop_item_add" -> "/open/jushuitan/skumap/upload";
			case "item_store_query" -> PATH_ITEM_STORE_QUERY;
			case "order_upload" -> "/open/jushuitan/orders/upload";
			case "order_cancel" -> "/open/jushuitan/orders/cancel";
			case "aftersale_add" -> "/open/jushuitan/aftersales/upload";
			default -> "";
		};
	}

	private static String assembleSignString(Map<String, String> sorted) {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> e : sorted.entrySet()) {
			sb.append(e.getKey()).append(e.getValue());
		}
		return sb.toString();
	}

	private static String md5HexBinaryAsHex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] raw = md.digest(s.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(raw);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static Object filterDeep(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Map<?, ?> m) {
			Map<String, Object> out = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				Object v = filterDeep(e.getValue());
				if (v == null) {
					continue;
				}
				if (v instanceof Map<?, ?> mm && mm.isEmpty()) {
					continue;
				}
				if (v instanceof List<?> l && l.isEmpty()) {
					continue;
				}
				out.put(String.valueOf(e.getKey()), v);
			}
			return out;
		}
		if (o instanceof List<?> l) {
			List<Object> out = new ArrayList<>();
			for (Object x : l) {
				Object v = filterDeep(x);
				if (v == null) {
					continue;
				}
				if (v instanceof List<?> ll && ll.isEmpty()) {
					continue;
				}
				if (v instanceof Map<?, ?> mm && mm.isEmpty()) {
					continue;
				}
				out.add(v);
			}
			return out;
		}
		return o;
	}
}
