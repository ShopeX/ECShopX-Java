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
import cn.shopex.ecshopx.common.payment.PaymentSubjectDistributorIdPort;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.payment.service.AlipayOpenapiTradeQueryService;
import cn.shopex.ecshopx.payment.service.AlipayPaymentConfigValidationService;
import cn.shopex.ecshopx.payment.service.DoumenIntlPaymentService;
import cn.shopex.ecshopx.payment.service.dto.AlipayTradeQueryParsedResponse;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
public class OrdersPaymentChannelQueryService {

	private static final String WX_ORDER_QUERY_URL = "https://api.mch.weixin.qq.com/pay/orderquery";

	private final OrdersExternalPayParamBuildService ordersExternalPayParamBuildService;
	private final AlipayOpenapiTradeQueryService alipayOpenapiTradeQueryService;
	private final AlipayPaymentConfigValidationService alipayPaymentConfigValidationService;
	private final DoumenIntlPaymentService doumenIntlPaymentService;
	private final PaymentSubjectDistributorIdPort paymentSubjectDistributorIdPort;
	private final RestTemplate restTemplate;

	public OrdersPaymentChannelQueryService(
			OrdersExternalPayParamBuildService ordersExternalPayParamBuildService,
			AlipayOpenapiTradeQueryService alipayOpenapiTradeQueryService,
			AlipayPaymentConfigValidationService alipayPaymentConfigValidationService,
			DoumenIntlPaymentService doumenIntlPaymentService,
			PaymentSubjectDistributorIdPort paymentSubjectDistributorIdPort) {
		this.ordersExternalPayParamBuildService = ordersExternalPayParamBuildService;
		this.alipayOpenapiTradeQueryService = alipayOpenapiTradeQueryService;
		this.alipayPaymentConfigValidationService = alipayPaymentConfigValidationService;
		this.doumenIntlPaymentService = doumenIntlPaymentService;
		this.paymentSubjectDistributorIdPort = paymentSubjectDistributorIdPort;
		this.restTemplate = new RestTemplate();
	}

	public Map<String, Object> query(Trade trade, Map<String, Object> authInfo) {
		if (trade == null) {
			throw new BadRequestException("支付失败");
		}
		String payTypeRaw = trade.getPayType() == null ? "" : trade.getPayType().trim().toLowerCase(Locale.ROOT);
		long companyId = parseCompanyId(trade);
		long distributorIdForSetting =
				paymentSubjectDistributorIdPort.resolveActualDistributorId(
						companyId, parseDistributorId(trade));

		if (payTypeRaw.startsWith("wxpay")) {
			String xml =
					ordersExternalPayParamBuildService.buildWxpayOrderQueryXml(
							companyId, distributorIdForSetting, trade);
			ResponseEntity<String> resp;
			try {
				resp = restTemplate.postForEntity(WX_ORDER_QUERY_URL, wxEntity(xml), String.class);
			} catch (Exception e) {
				throw new ResourceException("支付失败");
			}
			String respXml = resp.getBody();
			if (!StringUtils.hasText(respXml)) {
				throw new ResourceException("支付失败");
			}
			String returnCode = xmlText(respXml, "return_code");
			String resultCode = xmlText(respXml, "result_code");
			if (!"SUCCESS".equals(returnCode) || !"SUCCESS".equals(resultCode)) {
				throw new ResourceException("支付失败");
			}
			String tradeState = xmlText(respXml, "trade_state");
			String tradeStateDesc = xmlText(respXml, "trade_state_desc");
			String transactionId = xmlText(respXml, "transaction_id");
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("pay_type", trade.getPayType());
			out.put("trade_id", trade.getTradeId());
			if ("SUCCESS".equals(tradeState)) {
				out.put("status", "SUCCESS");
				String msg = StringUtils.hasText(tradeStateDesc) ? tradeStateDesc : "支付成功";
				out.put("msg", msg);
				if (StringUtils.hasText(transactionId)) {
					out.put("transaction_id", transactionId);
				}
				return out;
			}
			if ("USERPAYING".equals(tradeState)) {
				out.put("status", "USERPAYING");
				String msg = StringUtils.hasText(tradeStateDesc) ? tradeStateDesc : "支付处理中";
				out.put("msg", msg);
				return out;
			}
			throw new ResourceException("支付失败");
		}

		if (payTypeRaw.startsWith("alipay")) {
			alipayPaymentConfigValidationService.assertConfigComplete(companyId, distributorIdForSetting);
			final AlipayTradeQueryParsedResponse parsed;
			try {
				parsed =
						alipayOpenapiTradeQueryService.queryTrade(
								companyId, distributorIdForSetting, trade.getTradeId());
			} catch (BadRequestException e) {
				throw new ResourceException("支付失败");
			}
			String tradeStatus = parsed.tradeStatus() == null ? "" : parsed.tradeStatus();
			String tradeNo = parsed.tradeNo() == null ? "" : parsed.tradeNo();
			if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
				Map<String, Object> out = new LinkedHashMap<>();
				out.put("status", "SUCCESS");
				out.put("msg", "支付成功");
				out.put("pay_type", trade.getPayType());
				out.put("trade_id", trade.getTradeId());
				if (StringUtils.hasText(tradeNo)) {
					out.put("transaction_id", tradeNo);
				}
				return out;
			}
			throw new ResourceException("支付失败");
		}

		if ("doumen_intl".equals(payTypeRaw)) {
			String transactionId = trade.getTransactionId() == null ? "" : trade.getTransactionId().trim();
			if (!StringUtils.hasText(transactionId)) {
				throw new ResourceException("支付失败");
			}
			return doumenIntlPaymentService.query(companyId, transactionId);
		}

		if ("offline_pay".equals(payTypeRaw) || "paypal".equals(payTypeRaw)) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("status", "FAIL");
			m.put("msg", "支付失败");
			m.put("pay_type", trade.getPayType());
			m.put("trade_id", trade.getTradeId());
			return m;
		}

		throw new BadRequestException("支付失败");
	}

	private static long parseCompanyId(Trade trade) {
		try {
			return Long.parseLong(trade.getCompanyId() == null ? "0" : trade.getCompanyId().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("支付失败");
		}
	}

	private static long parseDistributorId(Trade trade) {
		try {
			return Long.parseLong(trade.getDistributorId() == null ? "0" : trade.getDistributorId().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("支付失败");
		}
	}

	private static HttpEntity<String> wxEntity(String xml) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_XML);
		return new HttpEntity<>(xml, headers);
	}

	private static String xmlText(String xml, String tag) {
		String open = "<" + tag + ">";
		String close = "</" + tag + ">";
		int a = xml.indexOf(open);
		int b = xml.indexOf(close);
		if (a < 0 || b < 0 || b <= a) {
			return "";
		}
		return xml.substring(a + open.length(), b).trim();
	}
}
