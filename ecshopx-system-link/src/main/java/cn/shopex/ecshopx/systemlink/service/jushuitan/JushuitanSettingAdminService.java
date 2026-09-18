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

package cn.shopex.ecshopx.systemlink.service.jushuitan;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class JushuitanSettingAdminService {

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final String appUrl;
	private final String jushuitanAppKey;
	private final String jushuitanAppSecret;
	private final String jushuitanOauthBaseUrl;

	public JushuitanSettingAdminService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			@Value("${APP_URL:}") String appUrl,
			@Value("${ecshopx.jushuitan.app-key:}") String jushuitanAppKey,
			@Value("${ecshopx.jushuitan.app-secret:}") String jushuitanAppSecret,
			@Value("${ecshopx.jushuitan.oauth-base-url:}") String jushuitanOauthBaseUrl) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.appUrl = appUrl;
		this.jushuitanAppKey = jushuitanAppKey;
		this.jushuitanAppSecret = jushuitanAppSecret;
		this.jushuitanOauthBaseUrl = jushuitanOauthBaseUrl;
	}

	public Map<String, Object> getSetting(long companyId) {
		String key = "JushuitanSetting:" + sha1HexUtf8(String.valueOf(companyId));
		String raw = companysRedisTemplate.opsForValue().get(key);

		LinkedHashMap<String, Object> data;
		if (!StringUtils.hasText(raw)) {
			data = new LinkedHashMap<>();
			data.put("is_open", Boolean.FALSE);
		} else {
			try {
				LinkedHashMap<String, Object> parsed =
						objectMapper.readValue(raw, new TypeReference<LinkedHashMap<String, Object>>() {});
				if (parsed == null || parsed.isEmpty()) {
					data = new LinkedHashMap<>();
					data.put("is_open", Boolean.FALSE);
				} else {
					data = parsed;
				}
			} catch (Exception e) {
				data = new LinkedHashMap<>();
				data.put("is_open", Boolean.FALSE);
			}
		}

		Map<String, Object> signParams = new LinkedHashMap<>();
		signParams.put("app_key", jushuitanAppKey);
		signParams.put("timestamp", Instant.now().getEpochSecond());
		signParams.put("state", companyId);
		signParams.put("charset", "utf-8");
		String sign = md5SignHexLower(jushuitanAppSecret, signParams);

		Map<String, Object> orderedParamsWithSign = new LinkedHashMap<>();
		orderedParamsWithSign.put("app_key", jushuitanAppKey);
		orderedParamsWithSign.put("timestamp", signParams.get("timestamp"));
		orderedParamsWithSign.put("state", companyId);
		orderedParamsWithSign.put("charset", "utf-8");
		orderedParamsWithSign.put("sign", sign);

		String Q = utf8EncodedQueryString(orderedParamsWithSign);

		String oauthBase = jushuitanOauthBaseUrl == null ? "" : jushuitanOauthBaseUrl.trim();
		if (!StringUtils.hasText(oauthBase)) {
			data.put("oauth_url", "?" + Q);
		} else {
			if (!oauthBase.startsWith("https://")) {
				throw new BadRequestException("聚水潭 OAuth 地址必须为 https 绝对 URL");
			}
			data.put("oauth_url", buildOauthQueryUrl(oauthBase, orderedParamsWithSign));
		}

		String baseApp = appUrl == null ? "" : appUrl.trim();
		String mid = "/api/systemlink/jushuitan/";
		String cid = String.valueOf(companyId);

		String shopCallbackUrl;
		if (!StringUtils.hasText(baseApp)) {
			shopCallbackUrl = mid + cid;
		} else if (baseApp.endsWith("/")) {
			shopCallbackUrl = baseApp.substring(0, baseApp.length() - 1) + mid + cid;
		} else {
			shopCallbackUrl = baseApp + mid + cid;
		}
		data.put("shop_callback_url", shopCallbackUrl);

		return data;
	}

	public void setSetting(long companyId, Map<String, Object> mergedInput) {
		String key = "JushuitanSetting:" + sha1HexUtf8(String.valueOf(companyId));
		String raw = companysRedisTemplate.opsForValue().get(key);

		Map<String, Object> setting;
		if (!StringUtils.hasText(raw)) {
			setting = new LinkedHashMap<>();
		} else {
			setting = new LinkedHashMap<>(parseJsonObject(raw, objectMapper));
		}

		boolean isOpen = mergedInput != null
				&& mergedInput.containsKey("is_open")
				&& isOpenLooseEqTrueString(mergedInput.get("is_open"));

		int shopId = 0;
		if (mergedInput != null && mergedInput.containsKey("shop_id") && mergedInput.get("shop_id") != null) {
			String t = mergedInput.get("shop_id").toString().trim();
			if (StringUtils.hasText(t)) {
				try {
					shopId = Integer.parseInt(t);
				} catch (NumberFormatException e) {
					shopId = 0;
				}
			}
		}

		setting.remove("is_open");
		setting.remove("shop_id");

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("is_open", isOpen);
		data.put("shop_id", shopId);
		Map<String, Object> out = new LinkedHashMap<>(data);
		out.putAll(setting);

		try {
			companysRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(out));
		} catch (JsonProcessingException e) {
			throw new BadRequestException("配置序列化失败");
		}
	}

	private static String utf8EncodedQueryString(Map<String, Object> orderedParamsWithSign) {
		UriComponentsBuilder b = UriComponentsBuilder.newInstance();
		for (Map.Entry<String, Object> e : orderedParamsWithSign.entrySet()) {
			String v = e.getValue() == null ? "" : String.valueOf(e.getValue());
			b.queryParam(e.getKey(), v);
		}
		UriComponents built = b.encode(StandardCharsets.UTF_8).build();
		String q = built.getQuery();
		return q != null ? q : "";
	}

	private String buildOauthQueryUrl(String oauthBase, Map<String, Object> orderedParamsWithSign) {
		UriComponentsBuilder b;
		try {
			b = UriComponentsBuilder.fromHttpUrl(oauthBase);
		} catch (IllegalArgumentException e) {
			throw new BadRequestException("聚水潭 OAuth 地址格式无效");
		}
		for (Map.Entry<String, Object> e : orderedParamsWithSign.entrySet()) {
			String v = e.getValue() == null ? "" : String.valueOf(e.getValue());
			b.queryParam(e.getKey(), v);
		}
		return b.encode(StandardCharsets.UTF_8).build().toUriString();
	}

	private static String md5SignHexLower(String appSecret, Map<String, Object> paramsForSign) {
		String assembled = assembleFlatSortedForSign(paramsForSign);
		String payload = (appSecret == null ? "" : appSecret) + assembled;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(payload.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String assembleFlatSortedForSign(Map<String, Object> params) {
		Map<String, Object> sorted = new TreeMap<>(Comparator.naturalOrder());
		for (Map.Entry<String, Object> e : params.entrySet()) {
			if ("sign".equals(e.getKey())) {
				continue;
			}
			sorted.put(e.getKey(), e.getValue());
		}
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, Object> e : sorted.entrySet()) {
			Object val = e.getValue();
			sb.append(e.getKey()).append(val == null ? "" : String.valueOf(val));
		}
		return sb.toString();
	}

	private static boolean isOpenLooseEqTrueString(Object v) {
		if (v == null) {
			return false;
		}
		if (Boolean.TRUE.equals(v)) {
			return true;
		}
		if (v instanceof String s) {
			return "true".equals(s);
		}
		return false;
	}

	private static Map<String, Object> parseJsonObject(String raw, ObjectMapper objectMapper) {
		if (!StringUtils.hasText(raw)) {
			return Map.of();
		}
		try {
			return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			return Map.of();
		}
	}

	private static String sha1HexUtf8(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(dig);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
