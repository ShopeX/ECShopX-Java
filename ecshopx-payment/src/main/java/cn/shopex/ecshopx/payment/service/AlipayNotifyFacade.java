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
import cn.shopex.ecshopx.payment.integration.alipay.AlipayNotifyDepositSidePort;
import cn.shopex.ecshopx.payment.integration.alipay.AlipayNotifyOrderSidePort;
import cn.shopex.ecshopx.payment.service.dto.AlipayNotifySigningMaterial;
import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AlipayNotifyFacade {

	private static final Logger log = LoggerFactory.getLogger(AlipayNotifyFacade.class);

	private final AlipayNotifyPaymentContextLoader loader;
	private final AlipayPaymentConfigValidationService alipayPaymentConfigValidationService;
	private final AlipayAsyncNotifyVerificationService alipayAsyncNotifyVerificationService;
	private final AlipayNotifyOrderSidePort alipayNotifyOrderSidePort;
	private final AlipayNotifyDepositSidePort alipayNotifyDepositSidePort;
	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public AlipayNotifyFacade(
			AlipayNotifyPaymentContextLoader loader,
			AlipayPaymentConfigValidationService alipayPaymentConfigValidationService,
			AlipayAsyncNotifyVerificationService alipayAsyncNotifyVerificationService,
			AlipayNotifyOrderSidePort alipayNotifyOrderSidePort,
			AlipayNotifyDepositSidePort alipayNotifyDepositSidePort,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.loader = loader;
		this.alipayPaymentConfigValidationService = alipayPaymentConfigValidationService;
		this.alipayAsyncNotifyVerificationService = alipayAsyncNotifyVerificationService;
		this.alipayNotifyOrderSidePort = alipayNotifyOrderSidePort;
		this.alipayNotifyDepositSidePort = alipayNotifyDepositSidePort;
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public ResponseEntity<String> handle(HttpServletRequest request) {
		Optional<AlipayNotifyPaymentContext> opt = loader.load(request);
		if (opt.isEmpty()) {
			return ResponseEntity.ok()
					.contentType(MediaType.APPLICATION_JSON)
					.body("{\"data\":[]}");
		}
		AlipayNotifyPaymentContext ctx = opt.get();

		alipayPaymentConfigValidationService.assertAsyncNotifyConfigComplete(
				ctx.getCompanyId(), ctx.getDistributorIdForSetting());
		AlipayNotifySigningMaterial signingMaterial = loadSigningMaterial(ctx);

		try {
			alipayAsyncNotifyVerificationService.verifySignedNotify(ctx.getEncodedParams(), signingMaterial);

			Map<String, String> encodedParams = ctx.getEncodedParams();
			String rawTradeStatus;
			if (!encodedParams.containsKey("trade_status")) {
				rawTradeStatus = "";
			} else if (encodedParams.get("trade_status") == null) {
				rawTradeStatus = "";
			} else {
				rawTradeStatus = encodedParams.get("trade_status");
			}
			String normalizedStatus;
			if ("TRADE_SUCCESS".equals(rawTradeStatus) || "TRADE_FINISHED".equals(rawTradeStatus)) {
				normalizedStatus = "SUCCESS";
			} else {
				normalizedStatus = rawTradeStatus;
			}

			boolean depositRecharge = ctx.getReturnData().containsKey("attach")
					&& "depositRecharge".equals(ctx.getReturnData().get("attach"));

			Map<String, Object> options = new LinkedHashMap<>();
			Map<String, String> passback = ctx.getReturnData();
			String payTypeFromPassback = passback.get("pay_type");
			options.put("pay_type", payTypeFromPassback != null ? payTypeFromPassback : "");
			String transactionId = encodedParams.get("trade_no");
			options.put("transaction_id", transactionId != null ? transactionId : "");

			if (depositRecharge) {
				String depositId = encodedParams.get("out_trade_no");
				log.debug("alipay notify depositRecharge out_trade_no={}", depositId);
				alipayNotifyDepositSidePort.rechargeCallback(depositId, normalizedStatus, options);
			} else {
				String orderTradeId = encodedParams.get("out_trade_no");
				alipayNotifyOrderSidePort.applyTradePaymentAfterAlipay(orderTradeId, normalizedStatus, options);
			}
			return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("success");
		} catch (Exception e) {
			log.error(
					"alipay notify inner failure out_trade_no={}",
					ctx.getEncodedParams() != null ? ctx.getEncodedParams().get("out_trade_no") : null,
					e);
			return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).build();
		}
	}

	private AlipayNotifySigningMaterial loadSigningMaterial(AlipayNotifyPaymentContext ctx) {
		String raw = companysRedisTemplate
				.opsForValue()
				.get(PaymentSettingRedisKeys.alipayRedisKey(
						ctx.getCompanyId(), ctx.getDistributorIdForSetting()));
		Map<String, Object> cfg = PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
		String appId = str(cfg.get("app_id"));
		String aliPk = str(cfg.get("ali_public_key"));
		String priv = str(cfg.get("private_key"));
		if (!StringUtils.hasText(appId)) {
			throw new BadRequestException("支付宝信息未配置，请联系商家");
		}
		return new AlipayNotifySigningMaterial(appId, aliPk, priv);
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}
}
