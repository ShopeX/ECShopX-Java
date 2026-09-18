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

package cn.shopex.ecshopx.adapay.service.callback.handler;

import cn.shopex.ecshopx.deposit.service.AdapayDepositTradeRechargeCallbackService;
import cn.shopex.ecshopx.orders.service.adapay.AdapayOrdersTradePaymentCallbackService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AdapayCallbackPaymentHandler {

	private final AdapayDepositTradeRechargeCallbackService adapayDepositTradeRechargeCallbackService;
	private final AdapayOrdersTradePaymentCallbackService adapayOrdersTradePaymentCallbackService;

	public AdapayCallbackPaymentHandler(
			AdapayDepositTradeRechargeCallbackService adapayDepositTradeRechargeCallbackService,
			AdapayOrdersTradePaymentCallbackService adapayOrdersTradePaymentCallbackService) {
		this.adapayDepositTradeRechargeCallbackService = adapayDepositTradeRechargeCallbackService;
		this.adapayOrdersTradePaymentCallbackService = adapayOrdersTradePaymentCallbackService;
	}

	public List<Object> succeeded(Map<String, Object> data) {
		String rawStatus = data.get("status") == null ? "" : data.get("status").toString();
		String normalized = "succeeded".equals(rawStatus) ? "SUCCESS" : "PAYERROR";
		Map<String, Object> options = new HashMap<>();
		options.put("pay_type", "adapay");
		options.put("pay_channel", data.get("pay_channel"));
		Object expend = data.get("expend");
		if (expend instanceof Map<?, ?> em) {
			Object bankType = em.get("bank_type");
			if (bankType != null) {
				options.put("bank_type", bankType);
			}
		}
		if (data.get("id") != null) {
			options.put("transaction_id", data.get("id"));
		}
		Object description = data.get("description");
		if (description != null && "depositRecharge".equals(description.toString())) {
			adapayDepositTradeRechargeCallbackService.rechargeCallback(null, normalized, options);
		} else {
			adapayOrdersTradePaymentCallbackService.applyPaymentSucceeded(data, normalized);
		}
		return List.of("success");
	}

	public List<Object> failed(Map<String, Object> data) {
		return List.of("success");
	}
}
