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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

@Service("dmCrmPointDetailListHttp")
@ConditionalOnProperty(
		prefix = "ecshopx.thirdparty.dm-crm.point-detail-list",
		name = "http-enabled",
		havingValue = "true")
public class DmCrmPointDetailListHttpService implements DmCrmPointDetailListPort {

	private static final String HOPE_BASE = "https://hope.demogic.com";
	private static final String WORKER = "/cgi-api/member/member_get_integral_detail";
	private static final String TOKEN_WORKER = "/cgi-api/auth/get_token";
	private static final String SETTING_PREFIX = "DmCrmSetting:";

	private static final Map<String, String> MEMBER_INTEGRAL_CODE_LABELS = Map.of(
			"1201", "消费抵现",
			"1202", "退款追扣",
			"1203", "手动扣除",
			"1204", "积分兑换",
			"1205", "扣减(其他)",
			"1206", "互动营销扣除");

	private final ObjectMapper objectMapper;
	private final StringRedisTemplate stringRedisTemplate;
	private final DmCrmApiLogService dmCrmApiLogService;
	private final RestTemplate restTemplate = new RestTemplate();

	public DmCrmPointDetailListHttpService(
			ObjectMapper objectMapper,
			StringRedisTemplate stringRedisTemplate,
			DmCrmApiLogService dmCrmApiLogService) {
		this.objectMapper = objectMapper;
		this.stringRedisTemplate = stringRedisTemplate;
		this.dmCrmApiLogService = dmCrmApiLogService;
	}

	@Override
	public DmCrmPointDetailListResult fetchDetailList(long companyId, DmCrmPointDetailListRequest request) {
		long t0 = System.currentTimeMillis();
		Map<String, Object> postBody = new LinkedHashMap<>();
		postBody.put("mobile", request.getMobile() != null ? request.getMobile() : "");
		postBody.put("currentPage", request.getCurrentPage());
		postBody.put("pageSize", request.getPageSize());
		if (StringUtils.hasText(request.getCardNo())) {
			postBody.put("cardNo", request.getCardNo());
		}

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
		} catch (RestClientException | com.fasterxml.jackson.core.JsonProcessingException e) {
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

		JsonNode resultNode = root.get("result");
		if (resultNode == null || !resultNode.isObject()) {
			throw new ResourceException("接口获取失败");
		}

		JsonNode itemsNode = resultNode.get("items");
		if (itemsNode == null || !itemsNode.isArray()) {
			throw new ResourceException("接口获取失败");
		}

		long totalCount = 0L;
		JsonNode totalNode = resultNode.get("totalCount");
		if (totalNode != null && totalNode.isNumber()) {
			totalCount = totalNode.asLong();
		}

		List<Map<String, Object>> rows = new ArrayList<>();
		for (JsonNode v : itemsNode) {
			rows.add(mapHopeItemToRow(v, request.getUserId(), companyId));
		}
		return new DmCrmPointDetailListResult(rows, totalCount);
	}

	private Map<String, Object> mapHopeItemToRow(JsonNode v, long userId, long companyId) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("user_id", userId);
		row.put("company_id", companyId);

		String codeKey = "";
		if (v.has("memberIntegralCode") && !v.get("memberIntegralCode").isNull()) {
			JsonNode c = v.get("memberIntegralCode");
			codeKey = c.asText("").trim();
			if (codeKey.isEmpty() && c.isNumber()) {
				codeKey = String.valueOf(c.asInt());
			}
		}
		row.put("journal_type", MEMBER_INTEGRAL_CODE_LABELS.getOrDefault(codeKey, ""));

		row.put("point_desc", textOrEmpty(v, "memberIntegralName"));

		int intervalInout = 0;
		if (v.has("intervalInout") && v.get("intervalInout").isIntegralNumber()) {
			intervalInout = v.get("intervalInout").asInt();
		} else if (v.has("intervalInout") && v.get("intervalInout").isTextual()) {
			try {
				intervalInout = Integer.parseInt(v.get("intervalInout").asText().trim());
			} catch (NumberFormatException ignored) {
				intervalInout = 0;
			}
		}

		int intervalAbs = 0;
		if (v.has("intervalHistory") && v.get("intervalHistory").isNumber()) {
			intervalAbs = (int) Math.abs(Math.round(v.get("intervalHistory").asDouble()));
		}

		if (intervalInout == 1) {
			row.put("income", intervalAbs);
			row.put("outcome", 0);
		} else {
			row.put("income", 0);
			row.put("outcome", intervalAbs);
		}

		String orelationId = textOrEmpty(v, "orelationId");
		String orderId = orelationId;
		int dash = orelationId.indexOf('-');
		if (dash >= 0) {
			orderId = orelationId.substring(0, dash);
		}
		row.put("order_id", orderId);

		long createdSec = 0L;
		if (v.has("createTime") && v.get("createTime").isNumber()) {
			createdSec = v.get("createTime").asLong() / 1000L;
		}
		row.put("created", createdSec);

		row.put("updated", 0);
		row.put("external_id", "");
		row.put("operater", "");
		row.put("operater_remark", "");

		Object sPoint = 0;
		if (v.has("lastInterval") && v.get("lastInterval").isNumber()) {
			if (v.get("lastInterval").isIntegralNumber()) {
				sPoint = v.get("lastInterval").asInt();
			} else {
				sPoint = v.get("lastInterval").asDouble();
			}
		}
		row.put("s_point", sPoint);

		row.put("point", intervalAbs);

		Long effectTimeSec = null;
		if (v.has("effectTime") && v.get("effectTime").isNumber()) {
			long et = v.get("effectTime").asLong();
			if (et > 0) {
				effectTimeSec = et / 1000L;
			}
		}
		row.put("effect_time", effectTimeSec);

		String relationId = textOrEmpty(v, "relationId");
		row.put("order_remark", orelationId + "--" + relationId);
		row.put("remark", textOrEmpty(v, "remark"));

		return row;
	}

	private static String textOrEmpty(JsonNode v, String field) {
		if (!v.has(field) || v.get(field).isNull()) {
			return "";
		}
		return v.get(field).asText("");
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
