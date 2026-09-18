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

package cn.shopex.ecshopx.goods.service.ome;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
public class ShopexErpOpenApiClient {

	private static final Logger log = LoggerFactory.getLogger(ShopexErpOpenApiClient.class);

	private static final int VER = 1;

	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper;
	private final String baseUrl;
	private final RestTemplate restTemplate;

	public ShopexErpOpenApiClient(
			@Qualifier("companysRedisTemplate") StringRedisTemplate redis,
			ObjectMapper objectMapper,
			@Value("${ecshopx.ome.openapi-url:}") String baseUrl) {
		this.redis = redis;
		this.objectMapper = objectMapper;
		this.baseUrl = baseUrl;
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(10_000);
		factory.setReadTimeout(10_000);
		this.restTemplate = new RestTemplate(factory);
	}

	public Map<String, Object> call(long companyId, String method, Map<String, Object> bizParams) {
		Map<String, Object> result = new LinkedHashMap<>();
		if (!StringUtils.hasText(baseUrl)) {
			result.put("fail_msg", "ome openapi url not configured");
			return result;
		}
		String flag = "";
		String token = "";
		String settingKey = "ShopexerpSetting:" + sha1Hex(String.valueOf(companyId));
		try {
			String rawSetting = redis.opsForValue().get(settingKey);
			if (StringUtils.hasText(rawSetting)) {
				Map<String, Object> setting = objectMapper.readValue(rawSetting, new TypeReference<Map<String, Object>>() {
				});
				Object f = setting.get("openapi_flag");
				Object t = setting.get("openapi_token");
				if (f != null) {
					flag = f.toString();
				}
				if (t != null) {
					token = t.toString();
				}
			}
		} catch (Exception e) {
			log.debug("ome openapi read setting failed companyId={} msg={}", companyId, e.getMessage());
		}

		try {
			Map<String, Object> merged = new LinkedHashMap<>();
			if (bizParams != null) {
				merged.putAll(bizParams);
			}
			merged.put("flag", flag);
			merged.put("method", method);
			merged.put("type", "json");
			merged.put("charset", "utf-8");
			merged.put("ver", VER);
			merged.put("timestamp", System.currentTimeMillis() / 1000L);
			String sign = genSign(merged, token);
			merged.put("sign", sign);

			MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
			for (Map.Entry<String, Object> e : merged.entrySet()) {
				Object v = e.getValue();
				if (v == null) {
					continue;
				}
				form.add(e.getKey(), stringifyFormValue(v));
			}

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			String url = trimSlash(baseUrl);
			log.debug("ome openapi request companyId={} method={} url={}", companyId, method, url);
			String body = restTemplate.postForObject(url, new HttpEntity<>(form, headers), String.class);
			if (!StringUtils.hasText(body)) {
				return result;
			}
			Map<String, Object> response = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {
			});
			result.put("rsp", "succ");
			if (response.containsKey("response")) {
				Object respVal = response.get("response");
				Map<String, Object> dataMap = new LinkedHashMap<>();
				if (respVal instanceof Map<?, ?> m) {
					for (Map.Entry<?, ?> e : m.entrySet()) {
						dataMap.put(String.valueOf(e.getKey()), e.getValue());
					}
				}
				dataMap.put("rsp", "succ");
				result.put("data", dataMap);
			} else {
				Object err = response.get("error_response");
				Map<String, Object> dataMap = new LinkedHashMap<>();
				if (err instanceof Map<?, ?> m) {
					for (Map.Entry<?, ?> e : m.entrySet()) {
						dataMap.put(String.valueOf(e.getKey()), e.getValue());
					}
				}
				dataMap.put("rsp", "fail");
				result.put("data", dataMap);
			}
		} catch (Exception e) {
			log.debug("ome openapi error companyId={} method={} msg={}", companyId, method, e.getMessage());
			result.put("fail_msg", e.getMessage());
		}
		return result;
	}

	private static String stringifyFormValue(Object v) {
		if (v instanceof Boolean b) {
			return b ? "1" : "0";
		}
		return String.valueOf(v);
	}

	private static String trimSlash(String u) {
		if (u.endsWith("/")) {
			return u.substring(0, u.length() - 1);
		}
		return u;
	}

	static String genSign(Map<String, Object> params, String token) {
		String assembled = assemble(params);
		String inner = md5UpperHex(assembled);
		return md5UpperHex(inner + token);
	}

	static String assemble(Map<String, Object> params) {
		if (params == null) {
			return "";
		}
		TreeMap<String, Object> sorted = new TreeMap<>(String::compareTo);
		sorted.putAll(params);
		StringBuilder sign = new StringBuilder();
		for (Map.Entry<String, Object> e : sorted.entrySet()) {
			Object val = e.getValue();
			if (val == null) {
				continue;
			}
			if (val instanceof Boolean b) {
				val = b ? 1 : 0;
			}
			sign.append(e.getKey());
			if (val instanceof Map<?, ?> m) {
				@SuppressWarnings("unchecked")
				Map<String, Object> nested = new LinkedHashMap<>();
				for (Map.Entry<?, ?> ne : m.entrySet()) {
					nested.put(String.valueOf(ne.getKey()), ne.getValue());
				}
				sign.append(assemble(nested));
			} else {
				sign.append(val);
			}
		}
		return sign.toString();
	}

	private static String md5UpperHex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] raw = md.digest(s.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(raw).toUpperCase();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String sha1Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] raw = md.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(raw);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
