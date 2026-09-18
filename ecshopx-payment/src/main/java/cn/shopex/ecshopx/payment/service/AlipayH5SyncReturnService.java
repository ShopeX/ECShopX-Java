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
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.payment.integration.alipay.AlipayNotifyDepositSidePort;
import cn.shopex.ecshopx.payment.integration.alipay.AlipayNotifyOrderSidePort;
import cn.shopex.ecshopx.payment.service.dto.AlipayNotifySigningMaterial;
import cn.shopex.ecshopx.payment.service.dto.AlipayTradeQueryParsedResponse;
import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AlipayH5SyncReturnService {

	private static final Logger log = LoggerFactory.getLogger(AlipayH5SyncReturnService.class);

	private final AlipayPaymentConfigValidationService alipayPaymentConfigValidationService;
	private final AlipayAsyncNotifyVerificationService alipayAsyncNotifyVerificationService;
	private final AlipayOpenapiTradeQueryService alipayOpenapiTradeQueryService;
	private final AlipayNotifyOrderSidePort alipayNotifyOrderSidePort;
	private final AlipayNotifyDepositSidePort alipayNotifyDepositSidePort;
	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public AlipayH5SyncReturnService(
			AlipayPaymentConfigValidationService alipayPaymentConfigValidationService,
			AlipayAsyncNotifyVerificationService alipayAsyncNotifyVerificationService,
			AlipayOpenapiTradeQueryService alipayOpenapiTradeQueryService,
			AlipayNotifyOrderSidePort alipayNotifyOrderSidePort,
			AlipayNotifyDepositSidePort alipayNotifyDepositSidePort,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.alipayPaymentConfigValidationService = alipayPaymentConfigValidationService;
		this.alipayAsyncNotifyVerificationService = alipayAsyncNotifyVerificationService;
		this.alipayOpenapiTradeQueryService = alipayOpenapiTradeQueryService;
		this.alipayNotifyOrderSidePort = alipayNotifyOrderSidePort;
		this.alipayNotifyDepositSidePort = alipayNotifyDepositSidePort;
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	/**
	 * @apiNote On {@code SUCCESS} for non-deposit trades, only {@link AlipayNotifyOrderSidePort} is invoked; dispatch
	 *     bus publish for Jushuitan stays in the orders-layer callback service shared with async notify. A successful
	 *     non-recharge sync return on this path eventually leads to trade-finish dispatch bus publish, which fans out
	 *     to listeners including the WXA template path that may dispatch job:114 under wxpay or deposit pay types and
	 *     distributor gates.
	 */
	public String alipayResult(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (raw instanceof Number) {
			companyId = ((Number) raw).longValue();
		} else {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		if (companyId <= 0) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}

		Map<String, Object> objectMap = FlexibleHttpServletParameterMap.toObjectMap(request);
		Map<String, String> rawFlat = NotifyHttpParameterFlatten.flattenObjectMap(objectMap);
		logAlipayResponseSummary(rawFlat);

		String outTradeNo = rawFlat.get("out_trade_no");
		if (!StringUtils.hasText(outTradeNo)) {
			throw new BadRequestException("缺少 out_trade_no");
		}

		String charsetRaw = rawFlat.get("charset");
		String charsetName = StringUtils.hasText(charsetRaw) ? charsetRaw.trim() : "gb2312";
		Charset sourceCharset = resolveSourceCharset(charsetName);
		Map<String, String> encodedFlat = encodeMapValuesToUtf8(rawFlat, sourceCharset);

		long distributorIdForSetting =
				alipayNotifyOrderSidePort.resolveDistributorIdForAlipayRedis(companyId, outTradeNo);

		alipayPaymentConfigValidationService.assertAsyncNotifyConfigComplete(companyId, distributorIdForSetting);
		AlipayNotifySigningMaterial signingMaterial = loadSigningMaterial(companyId, distributorIdForSetting);

		alipayAsyncNotifyVerificationService.verifySignedNotify(encodedFlat, signingMaterial);

		AlipayTradeQueryParsedResponse parsed =
				alipayOpenapiTradeQueryService.queryTrade(companyId, distributorIdForSetting, outTradeNo);

		String rawTs = parsed.tradeStatus() == null ? "" : parsed.tradeStatus();
		String normalizedStatus;
		if ("TRADE_SUCCESS".equals(rawTs) || "TRADE_FINISHED".equals(rawTs)) {
			normalizedStatus = "SUCCESS";
		} else {
			normalizedStatus = rawTs;
		}

		String passbackRaw = parsed.passbackParams();
		Map<String, String> returnData = new LinkedHashMap<>();
		if (StringUtils.hasText(passbackRaw)) {
			String decoded;
			try {
				decoded = URLDecoder.decode(passbackRaw.trim(), StandardCharsets.UTF_8);
			} catch (IllegalArgumentException e) {
				throw new BadRequestException("回传参数无法解码");
			}
			returnData = parseQueryString(decoded);
		}
		boolean depositRecharge =
				returnData.containsKey("attach") && "depositRecharge".equals(returnData.get("attach"));

		Map<String, Object> options = new LinkedHashMap<>();
		options.put("pay_type", "alipay");
		String transactionId = StringUtils.hasText(parsed.tradeNo()) ? parsed.tradeNo() : "";
		options.put("transaction_id", transactionId);

		if (depositRecharge) {
			alipayNotifyDepositSidePort.rechargeCallback(outTradeNo, normalizedStatus, options);
		} else {
			alipayNotifyOrderSidePort.applyTradePaymentAfterAlipay(outTradeNo, normalizedStatus, options);
		}
		return normalizedStatus;
	}

	private AlipayNotifySigningMaterial loadSigningMaterial(long companyId, long distributorIdForSetting) {
		String raw = companysRedisTemplate
				.opsForValue()
				.get(PaymentSettingRedisKeys.alipayRedisKey(companyId, distributorIdForSetting));
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

	private static void logAlipayResponseSummary(Map<String, String> rawFlat) {
		List<String> keys = new ArrayList<>(rawFlat.keySet());
		keys.removeIf(k -> "sign".equalsIgnoreCase(k) || "sign_type".equalsIgnoreCase(k));
		log.info("alipay:response: keys={}", keys);
	}

	private static Charset resolveSourceCharset(String rawName) {
		if (!StringUtils.hasText(rawName)) {
			return Charset.forName("GB2312");
		}
		try {
			return Charset.forName(rawName.trim());
		} catch (Exception e) {
			return Charset.forName("GB2312");
		}
	}

	private static Map<String, String> encodeMapValuesToUtf8(Map<String, String> in, Charset fromCharset) {
		Map<String, String> out = new LinkedHashMap<>();
		for (Map.Entry<String, String> e : in.entrySet()) {
			out.put(e.getKey(), convertParamValueToUtf8(e.getValue(), fromCharset));
		}
		return out;
	}

	private static String convertParamValueToUtf8(String value, Charset fromCharset) {
		if (value == null) {
			return null;
		}
		if (StandardCharsets.UTF_8.equals(fromCharset)
				|| "UTF-8".equalsIgnoreCase(fromCharset.name())) {
			return value;
		}
		byte[] bytes = value.getBytes(StandardCharsets.ISO_8859_1);
		return new String(bytes, fromCharset);
	}

	private static Map<String, String> parseQueryString(String qs) {
		Map<String, String> m = new LinkedHashMap<>();
		if (!StringUtils.hasText(qs)) {
			return m;
		}
		for (String part : qs.split("&")) {
			if (!StringUtils.hasText(part)) {
				continue;
			}
			int eq = part.indexOf('=');
			try {
				if (eq < 0) {
					String k = URLDecoder.decode(part, StandardCharsets.UTF_8);
					m.put(k, "");
				} else {
					String k = URLDecoder.decode(part.substring(0, eq), StandardCharsets.UTF_8);
					String v = URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8);
					m.put(k, v);
				}
			} catch (IllegalArgumentException e) {
				throw new BadRequestException("回传参数格式错误");
			}
		}
		return m;
	}
}
