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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.payment.service.membercard.MembercardAdapayPaymentSdkService;
import cn.shopex.ecshopx.payment.service.membercard.MembercardAdapayPaymentSdkService.AdapayMembercardOutcome;
import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class VipGradeMembercardAdapayPaymentService {

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final VipGradeMembercardTradeSupportService tradeSupportService;
	private final MembercardAdapayPaymentSdkService adapaySdkService;
	private final TradeMapper tradeMapper;

	public VipGradeMembercardAdapayPaymentService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			VipGradeMembercardTradeSupportService tradeSupportService,
			MembercardAdapayPaymentSdkService adapaySdkService,
			TradeMapper tradeMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.tradeSupportService = tradeSupportService;
		this.adapaySdkService = adapaySdkService;
		this.tradeMapper = tradeMapper;
	}

	@SuppressWarnings("unused")
	public Map<String, Object> pay(
			String authorizerAppId,
			String wxaAppId,
			Map<String, Object> data,
			boolean secondArgFalse) {
		long companyId = longFrom(data.get("company_id"));
		String orderId = stringVal(data.get("order_id"));
		String payChannel = stringVal(data.get("pay_channel"));

		Trade cached = tradeSupportService.findExistingWithPaymentParams(companyId, orderId, "adapay", payChannel);
		if (cached != null) {
			return mergeTradeInfo(parsePaymentParams(cached.getPaymentParams()), orderId, cached.getTradeId());
		}

		Map<String, Object> cfg = loadAdapaySettingOrThrow(companyId);

		Trade open = tradeSupportService.findOpenMembercardTrade(companyId, orderId, "adapay", payChannel);
		Trade trade = open != null ? open : tradeSupportService.createMembercardTrade(data, "adapay", payChannel, null);

		if (trade.getPayFee() != null && trade.getPayFee() == 0) {
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("pay_status", true);
			out.put("trade_info", baseTradeInfo(orderId, trade.getTradeId()));
			return out;
		}

		int payFeeFen = trade.getPayFee() != null ? trade.getPayFee() : 0;
		String goodsTitle = stringVal(data.get("detail"));
		if (!StringUtils.hasText(goodsTitle)) {
			goodsTitle = stringVal(data.get("body"));
		}
		String goodsDesc = stringVal(data.get("detail"));
		if (!StringUtils.hasText(goodsDesc)) {
			goodsDesc = goodsTitle;
		}
		String tradeSource = stringVal(data.get("trade_source_type"));
		if (!StringUtils.hasText(tradeSource)) {
			tradeSource = "membercard";
		}

		AdapayMembercardOutcome outcome =
				adapaySdkService.createPayment(
						companyId,
						cfg,
						trade.getTradeId(),
						payChannel,
						payFeeFen,
						stringVal(data.get("open_id")),
						goodsTitle,
						goodsDesc,
						tradeSource);

		String paymentJson;
		try {
			paymentJson = objectMapper.writeValueAsString(outcome.clientPaymentParams());
		} catch (JsonProcessingException e) {
			throw new BadRequestException("支付失败");
		}

		tradeMapper.update(
				null,
				Wrappers.<Trade>lambdaUpdate()
						.eq(Trade::getTradeId, trade.getTradeId())
						.set(Trade::getInitalRequest, outcome.initialRequestJson())
						.set(Trade::getTransactionId, outcome.transactionId())
						.set(Trade::getAdapayDivStatus, "NOTDIV")
						.set(Trade::getPaymentParams, paymentJson));

		return mergeTradeInfo(outcome.clientPaymentParams(), orderId, trade.getTradeId());
	}

	private Map<String, Object> loadAdapaySettingOrThrow(long companyId) {
		String key = "adaPaySetting:" + sha1Hex(String.valueOf(companyId));
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

	private static String sha1Hex(String companyId) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(companyId.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-1 not available", e);
		}
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
