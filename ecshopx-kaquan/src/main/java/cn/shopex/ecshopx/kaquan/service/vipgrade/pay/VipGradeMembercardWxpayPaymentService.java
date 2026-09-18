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
import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
public class VipGradeMembercardWxpayPaymentService {

	private static final String UNIFIED_ORDER_URL = "https://api.mch.weixin.qq.com/pay/unifiedorder";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;
	private final String wechatNotifyUrl;
	private final VipGradeMembercardTradeSupportService tradeSupportService;
	private final TradeMapper tradeMapper;

	public VipGradeMembercardWxpayPaymentService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			@Value("${ecshopx.payment.wechat.notify-url:}") String wechatNotifyUrl,
			VipGradeMembercardTradeSupportService tradeSupportService,
			TradeMapper tradeMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.restTemplate = new RestTemplate();
		this.wechatNotifyUrl = wechatNotifyUrl;
		this.tradeSupportService = tradeSupportService;
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

		Map<String, Object> cfg = loadAndValidateWxpayConfig(companyId, distributorId);
		Object isOpen = cfg.get("is_open");
		if (Boolean.FALSE.equals(isOpen) || "false".equalsIgnoreCase(String.valueOf(isOpen))) {
			throw new BadRequestException("不支持支付服务，请联系商家");
		}
		if (!StringUtils.hasText(wechatNotifyUrl)) {
			throw new BadRequestException("不支持支付服务，请联系商家");
		}
		boolean servicer = isServicer(cfg);
		String apiKey = stringVal(cfg.get("key"));
		String merchantId = stringVal(cfg.get("merchant_id"));
		data.put("mch_id", merchantId);

		String openId = stringVal(data.get("open_id"));
		if (!StringUtils.hasText(openId)) {
			throw new BadRequestException("创建交易单失败，请检查参数");
		}

		Trade trade = tradeSupportService.findExistingWxpay(companyId, orderId);
		if (trade == null) {
			trade = tradeSupportService.createMembercardTrade(data, "wxpay", "", merchantId);
		}

		if (trade.getPayFee() != null && trade.getPayFee() == 0) {
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("pay_status", true);
			out.put(
					"trade_info",
					Map.of(
							"order_id", orderId,
							"trade_id", trade.getTradeId(),
							"trade_source_type", "membercard"));
			return out;
		}

		int totalFeeFen = trade.getPayFee() != null ? trade.getPayFee() : 0;
		String clientWxaAppId = StringUtils.hasText(wxaAppId) ? wxaAppId : stringVal(data.get("wxa_appid"));

		String passbackInner =
				"company_id=" + urlEncodeUtf8(String.valueOf(companyId)) + "&pay_type=wxpay";
		String attachOuter = urlEncodeUtf8(passbackInner);
		String bodyDesc = stringVal(data.get("body"));
		if (bodyDesc.isEmpty()) {
			bodyDesc = "会员订单";
		}
		String nonceStr = randomNonce();
		String ip = stringVal(data.get("client_ip"));
		if (ip.isEmpty()) {
			ip = "127.0.0.1";
		}
		String detail = stringVal(data.get("detail"));
		if (detail.isEmpty()) {
			detail = bodyDesc;
		}

		TreeMap<String, String> unify = new TreeMap<>();
		if (servicer) {
			String subMch = merchantId;
			String servicerAppId = stringVal(cfg.get("servicer_app_id"));
			String servicerMchId = stringVal(cfg.get("servicer_merchant_id"));
			if (!StringUtils.hasText(servicerAppId) || !StringUtils.hasText(servicerMchId)) {
				throw new BadRequestException("不支持支付服务，请联系商家");
			}
			unify.put("appid", servicerAppId);
			unify.put("mch_id", servicerMchId);
			unify.put("sub_appid", clientWxaAppId);
			unify.put("sub_mch_id", subMch);
			unify.put("sub_openid", openId);
		} else {
			unify.put("appid", clientWxaAppId);
			unify.put("mch_id", merchantId);
			unify.put("openid", openId);
		}
		unify.put("nonce_str", nonceStr);
		unify.put("body", bodyDesc);
		unify.put("out_trade_no", trade.getTradeId());
		unify.put("total_fee", String.valueOf(totalFeeFen));
		unify.put("spbill_create_ip", ip);
		unify.put("notify_url", wechatNotifyUrl);
		unify.put("trade_type", "JSAPI");
		unify.put("attach", attachOuter);
		unify.put("detail", detail);
		unify.put("time_expire", timeExpireYmdHis(data.get("auto_cancel_time")));
		unify.put("sign", signParams(unify, apiKey));

		String xml = buildUnifiedOrderXml(unify);
		try {
			String reqJson = objectMapper.writeValueAsString(unify);
			tradeMapper.update(
					null,
					Wrappers.<Trade>lambdaUpdate()
							.eq(Trade::getTradeId, trade.getTradeId())
							.set(Trade::getInitalRequest, reqJson));
		} catch (JsonProcessingException ignored) {
			// skip persistence of request snapshot
		}

