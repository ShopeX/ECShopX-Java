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

package cn.shopex.ecshopx.payment.integration.doumenintl;

import cn.shopex.ecshopx.payment.config.DoumenIntlProperties;
import cn.shopex.ecshopx.payment.support.DoumenIntlSignature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * 斗门国际网关客户端：authorize / checkout / query / refund。
 */
@Component
public class DoumenIntlGatewayClient {

	private static final Logger log = LoggerFactory.getLogger(DoumenIntlGatewayClient.class);
	private static final String SUCCESS_CODE = "00000000";
	private static final int TOKEN_TTL_SAFETY_MARGIN = 60;

	private final DoumenIntlProperties properties;
	private final DoumenIntlTokenRedisStore tokenStore;
	private final ObjectMapper objectMapper;
	private final RestClient.Builder restClientBuilder;

	public DoumenIntlGatewayClient(
			DoumenIntlProperties properties,
			DoumenIntlTokenRedisStore tokenStore,
			ObjectMapper objectMapper,
			RestClient.Builder restClientBuilder) {
		this.properties = properties;
		this.tokenStore = tokenStore;
		this.objectMapper = objectMapper;
		this.restClientBuilder = restClientBuilder;
	}

	public String authorizeAndCache(String accessCode, String secretKey) {
		Map<String, Object> data = authorizeRaw(accessCode, secretKey);
		String token = stringVal(data.get("token"));
		int expireIn = intVal(data.get("expireIn"), -1);
		if (!StringUtils.hasText(token) || expireIn < 0) {
			throw new IllegalStateException(
					"Doumen Intl gateway authentication failed: authorize response missing token or expireIn.");
		}
		int ttlSeconds = Math.max(1, expireIn - TOKEN_TTL_SAFETY_MARGIN);
		tokenStore.setToken(accessCode, token, ttlSeconds);
		return token;
	}

	public boolean refreshToken(String accessCode, String secretKey) {
		try {
			authorizeAndCache(accessCode, secretKey);
			return true;
		} catch (Exception e) {
			log.info("Doumen Intl token refresh failed: {}", e.getMessage());
			return false;
		}
	}

	public Map<String, Object> createCheckout(
			String accessCode, String secretKey, Map<String, Object> body) {
		String token = resolveToken(accessCode, secretKey);
		String jsonBody = encodeJson(body);
		String signature = DoumenIntlSignature.signPostBody(jsonBody, secretKey);
		String path = "/api/acquire/checkout/create";
		Map<String, String> headers = businessHeaders(accessCode, token, signature);
		logGatewayRequest("POST", path, headers, jsonBody);
		String responseBody =
				client()
						.post()
						.uri(path)
						.headers(h -> applyBusinessHeaders(h, accessCode, token, signature))
						.body(jsonBody)
						.retrieve()
						.body(String.class);
		logGatewayResponse("POST", path, responseBody);
		return parseResponseData(responseBody);
	}

	public Map<String, Object> queryPayment(
			String accessCode, String secretKey, String transactionId) {
		String token = resolveToken(accessCode, secretKey);
		String path = "/api/acquire/payment/" + transactionId + "/get";
		Map<String, String> headers = businessHeaders(accessCode, token, null);
		logGatewayRequest("GET", path, headers, null);
		String responseBody =
				client()
						.get()
						.uri("/api/acquire/payment/{id}/get", transactionId)
						.headers(h -> applyBusinessHeaders(h, accessCode, token, null))
						.retrieve()
						.body(String.class);
		logGatewayResponse("GET", path, responseBody);
		return parseResponseData(responseBody);
	}

	public Map<String, Object> refundWithResult(
			String accessCode, String secretKey, String originalId, Map<String, Object> body) {
		String token = resolveToken(accessCode, secretKey);
		String jsonBody = encodeJson(body);
		String signature = DoumenIntlSignature.signPostBody(jsonBody, secretKey);
		String path = "/api/acquire/payment/" + originalId + "/refund";
		Map<String, String> headers = businessHeaders(accessCode, token, signature);
		logGatewayRequest("POST", path, headers, jsonBody);
		String responseBody =
				client()
						.post()
						.uri("/api/acquire/payment/{id}/refund", originalId)
						.headers(h -> applyBusinessHeaders(h, accessCode, token, signature))
						.body(jsonBody)
						.retrieve()
						.body(String.class);
		logGatewayResponse("POST", path, responseBody);
		return parseRefundResponse(responseBody);
	}

	private String resolveToken(String accessCode, String secretKey) {
		String cached = tokenStore.getValidToken(accessCode);
		if (StringUtils.hasText(cached)) {
			log.info(
					"Doumen Intl gateway token cache hit accessCode={} token={}",
					accessCode,
					cached);
			return cached;
		}
		return authorizeAndCache(accessCode, secretKey);
	}

