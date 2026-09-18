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

package cn.shopex.ecshopx.payment.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.payment.integration.doumenintl.DoumenIntlNotifyOrderSidePort;
import cn.shopex.ecshopx.payment.service.settings.DoumenIntlPaymentSettingReader;
import cn.shopex.ecshopx.payment.support.DoumenIntlSignature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

@Service
public class DoumenIntlNotifyFacade {

	private static final Logger log = LoggerFactory.getLogger(DoumenIntlNotifyFacade.class);
	private static final String SUCCESS_CODE = "00000000";

	private final DoumenIntlNotifyOrderSidePort orderSidePort;
	private final DoumenIntlPaymentSettingReader paymentSettingReader;
	private final ObjectMapper objectMapper;

	public DoumenIntlNotifyFacade(
			DoumenIntlNotifyOrderSidePort orderSidePort,
			DoumenIntlPaymentSettingReader paymentSettingReader,
			ObjectMapper objectMapper) {
		this.orderSidePort = orderSidePort;
		this.paymentSettingReader = paymentSettingReader;
		this.objectMapper = objectMapper;
	}

	public ResponseEntity<Map<String, String>> handle(HttpServletRequest request) {
		String rawBody;
		try {
			rawBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			log.warn("doumen_intl_notify_read_body_failed", e);
			return ack("10000001", "INVALID_PAYLOAD");
		}
		String signature = header(request, "X-Signature");

		Map<String, Object> payload;
		try {
			payload = objectMapper.readValue(rawBody, new TypeReference<>() {});
		} catch (Exception e) {
			log.warn("doumen_intl_notify_invalid_payload");
			return ack("10000001", "INVALID_PAYLOAD");
		}
		if (payload == null) {
			log.warn("doumen_intl_notify_invalid_payload");
			return ack("10000001", "INVALID_PAYLOAD");
		}

		log.info("doumen_intl_notify_payload keys={}", payload.keySet());
		Object tradeIdRaw = payload.get("merchantOrderId");
		String gatewayStatus = stringVal(payload.get("status"));
		Object transactionIdRaw = payload.get("id");

		if (!(tradeIdRaw instanceof String tradeId) || !StringUtils.hasText(tradeId)) {
			log.warn("doumen_intl_notify_missing_merchant_order_id");
			return ack("10000002", "MISSING_MERCHANT_ORDER_ID");
		}

		DoumenIntlNotifyOrderSidePort.TradeBrief trade = orderSidePort.findTrade(tradeId);
		if (trade == null) {
			log.warn(
					"doumen_intl_notify_unknown_trade trade_id={} gateway_status={}",
					tradeId,
					gatewayStatus);
			return ack("10000003", "UNKNOWN_TRADE");
		}

		Map<String, Object> setting = paymentSettingReader.getRaw(trade.companyId());
		String secretKey = stringVal(setting.get("X-SecretKey"));
		if (!DoumenIntlSignature.verifyNotify(rawBody, secretKey, signature)) {
			log.warn(
					"doumen_intl_notify_invalid_signature trade_id={} signature={}",
					tradeId,
					signature);
			return ack("10000004", "SIGNATURE_VERIFICATION_FAILED");
		}

		if ("PENDING".equals(gatewayStatus) || "RETRY_PENDING".equals(gatewayStatus)) {
			log.info(
					"doumen_intl_notify_pending_status trade_id={} gateway_status={}",
					tradeId,
					gatewayStatus);
			return ack(SUCCESS_CODE, "SUCCESS");
		}

		String transactionId =
				transactionIdRaw == null ? null : String.valueOf(transactionIdRaw);
		Map<String, Object> options = new LinkedHashMap<>();
		options.put("pay_type", "doumen_intl");
		options.put("transaction_id", transactionId == null ? "" : transactionId);

		if ("SUCCEED".equals(gatewayStatus)) {
			if ("SUCCESS".equals(trade.tradeState())) {
				return ack(SUCCESS_CODE, "SUCCESS");
			}
			applyQuietly(tradeId, "SUCCESS", options);
			return ack(SUCCESS_CODE, "SUCCESS");
		}

		if ("FAILED".equals(gatewayStatus)) {
			if ("PAYERROR".equals(trade.tradeState()) || "SUCCESS".equals(trade.tradeState())) {
				return ack(SUCCESS_CODE, "SUCCESS");
			}
			applyQuietly(tradeId, "PAYERROR", options);
			return ack(SUCCESS_CODE, "SUCCESS");
		}

		log.info(
				"doumen_intl_notify_unhandled_status trade_id={} gateway_status={}",
				tradeId,
				gatewayStatus);
		return ack(SUCCESS_CODE, "SUCCESS");
	}

	private void applyQuietly(String tradeId, String status, Map<String, Object> options) {
		try {
			orderSidePort.applyTradePayment(tradeId, status, options);
		} catch (ResourceException e) {
			log.info(
					"doumen_intl_notify_apply_skipped trade_id={} status={} msg={}",
					tradeId,
					status,
					e.getMessage());
		}
	}

	private static ResponseEntity<Map<String, String>> ack(String code, String message) {
		Map<String, String> body = new LinkedHashMap<>();
		body.put("code", code);
		body.put("message", message);
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
	}

	private static String header(HttpServletRequest request, String name) {
		String v = request.getHeader(name);
		return v == null ? "" : v;
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o);
	}
}
