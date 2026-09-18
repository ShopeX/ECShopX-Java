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

package cn.shopex.ecshopx.thirdparty.service.dada;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.thirdparty.config.DadaOpenPlatformProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
public class DadaOpenPlatformJsonApiClient {

	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;

	public DadaOpenPlatformJsonApiClient(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(10_000);
		factory.setReadTimeout(30_000);
		this.restTemplate = new RestTemplate(factory);
	}

	public JsonNode postJsonBody(DadaOpenPlatformProperties properties, String sourceId, String apiPath, String bodyJson) {
		return postJsonBody(properties, sourceId, apiPath, bodyJson, true);
	}

	public JsonNode postJsonBody(
			DadaOpenPlatformProperties properties,
			String sourceId,
			String apiPath,
			String bodyJson,
			boolean requireNonBlankSourceIdWhenOnline) {
		String appKey = properties.getAppKey();
		String appSecret = properties.getAppSecret();
		if (!StringUtils.hasText(appKey) || !StringUtils.hasText(appSecret)) {
			throw new ResourceException("达达开放平台未配置 appKey 或 appSecret");
		}
		String host = properties.isOnline() ? properties.getHostOnline() : properties.getHostSandbox();
		host = host.trim();
		while (host.endsWith("/")) {
			host = host.substring(0, host.length() - 1);
		}
		String path = apiPath.startsWith("/") ? apiPath : "/" + apiPath;
		String url = host + path;

		String src = sourceId == null ? "" : sourceId.trim();
		if (requireNonBlankSourceIdWhenOnline && properties.isOnline() && !StringUtils.hasText(src)) {
			throw new ResourceException("达达商户 source_id 未配置");
		}
		if (!properties.isOnline()) {
			src = properties.getSandboxSourceId() == null ? "" : properties.getSandboxSourceId().trim();
		}

		try {
			String inner = bodyJson == null ? "" : bodyJson;
			long ts = System.currentTimeMillis() / 1000L;
			TreeMap<String, String> signMap = new TreeMap<>();
			signMap.put("app_key", appKey);
			signMap.put("body", inner);
			signMap.put("format", "json");
			signMap.put("source_id", src);
			signMap.put("timestamp", Long.toString(ts));
			signMap.put("v", "1.0");
			StringBuilder chain = new StringBuilder();
			for (Map.Entry<String, String> e : signMap.entrySet()) {
				chain.append(e.getKey()).append(e.getValue());
			}
			String signature = md5HexUpper(appSecret + chain + appSecret);

			Map<String, Object> req = new LinkedHashMap<>();
			req.put("app_key", appKey);
			req.put("body", inner);
			req.put("format", "json");
			req.put("v", "1.0");
			req.put("source_id", src);
			req.put("timestamp", ts);
			req.put("signature", signature);

			String payload = objectMapper.writeValueAsString(req);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<String> entity = new HttpEntity<>(payload, headers);
			ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
			if (!resp.getStatusCode().is2xxSuccessful()) {
				throw new ResourceException("达达接口请求失败");
			}
			String raw = resp.getBody();
			if (!StringUtils.hasText(raw)) {
				throw new ResourceException("达达接口无响应");
			}
			return objectMapper.readTree(raw);
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("达达接口调用异常: " + e.getMessage());
		}
	}

	private static String md5HexUpper(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(dig.length * 2);
			for (byte b : dig) {
				sb.append(String.format("%02x", b & 0xff));
			}
			return sb.toString().toUpperCase();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
