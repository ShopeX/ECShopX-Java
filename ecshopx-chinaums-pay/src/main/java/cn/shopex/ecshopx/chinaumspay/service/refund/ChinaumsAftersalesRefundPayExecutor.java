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

package cn.shopex.ecshopx.chinaumspay.service.refund;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.refund.AftersalesRefundPayChannelExecutor;
import cn.shopex.ecshopx.common.refund.AftersalesRefundPaymentContext;
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
import java.util.HexFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 银联商务开放平台退款。网关签名算法对位 {@code UmsClient::request}；{@code resubmit} 为真时会重置退款单号后缀（对位
 * {@code ChinaumsPayService::changeMerOrd}）。
 */
@Service
@Order(52)
public class ChinaumsAftersalesRefundPayExecutor implements AftersalesRefundPayChannelExecutor {

	private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter BILL = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;

	@Value("${ecshopx.ums.app-id:}")
	private String umsAppId;

	@Value("${ecshopx.ums.app-key:}")
	private String umsAppKey;

	@Value("${ecshopx.ums.api-base-uri:}")
	private String umsApiBaseUri;

	@Value("${ecshopx.ums.order-id-prefix:}")
	private String umsOrderIdPrefix;

	public ChinaumsAftersalesRefundPayExecutor(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			OrderProcessLogPublishPort orderProcessLogPublishPort) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
	}

	/**
	 * Binds gateway credentials and base URI for unit tests in this package (avoids Spring property injection in
	 * probes).
	 */
	void bindGatewayForProbe(String appId, String appKey, String apiBaseUri) {
		this.umsAppId = appId == null ? "" : appId;
		this.umsAppKey = appKey == null ? "" : appKey;
		this.umsApiBaseUri = apiBaseUri == null ? "" : apiBaseUri;
	}

	@Override
	public boolean supports(String payTypeLower) {
		return "chinaums".equals(payTypeLower);
	}

	@Override
	public Map<String, Object> execute(AftersalesRefundPaymentContext ctx) {
		if (!StringUtils.hasText(umsAppId) || !StringUtils.hasText(umsAppKey) || !StringUtils.hasText(umsApiBaseUri)) {
			return fail("银联商务开放平台网关参数未配置（ecshopx.ums.*）");
		}
		Map<String, Object> paySetting = loadChinaumsPaymentSetting(ctx.getCompanyId());
		if (paySetting.isEmpty()) {
			return fail("请先完成银联商务支付配置");
		}
		String mid = str(paySetting.get("mid"));
		String tid = str(paySetting.get("tid"));
		if (!StringUtils.hasText(mid) || !StringUtils.hasText(tid)) {
			return fail("请先完成银联商务支付配置");
		}
		String pre = umsOrderIdPrefix == null ? "" : umsOrderIdPrefix;
		String merOrderId = pre + ctx.getTradeId();
		String refundNoBase = String.valueOf(ctx.getRefundBn());
		String refundOrderId = pre + refundNoBase;
		if (ctx.isResubmit()) {
			refundOrderId = pre + refundNoBase + ThreadLocalRandom.current().nextInt(10, 100);
		}
		int refundFen = Math.max(ctx.getRefundFeeFen(), 0);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("msgId", genMsgId());
		body.put("requestTimestamp", LocalDateTime.now().format(TS));
		body.put("mid", mid);
		body.put("tid", tid);
		body.put("merOrderId", merOrderId);
		body.put("instMid", "YUEDANDEFAULT");
		body.put("platformAmount", refundFen);
		body.put("refundAmount", refundFen);
		body.put("refundOrderId", refundOrderId);
		body.put("subOrders", Collections.emptyList());
		body.put("billDate", LocalDateTime.now().format(BILL));

		String timestamp = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
				.format(LocalDateTime.now());
		String nonce = java.util.UUID.randomUUID().toString().replace("-", "");
		String signature;
		try {
			signature = buildAuthorizationSignature(body, timestamp, nonce);
		} catch (Exception e) {
			return fail("银联商务退款签名失败");
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
			return fail("银联商务退款参数错误");
		}
		String url = umsApiBaseUri.replaceAll("/$", "") + "/refund";
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
				return fail("银联商务退款无响应");
			}
			JsonNode root = objectMapper.readTree(respBody);
			if (root.has("errCode")) {
				String errCode = root.get("errCode").asText("");
				if ("SUCCESS".equalsIgnoreCase(errCode)) {
					Map<String, Object> ok = new LinkedHashMap<>();
					ok.put("status", "SUCCESS");
					ok.put(
							"refund_id",
							root.has("refundTargetOrderId")
									? root.get("refundTargetOrderId").asText(refundOrderId)
									: (root.has("refund_id")
											? root.get("refund_id").asText(refundOrderId)
											: refundOrderId));
					publishRefundOrderProcessLog(ctx, unionPaySuccessDetail(ctx.getOrderId()));
					return ok;
				}
				String errMsg = root.has("errMsg") ? root.get("errMsg").asText("银联商务退款失败") : "银联商务退款失败";
				Map<String, Object> f = new LinkedHashMap<>();
				f.put("status", "FAIL");
				f.put("error_code", errCode);
				f.put("error_desc", errMsg);
				publishRefundOrderProcessLog(ctx, unionPayErrCodeFailDetail(ctx.getOrderId(), errMsg));
				return f;
			}
			if (root.has("status")) {
				String st = root.get("status").asText("");
				if ("SUCCESS".equalsIgnoreCase(st)) {
					Map<String, Object> ok = new LinkedHashMap<>();
					ok.put("status", "SUCCESS");
					ok.put(
							"refund_id",
							root.has("refund_id") ? root.get("refund_id").asText(refundOrderId) : refundOrderId);
					publishRefundOrderProcessLog(ctx, unionPaySuccessDetail(ctx.getOrderId()));
					return ok;
				}
				String errorDesc =
						root.has("error_desc") ? root.get("error_desc").asText("银联商务退款失败") : "银联商务退款失败";
				Map<String, Object> f = new LinkedHashMap<>();
				f.put("status", "FAIL");
				f.put("error_desc", errorDesc);
				publishRefundOrderProcessLog(ctx, unionPayStatusFailDetail(ctx.getOrderId(), errorDesc));
				return f;
			}
			return fail("银联商务退款响应无法解析");
		} catch (Exception e) {
			return fail("银联商务退款请求失败");
		}
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
			ResourceException ex = new ResourceException("SHA-1 不可用");
			ex.initCause(e);
			throw ex;
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

	private void publishRefundOrderProcessLog(AftersalesRefundPaymentContext ctx, String detail) {
		LinkedHashMap<String, Object> entities = new LinkedHashMap<>();
		entities.put("order_id", ctx.getOrderId());
		entities.put("company_id", ctx.getCompanyId());
		entities.put("operator_type", "system");
		entities.put("remarks", "订单退款");
		entities.put("detail", detail);
		orderProcessLogPublishPort.publish(entities);
	}

	private static String unionPaySuccessDetail(long orderId) {
		return "订单号：" + orderId + "，订单退款成功（银联支付渠道）";
	}

	private static String unionPayStatusFailDetail(long orderId, String errorDesc) {
		return "订单号：" + orderId + "，订单退款失败（银联支付渠道），失败原因：" + errorDesc;
	}

	private static String unionPayErrCodeFailDetail(long orderId, String errMsg) {
		return "订单号：" + orderId + "，订单退款失败（银联支付渠道），失败原因：" + errMsg;
	}

	private static Map<String, Object> fail(String msg) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("status", "FAIL");
		m.put("error_desc", msg);
		return m;
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}
}