		ResponseEntity<String> resp =
				restTemplate.postForEntity(UNIFIED_ORDER_URL, xmlEntity(xml), String.class);
		String respXml = resp.getBody();
		if (respXml == null || respXml.isEmpty()) {
			throw new BadRequestException("支付失败");
		}
		String returnCode = xmlText(respXml, "return_code");
		String resultCode = xmlText(respXml, "result_code");
		if (!"SUCCESS".equals(returnCode) || !"SUCCESS".equals(resultCode)) {
			String err = xmlText(respXml, "err_code_des");
			if (err == null || err.isEmpty()) {
				err = xmlText(respXml, "return_msg");
			}
			throw new BadRequestException(err == null || err.isEmpty() ? "支付失败" : err);
		}
		String prepayId = xmlText(respXml, "prepay_id");
		if (prepayId == null || prepayId.isEmpty()) {
			throw new BadRequestException("支付失败");
		}

		String timeStamp = String.valueOf(System.currentTimeMillis() / 1000L);
		String pkg = "prepay_id=" + prepayId;
		String payNonce = randomNonce();
		TreeMap<String, String> paySignMap = new TreeMap<>();
		paySignMap.put("appId", clientWxaAppId);
		paySignMap.put("timeStamp", timeStamp);
		paySignMap.put("nonceStr", payNonce);
		paySignMap.put("package", pkg);
		paySignMap.put("signType", "MD5");
		String paySign = signParams(paySignMap, apiKey);

		Map<String, Object> tradeInfo = new LinkedHashMap<>();
		tradeInfo.put("order_id", orderId);
		tradeInfo.put("trade_id", trade.getTradeId());
		tradeInfo.put("trade_source_type", "membercard");

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("appId", clientWxaAppId);
		out.put("timeStamp", timeStamp);
		out.put("nonceStr", payNonce);
		out.put("package", pkg);
		out.put("signType", "MD5");
		out.put("paySign", paySign);
		out.put("trade_info", tradeInfo);
		return out;
	}

	private static String timeExpireYmdHis(Object autoCancelTime) {
		Long epoch = parseAutoCancelEpochSeconds(autoCancelTime);
		java.time.ZonedDateTime when =
				epoch != null
						? java.time.Instant.ofEpochSecond(epoch).atZone(java.time.ZoneId.systemDefault())
						: java.time.Instant.now().plusSeconds(300).atZone(java.time.ZoneId.systemDefault());
		return java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(when);
	}

	private static Long parseAutoCancelEpochSeconds(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : null;
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s) || "0".equals(s) || "null".equalsIgnoreCase(s)) {
			return null;
		}
		if (!s.chars().allMatch(Character::isDigit)) {
			return null;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private Map<String, Object> loadAndValidateWxpayConfig(long companyId, long distributorIdForSetting) {
		String redisKey = PaymentSettingRedisKeys.wxpayRedisKey(companyId, distributorIdForSetting);
		String raw = companysRedisTemplate.opsForValue().get(redisKey);
		Map<String, Object> cfg = PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
		List<String> fields = List.of("app_id", "merchant_id", "key", "cert", "cert_key");
		for (String field : fields) {
			if (PaymentConfigJsonSupport.isRequiredCredentialMissing(cfg.get(field))) {
				throw new BadRequestException("不支持支付服务，请联系商家");
			}
		}
		return cfg;
	}

	private static boolean isServicer(Map<String, Object> cfg) {
		Object v = cfg.get("is_servicer");
		return v != null && "true".equalsIgnoreCase(v.toString().trim());
	}

	private static String signParams(TreeMap<String, String> sorted, String apiKey) {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> e : sorted.entrySet()) {
			if ("sign".equals(e.getKey())) {
				continue;
			}
			if (e.getValue() == null || e.getValue().isEmpty()) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append('&');
			}
			sb.append(e.getKey()).append('=').append(e.getValue());
		}
		sb.append("&key=").append(apiKey);
		return DigestUtils.md5DigestAsHex(sb.toString().getBytes(StandardCharsets.UTF_8))
				.toUpperCase(Locale.ROOT);
	}

	private static String buildUnifiedOrderXml(TreeMap<String, String> fields) {
		StringBuilder xml = new StringBuilder("<xml>");
		for (Map.Entry<String, String> e : fields.entrySet()) {
			xml.append('<').append(e.getKey()).append('>');
			xml.append("<![CDATA[").append(cdataSafe(e.getValue())).append("]]>");
			xml.append("</").append(e.getKey()).append('>');
		}
		xml.append("</xml>");
		return xml.toString();
	}

	private static String cdataSafe(String v) {
		return v.replace("]]>", "]]]]><![CDATA[>");
	}

	private static HttpEntity<String> xmlEntity(String xml) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(new MediaType("application", "xml", StandardCharsets.UTF_8));
		return new HttpEntity<>(xml, headers);
	}

	private static String xmlText(String xml, String tag) {
		String open = "<" + tag + ">";
		String close = "</" + tag + ">";
		int i = xml.indexOf(open);
		if (i < 0) {
			return "";
		}
		i += open.length();
		int j = xml.indexOf(close, i);
		if (j < 0) {
			return "";
		}
		String inner = xml.substring(i, j);
		if (inner.startsWith("<![CDATA[")) {
			return inner.substring(9, inner.length() - 3);
		}
		return inner;
	}

	private static String randomNonce() {
		String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		SecureRandom r = new SecureRandom();
		StringBuilder b = new StringBuilder(32);
		for (int i = 0; i < 32; i++) {
			b.append(chars.charAt(r.nextInt(chars.length())));
		}
		return b.toString();
	}

	private static String urlEncodeUtf8(String s) {
		return URLEncoder.encode(s, StandardCharsets.UTF_8);
	}

	private static String stringVal(Object o) {
		return o == null ? "" : o.toString().trim();
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

}
