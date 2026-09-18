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

package cn.shopex.ecshopx.payment.service.orderrefund;

import cn.shopex.ecshopx.common.payment.PaymentSubjectDistributorIdPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.refund.AftersalesRefundPayChannelExecutor;
import cn.shopex.ecshopx.common.refund.AftersalesRefundPaymentContext;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 微信 / 支付宝 / 斗拱等线上原路退款入口。
 */
@Service
@Order(50)
public class StandardOnlinePayAftersalesRefundExecutor implements AftersalesRefundPayChannelExecutor {

	private final WxpaySecapiAftersalesRefundRunner wxpaySecapiAftersalesRefundRunner;
	private final AlipayTradeAftersalesRefundRunner alipayTradeAftersalesRefundRunner;
	private final BsPayAftersalesOnlineRefundRunner bsPayAftersalesOnlineRefundRunner;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;
	private final PaymentSubjectDistributorIdPort paymentSubjectDistributorIdPort;

	public StandardOnlinePayAftersalesRefundExecutor(
			WxpaySecapiAftersalesRefundRunner wxpaySecapiAftersalesRefundRunner,
			AlipayTradeAftersalesRefundRunner alipayTradeAftersalesRefundRunner,
			BsPayAftersalesOnlineRefundRunner bsPayAftersalesOnlineRefundRunner,
			OrderProcessLogPublishPort orderProcessLogPublishPort,
			PaymentSubjectDistributorIdPort paymentSubjectDistributorIdPort) {
		this.wxpaySecapiAftersalesRefundRunner = wxpaySecapiAftersalesRefundRunner;
		this.alipayTradeAftersalesRefundRunner = alipayTradeAftersalesRefundRunner;
		this.bsPayAftersalesOnlineRefundRunner = bsPayAftersalesOnlineRefundRunner;
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
		this.paymentSubjectDistributorIdPort = paymentSubjectDistributorIdPort;
	}

	@Override
	public boolean supports(String payTypeLower) {
		if (payTypeLower == null || payTypeLower.isEmpty()) {
			return false;
		}
		return payTypeLower.startsWith("wxpay")
				|| payTypeLower.startsWith("alipay")
				|| "bspay".equals(payTypeLower);
	}

	@Override
	public Map<String, Object> execute(AftersalesRefundPaymentContext ctx) {
		String pt = ctx.payTypeLower();
		if ("bspay".equals(pt)) {
			Map<String, Object> payRes =
					bsPayAftersalesOnlineRefundRunner.refund(
							ctx.getCompanyId(),
							ctx.getTradeId(),
							ctx.getBspayReqDate(),
							ctx.getRefundBn(),
							ctx.getRefundFeeFen());
			publishBspayRefundOrderProcessLog(ctx, payRes);
			return payRes;
		}
		if (pt.startsWith("wxpay")) {
			long distributorIdForSetting =
					paymentSubjectDistributorIdPort.resolveActualDistributorId(
							ctx.getCompanyId(), ctx.getDistributorId());
			Map<String, Object> payRes =
					wxpaySecapiAftersalesRefundRunner.refund(
							ctx.getCompanyId(),
							distributorIdForSetting,
							ctx.getWxaAppId(),
							ctx.getTradeId(),
							ctx.getRefundBn(),
							ctx.getRefundFeeFen(),
							ctx.getPayFeeFen());
			publishWechatRefundOrderProcessLog(ctx, payRes);
			return payRes;
		}
		if (pt.startsWith("alipay")) {
			long distributorIdForSetting =
					paymentSubjectDistributorIdPort.resolveActualDistributorId(
							ctx.getCompanyId(), ctx.getDistributorId());
			Map<String, Object> payRes =
					alipayTradeAftersalesRefundRunner.refund(
							ctx.getCompanyId(),
							distributorIdForSetting,
							ctx.getTradeId(),
							ctx.getRefundBn(),
							ctx.getRefundFeeFen(),
							ctx.getPayFeeFen());
			publishAlipayRefundOrderProcessLog(ctx, payRes);
			return payRes;
		}
		return Map.of("status", "FAIL", "error_desc", "不支持的支付方式");
	}

