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

package cn.shopex.ecshopx.payment.service.membercard;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huifu.adapay.Adapay;
import com.huifu.adapay.core.exception.BaseAdaPayException;
import com.huifu.adapay.model.MerConfig;
import com.huifu.adapay.model.Payment;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 会员卡场景的 AdaPay（汇付）预下单：根据商户配置与订单信息调用 AdaPay 创建支付，返回客户端调起支付参数及落库所需字段。
 */
@Service
public class MembercardAdapayPaymentSdkService {

	private static final Object ADAPAY_MUTEX = new Object();

	private final ObjectMapper objectMapper;

	@Value("${ecshopx.adapay.notify-url:}")
	private String notifyUrl;

	@Value("${ecshopx.adapay.prod-mode:true}")
	private boolean prodMode;

	public MembercardAdapayPaymentSdkService(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * @param merchantCfg Redis {@code adaPaySetting:sha1(companyId)} JSON
	 * @return 客户端支付参数 Map（将序列化入 trade.payment_params）、AdaPay 支付对象 id、初始请求 JSON
	 */
	public AdapayMembercardOutcome createPayment(
			long companyId,
			Map<String, Object> merchantCfg,
			String tradeId,
			String payChannel,
			int payFeeFen,
			String openId,
			String goodsTitle,
			String goodsDesc,
			String tradeSourceType) {
		if (!StringUtils.hasText(notifyUrl)) {
			throw new BadRequestException("不支持支付服务，请联系商家");
		}
		String appId = str(merchantCfg.get("app_id"));
		String liveKey = str(merchantCfg.get("live_api_key"));
		String testKey = str(merchantCfg.get("test_api_key"));
		String rsaPrivate = str(merchantCfg.get("rsa_private_key"));
		if (!StringUtils.hasText(appId)
				|| !StringUtils.hasText(rsaPrivate)
				|| (!StringUtils.hasText(liveKey) && !StringUtils.hasText(testKey))) {
			throw new BadRequestException("不支持支付服务，请联系商家");
		}
		String payAmtYuan =
				BigDecimal.valueOf(payFeeFen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toPlainString();
		String timeExpire =
				DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
						.format(Instant.now().plusSeconds(3600).atZone(ZoneId.systemDefault()));

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("order_no", tradeId);
		params.put("app_id", appId);
		params.put("pay_channel", payChannel);
		params.put("pay_amt", payAmtYuan);
		params.put("goods_title", goodsTitle);
		params.put("goods_desc", goodsDesc);
		params.put("description", tradeSourceType);
		params.put("time_expire", timeExpire);
		params.put("notify_url", notifyUrl);
		params.put("currency", "cny");
		if (!"membercard".equalsIgnoreCase(tradeSourceType)) {
			params.put("pay_mode", "delay");
		}
		String ch = payChannel == null ? "" : payChannel.trim();
		if ("wx_lite".equals(ch) || "wx_pub".equals(ch)) {
			Map<String, Object> expend = new LinkedHashMap<>();
			expend.put("open_id", openId == null ? "" : openId.trim());
			params.put("expend", expend);
		}

		String merKey = String.valueOf(companyId);
		String initialRequestJson;
		try {
			initialRequestJson = objectMapper.writeValueAsString(params);
		} catch (JsonProcessingException e) {
			initialRequestJson = "{}";
		}

		Map<String, Object> root;
		synchronized (ADAPAY_MUTEX) {
			try {
				Adapay.prodMode = prodMode;
				MerConfig mc = new MerConfig();
				mc.setApiKey(prodMode ? firstNonBlank(liveKey, testKey) : firstNonBlank(testKey, liveKey));
				mc.setApiMockKey(testKey);
				mc.setRSAPrivateKey(rsaPrivate);
				Adapay.addMerConfig(mc, merKey);
				root = Payment.create(params, merKey);
			} catch (BaseAdaPayException e) {
				String msg = e.getMessage();
				throw new BadRequestException(
						StringUtils.hasText(msg) ? msg : "支付失败");
			} catch (Exception e) {
				throw new BadRequestException("支付失败");
			}
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> data = root == null ? null : (Map<String, Object>) root.get("data");
		if (data == null) {
			throw new BadRequestException("支付失败");
		}
		if ("failed".equalsIgnoreCase(String.valueOf(data.get("status")))) {
			String err = String.valueOf(data.getOrDefault("error_msg", "支付失败"));
			throw new BadRequestException(StringUtils.hasText(err) ? err : "支付失败");
		}

		String transactionId = Objects.toString(data.get("id"), "");
		Map<String, Object> clientParams = buildClientParams(payChannel, data);

		return new AdapayMembercardOutcome(clientParams, transactionId, initialRequestJson);
	}

	private Map<String, Object> buildClientParams(String payChannel, Map<String, Object> data) {
		String ch = payChannel == null ? "" : payChannel.trim();
		return switch (ch) {
			case "wx_lite", "wx_pub" -> parsePayInfoMap(data);
			case "alipay", "alipay_wap" -> {
				String payInfo = expendString(data, "pay_info");
				yield mapOfPayment(payInfo);
			}
			case "alipay_qr" -> {
				String qr = expendString(data, "qrcode_url");
				yield mapOfPayment(qr);
			}
			default -> parsePayInfoMap(data);
		};
	}

	private Map<String, Object> parsePayInfoMap(Map<String, Object> data) {
		String payInfo = expendString(data, "pay_info");
		if (!StringUtils.hasText(payInfo)) {
			return new LinkedHashMap<>(data);
		}
		try {
			return objectMapper.readValue(payInfo, new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("pay_info", payInfo);
			return m;
		}
	}

	private static String expendString(Map<String, Object> data, String key) {
		Object expend = data.get("expend");
		if (!(expend instanceof Map<?, ?> em)) {
			return "";
		}
		Object v = em.get(key);
		return v == null ? "" : v.toString();
	}

	private static Map<String, Object> mapOfPayment(String payment) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("payment", payment);
		return m;
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}

	private static String firstNonBlank(String a, String b) {
		if (StringUtils.hasText(a)) {
			return a;
		}
		return b == null ? "" : b;
	}

	public record AdapayMembercardOutcome(
			Map<String, Object> clientPaymentParams, String transactionId, String initialRequestJson) {}
}
