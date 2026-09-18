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

package cn.shopex.ecshopx.chinaumspay.service.query;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ChinaumsPayOrderInfoQueryService {

	private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	@Value("${ecshopx.ums.app-id:}")
	private String umsAppId;

	@Value("${ecshopx.ums.app-key:}")
	private String umsAppKey;

	@Value("${ecshopx.ums.api-base-uri:}")
	private String umsApiBaseUri;

	public ChinaumsPayOrderInfoQueryService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	/**
	 * Ensures platform UMS credentials and Redis merchant setting (mid/tid) exist.
	 * Call before validating trade-level fields for order-query flows.
	 */
	public void requireChinaumsPaymentSetting(long companyId) {
		if (!StringUtils.hasText(umsAppId) || !StringUtils.hasText(umsAppKey) || !StringUtils.hasText(umsApiBaseUri)) {
			throw new BadRequestException("请检查银联支付相关配置是否完成", 400);
		}
		Map<String, Object> paySetting = loadChinaumsPaymentSetting(companyId);
		if (paySetting.isEmpty()) {
			throw new BadRequestException("请检查银联支付相关配置是否完成", 400);
		}
		String mid = str(paySetting.get("mid"));
		String terminalId = str(paySetting.get("tid"));
		if (!StringUtils.hasText(mid) || !StringUtils.hasText(terminalId)) {
			throw new BadRequestException("请检查银联支付相关配置是否完成", 400);
		}
	}

	public Map<String, Object> queryPayOrderInfo(long companyId, String tradeId, String merOrderIdPrefix) {
		requireChinaumsPaymentSetting(companyId);
		Map<String, Object> paySetting = loadChinaumsPaymentSetting(companyId);
		String mid = str(paySetting.get("mid"));
		String terminalId = str(paySetting.get("tid"));
		String pre = merOrderIdPrefix == null ? "" : merOrderIdPrefix;
		String merOrderId = pre + tradeId;
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("msgId", genMsgId());
		body.put("requestTimestamp", LocalDateTime.now().format(TS));
		body.put("mid", mid);
		body.put("tid", terminalId);
		body.put("merOrderId", merOrderId);
		body.put("instMid", "YUEDANDEFAULT");

		String timestamp =
				java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
		String nonce = java.util.UUID.randomUUID().toString().replace("-", "");
		String signature;
		try {
			signature = buildAuthorizationSignature(body, timestamp, nonce);
		} catch (Exception e) {
			throw new BadRequestException("银联商务订单查询签名失败", 400);
		}
		String authorization =
				"OPEN-BODY-SIG AppId=\""
						+ umsAppId
						+ "\", Timestamp=\""
						+ timestamp
						+ "\", Nonce=\""
						+ nonce
						+ "\", Signature=\""
						+ signature
						+ "\"";
		String jsonBody;
		try {
			jsonBody = objectMapper.writeValueAsString(body);
		} catch (Exception e) {
			throw new BadRequestException("银联商务订单查询参数错误", 400);
		}
		String url = umsApiBaseUri.replaceAll("/$", "") + "/query";
		HttpRequest req =
				HttpRequest.newBuilder()
						.uri(URI.create(url))
						.header("Content-Type", "application/json;charset=UTF-8")
						.header("Authorization", authorization)
						.POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
						.build();
		try {
			HttpClient client = HttpClient.newHttpClient();
			HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			String respBody = resp.body();
			if (!StringUtils.hasText(respBody)) {
				throw new BadRequestException("银联商务订单查询无响应", 400);
			}
			JsonNode root = objectMapper.readTree(respBody);
			Map<String, Object> out = new LinkedHashMap<>();
			var it = root.fields();
			while (it.hasNext()) {
				var e = it.next();
				out.put(e.getKey(), jsonValueToJava(e.getValue()));
			}
			return out;
		} catch (BadRequestException e) {
			throw e;
		} catch (Exception e) {
			throw new BadRequestException("银联商务订单查询请求失败", 400);
		}
	}

	/**
	 * Refund query ({@code /refund-query}). Caller must ensure {@code targetRefundId} is non-blank.
	 */
	public Map<String, Object> queryRefundOrderInfo(
			long companyId, String refundBn, String targetRefundId, String merOrderIdPrefix) {
		requireChinaumsPaymentSetting(companyId);
		Map<String, Object> paySetting = loadChinaumsPaymentSetting(companyId);
		String mid = str(paySetting.get("mid"));
		String terminalId = str(paySetting.get("tid"));
		String pre = merOrderIdPrefix == null ? "" : merOrderIdPrefix;
		String merOrderId = pre + refundBn;
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("msgId", genMsgId());
		body.put("requestTimestamp", LocalDateTime.now().format(TS));
		body.put("mid", mid);
		body.put("tid", terminalId);
		body.put("merOrderId", merOrderId);
		body.put("instMid", "YUEDANDEFAULT");
		body.put("targetOrderId", targetRefundId);

		String timestamp =
				java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
		String nonce = java.util.UUID.randomUUID().toString().replace("-", "");
		String signature;
		try {
			signature = buildAuthorizationSignature(body, timestamp, nonce);
		} catch (Exception e) {
			throw new BadRequestException("银联商务退款查询签名失败", 400);
		}
		String authorization =
				"OPEN-BODY-SIG AppId=\""
						+ umsAppId
						+ "\", Timestamp=\""
						+ timestamp
						+ "\", Nonce=\""
						+ nonce
						+ "\", Signature=\""
						+ signature
						+ "\"";
		String jsonBody;
		try {
			jsonBody = objectMapper.writeValueAsString(body);
		} catch (Exception e) {
			throw new BadRequestException("银联商务退款查询参数错误", 400);
		}
		String url = umsApiBaseUri.replaceAll("/$", "") + "/refund-query";
		HttpRequest req =
				HttpRequest.newBuilder()
						.uri(URI.create(url))
						.header("Content-Type", "application/json;charset=UTF-8")
						.header("Authorization", authorization)
						.POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
						.build();
		try {
			HttpClient client = HttpClient.newHttpClient();
			HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			String respBody = resp.body();
			if (!StringUtils.hasText(respBody)) {
				throw new BadRequestException("银联商务退款查询无响应", 400);
			}
			JsonNode root = objectMapper.readTree(respBody);
			Map<String, Object> out = new LinkedHashMap<>();
			var it = root.fields();
			while (it.hasNext()) {
				var e = it.next();
				out.put(e.getKey(), jsonValueToJava(e.getValue()));
			}
			return out;
		} catch (BadRequestException e) {
			throw e;
		} catch (Exception e) {
			throw new BadRequestException("银联商务退款查询请求失败", 400);
		}
	}

	private static Object jsonValueToJava(JsonNode n) {
		if (n == null || n.isNull()) {
			return null;
		}
		if (n.isBoolean()) {
			return n.booleanValue();
		}
		if (n.isInt()) {
			return n.intValue();
		}
		if (n.isLong()) {
			return n.longValue();
		}
		if (n.isDouble() || n.isFloat()) {
			return n.doubleValue();
		}
		if (n.isTextual()) {
			return n.asText();
		}
		if (n.isArray()) {
			List<Object> list = new ArrayList<>();
			for (JsonNode c : n) {
				list.add(jsonValueToJava(c));
			}
			return list;
		}
		if (n.isObject()) {
			Map<String, Object> m = new LinkedHashMap<>();
			var oit = n.fields();
			while (oit.hasNext()) {
				var e = oit.next();
				m.put(e.getKey(), jsonValueToJava(e.getValue()));
			}
			return m;
		}
		return n.asText();
	}

	private Map<String, Object> loadChinaumsPaymentSetting(long companyId) {
		String key = "chinaumsPaymentSetting:" + sha1Hex(String.valueOf(companyId));
		String raw = companysRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			return Map.of();
		}
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> m = objectMapper.readValue(raw, Map.class);
			return m != null ? m : Map.of();
		} catch (Exception e) {
			return Map.of();
		}
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

	private String buildAuthorizationSignature(Map<String, Object> body, String timestamp, String nonce)
			throws Exception {
		String bodyJson = objectMapper.writeValueAsString(body);
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		byte[] dig = md.digest(bodyJson.getBytes(StandardCharsets.UTF_8));
		String str = HexFormat.of().formatHex(dig);
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(umsAppKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		byte[] sig = mac.doFinal((umsAppId + timestamp + nonce + str).getBytes(StandardCharsets.UTF_8));
		return java.util.Base64.getEncoder().encodeToString(sig);
	}

	private static String genMsgId() {
		return String.valueOf(System.currentTimeMillis()) + ThreadLocalRandom.current().nextInt(100_000, 999_999);
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}
}
