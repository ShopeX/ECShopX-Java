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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.payment.integration.alipay.AlipayNotifyDepositSidePort;
import cn.shopex.ecshopx.payment.integration.alipay.AlipayNotifyOrderSidePort;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxpayNotifyFacade {

	private static final Logger log = LoggerFactory.getLogger(WxpayNotifyFacade.class);

	private final WxpayNotifyPaymentContextLoader loader;
	private final WxpayAsyncNotifyVerificationService wxpayAsyncNotifyVerificationService;
	private final AlipayNotifyOrderSidePort alipayNotifyOrderSidePort;
	private final AlipayNotifyDepositSidePort alipayNotifyDepositSidePort;

	public WxpayNotifyFacade(
			WxpayNotifyPaymentContextLoader loader,
			WxpayAsyncNotifyVerificationService wxpayAsyncNotifyVerificationService,
			AlipayNotifyOrderSidePort alipayNotifyOrderSidePort,
			AlipayNotifyDepositSidePort alipayNotifyDepositSidePort) {
		this.loader = loader;
		this.wxpayAsyncNotifyVerificationService = wxpayAsyncNotifyVerificationService;
		this.alipayNotifyOrderSidePort = alipayNotifyOrderSidePort;
		this.alipayNotifyDepositSidePort = alipayNotifyDepositSidePort;
	}

	public ResponseEntity<String> handle(HttpServletRequest request) {
		try {
			Optional<WxpayNotifyPaymentContext> opt = loader.load(request);
			if (opt.isEmpty()) {
				return xmlResponse("FAIL", "invalid notify");
			}
			WxpayNotifyPaymentContext ctx = opt.get();
			wxpayAsyncNotifyVerificationService.verifySignedNotify(ctx.getNotifyParams(), ctx.getApiKey());

			Map<String, String> notify = ctx.getNotifyParams();
			boolean successful = "SUCCESS".equalsIgnoreCase(str(notify.get("result_code")));
			String status;
			if (successful) {
				status = "SUCCESS";
			} else {
				String tradeState = str(notify.get("trade_state"));
				status = StringUtils.hasText(tradeState) ? tradeState : "PAYERROR";
			}

			Map<String, String> returnData = ctx.getReturnData();
			boolean depositRecharge =
					returnData.containsKey("attach") && "depositRecharge".equals(returnData.get("attach"));

			Map<String, Object> options = new LinkedHashMap<>();
			for (Map.Entry<String, String> e : notify.entrySet()) {
				options.put(e.getKey(), e.getValue());
			}
			String payTypeFromAttach = returnData.get("pay_type");
			options.put("pay_type", payTypeFromAttach != null ? payTypeFromAttach : "");
			if (!options.containsKey("bank_type")) {
				options.put("bank_type", null);
			}
			if (!options.containsKey("transaction_id")) {
				options.put("transaction_id", null);
			}
			if (!options.containsKey("total_fee")) {
				options.put("total_fee", null);
			}

			String outTradeNo = str(notify.get("out_trade_no"));
			try {
				if (depositRecharge) {
					log.debug("wxpay notify depositRecharge out_trade_no={}", outTradeNo);
					alipayNotifyDepositSidePort.rechargeCallback(outTradeNo, status, options);
				} else {
					alipayNotifyOrderSidePort.applyTradePaymentAfterAlipay(outTradeNo, status, options);
				}
			} catch (BadRequestException | ResourceException e) {
				log.debug(
						"wxpay notify business already handled out_trade_no={} msg={}",
						outTradeNo,
						e.getMessage());
			}
			return xmlResponse("SUCCESS", "");
		} catch (Exception e) {
			log.error("wxpay notify failure", e);
			String msg = e.getMessage();
			return xmlResponse("FAIL", StringUtils.hasText(msg) ? msg : "notify error");
		}
	}

	private static ResponseEntity<String> xmlResponse(String returnCode, String returnMsg) {
		String body =
				"<xml><return_code><![CDATA["
						+ cdataSafe(returnCode)
						+ "]]></return_code><return_msg><![CDATA["
						+ cdataSafe(returnMsg == null ? "" : returnMsg)
						+ "]]></return_msg></xml>";
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(body);
	}

	private static String cdataSafe(String v) {
		if (v == null) {
			return "";
		}
		return v.replace("]]>", "]] >");
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}
}
