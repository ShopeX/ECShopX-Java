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

package cn.shopex.ecshopx.thirdparty.service.dmcrm;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.members.port.DmCrmMemberPointOverlayPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service("dmCrmMemberPointOverlayPortImpl")
@ConditionalOnProperty(
		prefix = "ecshopx.thirdparty.dm-crm.point-detail-list",
		name = "http-enabled",
		havingValue = "true")
public class DmCrmMemberPointOverlayHttpService implements DmCrmMemberPointOverlayPort {

	private static final String HOPE_BASE = "https://hope.demogic.com";
	private static final String WORKER = "/cgi-api/member/get_integral_detail";
	private static final String TOKEN_WORKER = "/cgi-api/auth/get_token";
	private static final String SETTING_PREFIX = "DmCrmSetting:";

	private final ObjectMapper objectMapper;
	private final StringRedisTemplate stringRedisTemplate;
	private final DmCrmApiLogService dmCrmApiLogService;
	private final RestTemplate restTemplate = new RestTemplate();

	public DmCrmMemberPointOverlayHttpService(
			ObjectMapper objectMapper,
			StringRedisTemplate stringRedisTemplate,
			DmCrmApiLogService dmCrmApiLogService) {
		this.objectMapper = objectMapper;
		this.stringRedisTemplate = stringRedisTemplate;
		this.dmCrmApiLogService = dmCrmApiLogService;
	}

	@Override
	public Map<String, Object> fetchPointOverlay(long companyId, String mobilePlain) {
		long t0 = System.currentTimeMillis();
		Map<String, Object> postBody = new LinkedHashMap<>();
		postBody.put("mobile", mobilePlain != null ? mobilePlain : "");

		String url;
		try {
			url = buildRequestUrl(companyId);
		} catch (Exception e) {
			int secs = (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - t0);
			dmCrmApiLogService.recordApiCall(
					companyId, WORKER, postBody, e.getMessage(), "request", "fail", Math.max(0, secs));
			throw new ResourceException("接口获取失败");
		}

		String rawBody;
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			String json = objectMapper.writeValueAsString(postBody);
			HttpEntity<String> entity = new HttpEntity<>(json, headers);
			ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
			rawBody = resp.getBody();
		} catch (RestClientException | JsonProcessingException e) {
			int secs = (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - t0);
			dmCrmApiLogService.recordApiCall(
					companyId, WORKER, postBody, e.getMessage(), "request", "fail", Math.max(0, secs));
			throw new ResourceException("接口获取失败");
		}

		int secs = (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - t0);
		dmCrmApiLogService.recordApiCall(companyId, WORKER, postBody, rawBody, "request", "success", Math.max(0, secs));

		if (rawBody == null || rawBody.isBlank()) {
			throw new ResourceException("接口获取失败");
		}

		JsonNode root;
		try {
			root = objectMapper.readTree(rawBody);
		} catch (Exception e) {
			throw new ResourceException("接口获取失败");
		}

		JsonNode codeNode = root.get("code");
		if (codeNode == null || !"0".equals(codeNode.asText())) {
			throw new ResourceException("接口获取失败");
		}

		JsonNode result = root.get("result");
		if (result == null || !result.isObject()) {
			throw new ResourceException("接口获取失败");
		}

		long integral = parseLongNode(result.get("integral"));
		long frozen = 0L;
		if (result.has("frozenIntegral") && !result.get("frozenIntegral").isNull()) {
			frozen = parseLongNode(result.get("frozenIntegral"));
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("integral", integral);
		out.put("frozenIntegral", frozen);
		return out;
	}

	private static long parseLongNode(JsonNode integralNode) {
		if (integralNode == null || integralNode.isNull()) {
			return 0L;
		}
		if (integralNode.isIntegralNumber()) {
			return integralNode.asLong();
		}
		if (integralNode.isTextual()) {
			try {
				return Long.parseLong(integralNode.asText().trim());
			} catch (NumberFormatException e) {
				throw new ResourceException("接口获取失败");
			}
		}
		if (integralNode.isBoolean()) {
			throw new ResourceException("接口获取失败");
		}
		throw new ResourceException("接口获取失败");
	}

