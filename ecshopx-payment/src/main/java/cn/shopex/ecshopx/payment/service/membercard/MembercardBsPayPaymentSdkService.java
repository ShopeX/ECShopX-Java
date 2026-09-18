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
import com.huifu.bspay.sdk.opps.client.BasePayClient;
import com.huifu.bspay.sdk.opps.core.BasePay;
import com.huifu.bspay.sdk.opps.core.config.MerConfig;
import com.huifu.bspay.sdk.opps.core.exception.BasePayException;
import com.huifu.bspay.sdk.opps.core.request.V3TradePaymentJspayRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 会员卡场景的汇付斗拱 JSAPI/小程序预下单：校验斗拱商户配置，组装 JSAPI 请求并返回客户端支付参数与网关响应摘要。
 */
@Service
public class MembercardBsPayPaymentSdkService {

	private static final Object BSPAY_MUTEX = new Object();

	private final ObjectMapper objectMapper;

	@Value("${ecshopx.bspay.notify-url:}")
	private String notifyUrlBase;

	@Value("${ecshopx.bspay.prod-mode:true}")
	private boolean bspayProdMode;

	public MembercardBsPayPaymentSdkService(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public BsPayMembercardOutcome createPayment(
			long companyId,
			Map<String, Object> merchantCfg,
			String tradeId,
			String orderId,
			String payChannel,
			int payFeeFen,
			String openId,
			String wxaAppId,
			String body,
			String goodsDesc,
			String clientIp,
			String remark,
			String source,
			boolean membercardTrade) {
		if (!StringUtils.hasText(notifyUrlBase)) {
			throw new BadRequestException("不支持支付服务，请联系商家");
		}
		String sysId = str(merchantCfg.get("sys_id"));
		String productId = str(merchantCfg.get("product_id"));
		String rsaMerchPrivate = str(merchantCfg.get("rsa_merch_private_key"));
		String rsaHuifuPublic = str(merchantCfg.get("rsa_huifu_public_key"));
		if (!StringUtils.hasText(sysId)
				|| !StringUtils.hasText(productId)
				|| !StringUtils.hasText(rsaMerchPrivate)
				|| !StringUtils.hasText(rsaHuifuPublic)) {
			throw new BadRequestException("不支持支付服务，请联系商家");
		}

		String payChannelNorm = payChannel == null ? "" : payChannel.trim();
		String payChannelForNotify = payChannelNorm;
		if ("wx_pub".equals(payChannelNorm) && "pc".equalsIgnoreCase(source == null ? "" : source.trim())) {
			payChannelForNotify = "wx_qr";
		}
		String notifyUrl = notifyUrlBase.replaceAll("/$", "") + "/pay." + payChannelForNotify;

		String transAmt =
				BigDecimal.valueOf(payFeeFen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toPlainString();
		String timeExpire =
				DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
						.format(java.time.Instant.now().plusSeconds(3600).atZone(java.time.ZoneId.systemDefault()));
		String reqDate = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

		V3TradePaymentJspayRequest req = new V3TradePaymentJspayRequest();
		req.setReqDate(reqDate);
		req.setReqSeqId(tradeId);
		req.setHuifuId(sysId);
		req.setGoodsDesc(goodsDesc);
		req.setTradeType(tradeTypeForChannel(payChannelNorm));
		req.setTransAmt(transAmt);

		req.addExtendInfo("time_expire", timeExpire);
		req.addExtendInfo("term_div_coupon_type", "0");
		req.addExtendInfo("limit_pay_type", "NO_CREDIT");
		req.addExtendInfo("delay_acct_flag", membercardTrade ? "N" : "Y");
		req.addExtendInfo("remark", remark);
		req.addExtendInfo("notify_url", notifyUrl);

		switch (payChannelNorm) {
			case "wx_lite", "wx_pub", "wx_qr" -> req.addExtendInfo("wx_data", buildWxDataJson(wxaAppId, openId, body, clientIp));
			case "alipay_wap", "alipay_qr" -> req.addExtendInfo(
					"alipay_data", buildAlipayDataJson(orderId, body));
			default -> throw new BadRequestException("不支持支付服务，请联系商家");
		}

		Map<String, Object> initialRequestSnapshot = new LinkedHashMap<>();
		initialRequestSnapshot.put("req_date", reqDate);
		initialRequestSnapshot.put("req_seq_id", tradeId);
		initialRequestSnapshot.put("huifu_id", sysId);
		initialRequestSnapshot.put("goods_desc", goodsDesc);
		initialRequestSnapshot.put("trade_type", tradeTypeForChannel(payChannelNorm));
		initialRequestSnapshot.put("trans_amt", transAmt);
		initialRequestSnapshot.put("notify_url", notifyUrl);
		String initialRequestJson;
		try {
			initialRequestJson = objectMapper.writeValueAsString(initialRequestSnapshot);
		} catch (JsonProcessingException e) {
			initialRequestJson = "{}";
		}

		String merKey = String.valueOf(companyId);
		Map<String, Object> resp;
		synchronized (BSPAY_MUTEX) {
			try {
				BasePay.prodMode = bspayProdMode ? BasePay.MODE_PROD : BasePay.MODE_TEST;
				BasePay.debug = false;
				MerConfig mc = new MerConfig();
				mc.setSysId(sysId);
				mc.setProcutId(productId);
				mc.setRsaPrivateKey(rsaMerchPrivate);
				mc.setRsaPublicKey(rsaHuifuPublic);
				BasePay.addMerConfig(mc, merKey);
				resp = BasePayClient.request(req, merKey, false);
			} catch (BasePayException e) {
				String msg = e.getMessage();
				throw new BadRequestException(StringUtils.hasText(msg) ? msg : "支付失败");
			} catch (IllegalAccessException e) {
				throw new BadRequestException("支付失败");
			} catch (Exception e) {
				throw new BadRequestException("支付失败");
			}
		}

		Map<String, Object> data = extractBsPayDataPayload(resp);
		if (data == null) {
			throw new BadRequestException("支付失败");
		}
		String respCode = Objects.toString(data.get("resp_code"), "");
		if (!"00000000".equals(respCode) && !"00000100".equals(respCode)) {
			String desc = Objects.toString(data.get("resp_desc"), "支付失败");
			throw new BadRequestException(StringUtils.hasText(desc) ? desc : "支付失败");
		}

		String partyOrderId = Objects.toString(data.get("party_order_id"), "");
		String reqDateOut = Objects.toString(data.get("req_date"), reqDate);
		Map<String, Object> result = buildBsPayClientResult(payChannelNorm, data);

		return new BsPayMembercardOutcome(result, partyOrderId, reqDateOut, initialRequestJson);
	}

	private static String tradeTypeForChannel(String payChannel) {
		return switch (payChannel) {
			case "wx_lite" -> "T_MINIAPP";
			case "wx_pub", "wx_qr" -> "T_JSAPI";
			case "alipay_wap", "alipay_qr" -> "A_NATIVE";
			default -> "";
		};
	}

	private String buildWxDataJson(String subAppid, String subOpenid, String body, String clientIp) {
		Map<String, Object> wx = new LinkedHashMap<>();
		wx.put("sub_appid", subAppid == null ? "" : subAppid.trim());
		wx.put("sub_openid", subOpenid == null ? "" : subOpenid.trim());
		wx.put("body", body == null ? "" : body.trim());
		wx.put("spbill_create_ip", StringUtils.hasText(clientIp) ? clientIp.trim() : "127.0.0.1");
		try {
			return objectMapper.writeValueAsString(wx);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("支付失败");
		}
	}

	private String buildAlipayDataJson(String orderId, String body) {
		Map<String, Object> ali = new LinkedHashMap<>();
		ali.put("merchant_order_no", orderId);
		ali.put("subject", body == null ? "" : body.trim());
		try {
			return objectMapper.writeValueAsString(ali);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("支付失败");
		}
	}

	private Map<String, Object> buildBsPayClientResult(String payChannel, Map<String, Object> data) {
		String ch = payChannel == null ? "" : payChannel.trim();
		String payInfo = Objects.toString(data.get("pay_info"), "");
		return switch (ch) {
			case "wx_lite", "wx_pub", "wx_qr" -> parseJsonToMap(payInfo);
			case "alipay_wap" -> {
				String qr = Objects.toString(data.get("qr_code"), "");
				yield mapOfPayment(qr);
			}
			case "alipay_qr" -> {
				String qr = Objects.toString(data.get("qr_code"), "");
				Map<String, Object> m = new LinkedHashMap<>();
				m.put("payment", qr);
				m.put("qrcode_url", qr);
				yield m;
			}
			default -> parseJsonToMap(payInfo);
		};
	}

	private Map<String, Object> parseJsonToMap(String json) {
		if (!StringUtils.hasText(json)) {
			return new LinkedHashMap<>();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("pay_info", json);
			return m;
		}
	}

	private static Map<String, Object> mapOfPayment(String payment) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("payment", payment);
		return m;
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}

	/**
	 * 兼容斗拱 SDK 返回体 {@code { data: { resp_code, ... } }} 与 {@code { data: { data: { resp_code, ... }}}}。
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Object> extractBsPayDataPayload(Map<String, Object> resp) {
		if (resp == null) {
			return null;
		}
		Object layer1 = resp.get("data");
		if (!(layer1 instanceof Map<?, ?> m1)) {
			return null;
		}
		Object inner = m1.get("data");
		if (inner instanceof Map<?, ?> m2 && m2.containsKey("resp_code")) {
			return (Map<String, Object>) m2;
		}
		if (m1.containsKey("resp_code")) {
			return (Map<String, Object>) m1;
		}
		return null;
	}

	public record BsPayMembercardOutcome(
			Map<String, Object> clientPaymentParams,
			String partyOrderId,
			String bspayReqDate,
			String initialRequestJson) {}
}
