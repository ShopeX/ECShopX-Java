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

package cn.shopex.ecshopx.hfpay.service.payment;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.port.OrdersMiniProgramHfpayPayPort;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HfpayOrdersMiniProgramPayPortService implements OrdersMiniProgramHfpayPayPort {

	private final HfPayPaymentSettingService hfPayPaymentSettingService;
	private final HfPayAcouJsonPostClient hfPayAcouJsonPostClient;
	private final ObjectMapper objectMapper;
	private final String bgRetUrl;

	public HfpayOrdersMiniProgramPayPortService(
			HfPayPaymentSettingService hfPayPaymentSettingService,
			HfPayAcouJsonPostClient hfPayAcouJsonPostClient,
			ObjectMapper objectMapper,
			@Value("${ecshopx.hfpay.bg-ret-url:}") String bgRetUrl) {
		this.hfPayPaymentSettingService = hfPayPaymentSettingService;
		this.hfPayAcouJsonPostClient = hfPayAcouJsonPostClient;
		this.objectMapper = objectMapper;
		this.bgRetUrl = bgRetUrl;
	}

	@Override
	public void assertConfigReady(long companyId, long distributorIdForSetting) {
		try {
			hfPayPaymentSettingService.loadForCompany(companyId);
		} catch (ResourceException ex) {
			throw new ResourceException("不支持支付服务，请联系商家");
		}
	}

	@Override
	public Map<String, Object> buildClientPayParams(
			long companyId,
			long distributorIdForSetting,
			Map<String, Object> authInfo,
			Map<String, Object> data,
			Trade tradeRow) {
		if (!StringUtils.hasText(bgRetUrl)) {
			throw new ResourceException("不支持支付服务，请联系商家");
		}
		Map<String, Object> setting;
		try {
			setting = hfPayPaymentSettingService.loadForCompany(companyId);
		} catch (ResourceException ex) {
			throw new ResourceException("不支持支付服务，请联系商家");
		}
		String merCustId = String.valueOf(setting.get("mer_cust_id")).trim();
		if (!StringUtils.hasText(merCustId)) {
			throw new ResourceException("不支持支付服务，请联系商家");
		}
		String openId = str(data.get("open_id"));
		String wxaAppId = str(data.get("wxa_appid"));
		if (!StringUtils.hasText(wxaAppId) && authInfo != null) {
			wxaAppId = str(authInfo.get("wxapp_appid"));
		}
		if (!StringUtils.hasText(openId) || !StringUtils.hasText(wxaAppId)) {
			throw new BadRequestException("缺少会员open_id或者小程序appid参数");
		}
		if (tradeRow == null || !StringUtils.hasText(tradeRow.getTradeId())) {
			throw new ResourceException("支付失败");
		}
		int payFeeFen = tradeRow.getPayFee() == null ? 0 : tradeRow.getPayFee();
		String transAmtYuan =
				BigDecimal.valueOf(payFeeFen)
						.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
						.toPlainString();
		String tradeId = tradeRow.getTradeId();
		String body = str(data.get("body"));
		if (!StringUtils.hasText(body)) {
			body = "订单支付";
		}
		String ip = str(data.get("client_ip"));
		if (!StringUtils.hasText(ip)) {
			ip = "127.0.0.1";
		}
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("version", "10");
		payload.put("mer_cust_id", merCustId);
		payload.put("order_id", tradeId);
		payload.put("order_date", LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.BASIC_ISO_DATE));
		payload.put("trans_amt", transAmtYuan);
		payload.put("goods_desc", body);
		payload.put("openid", openId);
		payload.put("app_id", wxaAppId);
		payload.put("bg_ret_url", bgRetUrl);
		payload.put("spbill_create_ip", ip);
		payload.put("attach", "company_id=" + urlEnc(String.valueOf(companyId)) + "&pay_type=hfpay");

		Map<String, Object> decrypted = hfPayAcouJsonPostClient.pay012(setting, payload);
		String respCode = decrypted.get("resp_code") == null ? "" : String.valueOf(decrypted.get("resp_code")).trim();
		if (!"C00000".equals(respCode)) {
			String msg = decrypted.get("resp_desc") == null ? "" : String.valueOf(decrypted.get("resp_desc")).trim();
			throw new ResourceException(StringUtils.hasText(msg) ? msg : "支付失败");
		}

		Map<String, Object> payLayer = new LinkedHashMap<>(decrypted);
		Object payInfoRaw = decrypted.get("pay_info");
		if (payInfoRaw instanceof String s && StringUtils.hasText(s)) {
			try {
				Map<String, Object> parsed =
						objectMapper.readValue(s, new TypeReference<LinkedHashMap<String, Object>>() {});
				if (parsed != null) {
					payLayer.putAll(parsed);
				}
			} catch (Exception ignored) {
				// rely on top-level keys
			}
		}

		Map<String, Object> client = new LinkedHashMap<>();
		putPayField(client, payLayer, "appId", "appId", "app_id");
		putPayField(client, payLayer, "timeStamp", "timeStamp", "time_stamp");
		putPayField(client, payLayer, "nonceStr", "nonceStr", "nonce_str");
		putPackageField(client, payLayer);
		putPayField(client, payLayer, "signType", "signType", "sign_type");
		putPayField(client, payLayer, "paySign", "paySign", "pay_sign");

		if (!StringUtils.hasText(str(client.get("appId")))
				|| !StringUtils.hasText(str(client.get("timeStamp")))
				|| !StringUtils.hasText(str(client.get("nonceStr")))
				|| !StringUtils.hasText(str(client.get("package")))
				|| !StringUtils.hasText(str(client.get("signType")))
				|| !StringUtils.hasText(str(client.get("paySign")))) {
			throw new ResourceException("支付失败");
		}

		String orderId = str(data.get("order_id"));
		String tradeSourceType = str(data.get("trade_source_type"));
		Map<String, Object> tradeInfo = new LinkedHashMap<>();
		tradeInfo.put("order_id", orderId);
		tradeInfo.put("trade_id", tradeId);
		tradeInfo.put("trade_source_type", tradeSourceType);
		client.put("trade_info", tradeInfo);
		return client;
	}

	private static void putPackageField(Map<String, Object> dest, Map<String, Object> src) {
		String pkg = str(firstPresent(src, "package", "Package"));
		if (!StringUtils.hasText(pkg)) {
			String prepay = str(firstPresent(src, "prepay_id", "prepayId"));
			if (StringUtils.hasText(prepay)) {
				pkg = prepay.contains("=") ? prepay : "prepay_id=" + prepay;
			}
		}
		if (StringUtils.hasText(pkg)) {
			dest.put("package", pkg);
		}
	}

	private static void putPayField(Map<String, Object> dest, Map<String, Object> src, String destKey, String... srcKeys) {
		for (String k : srcKeys) {
			Object v = src.get(k);
			if (v != null) {
				String s = String.valueOf(v).trim();
				if (StringUtils.hasText(s)) {
					dest.put(destKey, s);
					return;
				}
			}
		}
	}

	private static Object firstPresent(Map<String, Object> src, String... keys) {
		for (String k : keys) {
			if (src.containsKey(k) && src.get(k) != null) {
				return src.get(k);
			}
		}
		return null;
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}

	private static String urlEnc(String s) {
		return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
	}
}
