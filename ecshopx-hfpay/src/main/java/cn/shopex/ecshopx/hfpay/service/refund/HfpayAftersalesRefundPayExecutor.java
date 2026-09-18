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

package cn.shopex.ecshopx.hfpay.service.refund;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.hfpay.HfpayRefundSuccessEventPublishPort;
import cn.shopex.ecshopx.common.refund.AftersalesRefundPayChannelExecutor;
import cn.shopex.ecshopx.common.refund.AftersalesRefundPaymentContext;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayAcouJsonPostClient;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayPaymentSettingService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Order(45)
public class HfpayAftersalesRefundPayExecutor implements AftersalesRefundPayChannelExecutor {

	private static final String HFPAY_REFUND_DEV_INFO_JSON =
			"{\"ipAddr\":\"127.0.0.1\",\"devType\":\"2\",\"MAC\":\"D4-81-D7-F0-42-F8\"}";

	private final HfpayRefundSuccessEventPublishPort hfpayRefundSuccessEventPublishPort;
	private final HfPayAcouJsonPostClient hfPayAcouJsonPostClient;
	private final HfPayPaymentSettingService hfPayPaymentSettingService;

	public HfpayAftersalesRefundPayExecutor(
			HfpayRefundSuccessEventPublishPort hfpayRefundSuccessEventPublishPort,
			HfPayAcouJsonPostClient hfPayAcouJsonPostClient,
			HfPayPaymentSettingService hfPayPaymentSettingService) {
		this.hfpayRefundSuccessEventPublishPort = hfpayRefundSuccessEventPublishPort;
		this.hfPayAcouJsonPostClient = hfPayAcouJsonPostClient;
		this.hfPayPaymentSettingService = hfPayPaymentSettingService;
	}

	@Override
	public boolean supports(String payTypeLower) {
		return "hfpay".equals(payTypeLower);
	}

	@Override
	public Map<String, Object> execute(AftersalesRefundPaymentContext ctx) {
		if (!StringUtils.hasText(ctx.getHfOrderId())) {
			return fail("汇付退款单号缺失");
		}
		Map<String, Object> setting;
		try {
			setting = hfPayPaymentSettingService.loadForCompany(ctx.getCompanyId());
		} catch (ResourceException e) {
			return fail(e.getMessage());
		}
		String mer = String.valueOf(setting.get("mer_cust_id")).trim();
		if (!StringUtils.hasText(mer)) {
			return fail("商户支付配置缺失");
		}
		String transAmtYuan =
				BigDecimal.valueOf(ctx.getRefundFeeFen())
						.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
						.toPlainString();
		String orgYmd =
				StringUtils.hasText(ctx.getHfpayOrgOrderDateYmd())
						? ctx.getHfpayOrgOrderDateYmd().trim()
						: "";
		String refundYmd =
				StringUtils.hasText(ctx.getHfpayRefundOrderDateYmd())
						? ctx.getHfpayRefundOrderDateYmd().trim()
						: "";
		if (!StringUtils.hasText(orgYmd) || !StringUtils.hasText(refundYmd)) {
			return fail("退款日期参数缺失");
		}
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("version", "10");
		payload.put("mer_cust_id", mer);
		payload.put("order_id", ctx.getHfOrderId().trim());
		payload.put("order_date", refundYmd);
		payload.put("org_order_id", ctx.getTradeId().trim());
		payload.put("org_order_date", orgYmd);
		payload.put("trans_amt", transAmtYuan);
		payload.put("dev_info_json", HFPAY_REFUND_DEV_INFO_JSON);
		payload.put("mer_priv", "refund");
		if (ctx.getHfpayOrderIsProfitsharing() == 1) {
			payload.put("in_cust_id", mer);
		} else {
			payload.put("in_cust_id", "");
		}
		Map<String, Object> result;
		try {
			result = hfPayAcouJsonPostClient.reb001(setting, payload);
		} catch (ResourceException e) {
			return fail(e.getMessage());
		}
		String code = result.get("resp_code") == null ? "" : String.valueOf(result.get("resp_code")).trim();
		if (!"C00002".equals(code)) {
			Object desc = result.get("resp_desc");
			String msg = desc == null ? "" : String.valueOf(desc);
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("status", "FAIL");
			m.put("error_code", code);
			m.put("error_desc", StringUtils.hasText(msg) ? msg : "汇付退款失败");
			return m;
		}
		hfpayRefundSuccessEventPublishPort.publishSyncOnGatewayRefundSuccess(
				String.valueOf(ctx.getOrderId()), ctx.getRefundBn());
		Map<String, Object> ok = new LinkedHashMap<>();
		ok.put("status", "SUCCESS");
		Object gatewayOrderId = result.get("order_id");
		ok.put("refund_id", gatewayOrderId == null ? "" : String.valueOf(gatewayOrderId));
		return ok;
	}

	private static Map<String, Object> fail(String msg) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("status", "FAIL");
		m.put("error_desc", msg);
		return m;
	}
}
