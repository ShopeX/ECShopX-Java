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

package cn.shopex.ecshopx.payment.service.orderquery;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class PaypalPayOrderInfoQueryService {

	private static final Logger log = LoggerFactory.getLogger(PaypalPayOrderInfoQueryService.class);

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;

	public PaypalPayOrderInfoQueryService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.restTemplate = new RestTemplate();
	}

	/**
	 * Loads PayPal Checkout order via REST v2. Configuration: Redis key {@code paypalPaymentSetting:sha1(companyId)}
	 * JSON with {@code client_id}, {@code client_secret}, and sandbox/live flags. Any failure is returned as a Map
	 * with {@code status=ERROR} and {@code error}; nothing is thrown to callers.
	 */
	public Map<String, Object> queryPayOrderInfo(long companyId, String tradeId) {
		String id = tradeId == null ? "" : tradeId.trim();
		try {
			Map<String, Object> cfg = loadPaypalSetting(companyId);
			String clientId = firstConfigString(
					cfg, "client_id", "clientId", "paypal_client_id", "PAYPAL_CLIENT_ID");
			String secret = firstConfigString(
					cfg, "client_secret", "clientSecret", "paypal_client_secret", "PAYPAL_CLIENT_SECRET");
			if (!StringUtils.hasText(clientId) || !StringUtils.hasText(secret)) {
				return errorResult(id, "PayPal 支付未配置");
			}
			if (!StringUtils.hasText(id)) {
				return errorResult(id, "缺少订单标识");
			}
			String base = resolveApiBase(cfg);
			String accessToken = fetchAccessToken(base, clientId, secret);
			if (!StringUtils.hasText(accessToken)) {
				return errorResult(id, "PayPal 授权失败");
			}
			if (id.regionMatches(true, 0, "PAY-", 0, 4)) {
				return fetchLegacyPaymentMap(base, accessToken, id);
			}
			return fetchOrderMap(base, accessToken, id);
		} catch (HttpStatusCodeException e) {
			log.warn("paypal order query http error companyId={} tradeId={} status={}", companyId, id, e.getStatusCode(), e);
			return errorResult(id, summarizePaypalHttpErrorBody(e));
		} catch (RestClientException e) {
			log.warn("paypal order query client error companyId={} tradeId={}", companyId, id, e);
			return errorResult(id, e.getMessage());
		} catch (Exception e) {
			log.warn("paypal order query failed companyId={} tradeId={}", companyId, id, e);
			return errorResult(id, e.getMessage());
		}
	}

	/**
	 * Loads a refund via REST v2 {@code GET /v2/payments/refunds/{id}}. Any failure returns the same {@code id} /
	 * {@code status=ERROR} / {@code error} envelope as {@link #queryPayOrderInfo}; nothing is thrown.
	 */
	public Map<String, Object> queryRefundOrderInfo(long companyId, String paypalRefundId) {
		String id = paypalRefundId == null ? "" : paypalRefundId.trim();
		try {
			Map<String, Object> cfg = loadPaypalSetting(companyId);
			String clientId = firstConfigString(
					cfg, "client_id", "clientId", "paypal_client_id", "PAYPAL_CLIENT_ID");
			String secret = firstConfigString(
					cfg, "client_secret", "clientSecret", "paypal_client_secret", "PAYPAL_CLIENT_SECRET");
			if (!StringUtils.hasText(clientId) || !StringUtils.hasText(secret)) {
				return errorResult(id, "PayPal 支付未配置");
			}
			if (!StringUtils.hasText(id)) {
				return errorResult(id, "缺少退款标识");
			}
			String base = resolveApiBase(cfg);
			String accessToken = fetchAccessToken(base, clientId, secret);
			if (!StringUtils.hasText(accessToken)) {
				return errorResult(id, "PayPal 授权失败");
			}
			return fetchRefundMap(base, accessToken, id);
		} catch (HttpStatusCodeException e) {
			log.warn("paypal refund query http error companyId={} refundId={} status={}", companyId, id, e.getStatusCode(), e);
			return errorResult(id, summarizePaypalHttpErrorBody(e));
		} catch (RestClientException e) {
			log.warn("paypal refund query client error companyId={} refundId={}", companyId, id, e);
			return errorResult(id, e.getMessage());
		} catch (Exception e) {
			log.warn("paypal refund query failed companyId={} refundId={}", companyId, id, e);
			return errorResult(id, e.getMessage());
		}
	}

	/**
	 * Same three-key error envelope as {@link #queryPayOrderInfo} for callers that skip the HTTP call (e.g. missing
	 * gateway refund id).
	 */
	public Map<String, Object> paypalQueryErrorEnvelope(String id, String message) {
		return errorResult(id, message);
	}

	/** Legacy REST payment id ({@code PAY-…}) uses v1 payments resource. */
	private Map<String, Object> fetchLegacyPaymentMap(String apiBase, String bearer, String paymentId) throws Exception {
		String url = apiBase + "/v1/payments/payment/" + paymentId;
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(bearer);
		headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
		ResponseEntity<String> resp =
				restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
		String body = resp.getBody();
		if (body == null || body.isBlank()) {
			return errorResult(paymentId, "PayPal 查询无响应");
		}
		Map<String, Object> payment =
				objectMapper.readValue(body, new TypeReference<LinkedHashMap<String, Object>>() {});
		if (!payment.containsKey("id")) {
			payment.put("id", paymentId);
		}
		return payment;
	}

	private Map<String, Object> fetchOrderMap(String apiBase, String bearer, String orderId) throws Exception {
		String url = apiBase + "/v2/checkout/orders/" + orderId;
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(bearer);
		headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
		ResponseEntity<String> resp =
				restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
		String body = resp.getBody();
		if (body == null || body.isBlank()) {
			return errorResult(orderId, "PayPal 查询无响应");
		}
		Map<String, Object> order =
				objectMapper.readValue(body, new TypeReference<LinkedHashMap<String, Object>>() {});
		if (!order.containsKey("id")) {
			order.put("id", orderId);
		}
		return order;
	}

	private Map<String, Object> fetchRefundMap(String apiBase, String bearer, String refundId) throws Exception {
		String url = apiBase + "/v2/payments/refunds/" + refundId;
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(bearer);
		headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
		ResponseEntity<String> resp =
				restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
		String body = resp.getBody();
		if (body == null || body.isBlank()) {
			return errorResult(refundId, "PayPal 查询无响应");
		}
		Map<String, Object> refund =
				objectMapper.readValue(body, new TypeReference<LinkedHashMap<String, Object>>() {});
		if (!refund.containsKey("id")) {
			refund.put("id", refundId);
		}
		return refund;
	}

	private String fetchAccessToken(String apiBase, String clientId, String clientSecret) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		String encoded = Base64.getEncoder().encodeToString(
				(clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
		headers.set("Authorization", "Basic " + encoded);
		HttpEntity<String> entity = new HttpEntity<>("grant_type=client_credentials", headers);
		ResponseEntity<String> resp =
				restTemplate.postForEntity(apiBase + "/v1/oauth2/token", entity, String.class);
		if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
			return "";
		}
		JsonNode n = objectMapper.readTree(resp.getBody());
		return n.path("access_token").asText("");
	}

	private String summarizePaypalHttpErrorBody(HttpStatusCodeException ex) {
		String body = ex.getResponseBodyAsString();
		if (!StringUtils.hasText(body)) {
			return ex.getStatusCode().value() + " " + Objects.toString(ex.getStatusText(), "");
		}
		try {
			JsonNode n = objectMapper.readTree(body);
			if (n.hasNonNull("message")) {
				String m = n.get("message").asText();
				if (StringUtils.hasText(m)) {
					return m;
				}
			}
			JsonNode details = n.get("details");
			if (details != null && details.isArray() && details.size() > 0) {
				JsonNode d0 = details.get(0);
				if (d0.hasNonNull("issue")) {
					return d0.get("issue").asText();
				}
				if (d0.hasNonNull("description")) {
					return d0.get("description").asText();
				}
			}
		} catch (Exception ignored) {
		}
		return body.length() > 500 ? body.substring(0, 500) : body;
	}

	private Map<String, Object> loadPaypalSetting(long companyId) {
		String key = "paypalPaymentSetting:" + sha1Hex(String.valueOf(companyId));
		String raw = companysRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			return Map.of();
		}
		try {
			return objectMapper.readValue(raw, new TypeReference<>() {});
		} catch (Exception e) {
			return Map.of();
		}
	}

	private static String firstConfigString(Map<String, Object> cfg, String... keys) {
		if (cfg == null || keys == null) {
			return "";
		}
		for (String k : keys) {
			Object v = cfg.get(k);
			if (v == null) {
				continue;
			}
			String s = Objects.toString(v, "").trim();
			if (StringUtils.hasText(s)) {
				return s;
			}
		}
		return "";
	}

	private static String resolveApiBase(Map<String, Object> cfg) {
		boolean sandbox = false;
		Object sb = cfg.get("sandbox");
		if (sb instanceof Boolean b) {
			sandbox = b;
		} else if (sb != null && "true".equalsIgnoreCase(Objects.toString(sb, "").trim())) {
			sandbox = true;
		}
		Object mode = cfg.get("mode");
		if (mode != null) {
			String m = Objects.toString(mode, "").trim().toLowerCase(Locale.ROOT);
			if ("sandbox".equals(m) || "development".equals(m)) {
				sandbox = true;
			} else if ("live".equals(m) || "production".equals(m)) {
				sandbox = false;
			}
		}
		Object env = cfg.get("environment");
		if (env != null) {
			String e = Objects.toString(env, "").trim().toLowerCase(Locale.ROOT);
			if ("sandbox".equals(e)) {
				sandbox = true;
			} else if ("live".equals(e) || "production".equals(e)) {
				sandbox = false;
			}
		}
		Object live = cfg.get("live");
		if (live instanceof Boolean b) {
			sandbox = !b;
		} else if (live != null && "true".equalsIgnoreCase(Objects.toString(live, "").trim())) {
			sandbox = false;
		}
		return sandbox ? "https://api-m.sandbox.paypal.com" : "https://api-m.paypal.com";
	}

	private static Map<String, Object> errorResult(String tradeId, String message) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", tradeId == null ? "" : tradeId);
		m.put("status", "ERROR");
		m.put("error", StringUtils.hasText(message) ? message : "PayPal 查询失败");
		return m;
	}

	private static String sha1Hex(String companyId) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(companyId.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-1 not available", e);
		}
	}
}
