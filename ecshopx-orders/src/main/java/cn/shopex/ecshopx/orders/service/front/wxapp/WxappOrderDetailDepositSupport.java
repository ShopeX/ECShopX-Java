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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.deposit.service.DepositTradeWxappPaymentService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxappOrderDetailDepositSupport {

	private final DepositTradeWxappPaymentService depositTradeWxappPaymentService;

	public WxappOrderDetailDepositSupport(DepositTradeWxappPaymentService depositTradeWxappPaymentService) {
		this.depositTradeWxappPaymentService = depositTradeWxappPaymentService;
	}

	public Map<String, Object> buildResult(String depositTradeId) {
		Map<String, Object> deposit = depositTradeWxappPaymentService.getDepositTradeRowMapOrThrow(depositTradeId);
		String tradeStatus = deposit.get("trade_status") == null ? "" : String.valueOf(deposit.get("trade_status")).trim();
		String orderStatus = "NOTPAY";
		if ("SUCCESS".equals(tradeStatus)) {
			orderStatus = "DONE";
		}
		Map<String, Object> orderInfo = new LinkedHashMap<>();
		orderInfo.put("order_id", deposit.get("deposit_trade_id"));
		orderInfo.put("order_type", "");
		orderInfo.put("pay_type", deposit.get("pay_type"));
		orderInfo.put("point", "");
		orderInfo.put("title", deposit.get("detail"));
		orderInfo.put("total_fee", intVal(deposit.get("money")));
		orderInfo.put("create_time", (int) (System.currentTimeMillis() / 1000L));
		orderInfo.put("order_status", orderStatus);

		Map<String, Object> tradeInfo = new LinkedHashMap<>();
		tradeInfo.put("orderId", deposit.get("deposit_trade_id"));
		tradeInfo.put("tradeState", tradeStatus);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("orderInfo", orderInfo);
		result.put("tradeInfo", tradeInfo);
		return result;
	}

	private static int intVal(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
