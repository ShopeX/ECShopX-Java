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

package cn.shopex.ecshopx.orders.dispatch.profit;

import cn.shopex.ecshopx.common.port.orders.TradeFinishProfitSalespersonSideEffectsPort;
import cn.shopex.ecshopx.orders.domain.OrderProfit;
import cn.shopex.ecshopx.orders.service.OrderProfitService;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class TradeFinishProfitBusService {

	private final OrderProfitService orderProfitService;
	private final TradeFinishProfitSalespersonSideEffectsPort tradeFinishProfitSalespersonSideEffects;

	public TradeFinishProfitBusService(
			OrderProfitService orderProfitService,
			TradeFinishProfitSalespersonSideEffectsPort tradeFinishProfitSalespersonSideEffects) {
		this.orderProfitService = orderProfitService;
		this.tradeFinishProfitSalespersonSideEffects = tradeFinishProfitSalespersonSideEffects;
	}

	public void handleTradeFinishRow(Map<String, Object> tradeRow) {
		Long orderId = parsePositiveLong(first(tradeRow, "order_id", "orderId"));
		Long companyId = parsePositiveLong(first(tradeRow, "company_id", "companyId"));
		Long userId = parsePositiveLong(first(tradeRow, "user_id", "userId"));
		if (orderId == null || companyId == null || userId == null) {
			return;
		}

		Optional<OrderProfit> info = orderProfitService.findForTradeFinishProfit(orderId, companyId, userId);
		if (info.isEmpty()) {
			log.debug("订单:{}无分润信息", orderId);
			return;
		}
		OrderProfit row = info.get();
		Long popularizeSellerId = row.getPopularizeSellerId();
		if (popularizeSellerId != null && popularizeSellerId > 0L) {
			Long orderDistributorId = row.getOrderDistributorId();
			if (orderDistributorId == null) {
				orderDistributorId = 0L;
			}
			Long payFee = row.getPayFee();
			long payFeeFen = payFee != null ? payFee : 0L;
			tradeFinishProfitSalespersonSideEffects.applyOnPopularizeSeller(
					companyId,
					orderDistributorId,
					popularizeSellerId,
					payFeeFen,
					userId,
					orderId);
		}

		long planCloseEpoch = java.time.Instant.now().getEpochSecond() + 86400L * 10L * 365L;
		try {
			orderProfitService.markTradeFinishProfitClosed(orderId, companyId, userId, planCloseEpoch);
		} catch (Exception e) {
			log.error("TradeFinishProfit mark closed failed orderId={}", orderId, e);
		}
	}

	private static Object first(Map<String, Object> m, String a, String b) {
		Object x = m.get(a);
		if (x != null) {
			return x;
		}
		return m.get(b);
	}

	private static Long parsePositiveLong(Object raw) {
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
