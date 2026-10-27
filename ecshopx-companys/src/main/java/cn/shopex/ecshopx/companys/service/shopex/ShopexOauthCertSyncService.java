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

import cn.shopex.ecshopx.thirdparty.service.prism.PrismCoreHttpClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Fetches OAuth SaaS certificate metadata from the Prism gateway and caches the result under the {@code prism:} Redis
 * namespace for downstream license validation.
 */
@Service
public class ShopexOauthCertSyncService {

	private final StringRedisTemplate prismRedisTemplate;
	private final ObjectMapper objectMapper;
	private final PrismCoreHttpClient prismCoreHttpClient;

	@Value("${ecshopx.thirdparty.prism-core.base-url:}")
	private String prismBaseUrl;

	@Value("${common.certi-base-url:}")
	private String certiBaseUrl;

	@Value("${common.store-key:}")
	private String storeKey;

	public ShopexOauthCertSyncService(
			@Qualifier("prismRedisTemplate") StringRedisTemplate prismRedisTemplate,
			ObjectMapper objectMapper,
			PrismCoreHttpClient prismCoreHttpClient) {
		this.prismRedisTemplate = prismRedisTemplate;
		this.objectMapper = objectMapper;
		this.prismCoreHttpClient = prismCoreHttpClient;
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
		String certiUrl = certiBaseUrl.replaceAll("/+$", "") + "/" + companyId + "/";
		String validateUrl = certiBaseUrl.replaceAll("/+$", "") + "/api/v1/third/saascert/cert/validate";

		LinkedHashMap<String, Object> form = new LinkedHashMap<>();
		form.put("certi_url", certiUrl);
		form.put("certi_session", storeKey);
		form.put("certi_validate_url", validateUrl);
		form.put("shop_version", "1.0");

		try {
			String raw = prismCoreHttpClient.postSignedForm("/auth/license.add", form, accessToken);
			if (!StringUtils.hasText(raw)) {
				return;
			}
			JsonNode root = objectMapper.readTree(raw);
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
