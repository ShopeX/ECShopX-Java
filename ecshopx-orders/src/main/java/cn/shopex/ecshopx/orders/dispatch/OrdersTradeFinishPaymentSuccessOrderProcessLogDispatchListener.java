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

package cn.shopex.ecshopx.orders.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OrdersTradeFinishPaymentSuccessOrderProcessLogDispatchListener implements DispatchListener {

	private final OrderProcessLogPublishPort orderProcessLogPublishPort;

	public OrdersTradeFinishPaymentSuccessOrderProcessLogDispatchListener(
			OrderProcessLogPublishPort orderProcessLogPublishPort) {
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		if (payload == null) {
			return;
		}
		String tradeState = stringify(payload.get("trade_state"));
		if (!"SUCCESS".equals(tradeState)) {
			return;
		}
		String orderIdStr = stringify(payload.get("order_id"));
		String companyIdStr = stringify(payload.get("company_id"));
		if (!StringUtils.hasText(orderIdStr) || !StringUtils.hasText(companyIdStr)) {
			return;
		}
		LinkedHashMap<String, Object> entities = new LinkedHashMap<>();
		entities.put("order_id", orderIdStr);
		entities.put("company_id", companyIdStr);
		entities.put("operator_type", "system");
		entities.put("is_show", Boolean.TRUE);
		entities.put("remarks", "订单支付");
		entities.put("detail", "订单号：" + orderIdStr + "，订单支付成功");
		orderProcessLogPublishPort.publish(entities);
	}

	private static String stringify(Object raw) {
		return raw == null ? "" : String.valueOf(raw).trim();
	}
}
