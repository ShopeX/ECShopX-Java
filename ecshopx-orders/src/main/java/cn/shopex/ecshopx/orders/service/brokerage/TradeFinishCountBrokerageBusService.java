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

package cn.shopex.ecshopx.orders.service.brokerage;

import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderBrokerageOnFinishService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Trade-finish path: load the normal order and run the same brokerage hooks used on other finish flows
 * (confirm receipt, batch finish, ziti write-off).
 */
@Service
public class TradeFinishCountBrokerageBusService {

	private static final Logger log = LoggerFactory.getLogger(TradeFinishCountBrokerageBusService.class);

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrderBrokerageOnFinishService normalOrderBrokerageOnFinishService;

	public TradeFinishCountBrokerageBusService(
			NormalOrdersMapper normalOrdersMapper,
			NormalOrderBrokerageOnFinishService normalOrderBrokerageOnFinishService) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrderBrokerageOnFinishService = normalOrderBrokerageOnFinishService;
	}

	public void handleTradeFinishRow(Map<String, Object> tradeRowSnakeCase) {
		if (tradeRowSnakeCase == null || tradeRowSnakeCase.isEmpty()) {
			return;
		}
		Long companyId = parseCompanyId(tradeRowSnakeCase.get("company_id"));
		Long orderId = parsePositiveOrderId(tradeRowSnakeCase.get("order_id"));
		if (companyId == null || orderId == null) {
			log.debug("TradeFinishCountBrokerage: skip, invalid company_id or order_id");
			return;
		}
		if (isPointPayType(tradeRowSnakeCase.get("pay_type"))) {
			log.debug("TradeFinishCountBrokerage: skip, pay_type is point");
			return;
		}
		NormalOrders order = normalOrdersMapper.selectById(orderId);
		if (order == null) {
			log.debug("TradeFinishCountBrokerage: skip, order not found for order_id={}", orderId);
			return;
		}
		if (order.getCompanyId() != null && !order.getCompanyId().equals(companyId)) {
			log.debug("TradeFinishCountBrokerage: skip, company_id mismatch for order_id={}", orderId);
			return;
		}
		try {
			normalOrderBrokerageOnFinishService.orderFinishBrokerage(companyId, orderId, order);
		} catch (Throwable t) {
			log.debug("TradeFinishCountBrokerage: orderFinishBrokerage failed: {}", t.toString());
		}
	}

	private static boolean isPointPayType(Object raw) {
		if (raw == null) {
			return false;
		}
		String s = String.valueOf(raw).trim();
		return "point".equalsIgnoreCase(s);
	}

	private static Long parseCompanyId(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : null;
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Long parsePositiveOrderId(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : null;
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