	private String buildRequestUrl(long companyId) throws JsonProcessingException {
		JsonNode settings = loadSettings(companyId);
		String appKey = textFromSettings(settings, "app_key");
		String appSecret = textFromSettings(settings, "app_secret");
		String entSign = textFromSettings(settings, "ent_sign");
		if (!StringUtils.hasText(appKey) || !StringUtils.hasText(appSecret) || !StringUtils.hasText(entSign)) {
			throw new IllegalStateException("missing dm crm credentials");
		}
		String token = getOrFetchToken(companyId, appKey, appSecret);
		if (!StringUtils.hasText(token)) {
			throw new IllegalStateException("token unavailable");
		}
		return HOPE_BASE + WORKER + "?token=" + urlEncode(token) + "&entSign=" + urlEncode(entSign);
	}

	private static String urlEncode(String s) {
		return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
	}

	private JsonNode loadSettings(long companyId) throws JsonProcessingException {
		String key = SETTING_PREFIX + sha1Hex(String.valueOf(companyId));
		String raw = stringRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			return objectMapper.readTree("{\"is_open\":false}");
		}
		return objectMapper.readTree(raw);
	}

	private static String textFromSettings(JsonNode settings, String field) {
		if (settings == null || !settings.has(field) || settings.get(field).isNull()) {
			return "";
		}
		JsonNode n = settings.get(field);
		if (n.isTextual()) {
			return n.asText();
		}
		return String.valueOf(n.asText(""));
	}

	private String getOrFetchToken(long companyId, String appKey, String appSecret) throws JsonProcessingException {
		String tokenKey = "damo_token:" + companyId;
		String cached = stringRedisTemplate.opsForValue().get(tokenKey);
		if (StringUtils.hasText(cached)) {
			JsonNode wrap = objectMapper.readTree(cached);
			String token = wrap.has("token") ? wrap.get("token").asText("") : "";
			long expireTime = wrap.has("expireTime") && wrap.get("expireTime").isNumber()
					? wrap.get("expireTime").asLong()
					: 0L;
			long nowMs = System.currentTimeMillis();
			if (StringUtils.hasText(token) && expireTime - nowMs > 60L * 60L * 1000L) {
				return token;
			}
		}
		return fetchNewToken(companyId, appKey, appSecret, tokenKey);
	}

	private String fetchNewToken(long companyId, String appKey, String appSecret, String tokenKey)
			throws JsonProcessingException {
		String url = HOPE_BASE + TOKEN_WORKER;
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("appKey", appKey);
		body.put("appSecret", appSecret);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String json = objectMapper.writeValueAsString(body);
		HttpEntity<String> entity = new HttpEntity<>(json, headers);
		ResponseEntity<String> resp;
		try {
			resp = restTemplate.postForEntity(url, entity, String.class);
		} catch (RestClientException e) {
			return null;
		}
		String raw = resp.getBody();
		if (raw == null || raw.isBlank()) {
			return null;
		}
		JsonNode data = objectMapper.readTree(raw);
		JsonNode codeNode = data.get("code");
		boolean ok = codeNode != null && "0".equals(codeNode.asText());
		if (!ok) {
			return null;
		}
		JsonNode result = data.get("result");
		if (result == null || !result.isObject()) {
			return null;
		}
		String token = result.has("token") ? result.get("token").asText("") : "";
		long expireTime = result.has("expireTime") && result.get("expireTime").isNumber()
				? result.get("expireTime").asLong()
				: 0L;
		if (!StringUtils.hasText(token) || expireTime <= 0) {
			return null;
		}
		Map<String, Object> toStore = new LinkedHashMap<>();
		toStore.put("token", token);
		toStore.put("expireTime", expireTime);
		long nowMs = System.currentTimeMillis();
		long ttlMillis = expireTime - nowMs;
		long ttlSeconds = Math.max(60L, ttlMillis / 1000L);
		stringRedisTemplate.opsForValue().set(tokenKey, objectMapper.writeValueAsString(toStore), ttlSeconds, TimeUnit.SECONDS);
		return token;
	}

	private static String sha1Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] d = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(d.length * 2);
			for (byte b : d) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
