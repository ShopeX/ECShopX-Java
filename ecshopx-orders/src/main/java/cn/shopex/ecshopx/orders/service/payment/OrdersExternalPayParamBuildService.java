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

package cn.shopex.ecshopx.orders.service.payment;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.port.OrdersMiniProgramHfpayPayPort;
import cn.shopex.ecshopx.payment.service.AlipayExternalPaySdkService;
import cn.shopex.ecshopx.payment.service.AlipayPaymentConfigValidationService;
import cn.shopex.ecshopx.payment.service.DoumenIntlPaymentService;
import cn.shopex.ecshopx.payment.service.WxpayPaymentConfigValidationService;
import cn.shopex.ecshopx.payment.service.dto.AlipayTradePayShallowResult;
import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.binarywang.wxpay.bean.request.WxPayMicropayRequest;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderRequest;
import com.github.binarywang.wxpay.bean.result.WxPayMicropayResult;
import com.github.binarywang.wxpay.bean.result.WxPayUnifiedOrderResult;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrdersExternalPayParamBuildService {

	private static final Logger log = LoggerFactory.getLogger(OrdersExternalPayParamBuildService.class);

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final WxpayPaymentConfigValidationService wxpayPaymentConfigValidationService;
	private final AlipayPaymentConfigValidationService alipayPaymentConfigValidationService;
	private final AlipayExternalPaySdkService alipayExternalPaySdkService;
	private final DoumenIntlPaymentService doumenIntlPaymentService;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final TradeMapper tradeMapper;
	private final String wechatNotifyUrl;
	private final String alipayNotifyUrl;
	private final ObjectProvider<OrdersMiniProgramHfpayPayPort> hfpayPayPort;

	public OrdersExternalPayParamBuildService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			WxpayPaymentConfigValidationService wxpayPaymentConfigValidationService,
			AlipayPaymentConfigValidationService alipayPaymentConfigValidationService,
			AlipayExternalPaySdkService alipayExternalPaySdkService,
			DoumenIntlPaymentService doumenIntlPaymentService,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			TradeMapper tradeMapper,
			ObjectProvider<OrdersMiniProgramHfpayPayPort> hfpayPayPort,
			@Value("${ecshopx.payment.wechat.notify-url:}") String wechatNotifyUrl,
			@Value("${ecshopx.payment.alipay.notify-url:}") String alipayNotifyUrl) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.wxpayPaymentConfigValidationService = wxpayPaymentConfigValidationService;
		this.alipayPaymentConfigValidationService = alipayPaymentConfigValidationService;
		this.alipayExternalPaySdkService = alipayExternalPaySdkService;
		this.doumenIntlPaymentService = doumenIntlPaymentService;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.tradeMapper = tradeMapper;
		this.hfpayPayPort = hfpayPayPort;
		this.wechatNotifyUrl = wechatNotifyUrl;
		this.alipayNotifyUrl = alipayNotifyUrl;
	}

	public Map<String, Object> buildPayParamsOrInvoke(
			String payType,
			long companyId,
			long distributorIdForSetting,
			Map<String, Object> authInfo,
			Map<String, Object> data,
			Trade tradeRow) {
		String pt = payType == null ? "" : payType.trim().toLowerCase(Locale.ROOT);
		if ("hfpay".equals(pt)) {
			OrdersMiniProgramHfpayPayPort port = hfpayPayPort.getIfAvailable();
			if (port == null) {
				throw new ResourceException("不支持支付服务，请联系商家");
			}
			port.assertConfigReady(companyId, distributorIdForSetting);
			return port.buildClientPayParams(companyId, distributorIdForSetting, authInfo, data, tradeRow);
		}
		if (pt.startsWith("wxpay")) {
			wxpayPaymentConfigValidationService.assertConfigComplete(companyId, distributorIdForSetting);
			return wxPayClientParams(pt, companyId, distributorIdForSetting, authInfo, data, tradeRow);
		}
		if (pt.startsWith("alipay")) {
			alipayPaymentConfigValidationService.assertConfigComplete(companyId, distributorIdForSetting);
			return alipayClientParams(pt, companyId, distributorIdForSetting, authInfo, data, tradeRow);
		}
		if ("offline_pay".equals(pt) || "paypal".equals(pt)) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("pay_type", pt);
			m.put("trade_id", tradeRow.getTradeId());
			return m;
		}
		if ("doumen_intl".equals(pt)) {
			return invokeDoumenIntlCheckout(companyId, data, tradeRow);
		}
		throw new ResourceException("无此类型支付");
	}

	private Map<String, Object> invokeDoumenIntlCheckout(
			long companyId, Map<String, Object> data, Trade tradeRow) {
		Map<String, Object> payData = new LinkedHashMap<>(data == null ? Map.of() : data);
		payData.put("company_id", companyId);
		payData.put("trade_id", tradeRow.getTradeId());
		if (!StringUtils.hasText(String.valueOf(payData.getOrDefault("fee_type", "")))) {
			payData.put(
					"fee_type",
					StringUtils.hasText(tradeRow.getFeeType()) ? tradeRow.getFeeType() : "CNY");
		}
		if (!payData.containsKey("pay_fee") || payData.get("pay_fee") == null) {
			payData.put("pay_fee", tradeRow.getPayFee() == null ? 0 : tradeRow.getPayFee());
		}
		List<NormalOrdersItems> items =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, parseOrderIdLong(payData.get("order_id"))));
		List<Map<String, Object>> itemMaps = new ArrayList<>();
		for (NormalOrdersItems it : items) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("item_bn", it.getItemBn());
			row.put("item_name", it.getItemName());
			row.put("item_spec_desc", it.getItemSpecDesc());
			row.put("num", it.getNum());
			row.put("price", it.getPrice());
			row.put("total_fee", it.getTotalFee());
			itemMaps.add(row);
		}
		List<Map<String, Object>> products = DoumenIntlPaymentService.mapOrderItemsToProducts(itemMaps);
		Map<String, Object> payResult = doumenIntlPaymentService.doPay(payData, products);
		String transactionId = String.valueOf(payResult.getOrDefault("transaction_id", ""));
		if (StringUtils.hasText(transactionId)) {
			tradeMapper.update(
					null,
					new LambdaUpdateWrapper<Trade>()
							.eq(Trade::getTradeId, tradeRow.getTradeId())
							.set(Trade::getTransactionId, transactionId));
		}
		Map<String, Object> client = new LinkedHashMap<>();
		client.put("pay_type", "doumen_intl");
		client.put("pay_url", payResult.get("pay_url"));
		client.put("trade_id", tradeRow.getTradeId());
		client.put("transaction_id", transactionId);
		return client;
	}

	private static long parseOrderIdLong(Object orderId) {
		if (orderId instanceof Number n) {
			return n.longValue();
		}
		if (orderId == null) {
			return 0L;
		}
		try {
			return Long.parseLong(String.valueOf(orderId).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	/**
	 * Builds WeChat pay orderquery request XML (UTF-8), for POST to {@code https://api.mch.weixin.qq.com/pay/orderquery}.
	 */
	public String buildWxpayOrderQueryXml(long companyId, long distributorIdForSetting, Trade trade) {
		if (trade == null) {
			throw new ResourceException("支付失败");
		}
		wxpayPaymentConfigValidationService.assertConfigComplete(companyId, distributorIdForSetting);
		Map<String, Object> cfg = loadWxCfg(companyId, distributorIdForSetting);
		boolean servicer = isServicer(cfg);
		String apiKey = str(cfg.get("key"));
		String merchantId = str(cfg.get("merchant_id"));
		String tradeId = trade.getTradeId();
		TreeMap<String, String> q = new TreeMap<>();
		if (servicer) {
			String servicerAppId = str(cfg.get("servicer_app_id"));
			String servicerMchId = str(cfg.get("servicer_merchant_id"));
			if (!StringUtils.hasText(servicerAppId) || !StringUtils.hasText(servicerMchId)) {
				throw new ResourceException("不支持支付服务，请联系商家");
			}
			q.put("appid", servicerAppId);
			q.put("mch_id", servicerMchId);
			String subAppid =
					StringUtils.hasText(trade.getWxaAppid()) ? str(trade.getWxaAppid()) : str(trade.getAuthorizerAppid());
			if (StringUtils.hasText(subAppid)) {
				q.put("sub_appid", subAppid);
			}
			q.put("sub_mch_id", merchantId);
		} else {
			String appId =
					StringUtils.hasText(trade.getWxaAppid()) ? str(trade.getWxaAppid()) : str(trade.getAuthorizerAppid());
			if (!StringUtils.hasText(appId)) {
				appId = str(cfg.get("app_id"));
			}
			q.put("appid", appId);
			q.put("mch_id", merchantId);
		}
		q.put("nonce_str", randomNonce());
		q.put("out_trade_no", tradeId);
		q.put("sign", signParams(q, apiKey));
		return buildXml(q);
	}

	/**
	 * Builds WeChat refundquery request XML (UTF-8), for POST to {@code https://api.mch.weixin.qq.com/pay/refundquery}.
	 */
	public String buildWxpayRefundQueryXml(long companyId, long distributorIdForSetting, String outRefundNo) {
		if (!StringUtils.hasText(outRefundNo)) {
			throw new ResourceException("支付失败");
		}
		wxpayPaymentConfigValidationService.assertConfigComplete(companyId, distributorIdForSetting);
		Map<String, Object> cfg = loadWxCfg(companyId, distributorIdForSetting);
		boolean servicer = isServicer(cfg);
		String apiKey = str(cfg.get("key"));
		String merchantId = str(cfg.get("merchant_id"));
		TreeMap<String, String> q = new TreeMap<>();
		if (servicer) {
			String servicerAppId = str(cfg.get("servicer_app_id"));
			String servicerMchId = str(cfg.get("servicer_merchant_id"));
			if (!StringUtils.hasText(servicerAppId) || !StringUtils.hasText(servicerMchId)) {
				throw new ResourceException("不支持支付服务，请联系商家");
			}
			q.put("appid", servicerAppId);
			q.put("mch_id", servicerMchId);
			q.put("sub_mch_id", merchantId);
		} else {
			String appId = str(cfg.get("app_id"));
			q.put("appid", appId);
			q.put("mch_id", merchantId);
		}
		q.put("nonce_str", randomNonce());
		q.put("out_refund_no", outRefundNo);
		q.put("sign", signParams(q, apiKey));
		return buildXml(q);
	}

	/**
	 * Builds WeChat pay customs {@code customdeclareorder} request XML (UTF-8), for POST to {@code
	 * https://api.mch.weixin.qq.com/cgi-bin/mch/customs/customdeclareorder}. This endpoint is documented as
	 * nonceless (no {@code nonce_str} field).
	 */
	public String buildWxpayCustomDeclareOrderXml(
			long companyId,
			long distributorIdForSetting,
			Trade trade,
			String customsCode,
			String mchCustomsNo,
			String certType,
			String certId,
			String buyerName,
			int dutyFen,
			int transportFen,
			int productFen,
			int orderFeeFen,
			String subOrderNo,
			String actionType) {
		if (trade == null) {
			throw new ResourceException("支付失败");
		}
		if (!StringUtils.hasText(trade.getTradeId()) || !StringUtils.hasText(trade.getTransactionId())) {
			throw new ResourceException("支付失败");
		}
		if (!StringUtils.hasText(customsCode)
				|| !StringUtils.hasText(mchCustomsNo)
				|| !StringUtils.hasText(certType)
				|| !StringUtils.hasText(certId)
				|| !StringUtils.hasText(buyerName)) {
			throw new ResourceException("支付失败");
		}
		if (!StringUtils.hasText(subOrderNo)) {
			throw new ResourceException("支付失败");
		}
		String at = StringUtils.hasText(actionType) ? actionType.trim() : "ADD";
		wxpayPaymentConfigValidationService.assertConfigComplete(companyId, distributorIdForSetting);
		Map<String, Object> cfg = loadWxCfg(companyId, distributorIdForSetting);
		boolean servicer = isServicer(cfg);
		String apiKey = str(cfg.get("key"));
		String merchantId = str(cfg.get("merchant_id"));
		TreeMap<String, String> q = new TreeMap<>();
		if (servicer) {
			String servicerAppId = str(cfg.get("servicer_app_id"));
			String servicerMchId = str(cfg.get("servicer_merchant_id"));
			if (!StringUtils.hasText(servicerAppId) || !StringUtils.hasText(servicerMchId)) {
				throw new ResourceException("不支持支付服务，请联系商家");
			}
			q.put("appid", servicerAppId);
			q.put("mch_id", servicerMchId);
			String subAppid =
					StringUtils.hasText(trade.getWxaAppid()) ? str(trade.getWxaAppid()) : str(trade.getAuthorizerAppid());
			if (StringUtils.hasText(subAppid)) {
				q.put("sub_appid", subAppid);
			}
			q.put("sub_mch_id", merchantId);
		} else {
			String appId =
					StringUtils.hasText(trade.getWxaAppid()) ? str(trade.getWxaAppid()) : str(trade.getAuthorizerAppid());
			if (!StringUtils.hasText(appId)) {
				appId = str(cfg.get("app_id"));
			}
			q.put("appid", appId);
			q.put("mch_id", merchantId);
		}
		q.put("out_trade_no", trade.getTradeId());
		q.put("transaction_id", str(trade.getTransactionId()));
		q.put("customs", customsCode.trim());
		q.put("mch_customs_no", mchCustomsNo.trim());
		q.put("duty", String.valueOf(Math.max(0, dutyFen)));
		q.put("action_type", at);
		q.put("sub_order_no", subOrderNo.trim());
		q.put("fee_type", "CNY");
		q.put("order_fee", String.valueOf(Math.max(0, orderFeeFen)));
		q.put("transport_fee", String.valueOf(Math.max(0, transportFen)));
		q.put("product_fee", String.valueOf(Math.max(0, productFen)));
		q.put("cert_type", certType.trim());
		q.put("cert_id", certId.trim());
		q.put("name", buyerName.trim());
		q.put("sign", signParams(q, apiKey));
		return buildXml(q);
	}

	private Map<String, Object> wxPayClientParams(
			String payTypeLc,
			long companyId,
			long distributorIdForSetting,
			Map<String, Object> authInfo,
			Map<String, Object> data,
			Trade tradeRow) {
		Map<String, Object> cfg = loadWxCfg(companyId, distributorIdForSetting);
		boolean servicer = isServicer(cfg);
		String apiKey = str(cfg.get("key"));
		String merchantId = str(cfg.get("merchant_id"));
		String woaAppId = authInfo == null ? "" : str(authInfo.get("woa_appid"));
		String wxaAppId = authInfo == null ? "" : str(authInfo.get("wxapp_appid"));
		if (!StringUtils.hasText(wxaAppId)) {
			wxaAppId = str(data.get("wxa_appid"));
		}
		String cfgAppId = str(cfg.get("app_id"));
		boolean h5OrJsPay = "wxpayh5".equals(payTypeLc) || "wxpayjs".equals(payTypeLc);
		String miniAppIdForUnified =
				h5OrJsPay && StringUtils.hasText(cfgAppId) ? cfgAppId : wxaAppId;
		String openId = str(data.get("open_id"));
		String orderId = str(data.get("order_id"));
		String tradeId = tradeRow.getTradeId();
		int payFee = tradeRow.getPayFee() == null ? 0 : tradeRow.getPayFee();
		String body = str(data.get("body"));
		if (!StringUtils.hasText(body)) {
			body = "订单支付";
		}
		String ip = str(data.get("client_ip"));
		if (!StringUtils.hasText(ip)) {
			ip = "127.0.0.1";
		}
		String passbackInner =
				"company_id=" + urlEnc(String.valueOf(companyId)) + "&pay_type=" + urlEnc(payTypeLc);
		String attachOuter = urlEnc(passbackInner);

		if ("wxpaypos".equals(payTypeLc)) {
			String authCode = str(data.get("auth_code"));
			if (!StringUtils.hasText(authCode)) {
				throw new BadRequestException("缺少付款码");
			}
			String nonceMic = randomNonce();
			WxPayMicropayRequest mic =
					OrdersExternalWxPayV2Requests.micropayRequest(
							servicer,
							str(cfg.get("servicer_app_id")),
							str(cfg.get("servicer_merchant_id")),
							merchantId,
							wxaAppId,
							woaAppId,
							body,
							tradeId,
							payFee,
							ip,
							authCode,
							nonceMic);
			logWxPayMicropayRequest(companyId, orderId, payTypeLc, mic);
			if (servicer
					&& (!StringUtils.hasText(mic.getAppid()) || !StringUtils.hasText(mic.getMchId()))) {
				throw new ResourceException("不支持支付服务，请联系商家");
			}
			WxPayConfig wxCfgMic = OrdersExternalWxPayV2NativeClient.v2Md5Config(apiKey);
			try {
				WxPayMicropayResult micRes = OrdersExternalWxPayV2NativeClient.micropay(wxCfgMic, mic);
				Map<String, Object> out = new LinkedHashMap<>();
				out.put("pay_status", true);
				out.put("transaction_id", micRes.getTransactionId());
				attachTradeInfo(out, orderId, tradeId, str(data.get("trade_source_type")));
				return out;
			} catch (WxPayException e) {
				throw new ResourceException(wxPayErrorMessage(e));
			}
		}

		if (!StringUtils.hasText(wechatNotifyUrl)) {
			throw new ResourceException("不支持支付服务，请联系商家");
		}

		String tradeType =
				switch (payTypeLc) {
					case "wxpayh5" -> "MWEB";
					case "wxpayapp" -> "APP";
					default -> "JSAPI";
				};

		String timeExpire = OrdersExternalWxPayV2Requests.formatTimeExpire(data.get("auto_cancel_time"));
		String nonceUnify = randomNonce();
		WxPayUnifiedOrderRequest unifyReq =
				OrdersExternalWxPayV2Requests.unifiedOrderRequest(
						servicer,
						str(cfg.get("servicer_app_id")),
						str(cfg.get("servicer_merchant_id")),
						merchantId,
						miniAppIdForUnified,
						wxaAppId,
						woaAppId,
						cfgAppId,
						h5OrJsPay,
						tradeType,
						openId,
						body,
						str(data.get("detail")),
						tradeId,
						payFee,
						ip,
						wechatNotifyUrl,
						attachOuter,
						timeExpire,
						nonceUnify);
		logWxPayUnifiedOrderRequest(companyId, orderId, payTypeLc, unifyReq);
		if (servicer
				&& (!StringUtils.hasText(unifyReq.getAppid())
						|| !StringUtils.hasText(unifyReq.getMchId()))) {
			throw new ResourceException("不支持支付服务，请联系商家");
		}
		WxPayConfig wxCfg = OrdersExternalWxPayV2NativeClient.v2Md5Config(apiKey);
		final WxPayUnifiedOrderResult unifyRes;
		try {
			unifyRes = OrdersExternalWxPayV2NativeClient.unifiedOrder(wxCfg, unifyReq);
		} catch (WxPayException e) {
			throw new ResourceException(wxPayErrorMessage(e));
		}

		if ("MWEB".equals(tradeType)) {
			String mwebUrl = unifyRes.getMwebUrl();
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("mweb_url", mwebUrl);
			attachTradeInfo(out, orderId, tradeId, str(data.get("trade_source_type")));
			return out;
		}
		if ("APP".equals(tradeType)) {
			String prepayId = unifyRes.getPrepayId();
			String timeStamp = String.valueOf(System.currentTimeMillis() / 1000L);
			String payNonce = randomNonce();
			TreeMap<String, String> appSign = new TreeMap<>();
			appSign.put("appid", str(unifyReq.getAppid()));
			appSign.put("partnerid", merchantId);
			appSign.put("prepayid", prepayId);
			appSign.put("package", "Sign=WXPay");
			appSign.put("noncestr", payNonce);
			appSign.put("timestamp", timeStamp);
			String paySign = signParams(appSign, apiKey);
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("appid", appSign.get("appid"));
			out.put("partnerid", merchantId);
			out.put("prepayid", prepayId);
			out.put("package", "Sign=WXPay");
			out.put("noncestr", payNonce);
			out.put("timestamp", timeStamp);
			out.put("sign", paySign);
			attachTradeInfo(out, orderId, tradeId, str(data.get("trade_source_type")));
			return out;
		}

		String prepayId = unifyRes.getPrepayId();
		String timeStamp = String.valueOf(System.currentTimeMillis() / 1000L);
		String pkg = "prepay_id=" + prepayId;
		String payNonce = randomNonce();
		String clientAppId = servicer ? str(unifyReq.getSubAppId()) : str(unifyReq.getAppid());
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
		attachTradeInfo(out, orderId, tradeId, str(data.get("trade_source_type")));
		return out;
	}

	/**
	 * Alipay 拉起参数：通过 {@link AlipayExternalPaySdkService} 调用 OpenAPI {@code alipay.trade.app.pay}
	 *（{@code sdkExecute}）。{@code alipaymini} 仍使用 {@code FACE_TO_FACE_PAYMENT} 与可选 {@code buyer_id}，与迁移前
	 * {@code biz_content} 一致。
	 */
	private Map<String, Object> alipayClientParams(
			String payTypeLc,
			long companyId,
			long distributorIdForSetting,
			Map<String, Object> authInfo,
			Map<String, Object> data,
			Trade tradeRow) {
		Map<String, Object> cfg = loadAlipayCfg(companyId, distributorIdForSetting);
		String appId = str(cfg.get("app_id"));
		String privateKey = str(cfg.get("private_key"));
		if (!StringUtils.hasText(appId) || !StringUtils.hasText(privateKey)) {
			throw new ResourceException("不支持支付服务，请联系商家");
		}
		String orderId = str(data.get("order_id"));
		String tradeId = tradeRow.getTradeId();
		int payFee = tradeRow.getPayFee() == null ? 0 : tradeRow.getPayFee();
		String totalYuan = java.math.BigDecimal.valueOf(payFee).divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP).toPlainString();
		String subject = str(data.get("body"));
		if (!StringUtils.hasText(subject)) {
			subject = "订单支付";
		}
		if ("alipaypos".equals(payTypeLc)) {
			return alipayTradePayBarCode(companyId, distributorIdForSetting, orderId, tradeId, totalYuan, subject, data);
		}
		if ("alipayh5".equals(payTypeLc)) {
			Map<String, Object> h5Biz = new LinkedHashMap<>();
			h5Biz.put("out_trade_no", tradeId);
			h5Biz.put("total_amount", totalYuan);
			h5Biz.put("subject", subject);
			h5Biz.put("product_code", "QUICK_WAP_WAY");
			String passbackQuery = "company_id=" + companyId + "&pay_type=alipayh5";
			h5Biz.put(
					"passback_params",
					URLEncoder.encode(passbackQuery, StandardCharsets.UTF_8));
			String h5BizJson;
			try {
				h5BizJson = objectMapper.writeValueAsString(h5Biz);
			} catch (JsonProcessingException e) {
				throw new ResourceException("支付失败");
			}
			String payment =
					alipayExternalPaySdkService.pageWapPayFormHtml(
							companyId,
							distributorIdForSetting,
							h5BizJson,
							str(data.get("return_url")),
							alipayNotifyUrl);
			Map<String, Object> h5Out = new LinkedHashMap<>();
			h5Out.put("payment", payment);
			attachTradeInfo(h5Out, orderId, tradeId, str(data.get("trade_source_type")));
			return h5Out;
		}
		Map<String, Object> biz = new LinkedHashMap<>();
		biz.put("subject", subject);
		biz.put("out_trade_no", tradeId);
		biz.put("total_amount", totalYuan);
		biz.put("product_code", "QUICK_MSECURITY_PAY");
		if ("alipaymini".equals(payTypeLc)) {
			biz.put("product_code", "FACE_TO_FACE_PAYMENT");
			String buyerId = authInfo == null ? "" : str(authInfo.get("alipay_user_id"));
			if (StringUtils.hasText(buyerId)) {
				biz.put("buyer_id", buyerId);
			}
		}
		String bizJson;
		try {
			bizJson = objectMapper.writeValueAsString(biz);
		} catch (JsonProcessingException e) {
			throw new ResourceException("支付失败");
		}
		String orderString = alipayExternalPaySdkService.sdkAppPayOrderString(companyId, distributorIdForSetting, bizJson);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("orderString", orderString);
		attachTradeInfo(out, orderId, tradeId, str(data.get("trade_source_type")));
		return out;
	}

	private Map<String, Object> alipayTradePayBarCode(
			long companyId,
			long distributorIdForSetting,
			String orderId,
			String tradeId,
			String totalYuan,
			String subject,
			Map<String, Object> data) {
		String authCode = str(data.get("auth_code"));
		if (!StringUtils.hasText(authCode)) {
			throw new BadRequestException("缺少付款码");
		}
		AlipayTradePayShallowResult payResp =
				alipayExternalPaySdkService.tradePayBarcodeFaceToFace(
						companyId, distributorIdForSetting, tradeId, totalYuan, subject, authCode);
		String code = payResp.code();
		String msg = payResp.msg();
		String subCode = payResp.subCode();
		String subMsg = payResp.subMsg();

		if ("10000".equals(code)) {
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("pay_status", true);
			out.put("status", "SUCCESS");
			out.put("msg", "支付成功");
			out.put("pay_type", "alipaypos");
			if (payResp.hasTextTradeNo()) {
				out.put("transaction_id", payResp.tradeNo());
			}
			attachTradeInfo(out, orderId, tradeId, str(data.get("trade_source_type")));
			return out;
		}
		if ("10003".equals(code)) {
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("pay_status", false);
			out.put("status", "USERPAYING");
			String userMsg = StringUtils.hasText(subMsg) ? subMsg : msg;
			if (!StringUtils.hasText(userMsg)) {
				userMsg = "支付处理中";
			}
			out.put("msg", userMsg);
			out.put("pay_type", "alipaypos");
			attachTradeInfo(out, orderId, tradeId, str(data.get("trade_source_type")));
			return out;
		}

		String errMsg = StringUtils.hasText(subMsg) ? subMsg : (StringUtils.hasText(msg) ? msg : "支付失败");
		if (isAlipayInvalidAuthBarcode(subCode)) {
			throw new BadRequestException(errMsg);
		}
		throw new ResourceException(errMsg);
	}

	private static boolean isAlipayInvalidAuthBarcode(String subCode) {
		if (!StringUtils.hasText(subCode)) {
			return false;
		}
		return "ACQ.PAYMENT_AUTH_CODE_INVALID".equals(subCode)
				|| "ACQ.ILLEGAL_AUTH_CODE".equals(subCode)
				|| "ACQ.AUTH_CODE_INVALID_OR_EXPIRE".equals(subCode);
	}

	private void attachTradeInfo(Map<String, Object> out, String orderId, String tradeId, String tradeSourceType) {
		Map<String, Object> tradeInfo = new LinkedHashMap<>();
		tradeInfo.put("order_id", orderId);
		tradeInfo.put("trade_id", tradeId);
		tradeInfo.put("trade_source_type", tradeSourceType);
		out.put("trade_info", tradeInfo);
	}

	private Map<String, Object> loadWxCfg(long companyId, long distributorIdForSetting) {
		String raw =
				companysRedisTemplate
						.opsForValue()
						.get(PaymentSettingRedisKeys.wxpayRedisKey(companyId, distributorIdForSetting));
		return PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
	}

	private Map<String, Object> loadAlipayCfg(long companyId, long distributorIdForSetting) {
		String raw =
				companysRedisTemplate
						.opsForValue()
						.get(PaymentSettingRedisKeys.alipayRedisKey(companyId, distributorIdForSetting));
		return PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
	}

	private static boolean isServicer(Map<String, Object> cfg) {
		Object v = cfg.get("is_servicer");
		return v != null && "true".equalsIgnoreCase(v.toString().trim());
	}

	private void logWxPayUnifiedOrderRequest(
			long companyId, String orderId, String payTypeLc, WxPayUnifiedOrderRequest req) {
		try {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("company_id", companyId);
			m.put("order_id", orderId);
			m.put("pay_type", payTypeLc);
			m.put("appid", req.getAppid());
			m.put("mch_id", req.getMchId());
			m.put("sub_appid", req.getSubAppId());
			m.put("sub_mch_id", req.getSubMchId());
			m.put("sub_openid", req.getSubOpenid());
			m.put("openid", req.getOpenid());
			m.put("nonce_str", req.getNonceStr());
			m.put("body", req.getBody());
			m.put("detail", req.getDetail());
			m.put("out_trade_no", req.getOutTradeNo());
			m.put("total_fee", req.getTotalFee());
			m.put("spbill_create_ip", req.getSpbillCreateIp());
			m.put("notify_url", req.getNotifyUrl());
			m.put("trade_type", req.getTradeType());
			m.put("attach", req.getAttach());
			m.put("time_expire", req.getTimeExpire());
			m.put("scene_info", req.getSceneInfo());
			log.info("wxpay unifiedorder request params: {}", objectMapper.writeValueAsString(m));
		} catch (JsonProcessingException e) {
			log.warn("wxpay unifiedorder request params serialization failed: {}", e.toString());
		}
	}

	private void logWxPayMicropayRequest(long companyId, String orderId, String payTypeLc, WxPayMicropayRequest req) {
		try {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("company_id", companyId);
			m.put("order_id", orderId);
			m.put("pay_type", payTypeLc);
			m.put("appid", req.getAppid());
			m.put("mch_id", req.getMchId());
			m.put("sub_appid", req.getSubAppId());
			m.put("sub_mch_id", req.getSubMchId());
			m.put("nonce_str", req.getNonceStr());
			m.put("body", req.getBody());
			m.put("out_trade_no", req.getOutTradeNo());
			m.put("total_fee", req.getTotalFee());
			m.put("spbill_create_ip", req.getSpbillCreateIp());
			m.put("auth_code", maskSensitiveBarcode(req.getAuthCode()));
			log.info("wxpay micropay request params: {}", objectMapper.writeValueAsString(m));
		} catch (JsonProcessingException e) {
			log.warn("wxpay micropay request params serialization failed: {}", e.toString());
		}
	}

	private static String maskSensitiveBarcode(String code) {
		if (!StringUtils.hasText(code)) {
			return "";
		}
		String t = code.trim();
		if (t.length() <= 4) {
			return "****";
		}
		return "****" + t.substring(t.length() - 4);
	}

	private static String wxPayErrorMessage(WxPayException e) {
		String err = e.getErrCodeDes();
		if (!StringUtils.hasText(err)) {
			err = e.getCustomErrorMsg();
		}
		if (!StringUtils.hasText(err)) {
			err = e.getReturnMsg();
		}
		if (!StringUtils.hasText(err)) {
			err = e.getMessage();
		}
		return StringUtils.hasText(err) ? err : "支付失败";
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
		return DigestUtils.md5DigestAsHex(sb.toString().getBytes(StandardCharsets.UTF_8)).toUpperCase();
	}

	private static String randomNonce() {
		byte[] b = new byte[16];
		new SecureRandom().nextBytes(b);
		StringBuilder sb = new StringBuilder(32);
		for (byte value : b) {
			sb.append(String.format("%02x", value));
		}
		return sb.toString();
	}

	private static String buildXml(TreeMap<String, String> params) {
		StringBuilder sb = new StringBuilder();
		sb.append("<xml>");
		for (Map.Entry<String, String> e : params.entrySet()) {
			sb.append('<').append(e.getKey()).append('>');
			sb.append("<![CDATA[").append(cdataSafe(e.getValue())).append("]]>");
			sb.append("</").append(e.getKey()).append('>');
		}
		sb.append("</xml>");
		return sb.toString();
	}

	private static String cdataSafe(String v) {
		if (v == null) {
			return "";
		}
		return v.replace("]]>", "]]]]><![CDATA[>");
	}

	private static String urlEnc(String s) {
		return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}

}
