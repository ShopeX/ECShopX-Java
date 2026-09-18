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

package cn.shopex.ecshopx.kaquan.service.vipgrade.pay;

import cn.shopex.ecshopx.ali.service.h5.AlipayMiniEasySdkFactory;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.Config;
import com.alipay.easysdk.payment.common.models.AlipayTradeCreateResponse;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class VipGradeMembercardAlipayMiniPaymentService {

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final VipGradeMembercardTradeSupportService tradeSupportService;
	private final AlipayMiniEasySdkFactory alipayMiniEasySdkFactory;
	private final TradeMapper tradeMapper;

	public VipGradeMembercardAlipayMiniPaymentService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			VipGradeMembercardTradeSupportService tradeSupportService,
			AlipayMiniEasySdkFactory alipayMiniEasySdkFactory,
			TradeMapper tradeMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.tradeSupportService = tradeSupportService;
		this.alipayMiniEasySdkFactory = alipayMiniEasySdkFactory;
		this.tradeMapper = tradeMapper;
	}

	@SuppressWarnings("unused")
	public Map<String, Object> pay(
			String authorizerAppId,
			String wxaAppId,
			Map<String, Object> data,
			boolean secondArgFalse) {
		long companyId = longFrom(data.get("company_id"));
		long distributorId = longFrom(data.get("distributor_id"));
		String orderId = stringVal(data.get("order_id"));

		Trade cached = tradeSupportService.findExistingWithPaymentParams(companyId, orderId, "alipaymini", "");
		if (cached != null) {
			return mergeTradeInfo(parsePaymentParams(cached.getPaymentParams()), orderId, cached.getTradeId());
		}

		Map<String, Object> cfg = loadAlipaySettingOrThrow(companyId, distributorId);

		Trade open = tradeSupportService.findOpenMembercardTrade(companyId, orderId, "alipaymini", "");
		Trade trade = open != null ? open : tradeSupportService.createMembercardTrade(data, "alipaymini", "", null);

		if (trade.getPayFee() != null && trade.getPayFee() == 0) {
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("pay_status", true);
			out.put("trade_info", baseTradeInfo(orderId, trade.getTradeId()));
			return out;
		}

		String alipayUserId = stringVal(data.get("alipay_user_id"));
		if (!StringUtils.hasText(alipayUserId)) {
			throw new BadRequestException("请在支付宝小程序授权登录");
		}

		Config config;
		try {
			config = alipayMiniEasySdkFactory.buildConfig(cfg);
		} catch (ResourceException e) {
			throw new BadRequestException("不支持支付服务，请联系商家");
		}

		String subject = stringVal(data.get("body"));
		if (!StringUtils.hasText(subject)) {
			subject = stringVal(data.get("detail"));
		}
		if (!StringUtils.hasText(subject)) {
			subject = "会员订单";
		}
		int payFeeFen = trade.getPayFee() != null ? trade.getPayFee() : 0;
		String totalAmount =
				BigDecimal.valueOf(payFeeFen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toPlainString();

		String passbackRaw = "company_id=" + companyId + "&pay_type=alipaymini";
		String passbackParams = URLEncoder.encode(passbackRaw, StandardCharsets.UTF_8);

		AlipayTradeCreateResponse resp;
		try {
			Factory.setOptions(config);
			resp = Factory.Payment.Common()
					.batchOptional(Map.of("passback_params", passbackParams))
					.create(subject, trade.getTradeId(), totalAmount, alipayUserId);
		} catch (Exception e) {
			throw new BadRequestException("支付失败");
		}

		if (resp == null || !"10000".equals(resp.getCode())) {
			String msg = resp != null && StringUtils.hasText(resp.getSubMsg()) ? resp.getSubMsg() : "支付失败";
			throw new BadRequestException(msg);
		}
		String tradeNo = resp.getTradeNo();
		if (!StringUtils.hasText(tradeNo)) {
			throw new BadRequestException("支付失败");
		}

		Map<String, Object> paymentParams = new LinkedHashMap<>();
		paymentParams.put("trade_no", tradeNo);
		String paymentJson;
		try {
			paymentJson = objectMapper.writeValueAsString(paymentParams);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("支付失败");
		}

		tradeMapper.update(
				null,
				Wrappers.<Trade>lambdaUpdate()
						.eq(Trade::getTradeId, trade.getTradeId())
						.set(Trade::getTransactionId, tradeNo)
						.set(Trade::getPaymentParams, paymentJson));

		return mergeTradeInfo(paymentParams, orderId, trade.getTradeId());
	}

	private Map<String, Object> loadAlipaySettingOrThrow(long companyId, long distributorId) {
		String key = PaymentSettingRedisKeys.alipayRedisKey(companyId, distributorId);
		String raw = companysRedisTemplate.opsForValue().get(key);
		Map<String, Object> cfg = PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
		if (cfg.isEmpty()) {
			throw new BadRequestException("不支持支付服务，请联系商家");
		}
		Object open = cfg.get("is_open");
		boolean isOpen = Boolean.TRUE.equals(open) || "true".equalsIgnoreCase(String.valueOf(open).trim());
		if (!isOpen) {
			throw new BadRequestException("不支持支付服务，请联系商家");
		}
		return cfg;
	}

	private Map<String, Object> parsePaymentParams(String json) {
		if (json == null || json.isBlank()) {
			return new LinkedHashMap<>();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			return new LinkedHashMap<>();
		}
	}

	private Map<String, Object> mergeTradeInfo(Map<String, Object> payment, String orderId, String tradeId) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(payment);
		out.put("trade_info", baseTradeInfo(orderId, tradeId));
		return out;
	}

	private static Map<String, Object> baseTradeInfo(String orderId, String tradeId) {
		Map<String, Object> tradeInfo = new LinkedHashMap<>();
		tradeInfo.put("order_id", orderId);
		tradeInfo.put("trade_id", tradeId);
		tradeInfo.put("trade_source_type", "membercard");
		return tradeInfo;
	}

	private static long longFrom(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String stringVal(Object o) {
		return o == null ? "" : o.toString().trim();
	}
}
