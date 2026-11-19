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

package cn.shopex.ecshopx.orders.service.saaserp;

import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpTradeUpdateBodyPort;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class OrdersSaasErpTradeUpdateBodyAdapter implements SaasErpTradeUpdateBodyPort {

	private final OrdersSaasErpOrderBundleReader orderReader;
	private final OrdersSaasErpOrderStructAdapter orderStructAdapter;

	public OrdersSaasErpTradeUpdateBodyAdapter(
			OrdersSaasErpOrderBundleReader orderReader, OrdersSaasErpOrderStructAdapter orderStructAdapter) {
		this.orderReader = orderReader;
		this.orderStructAdapter = orderStructAdapter;
	}

	@Override
	public Optional<Map<String, Object>> tradeUpdateBody(long companyId, String orderId, String orderClass) {
		String sourceType = orderClass == null || orderClass.isBlank() ? "normal" : orderClass.trim();
		Optional<Map<String, Object>> loaded = orderReader.read(companyId, orderId, sourceType);
		if (loaded.isEmpty()) {
			return Optional.empty();
		}
		Map<String, Object> orderInfo = asStringKeyMap(loaded.get().get("orderInfo"));
		if (orderInfo.isEmpty()) {
			return Optional.empty();
		}
		String orderStatus = text(orderInfo.get("order_status"));
		if ("NOTPAY".equals(orderStatus) || "WAIT_BUYER_CONFIRM".equals(orderStatus)) {
			return Optional.empty();
		}
		return orderStructAdapter.getOrderStruct(companyId, orderId, sourceType);
	}

	private static Map<String, Object> asStringKeyMap(Object raw) {
		if (!(raw instanceof Map<?, ?> map)) {
			return Map.of();
		}
		Map<String, Object> out = new java.util.LinkedHashMap<>();
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			if (entry.getKey() != null) {
				out.put(String.valueOf(entry.getKey()), entry.getValue());
			}
		}
		return out;
	}

	private static String text(Object raw) {
		return raw == null ? "" : String.valueOf(raw);
	}
}
