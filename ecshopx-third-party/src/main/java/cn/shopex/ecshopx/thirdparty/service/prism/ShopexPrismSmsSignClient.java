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

package cn.shopex.ecshopx.thirdparty.service.prism;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

/**
 * Prism SMS signature add/update client (session token + forever token in Redis).
 */
public class ShopexPrismSmsSignClient {

	private static final Logger log = LoggerFactory.getLogger(ShopexPrismSmsSignClient.class);

	private static final String FAN_OUT_SOURCE = "533218";
	private static final String FAN_OUT_SOURCE_TOKEN = "0c4b3f44cee06df91b76deaa57608fbb";

	private final PrismCoreHttpClient prismCoreHttpClient;
	private final StringRedisTemplate prismRedisTemplate;
	private final ObjectMapper objectMapper;
	private final long companyId;
	private final String shopexUid;

	private String sessionAccessToken;

	public ShopexPrismSmsSignClient(
			PrismCoreHttpClient prismCoreHttpClient,
			StringRedisTemplate prismRedisTemplate,
			ObjectMapper objectMapper,
			long companyId,
			String shopexUid) {
		this.prismCoreHttpClient = prismCoreHttpClient;
		this.prismRedisTemplate = prismRedisTemplate;
		this.objectMapper = objectMapper;
		this.companyId = companyId;
		this.shopexUid = shopexUid;
	}

	public void addSmsSign(String content) {
		ensureSession();
		LinkedHashMap<String, Object> form = new LinkedHashMap<>();
		form.put("access_token", sessionAccessToken);
		form.put("shopexid", uidKeyPart());
		form.put("content", content);
		form.put("token", getForeverToken());
		String raw = prismCoreHttpClient.postSignedForm("/addcontent/newbytoken", form);
		if (raw == null) {
			throw new ForbiddenException("添加短信签名失败");
		}
		JsonNode root;
		try {
			root = objectMapper.readTree(raw);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Prism添加签名响应非合法JSON", e);
		}
		if (!root.has("res") || "error".equals(root.path("res").asText())) {
			throw new ForbiddenException("添加短信签名失败");
		}
	}

