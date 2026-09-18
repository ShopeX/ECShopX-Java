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

package cn.shopex.ecshopx.espier.integration.yilianyun;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class YilianyunPrintRestAdapter implements YilianyunPrintPort {

	private static final String CACHE_KEY_PREFIX = "yly:oauth:access_token:";

	private final RestTemplate restTemplate;
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	@Value("${ecshopx.yilianyun.api-base:https://open-api.10ss.net}")
	private String apiBase;

	public YilianyunPrintRestAdapter(
			@Qualifier("companysRedisTemplate") StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
		this.restTemplate = new RestTemplate();
	}

	@Override
	public void printTicket(YilianyunPrintCommand command) {
		String accessToken = getOrLoadAccessToken(command.clientId(), command.clientSecret());
		MultiValueMap<String, String> printForm = new LinkedMultiValueMap<>();
		printForm.add("machine_code", command.machineCode());
		printForm.add("content", command.content());
		printForm.add("origin_id", command.originId());
		printForm.add("access_token", accessToken);
		printForm.add("idempotence", "1");
		appendSigned(printForm, command.clientId(), command.clientSecret());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		postForJsonBody(apiBase + "/print/index", new HttpEntity<>(printForm, headers));
	}

	private String getOrLoadAccessToken(String clientId, String clientSecret) {
		String cacheKey = CACHE_KEY_PREFIX + clientId;
		String cached = redisTemplate.opsForValue().get(cacheKey);
		if (cached != null && !cached.isBlank()) {
			return cached;
		}

		MultiValueMap<String, String> oauthForm = new LinkedMultiValueMap<>();
		oauthForm.add("grant_type", "client_credentials");
		oauthForm.add("scope", "all");
		appendSigned(oauthForm, clientId, clientSecret);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		String body = postForJsonBody(apiBase + "/oauth/oauth", new HttpEntity<>(oauthForm, headers));

		JsonNode root;
		try {
			root = objectMapper.readTree(body);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("yilianyun oauth json", e);
		}
		JsonNode tokenNode = locateAccessTokenNode(root);
		if (tokenNode == null || tokenNode.isMissingNode() || tokenNode.asText().isBlank()) {
			throw new IllegalStateException("yilianyun oauth missing access_token");
		}
		String token = tokenNode.asText();

		long ttlSeconds = resolveExpiresInSeconds(root);
		if (ttlSeconds <= 0L) {
			ttlSeconds = Duration.ofDays(20).getSeconds();
		}
		redisTemplate.opsForValue().set(cacheKey, token, Duration.ofSeconds(ttlSeconds));

		return token;
	}

	private static JsonNode locateAccessTokenNode(JsonNode root) {
		if (root == null) {
			return null;
		}
		JsonNode body = root.get("body");
		if (body != null && body.hasNonNull("access_token")) {
			return body.get("access_token");
		}
		if (root.hasNonNull("access_token")) {
			return root.get("access_token");
		}
		return null;
	}

	private static long resolveExpiresInSeconds(JsonNode root) {
		if (root == null) {
			return 0L;
		}
		JsonNode body = root.get("body");
		JsonNode expires = body != null ? body.get("expires_in") : root.get("expires_in");
		if (expires == null || expires.isMissingNode() || expires.isNull()) {
			return 0L;
		}
		long sec = expires.asLong(0L);
		return Math.max(0L, sec - 120L);
	}

	private String postForJsonBody(String url, HttpEntity<MultiValueMap<String, String>> entity) {
		ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
		String raw = response.getBody();
		if (raw == null || raw.isBlank()) {
			throw new IllegalStateException("yilianyun empty response url=" + url);
		}
		JsonNode root;
		try {
			root = objectMapper.readTree(raw);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("yilianyun invalid json url=" + url, e);
		}
		if (root.has("error") && root.get("error").asInt(0) != 0) {
			String msg =
					root.has("error_description")
							? root.get("error_description").asText()
							: "yilianyun api error";
			throw new IllegalStateException(msg);
		}
		return raw;
	}

	private static void appendSigned(
			MultiValueMap<String, String> target, String clientId, String clientSecret) {
		long ts = System.currentTimeMillis() / 1000L;
		String timestamp = Long.toString(ts);
		String sign = md5Hex(clientId + timestamp + clientSecret);
		String id = UUID.randomUUID().toString().toUpperCase();
		target.add("client_id", clientId);
		target.add("timestamp", timestamp);
		target.add("sign", sign);
		target.add("id", id);
	}

	private static String md5Hex(String raw) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] dig = md.digest(raw.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(dig.length * 2);
			for (byte b : dig) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (Exception e) {
			throw new IllegalStateException("md5", e);
		}
	}
}
