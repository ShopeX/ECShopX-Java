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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.payment.config.DoumenIntlProperties;
import cn.shopex.ecshopx.payment.integration.doumenintl.DoumenIntlGatewayClient;
import cn.shopex.ecshopx.payment.service.settings.DoumenIntlPaymentSettingReader;
import cn.shopex.ecshopx.payment.support.DoumenIntlStatusMapper;
import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
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
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

/**
 * 斗门国际：拉起收银台 / 查询 / 退款。
 */
@Service
public class DoumenIntlPaymentService {

	private static final Logger log = LoggerFactory.getLogger(DoumenIntlPaymentService.class);

	private static final DateTimeFormatter REFUND_TIME =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final DoumenIntlPaymentSettingReader settingReader;
	private final DoumenIntlGatewayClient gatewayClient;
	private final DoumenIntlProperties properties;

	public DoumenIntlPaymentService(
			DoumenIntlPaymentSettingReader settingReader,
			DoumenIntlGatewayClient gatewayClient,
			DoumenIntlProperties properties) {
		this.settingReader = settingReader;
		this.gatewayClient = gatewayClient;
		this.properties = properties;
	}

	/**
	 * @param products 订单明细映射后的网关 products；空则拒绝
	 * @return {@code pay_url}；副作用由调用方写 trade.transaction_id（本方法返回 {@code transaction_id}）
	 */
	public Map<String, Object> doPay(Map<String, Object> data, List<Map<String, Object>> products) {
		assertNormalTradeSource(data);
		long companyId = longVal(data.get("company_id"));
		assertMerchantPayReady(companyId);
		Map<String, Object> setting = settingReader.getRaw(companyId);
		String returnUrl = stringVal(data.get("return_url"));
		if (!StringUtils.hasText(returnUrl)) {
			returnUrl = stringVal(setting.get("return_url"));
		}
		if (products == null || products.isEmpty()) {
			throw new BadRequestException("斗门国际支付缺少订单商品明细");
		}

		String tradeId = stringVal(data.get("trade_id"));
		Map<String, Object> checkoutBody = new LinkedHashMap<>();
		checkoutBody.put("checkoutType", "DOU_MEN");
		checkoutBody.put("requestId", md5Hex(UUID.randomUUID() + "-" + tradeId));
		checkoutBody.put("appId", setting.get("appId"));
		checkoutBody.put("merchantOrderId", tradeId);
		checkoutBody.put("amount", intVal(data.get("pay_fee"), 0));
		checkoutBody.put("currency", stringVal(data.get("fee_type")));
		checkoutBody.put("successUrl", returnUrl);
		checkoutBody.put("failureUrl", returnUrl);
		checkoutBody.put("cancelUrl", returnUrl);
		checkoutBody.put("notificationUrl", properties.getNotifyUrl());
		checkoutBody.put("products", products);

		Object autoCancel = data.get("auto_cancel_time");
		if (autoCancel != null && StringUtils.hasText(String.valueOf(autoCancel))) {
			int autoCancelTime = intVal(autoCancel, 0);
			int now = (int) (System.currentTimeMillis() / 1000L);
			if (autoCancelTime > now) {
				checkoutBody.put("validityPeriod", autoCancelTime - now);
			}
		}

		String accessCode = stringVal(setting.get("X-AccessCode"));
		String secretKey = stringVal(setting.get("X-SecretKey"));
		Map<String, Object> checkoutResult =
				invokeGateway(
						() -> gatewayClient.createCheckout(accessCode, secretKey, checkoutBody),
						"斗门国际支付创建失败");
		if (!"REQUEST_CUSTOMER_ACTION".equals(stringVal(checkoutResult.get("status")))) {
			throw new BadRequestException("斗门国际支付创建失败");
		}
		Object nextAction = checkoutResult.get("nextAction");
		String payUrl = "";
		if (nextAction instanceof Map<?, ?> na) {
			Object url = na.get("url");
			payUrl = url == null ? "" : String.valueOf(url);
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("pay_url", payUrl);
		out.put("transaction_id", stringVal(checkoutResult.get("id")));
		out.put("pay_type", "doumen_intl");
		return out;
	}

	public Map<String, Object> query(long companyId, String transactionId) {
		assertMerchantPayReady(companyId);
		Map<String, Object> setting = settingReader.getRaw(companyId);
		Map<String, Object> gatewayResult =
				invokeGateway(
						() ->
								gatewayClient.queryPayment(
										stringVal(setting.get("X-AccessCode")),
										stringVal(setting.get("X-SecretKey")),
										transactionId),
						"请检查斗门国际支付配置");
		Map<String, Object> mapped =
				DoumenIntlStatusMapper.mapPaymentQueryStatus(stringVal(gatewayResult.get("status")));
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("status", mapped.get("status"));
		result.put("transaction_id", transactionId);
		result.put("pay_type", "doumen_intl");
		if (mapped.containsKey("msg")) {
			result.put("msg", mapped.get("msg"));
		}
		return result;
	}

	public Map<String, Object> doRefund(Map<String, Object> data) {
		long companyId = longVal(data.get("company_id"));
		assertMerchantPayReady(companyId);
		Map<String, Object> setting = settingReader.getRaw(companyId);
		String merchantOrderId = stringVal(data.get("refund_bn"));
		if (!StringUtils.hasText(merchantOrderId)) {
			merchantOrderId = stringVal(data.get("trade_id"));
		}
		Map<String, Object> refundBody = new LinkedHashMap<>();
		refundBody.put("requestId", md5Hex(UUID.randomUUID() + "-" + merchantOrderId));
		refundBody.put("appId", setting.get("appId"));
		refundBody.put("merchantOrderId", merchantOrderId);
		refundBody.put("refundTime", LocalDateTime.now().format(REFUND_TIME));
		refundBody.put("amount", intVal(data.get("refund_fee"), 0));
		refundBody.put("currency", stringVal(data.get("fee_type")));
		refundBody.put("refundReason", "Refund");
		refundBody.put("merchantMemo", stringVal(data.get("order_id")));
		refundBody.put("notificationUrl", properties.getNotifyUrl());

		Map<String, Object> result =
				invokeGateway(
						() ->
								gatewayClient.refundWithResult(
										stringVal(setting.get("X-AccessCode")),
										stringVal(setting.get("X-SecretKey")),
										stringVal(data.get("transaction_id")),
										refundBody),
						"请检查斗门国际支付配置");
		if (!Boolean.TRUE.equals(result.get("ok"))) {
			Map<String, Object> fail = new LinkedHashMap<>();
			fail.put("status", "FAIL");
			fail.put("error_code", result.get("code"));
			fail.put("error_desc", result.get("message"));
			return fail;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> dataMap =
				result.get("data") instanceof Map<?, ?> m
						? (Map<String, Object>) m
						: Map.of();
		Map<String, Object> ok = new LinkedHashMap<>();
		ok.put("status", "SUCCESS");
		ok.put("refund_id", stringVal(dataMap.get("id")));
		return ok;
	}

	public static List<Map<String, Object>> mapOrderItemsToProducts(
			List<Map<String, Object>> orderItems) {
		List<Map<String, Object>> products = new ArrayList<>();
		if (orderItems == null) {
			return products;
		}
		for (Map<String, Object> item : orderItems) {
			products.add(mapOneItem(item));
		}
		return products;
	}

	private static Map<String, Object> mapOneItem(Map<String, Object> item) {
		String itemBn = stringVal(item.get("item_bn"));
		String itemName = stringVal(item.get("item_name"));
		String itemSpecDesc = stringVal(item.get("item_spec_desc"));
		if (!StringUtils.hasText(itemSpecDesc)) {
			itemSpecDesc = itemName;
		}
		Map<String, Object> p = new LinkedHashMap<>();
		p.put("type", "");
		p.put("url", "");
		p.put("code", itemBn);
		p.put("sku", itemBn);
		p.put("name", itemName);
		p.put("desc", itemSpecDesc);
		p.put("quantity", intVal(item.get("num"), 0));
		p.put("unitPrice", intVal(item.get("price"), 0));
		p.put("totalAmount", intVal(item.get("total_fee"), 0));
		return p;
	}

	private void assertMerchantPayReady(long companyId) {
		Map<String, Object> setting = settingReader.getRaw(companyId);
		if (setting.isEmpty() || !PaymentConfigJsonSupport.normalizeIsOpen(setting.get("is_open"))) {
			throw new BadRequestException("请检查斗门国际支付配置");
		}
		if (!settingReader.isConfigured(companyId)) {
			throw new BadRequestException("请检查斗门国际支付配置");
		}
		assertGatewayEnvReady();
	}

	private void assertGatewayEnvReady() {
		if (!StringUtils.hasText(properties.getBaseUrl())) {
			throw new BadRequestException("斗门国际支付网关地址未配置");
		}
	}

	private Map<String, Object> invokeGateway(
			java.util.function.Supplier<Map<String, Object>> action, String businessFailureMessage) {
		try {
			return action.get();
		} catch (BadRequestException e) {
			throw e;
		} catch (RuntimeException e) {
			log.warn("Doumen Intl gateway invoke failed: {}", e.getMessage());
			throw mapGatewayFailure(e, businessFailureMessage);
		}
	}

	private static BadRequestException mapGatewayFailure(RuntimeException e, String businessFailureMessage) {
		String msg = e.getMessage() == null ? "" : e.getMessage();
		if (msg.contains("URI is not absolute") || msg.contains("Target host is not specified")) {
			return new BadRequestException("斗门国际支付网关地址未配置");
		}
		if (hasRestClientCause(e)) {
			return new BadRequestException("斗门国际支付网关连接失败，请检查支付配置");
		}
		if (msg.contains("gateway authentication failed")) {
			return new BadRequestException("斗门国际支付网关鉴权失败，请检查 AccessCode/SecretKey");
		}
		if (msg.contains("gateway business error")) {
			return new BadRequestException(businessFailureMessage);
		}
		return new BadRequestException("请检查斗门国际支付配置");
	}

	private static boolean hasRestClientCause(Throwable e) {
		for (Throwable cur = e; cur != null; cur = cur.getCause()) {
			if (cur instanceof RestClientException) {
				return true;
			}
		}
		return false;
	}

	private static void assertNormalTradeSource(Map<String, Object> data) {
		String tradeSourceType = stringVal(data.get("trade_source_type"));
		if (!StringUtils.hasText(tradeSourceType)) {
			tradeSourceType = "normal";
		}
		if ("normal".equals(tradeSourceType) || tradeSourceType.startsWith("normal_")) {
			return;
		}
		throw new BadRequestException("斗门国际支付不支持该交易场景");
	}

	private static String md5Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static int intVal(Object o, int dft) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		if (o == null) {
			return dft;
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return dft;
		}
	}

	private static long longVal(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o == null) {
			return 0L;
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