	private Map<String, Object> authorizeRaw(String accessCode, String secretKey) {
		try {
			String path = "/authorize";
			Map<String, String> headers = new LinkedHashMap<>();
			headers.put("X-AccessCode", accessCode);
			headers.put("X-SecretKey", secretKey);
			logGatewayRequest("GET", path, headers, null);
			String responseBody =
					client()
							.get()
							.uri(path)
							.header("X-AccessCode", accessCode)
							.header("X-SecretKey", secretKey)
							.retrieve()
							.body(String.class);
			logGatewayResponse("GET", path, responseBody);
			return parseResponseData(responseBody);
		} catch (Exception e) {
			throw new IllegalStateException(
					"Doumen Intl gateway authentication failed: " + e.getMessage(), e);
		}
	}

	private RestClient client() {
		String base = properties.getBaseUrl();
		RestClient.Builder b = restClientBuilder;
		if (StringUtils.hasText(base)) {
			b = b.baseUrl(base.endsWith("/") ? base.substring(0, base.length() - 1) : base);
		}
		return b.build();
	}

	private String resolveBaseUrl() {
		String base = properties.getBaseUrl();
		if (!StringUtils.hasText(base)) {
			return "";
		}
		return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
	}

	private Map<String, String> businessHeaders(
			String accessCode, String token, String signature) {
		Map<String, String> headers = new LinkedHashMap<>();
		headers.put("Authorization", "Bearer " + token);
		headers.put("X-AccessCode", accessCode);
		if (signature != null) {
			headers.put("X-Signature", signature);
			headers.put("Content-Type", "application/json; charset=utf-8");
		}
		return headers;
	}

	private void logGatewayRequest(
			String method, String path, Map<String, String> headers, String body) {
		log.info(
				"Doumen Intl gateway request method={} url={} headers={} body={}",
				method,
				resolveBaseUrl() + path,
				headers,
				body == null ? "" : body);
	}

	private void logGatewayResponse(String method, String path, String body) {
		log.info(
				"Doumen Intl gateway response method={} url={} body={}",
				method,
				resolveBaseUrl() + path,
				body == null ? "" : body);
	}

	private void applyBusinessHeaders(
			org.springframework.http.HttpHeaders headers,
			String accessCode,
			String token,
			String signature) {
		headers.setBearerAuth(token);
		headers.set("X-AccessCode", accessCode);
		if (signature != null) {
			headers.set("X-Signature", signature);
			headers.setContentType(MediaType.parseMediaType("application/json; charset=utf-8"));
		}
	}

	private String encodeJson(Map<String, Object> payload) {
		try {
			return objectMapper.writeValueAsString(payload);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to encode Doumen Intl request body.", e);
		}
	}

	private Map<String, Object> parseResponseData(String body) {
		Map<String, Object> decoded = readJson(body);
		Object code = decoded.get("code");
		if (!SUCCESS_CODE.equals(String.valueOf(code == null ? "" : code))) {
			throw new IllegalStateException(
					"Doumen Intl gateway business error: " + String.valueOf(code));
		}
		Object data = decoded.get("data");
		if (!(data instanceof Map<?, ?> m)) {
			throw new IllegalStateException("Doumen Intl response missing data.");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> cast = (Map<String, Object>) m;
		return new LinkedHashMap<>(cast);
	}

	private Map<String, Object> parseRefundResponse(String body) {
		Map<String, Object> decoded = readJson(body);
		String code = String.valueOf(decoded.get("code") == null ? "" : decoded.get("code"));
		Map<String, Object> result = new LinkedHashMap<>();
		if (!SUCCESS_CODE.equals(code)) {
			result.put("ok", Boolean.FALSE);
			result.put("code", code);
			result.put("message", stringVal(decoded.get("message")));
			return result;
		}
		Object data = decoded.get("data");
		if (!(data instanceof Map<?, ?>)) {
			throw new IllegalStateException("Doumen Intl response missing data.");
		}
		result.put("ok", Boolean.TRUE);
		@SuppressWarnings("unchecked")
		Map<String, Object> cast = (Map<String, Object>) data;
		result.put("data", new LinkedHashMap<>(cast));
		return result;
	}

	private Map<String, Object> readJson(String body) {
		try {
			Map<String, Object> decoded =
					objectMapper.readValue(body, new TypeReference<>() {});
			return decoded == null ? Map.of() : decoded;
		} catch (Exception e) {
			throw new IllegalStateException("Doumen Intl response is not valid JSON.", e);
		}
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static int intVal(Object o, int defaultVal) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		if (o == null) {
			return defaultVal;
		}
		try {
			return Integer.parseInt(String.valueOf(o));
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}
}