	private void publishBspayRefundOrderProcessLog(AftersalesRefundPaymentContext ctx, Map<String, Object> payRes) {
		long orderId = ctx.getOrderId();
		long companyId = ctx.getCompanyId();
		String status = payRes.get("status") == null ? "" : String.valueOf(payRes.get("status"));
		String detail;
		if ("SUCCESS".equals(status) || "PROCESSING".equals(status)) {
			detail = "订单号：" + orderId + "，订单退款成功（斗拱支付渠道）";
		} else {
			String err = resolveBspayRefundErrorSummary(payRes);
			detail = "订单号：" + orderId + "，订单退款失败（斗拱支付渠道），失败原因：" + err;
		}
		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("order_id", orderId);
		entities.put("company_id", companyId);
		entities.put("operator_type", "system");
		entities.put("remarks", "订单退款");
		entities.put("detail", detail);
		orderProcessLogPublishPort.publish(entities);
	}

	private static String resolveBspayRefundErrorSummary(Map<String, Object> payRes) {
		Object ed = payRes.get("error_desc");
		if (ed != null && StringUtils.hasText(String.valueOf(ed))) {
			return String.valueOf(ed).trim();
		}
		return "斗拱退款失败";
	}

	private void publishAlipayRefundOrderProcessLog(AftersalesRefundPaymentContext ctx, Map<String, Object> payRes) {
		long orderId = ctx.getOrderId();
		long companyId = ctx.getCompanyId();
		String status = payRes.get("status") == null ? "" : String.valueOf(payRes.get("status"));
		String detail;
		if ("SUCCESS".equals(status) || "PROCESSING".equals(status)) {
			detail = "订单号：" + orderId + "，订单退款成功（支付宝渠道）";
		} else {
			String err = resolveAlipayRefundErrorSummary(payRes);
			detail = "订单号：" + orderId + "，订单退款失败（支付宝渠道），失败原因：" + err;
		}
		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("order_id", orderId);
		entities.put("company_id", companyId);
		entities.put("operator_type", "system");
		entities.put("remarks", "订单退款");
		entities.put("detail", detail);
		orderProcessLogPublishPort.publish(entities);
	}

	private static String resolveAlipayRefundErrorSummary(Map<String, Object> payRes) {
		Object ed = payRes.get("error_desc");
		if (ed != null && StringUtils.hasText(String.valueOf(ed))) {
			return String.valueOf(ed).trim();
		}
		Object ec = payRes.get("error_code");
		if (ec != null && StringUtils.hasText(String.valueOf(ec))) {
			return String.valueOf(ec).trim();
		}
		return "支付宝退款失败";
	}

	private void publishWechatRefundOrderProcessLog(AftersalesRefundPaymentContext ctx, Map<String, Object> payRes) {
		long orderId = ctx.getOrderId();
		long companyId = ctx.getCompanyId();
		String status = payRes.get("status") == null ? "" : String.valueOf(payRes.get("status"));
		String detail;
		if ("SUCCESS".equals(status)) {
			detail = "订单号：" + orderId + "，订单退款成功（微信支付渠道）";
		} else {
			String err = resolveWechatRefundErrorSummary(payRes);
			detail = "订单号：" + orderId + "，订单退款失败（微信支付渠道），失败原因：" + err;
		}
		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("order_id", orderId);
		entities.put("company_id", companyId);
		entities.put("operator_type", "system");
		entities.put("remarks", "订单退款");
		entities.put("detail", detail);
		orderProcessLogPublishPort.publish(entities);
	}

	private static String resolveWechatRefundErrorSummary(Map<String, Object> payRes) {
		Object ed = payRes.get("error_desc");
		if (ed != null && StringUtils.hasText(String.valueOf(ed))) {
			return String.valueOf(ed).trim();
		}
		return "微信退款失败";
	}
}
