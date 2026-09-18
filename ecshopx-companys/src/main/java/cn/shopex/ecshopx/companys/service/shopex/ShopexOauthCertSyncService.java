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

package cn.shopex.ecshopx.companys.service.shopex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Fetches OAuth SaaS certificate metadata from the Prism gateway and caches the result under the {@code prism:} Redis
 * namespace for downstream license validation.
 */
@Service
public class ShopexOauthCertSyncService {

	private static final int CONNECT_TIMEOUT_SEC = 10;
	private static final int READ_TIMEOUT_SEC = 30;

	private final StringRedisTemplate prismRedisTemplate;
	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;

	@Value("${ecshopx.thirdparty.prism-core.base-url:}")
	private String prismBaseUrl;

	@Value("${common.certi-base-url:}")
	private String certiBaseUrl;

	@Value("${common.store-key:}")
	private String storeKey;

	public ShopexOauthCertSyncService(
			@Qualifier("prismRedisTemplate") StringRedisTemplate prismRedisTemplate, ObjectMapper objectMapper) {
		this.prismRedisTemplate = prismRedisTemplate;
		this.objectMapper = objectMapper;
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout((int) Duration.ofSeconds(CONNECT_TIMEOUT_SEC).toMillis());
		factory.setReadTimeout((int) Duration.ofSeconds(READ_TIMEOUT_SEC).toMillis());
		this.restTemplate = new RestTemplate(factory);
	}

	public void syncAfterOauthLogin(long companyId, String passportUid, String accessToken) {
		if (!StringUtils.hasText(passportUid) || !StringUtils.hasText(accessToken)) {
			return;
		}
		String redisKey = certRedisKey(passportUid, companyId);
		if (hasCompleteCert(redisKey)) {
			return;
		}
		if (!StringUtils.hasText(prismBaseUrl) || !StringUtils.hasText(certiBaseUrl) || !StringUtils.hasText(storeKey)) {
			return;
		}
		String base = prismBaseUrl.replaceAll("/+$", "");
		String url = base + "/auth/license.add";
		String certiUrl = certiBaseUrl.replaceAll("/+$", "") + "/" + companyId + "/";
		String validateUrl = certiBaseUrl.replaceAll("/+$", "") + "/api/third/saascert/cert/validate";

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("certi_url", certiUrl);
		form.add("certi_session", storeKey);
		form.add("certi_validate_url", validateUrl);
		form.add("shop_version", "1.0");

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		headers.setBearerAuth(accessToken);
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
		try {
			ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
			if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
				return;
			}
			JsonNode root = objectMapper.readTree(resp.getBody());
			if (root == null || !"success".equalsIgnoreCase(text(root, "status"))) {
				return;
			}
			JsonNode data = root.get("data");
			if (data == null || !data.isObject()) {
				return;
			}
			String certificateId = text(data, "certificate_id");
			String nodeId = text(data, "node_id");
			String token = text(data, "token");
			if (!StringUtils.hasText(certificateId) || !StringUtils.hasText(nodeId) || !StringUtils.hasText(token)) {
				return;
			}
			String json =
					"{\"cert_id\":\""
							+ escapeJson(certificateId)
							+ "\",\"node_id\":\""
							+ escapeJson(nodeId)
							+ "\",\"token\":\""
							+ escapeJson(token)
							+ "\"}";
			prismRedisTemplate.opsForValue().set(redisKey, json);
			if (StringUtils.hasText(nodeId)) {
				String nodeKey = "prism:" + sha1Hex(nodeId + "_SaasCert");
				prismRedisTemplate.opsForValue().set(
						nodeKey,
						"{\"cert_id\":\""
								+ escapeJson(certificateId)
								+ "\",\"company_id\":"
								+ companyId
								+ ",\"token\":\""
								+ escapeJson(token)
								+ "\"}");
			}
		} catch (Exception e) {
			// Best-effort; login still succeeds without cert cache
		}
	}

	private boolean hasCompleteCert(String redisKey) {
		String raw = prismRedisTemplate.opsForValue().get(redisKey);
		if (!StringUtils.hasText(raw)) {
			return false;
		}
		try {
			JsonNode n = objectMapper.readTree(raw);
			return StringUtils.hasText(text(n, "cert_id"))
					&& StringUtils.hasText(text(n, "node_id"))
					&& StringUtils.hasText(text(n, "token"));
		} catch (Exception e) {
			return false;
		}
	}

	private static String text(JsonNode n, String field) {
		if (n == null || !n.has(field) || n.get(field).isNull()) {
			return "";
		}
		return n.get(field).asText("");
	}

	private static String escapeJson(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private static String certRedisKey(String passportUid, long companyId) {
		return "prism:" + sha1Hex(passportUid + "_" + companyId + "_SaasCert");
	}

	private static String sha1Hex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
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
