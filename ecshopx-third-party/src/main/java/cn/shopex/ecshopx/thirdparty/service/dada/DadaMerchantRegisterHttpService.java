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
import cn.shopex.ecshopx.common.port.DadaMerchantRegisterBody;
import cn.shopex.ecshopx.common.port.DadaMerchantRegisterPort;
import cn.shopex.ecshopx.thirdparty.config.DadaOpenPlatformProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service("dadaMerchantRegisterHttp")
@ConditionalOnProperty(prefix = "ecshopx.thirdparty.dada", name = "http-enabled", havingValue = "true")
public class DadaMerchantRegisterHttpService implements DadaMerchantRegisterPort {

	private final DadaOpenPlatformProperties properties;

	private final ObjectMapper objectMapper;

	private final RestTemplate restTemplate;

	public DadaMerchantRegisterHttpService(DadaOpenPlatformProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(10_000);
		factory.setReadTimeout(30_000);
		this.restTemplate = new RestTemplate(factory);
	}

	@Override
	public String registerMerchant(long companyId, String existingSourceIdOrBlank, DadaMerchantRegisterBody body) {
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
		String url = host + "/merchantApi/merchant/add";

		try {
			Map<String, Object> inner = new LinkedHashMap<>();
			inner.put("mobile", body.mobile());
			inner.put("city_name", body.cityName());
			inner.put("enterprise_name", body.enterpriseName());
			inner.put("enterprise_address", body.enterpriseAddress());
			inner.put("contact_name", body.contactName());
			inner.put("contact_phone", body.contactPhone());
			inner.put("email", body.email());
			String bodyJson = objectMapper.writeValueAsString(inner);

			String sourceId = existingSourceIdOrBlank == null ? "" : existingSourceIdOrBlank.trim();
			String timestamp = Long.toString(System.currentTimeMillis() / 1000L);

			TreeMap<String, String> signParams = new TreeMap<>();
			signParams.put("app_key", appKey);
			signParams.put("body", bodyJson);
			signParams.put("format", "json");
			signParams.put("source_id", sourceId);
			signParams.put("timestamp", timestamp);
			signParams.put("v", "1.0");

			StringBuilder chain = new StringBuilder();
			for (Map.Entry<String, String> e : signParams.entrySet()) {
				chain.append(e.getKey()).append(e.getValue());
			}
			String signature = md5HexUpper(appSecret + chain + appSecret);

			MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
			form.add("app_key", appKey);
			form.add("body", bodyJson);
			form.add("format", "json");
			form.add("v", "1.0");
			form.add("source_id", sourceId);
			form.add("timestamp", timestamp);
			form.add("signature", signature);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
			ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
			if (!resp.getStatusCode().is2xxSuccessful()) {
				throw new ResourceException("达达商户注册请求失败");
			}
			String raw = resp.getBody();
			if (!StringUtils.hasText(raw)) {
				throw new ResourceException("达达接口无响应");
			}
			JsonNode root = objectMapper.readTree(raw);
			String code = nodeAsTrimmedText(root.get("code"));
			if ("-1".equals(code)) {
				String statusMsg = nodeAsTrimmedText(root.get("status"));
				if (!StringUtils.hasText(statusMsg)) {
					statusMsg = "达达商户注册失败";
				}
				throw new ResourceException(statusMsg);
			}
			JsonNode resultNode = root.get("result");
			if (resultNode == null || resultNode.isNull()) {
				throw new ResourceException("达达接口未返回 result");
			}
			return resultNode.isTextual() ? resultNode.asText() : resultNode.toString();
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("达达商户注册调用异常: " + e.getMessage());
		}
	}

	private static String nodeAsTrimmedText(JsonNode n) {
		if (n == null || n.isNull() || !n.isValueNode()) {
			return "";
		}
		String t = n.asText();
		return t == null ? "" : t.trim();
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