	public Map<String, Object> getSmsRemainder() {
		ensureSession();
		LinkedHashMap<String, Object> form = new LinkedHashMap<>();
		form.put("access_token", sessionAccessToken);
		form.put("shopexid", uidKeyPart());
		form.put("certi_app", "sms.newinfo");
		form.put("token", getForeverToken());
		String raw = prismCoreHttpClient.postSignedForm("/smsv2/send", form);
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			log.error("Prism SMS remainder: empty response");
			return null;
		}
		String trimmed = raw.trim();
		JsonNode root;
		try {
			root = objectMapper.readTree(trimmed);
		} catch (JsonProcessingException e) {
			log.error("Prism SMS remainder: invalid JSON");
			return null;
		}
		if (!root.isObject()) {
			log.error("Prism SMS remainder: root is not a JSON object");
			return null;
		}
		String res = root.has("res") && !root.get("res").isNull() ? root.get("res").asText(null) : null;
		if (res != null && "fail".equals(res)) {
			log.error("Prism SMS remainder: res=fail, raw={}", truncateForLog(trimmed, 500));
			return null;
		}
		try {
			return objectMapper.convertValue(root, new TypeReference<Map<String, Object>>() {});
		} catch (IllegalArgumentException e) {
			log.error("Prism SMS remainder: failed to convert response to map", e);
			return null;
		}
	}

	public String getSmsBuyUrl() {
		try {
			LinkedHashMap<String, Object> src = new LinkedHashMap<>();
			src.put("company_id", Long.valueOf(companyId));
			src.put("shopexid", uidKeyPart());
			String plain = objectMapper.writeValueAsString(src);
			byte[] utf8 = plain.getBytes(StandardCharsets.UTF_8);
			String base64 = Base64.getEncoder().encodeToString(utf8);
			String sourceParam = URLEncoder.encode(base64, StandardCharsets.UTF_8);
			return "http://sms.shopex.cn/?ctl=sms&act=prdsList&source=" + sourceParam;
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("SMS buy URL JSON build failed", e);
		}
	}

	/**
	 * Ensures Prism session tokens are loaded or refreshed before read-only operations
	 * that depend on the same Redis keys as add/update flows.
	 */
	public void primeSession() {
		ensureSession();
	}

	public void sendFanOutMarketingContents(List<Map<String, Object>> contents, String sendType) {
		ensureSession();
		String contentsJson;
		try {
			contentsJson = objectMapper.writeValueAsString(contents);
		} catch (JsonProcessingException e) {
			throw new ForbiddenException("短信发送失败");
		}
		LinkedHashMap<String, Object> form = new LinkedHashMap<>();
		form.put("access_token", sessionAccessToken);
		form.put("shopexid", uidKeyPart());
		form.put("certi_app", "sms.newsend");
		form.put("sendType", sendType);
		form.put("token", getForeverToken());
		form.put("source", FAN_OUT_SOURCE);
		form.put("contents", contentsJson);
		form.put("certi_ac", makeShopexAc(form, FAN_OUT_SOURCE_TOKEN));
		String raw = prismCoreHttpClient.postSignedForm("/smsv2/send", form);
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			throw new ForbiddenException("短信发送失败");
		}
		JsonNode root;
		try {
			root = objectMapper.readTree(raw.trim());
		} catch (JsonProcessingException e) {
			throw new ForbiddenException("短信发送失败");
		}
		if (!root.isObject()) {
			throw new ForbiddenException("短信发送失败");
		}
		JsonNode resNode = root.path("res");
		if (resNode.isMissingNode() || resNode.isNull()) {
			throw new ForbiddenException("短信发送失败");
		}
		if (resNode.isObject() || resNode.isArray()) {
			throw new ForbiddenException("短信发送失败");
		}
		if (resNode.isTextual()) {
			String res = resNode.asText("");
			if (!StringUtils.hasText(res)) {
				throw new ForbiddenException("短信发送失败");
			}
			if ("fail".equals(res)) {
				String code = root.path("msg").asText("");
				String msg = "1124".equals(code) ? "短信账户余额不足，请核实后再试" : "短信发送失败";
				throw new ForbiddenException(msg);
			}
			return;
		}
		if (resNode.isBoolean()) {
			if (!resNode.asBoolean()) {
				throw new ForbiddenException("短信发送失败");
			}
			return;
		}
		if (resNode.isNumber()) {
			if (resNode.asDouble() == 0.0d) {
				throw new ForbiddenException("短信发送失败");
			}
			return;
		}
		throw new ForbiddenException("短信发送失败");
	}

	public void updateSmsSign(String newContent, String oldContent) {
		ensureSession();
		LinkedHashMap<String, Object> form = new LinkedHashMap<>();
		form.put("access_token", sessionAccessToken);
		form.put("shopexid", uidKeyPart());
		form.put("new_content", newContent);
		form.put("old_content", oldContent);
		form.put("token", getForeverToken());
		String raw = prismCoreHttpClient.postSignedForm("/addcontent/updatebytoken", form);
		if (raw == null) {
			throw new ForbiddenException("请求更新接口出错");
		}
		JsonNode root;
		try {
			root = objectMapper.readTree(raw);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Prism更新签名响应非合法JSON", e);
		}
		if (!root.isObject()) {
			throw new ForbiddenException("请求更新接口出错");
		}
		if ("error".equals(root.path("res").asText())) {
			throw new ForbiddenException(root.path("data").asText("请求更新接口出错"));
		}
	}

	private void ensureSession() {
		checkToken();
		String at = prismRedisTemplate.opsForValue().get(accessTokenRedisKey());
		if (!StringUtils.hasText(at)) {
			throw new ForbiddenException("登录有误,请重新登录");
		}
		this.sessionAccessToken = at;
	}

	private void checkToken() {
		String access = prismRedisTemplate.opsForValue().get(accessTokenRedisKey());
		String refresh = prismRedisTemplate.opsForValue().get(refreshTokenRedisKey());
		if (!StringUtils.hasText(access) && !StringUtils.hasText(refresh)) {
			throw new ForbiddenException("登录有误,请重新登录");
		}
		if (StringUtils.hasText(access) && StringUtils.hasText(refresh)) {
			return;
		}
		LinkedHashMap<String, Object> form = new LinkedHashMap<>();
		form.put("grant_type", "refresh_token");
		form.put("refresh_token", refresh);
		String raw = prismCoreHttpClient.postSignedForm("/oauth/token", form);
		if (!StringUtils.hasText(raw)) {
			throw new ForbiddenException("请重新登录");
		}
		JsonNode root;
		try {
			root = objectMapper.readTree(raw);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Prism刷新令牌响应非合法JSON", e);
		}
		if ("error".equals(root.path("result").asText())) {
			throw new ForbiddenException("请重新登录");
		}
		String newAccess = root.path("access_token").asText("");
		if (!StringUtils.hasText(newAccess)) {
			throw new ForbiddenException("请重新登录");
		}
		long expiresField = root.path("expires_in").asLong(0L);
		String newRefresh = root.path("refresh_token").asText("");
		long refreshExpiresField = root.path("refresh_expires").asLong(0L);
		writeAccessToken(newAccess, expiresField);
		if (StringUtils.hasText(newRefresh)) {
			writeRefreshToken(newRefresh, refreshExpiresField);
		}
	}

	private String getForeverToken() {
		String existing = prismRedisTemplate.opsForValue().get(foreverTokenRedisKey());
		if (StringUtils.hasText(existing)) {
			return existing;
		}
		return fetchAndStoreForeverToken();
	}

	private String fetchAndStoreForeverToken() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put("product_code", "yuan_yuan_ke");
		String raw = prismCoreHttpClient.postSignedForm("/auth/auth.gettoken", map);
		if (raw == null) {
			throw new ForbiddenException("获取Prism永久令牌失败");
		}
		JsonNode root;
		try {
			root = objectMapper.readTree(raw);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Prism获取永久令牌响应非合法JSON", e);
		}
		if (!root.isObject()) {
			throw new ForbiddenException("获取Prism永久令牌失败");
		}
		if ("error".equals(root.path("status").asText())) {
			throw new ForbiddenException("获取Prism永久令牌失败");
		}
		JsonNode data = root.path("data");
		if (!data.isObject()) {
			throw new ForbiddenException("获取Prism永久令牌失败");
		}
		String token = data.path("token").asText("");
		if (!StringUtils.hasText(token)) {
			throw new ForbiddenException("获取Prism永久令牌失败");
		}
		prismRedisTemplate.opsForValue().set(foreverTokenRedisKey(), token);
		return token;
	}

	private void writeAccessToken(String accessToken, long expiresField) {
		String key = accessTokenRedisKey();
		prismRedisTemplate.opsForValue().set(key, accessToken);
		long expireAt = toExpireAtEpochSeconds(expiresField);
		if (expireAt > 0L) {
			prismRedisTemplate.expireAt(key, Instant.ofEpochSecond(expireAt));
		}
	}

	private void writeRefreshToken(String refreshToken, long refreshExpiresField) {
		String key = refreshTokenRedisKey();
		prismRedisTemplate.opsForValue().set(key, refreshToken);
		long expireAt = toExpireAtEpochSeconds(refreshExpiresField);
		if (expireAt > 0L) {
			prismRedisTemplate.expireAt(key, Instant.ofEpochSecond(expireAt));
		}
	}

	private static long toExpireAtEpochSeconds(long field) {
		if (field <= 0L) {
			return 0L;
		}
		long now = Instant.now().getEpochSecond();
		if (field > 1_000_000_000L) {
			return field;
		}
		return now + field;
	}

	private String uidKeyPart() {
		return shopexUid == null ? "" : shopexUid;
	}

	private String accessTokenRedisKey() {
		return "prism:" + sha1Hex(uidKeyPart() + "_" + companyId + "_AccessToken");
	}

	private String refreshTokenRedisKey() {
		return "prism:" + sha1Hex(uidKeyPart() + "_" + companyId + "_RefreshToken");
	}

	private String foreverTokenRedisKey() {
		return "prism:" + sha1Hex(uidKeyPart() + "_" + companyId + "_ForeverToken");
	}

	private static String makeShopexAc(Map<String, Object> tempArr, String token) {
		TreeMap<String, Object> sorted = new TreeMap<>(tempArr);
		StringBuilder str = new StringBuilder();
		for (Map.Entry<String, Object> e : sorted.entrySet()) {
			if ("certi_ac".equals(e.getKey())) {
				continue;
			}
			Object v = e.getValue();
			str.append(v == null ? "" : String.valueOf(v));
		}
		String inner = md5LowerHex(token).toLowerCase(Locale.ROOT);
		return md5LowerHex(str + inner).toLowerCase(Locale.ROOT);
	}

	private static String md5LowerHex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] d = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : d) {
				hex.append(String.format(Locale.ROOT, "%02x", b & 0xFF));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String truncateForLog(String s, int maxLen) {
		if (s.length() <= maxLen) {
			return s;
		}
		return s.substring(0, maxLen) + "...";
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
