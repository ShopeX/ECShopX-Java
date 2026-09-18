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
import cn.shopex.ecshopx.payment.integration.alipay.AlipayNotifyOrderSidePort;
import cn.shopex.ecshopx.payment.integration.wxpay.WxpayNotifyCompanyIdPort;
import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

@Service
public class WxpayNotifyPaymentContextLoader {

	private static final Logger log = LoggerFactory.getLogger(WxpayNotifyPaymentContextLoader.class);

	private final WxpayNotifyCompanyIdPort wxpayNotifyCompanyIdPort;
	private final AlipayNotifyOrderSidePort alipayNotifyOrderSidePort;
	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public WxpayNotifyPaymentContextLoader(
			WxpayNotifyCompanyIdPort wxpayNotifyCompanyIdPort,
			AlipayNotifyOrderSidePort alipayNotifyOrderSidePort,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.wxpayNotifyCompanyIdPort = wxpayNotifyCompanyIdPort;
		this.alipayNotifyOrderSidePort = alipayNotifyOrderSidePort;
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public Optional<WxpayNotifyPaymentContext> load(HttpServletRequest request) {
		String body;
		try {
			body = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			log.warn("wxpay notify read body failed: {}", e.getMessage());
			return Optional.empty();
		}
		Optional<Map<String, String>> parsed = WxpayNotifyXmlSupport.tryParseNotifyXml(body);
		if (parsed.isEmpty()) {
			return Optional.empty();
		}
		Map<String, String> notifyParams = parsed.get();
		log.info(
				"wxpay:response: out_trade_no={} result_code={} appid={}",
				notifyParams.get("out_trade_no"),
				notifyParams.get("result_code"),
				notifyParams.get("appid"));

		String appId = str(notifyParams.get("appid"));
		Long companyIdFromAuth = wxpayNotifyCompanyIdPort.resolveCompanyIdByAuthorizerAppId(appId);
		long companyId;
		if (companyIdFromAuth != null && companyIdFromAuth > 0L) {
			companyId = companyIdFromAuth;
		} else {
			companyId = resolveCompanyIdFromRedis(appId, str(notifyParams.get("trade_type")));
		}
		if (companyId <= 0L) {
			throw new BadRequestException("微信支付信息未配置，请联系商家");
		}

		String outTradeNo = str(notifyParams.get("out_trade_no"));
		long distributorIdForSetting =
				alipayNotifyOrderSidePort.resolveDistributorIdForAlipayRedis(companyId, outTradeNo);

		String raw = companysRedisTemplate
				.opsForValue()
				.get(PaymentSettingRedisKeys.wxpayRedisKey(companyId, distributorIdForSetting));
		Map<String, Object> cfg = PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
		String apiKey = str(cfg.get("key"));
		if (!StringUtils.hasText(apiKey)) {
			throw new BadRequestException("微信支付信息未配置，请联系商家");
		}

		Map<String, String> returnData = WxpayNotifyAttachParser.parseAttach(str(notifyParams.get("attach")));
		return Optional.of(
				new WxpayNotifyPaymentContext(
						notifyParams, returnData, companyId, distributorIdForSetting, apiKey));
	}

	private long resolveCompanyIdFromRedis(String appId, String tradeType) {
		if (!StringUtils.hasText(appId)) {
			return 0L;
		}
		String companyRaw;
		if ("APP".equals(tradeType)) {
			companyRaw = companysRedisTemplate
					.opsForValue()
					.get(PaymentSettingRedisKeys.wechatAppPaymentCompanyByAppIdKey(appId));
		} else {
			companyRaw = companysRedisTemplate
					.opsForValue()
					.get(PaymentSettingRedisKeys.wechatPaymentCompanyByAppIdKey(appId));
		}
		if (!StringUtils.hasText(companyRaw)) {
			companyRaw = companysRedisTemplate
					.opsForValue()
					.get(PaymentSettingRedisKeys.wechatServicerPaymentCompanyByAppIdKey(appId));
		}
		if (!StringUtils.hasText(companyRaw)) {
			return 0L;
		}
		try {
			return Long.parseLong(companyRaw.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}
}
