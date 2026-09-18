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

package cn.shopex.ecshopx.deposit.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class DepositWxpayRechargeUnifyService {

	private static final String UNIFIED_ORDER_URL = "https://api.mch.weixin.qq.com/pay/unifiedorder";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;
	private final String wechatNotifyUrl;

	public DepositWxpayRechargeUnifyService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			@Qualifier("depositRestTemplate") RestTemplate restTemplate,
			@Value("${ecshopx.payment.wechat.notify-url}") String wechatNotifyUrl) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.restTemplate = restTemplate;
		this.wechatNotifyUrl = wechatNotifyUrl;
	}

	public Map<String, Object> unifyAndBuildClientPayParams(
			long companyId,
			long distributorIdForSetting,
			String depositTradeId,
			long totalFeeFenAfterExchange,
			String openId,
			String wxaAppId,
			String woaAppId,
			String shopName,
			String detail,
			String spbillCreateIp) {
		return unifyAndBuildClientPayParams(
				companyId,
				distributorIdForSetting,
				depositTradeId,
				totalFeeFenAfterExchange,
				openId,
				wxaAppId,
				woaAppId,
				shopName,
				detail,
				spbillCreateIp,
				"wxpay");
	}

	public Map<String, Object> unifyAndBuildClientPayParams(
			long companyId,
			long distributorIdForSetting,
			String depositTradeId,
			long totalFeeFenAfterExchange,
			String openId,
			String wxaAppId,
			String woaAppId,
			String shopName,
			String detail,
			String spbillCreateIp,
			String payType) {
		Map<String, Object> cfg = loadAndValidateWxpayConfig(companyId, distributorIdForSetting);
		boolean servicer = isServicer(cfg);
		String apiKey = stringVal(cfg.get("key"));
		String merchantId = stringVal(cfg.get("merchant_id"));
		String cfgAppId = stringVal(cfg.get("app_id"));
		String payTypeLc = normalizePayType(payType);
		String tradeType = resolveTradeType(payTypeLc);
		boolean h5OrJsPay = "wxpayh5".equals(payTypeLc) || "wxpayjs".equals(payTypeLc);
		String passbackInner =
				"company_id="
						+ urlEncodeUtf8(String.valueOf(companyId))
						+ "&pay_type="
						+ urlEncodeUtf8(payTypeLc)
						+ "&attach=depositRecharge";
		String attachOuter = urlEncodeUtf8(passbackInner);
		String bodyDesc = (shopName == null ? "" : shopName) + "充值";
		String nonceStr = randomNonce();
		String ip = spbillCreateIp == null || spbillCreateIp.isEmpty() ? "127.0.0.1" : spbillCreateIp;
		String openIdVal = openId == null ? "" : openId.trim();
		String wxaVal = wxaAppId == null ? "" : wxaAppId.trim();
		String woaVal = woaAppId == null ? "" : woaAppId.trim();
		String miniAppIdForUnified = h5OrJsPay && StringUtils.hasText(cfgAppId) ? cfgAppId : wxaVal;

		TreeMap<String, String> unify = new TreeMap<>();
		if (servicer) {
			String servicerAppId = stringVal(cfg.get("servicer_app_id"));
			String servicerMchId = stringVal(cfg.get("servicer_merchant_id"));
			if (!StringUtils.hasText(servicerAppId) || !StringUtils.hasText(servicerMchId)) {
				throw new BadRequestException("不支持支付服务，请联系商家");
			}
			unify.put("appid", servicerAppId);
			unify.put("mch_id", servicerMchId);
			putIfHasText(unify, "sub_appid", miniAppIdForUnified);
			unify.put("sub_mch_id", merchantId);
			if ("JSAPI".equals(tradeType)) {
				putIfHasText(unify, "sub_openid", openIdVal);
			}
		} else {
			String appIdForUnified = resolveAppIdForUnified(tradeType, h5OrJsPay, wxaVal, woaVal, cfg);
			if (!StringUtils.hasText(appIdForUnified)) {
				throw new BadRequestException("不支持支付服务，请联系商家");
			}
			unify.put("appid", appIdForUnified);
			unify.put("mch_id", merchantId);
			if ("JSAPI".equals(tradeType)) {
				putIfHasText(unify, "openid", openIdVal);
			}
		}
		unify.put("nonce_str", nonceStr);
		unify.put("body", bodyDesc);
		unify.put("out_trade_no", depositTradeId);
		unify.put("total_fee", String.valueOf(totalFeeFenAfterExchange));
		unify.put("spbill_create_ip", ip);
		unify.put("notify_url", wechatNotifyUrl);
		unify.put("trade_type", tradeType);
		unify.put("attach", attachOuter);
		if (detail != null && !detail.isEmpty()) {
			unify.put("detail", detail);
		}
		unify.put("sign", signParams(unify, apiKey));

		String xml = buildUnifiedOrderXml(unify);
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

		Map<String, Object> tradeInfo = new LinkedHashMap<>();
		tradeInfo.put("order_id", depositTradeId);
		tradeInfo.put("trade_id", depositTradeId);

		if ("MWEB".equals(tradeType)) {
			String mwebUrl = xmlText(respXml, "mweb_url");
			if (mwebUrl == null || mwebUrl.isEmpty()) {
				throw new BadRequestException("支付失败");
			}
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("mweb_url", mwebUrl);
			out.put("trade_info", tradeInfo);
			return out;
		}

		String prepayId = xmlText(respXml, "prepay_id");
		if (prepayId == null || prepayId.isEmpty()) {
			throw new BadRequestException("支付失败");
		}

		String timeStamp = String.valueOf(System.currentTimeMillis() / 1000L);
		String pkg = "prepay_id=" + prepayId;
		String payNonce = randomNonce();
		String clientAppId =
				servicer ? stringVal(unify.get("sub_appid")) : stringVal(unify.get("appid"));
		TreeMap<String, String> paySignMap = new TreeMap<>();
		paySignMap.put("appId", clientAppId);
		paySignMap.put("timeStamp", timeStamp);
		paySignMap.put("nonceStr", payNonce);
		paySignMap.put("package", pkg);
		paySignMap.put("signType", "MD5");
		String paySign = signParams(paySignMap, apiKey);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("appId", clientAppId);
		out.put("timeStamp", timeStamp);
		out.put("nonceStr", payNonce);
		out.put("package", pkg);
		out.put("signType", "MD5");
		out.put("paySign", paySign);
		out.put("trade_info", tradeInfo);
		return out;
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

	private static String normalizePayType(String payType) {
		if (payType == null) {
			return "wxpay";
		}
		String t = payType.trim().toLowerCase(Locale.ROOT);
		return t.isEmpty() ? "wxpay" : t;
	}

	private static String resolveTradeType(String payTypeLc) {
		return switch (payTypeLc) {
			case "wxpayh5" -> "MWEB";
			case "wxpayapp" -> "APP";
			default -> "JSAPI";
		};
	}

	private static String resolveAppIdForUnified(
			String tradeType,
			boolean h5OrJsPay,
			String wxaAppId,
			String woaAppId,
			Map<String, Object> cfg) {
		String cfgAppId = stringVal(cfg.get("app_id"));
		if ("APP".equals(tradeType)) {
			return firstNonEmpty(woaAppId, stringVal(cfg.get("app_app_id")), cfgAppId);
		}
		if (h5OrJsPay && StringUtils.hasText(cfgAppId)) {
			return cfgAppId;
		}
		return firstNonEmpty(wxaAppId, woaAppId, cfgAppId);
	}

	private static String firstNonEmpty(String... values) {
		if (values == null) {
			return "";
		}
		for (String v : values) {
			if (StringUtils.hasText(v)) {
				return v.trim();
			}
		}
		return "";
	}

	private static void putIfHasText(TreeMap<String, String> map, String key, String value) {
		if (StringUtils.hasText(value)) {
			map.put(key, value);
		}
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
			if (e.getValue() == null || e.getValue().isEmpty()) {
				continue;
			}
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
		return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
	}

	private static String stringVal(Object o) {
		return o == null ? "" : o.toString();
	}
}
