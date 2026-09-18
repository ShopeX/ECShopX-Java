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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class DmCrmNormalOrderDeliverySyncHttpService implements DmCrmNormalOrderDeliverySyncPort {

	private static final Logger log = LoggerFactory.getLogger(DmCrmNormalOrderDeliverySyncHttpService.class);

	private static final String HOPE_BASE = "https://hope.demogic.com";
	/**
	 * Shipment notification for an existing online-store order. Path follows the same {@code /cgi-api/order/…}
	 * convention as other Damo CRM workers in this codebase; adjust if the vendor contract differs.
	 */
	private static final String WORKER = "/cgi-api/order/notify_online_store_order_shipped";
	private static final String TOKEN_WORKER = "/cgi-api/auth/get_token";
	private static final String SETTING_PREFIX = "DmCrmSetting:";

	private final DmCrmSettingReadPort dmCrmSettingReadPort;
	private final RestTemplate restTemplate;
	private final StringRedisTemplate stringRedisTemplate;
	private final DmCrmApiLogService dmCrmApiLogService;
	private final ObjectMapper objectMapper;

	public DmCrmNormalOrderDeliverySyncHttpService(
			DmCrmSettingReadPort dmCrmSettingReadPort,
			@Qualifier("dmCrmOutboundRestTemplate") RestTemplate restTemplate,
			StringRedisTemplate stringRedisTemplate,
			DmCrmApiLogService dmCrmApiLogService,
			ObjectMapper objectMapper) {
		this.dmCrmSettingReadPort = dmCrmSettingReadPort;
		this.restTemplate = restTemplate;
		this.stringRedisTemplate = stringRedisTemplate;
		this.dmCrmApiLogService = dmCrmApiLogService;
		this.objectMapper = objectMapper;
	}

	@Override
	public void syncNormalOrderDelivery(long companyId, long orderId, Map<String, Object> busPayload) {
		if (!dmCrmSettingReadPort.isPointIntegrationOpen(companyId)) {
			return;
		}
		long t0 = System.currentTimeMillis();
		Map<String, Object> postBody = buildPostBody(orderId);
		String url;
		try {
			url = buildRequestUrl(companyId);
		} catch (JsonProcessingException e) {
			log.debug("dm crm normal delivery skip url build: {}", e.toString());
			return;
		}
		if (!StringUtils.hasText(url)) {
			return;
		}
		String json;
		try {
			json = objectMapper.writeValueAsString(postBody);
		} catch (JsonProcessingException e) {
			log.debug("dm crm normal delivery skip serialize: {}", e.toString());
			return;
		}
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.add("ruid", Objects.toString(busPayload != null ? busPayload.get("order_id") : null, String.valueOf(orderId)));
			HttpEntity<String> entity = new HttpEntity<>(json, headers);
			ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
			int secs = (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - t0);
			dmCrmApiLogService.recordApiCall(companyId, WORKER, postBody, resp.getBody(), "request", "success", Math.max(0, secs));
		} catch (RestClientException e) {
			int secs = (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - t0);
			dmCrmApiLogService.recordApiCall(companyId, WORKER, postBody, e.getMessage(), "request", "fail", Math.max(0, secs));
			log.debug("dm crm normal delivery http error: {}", e.toString());
		}
	}

	private static Map<String, Object> buildPostBody(long orderId) {
		Map<String, Object> root = new LinkedHashMap<>();
		root.put("orderNo", String.valueOf(orderId));
		root.put("channelCode", "c_brand_mall");
		root.put("deliverStatus", 1);
		return root;
	}

	private String buildRequestUrl(long companyId) throws JsonProcessingException {
		JsonNode settings = loadSettings(companyId);
		String appKey = textFromSettings(settings, "app_key");
		String appSecret = textFromSettings(settings, "app_secret");
		String entSign = textFromSettings(settings, "ent_sign");
		if (!StringUtils.hasText(appKey) || !StringUtils.hasText(appSecret) || !StringUtils.hasText(entSign)) {
			log.debug("dm crm normal delivery skip: missing dm crm credentials for companyId={}", companyId);
			return null;
		}
		String token = getOrFetchToken(companyId, appKey, appSecret);
		if (!StringUtils.hasText(token)) {
			log.debug("dm crm normal delivery skip: token unavailable for companyId={}", companyId);
			return null;
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
			long expireTime =
					wrap.has("expireTime") && wrap.get("expireTime").isNumber()
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
		String rawBody = resp.getBody();
		if (rawBody == null || rawBody.isBlank()) {
			return null;
		}
		JsonNode data = objectMapper.readTree(rawBody);
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
		long expireTime =
				result.has("expireTime") && result.get("expireTime").isNumber()
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
		stringRedisTemplate
				.opsForValue()
				.set(tokenKey, objectMapper.writeValueAsString(toStore), ttlSeconds, TimeUnit.SECONDS);
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
			log.error("dm crm normal delivery: SHA-1 algorithm not available", e);
			throw new ResourceException("SHA-1 algorithm not available in this JVM");
		}
	}
}
