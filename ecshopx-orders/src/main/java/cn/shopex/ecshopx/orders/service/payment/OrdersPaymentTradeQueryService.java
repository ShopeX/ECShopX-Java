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
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrdersPaymentTradeQueryService {

	private static final Logger log = LoggerFactory.getLogger(OrdersPaymentTradeQueryService.class);

	private final TradeMapper tradeMapper;
	private final OrdersPaymentChannelQueryService ordersPaymentChannelQueryService;
	private final OrdersPaymentDoPaymentService ordersPaymentDoPaymentService;

	public OrdersPaymentTradeQueryService(
			TradeMapper tradeMapper,
			OrdersPaymentChannelQueryService ordersPaymentChannelQueryService,
			OrdersPaymentDoPaymentService ordersPaymentDoPaymentService) {
		this.tradeMapper = tradeMapper;
		this.ordersPaymentChannelQueryService = ordersPaymentChannelQueryService;
		this.ordersPaymentDoPaymentService = ordersPaymentDoPaymentService;
	}

	public Map<String, Object> resolve(String tradeId, Map<String, Object> authInfo) {
		if (tradeId == null || tradeId.isEmpty()) {
			throw new BadRequestException("支付单不存在", 400);
		}
		Trade row = tradeMapper.selectById(tradeId);
		if (row == null) {
			throw new BadRequestException("支付单不存在", 400);
		}
		if ("SUCCESS".equalsIgnoreCase(safe(row.getTradeState()))) {
			Map<String, Object> done = new LinkedHashMap<>();
			done.put("status", "SUCCESS");
			done.put("msg", "支付成功");
			return done;
		}

		Map<String, Object> payResult;
		try {
			payResult = ordersPaymentChannelQueryService.query(row, authInfo);
		} catch (BadRequestException e) {
			throw e;
		} catch (RuntimeException e) {
			log.error("payment channel query failed, tradeId={}", tradeId, e);
			throw new BadRequestException("支付失败", 400);
		}

		String status = normalizeStatus(payResult.get("status"));
		if ("SUCCESS".equals(status)) {
			ordersPaymentDoPaymentService.finalizeTradeSuccessAfterChannelQuery(
					row, transactionIdFrom(payResult), payResult);
			return mergeTradeKeys(payResult, row);
		}
		return mergeTradeKeys(payResult, row);
	}

	private static String safe(String s) {
		return s == null ? "" : s.trim();
	}

	private static String normalizeStatus(Object o) {
		return o == null ? "" : o.toString().trim().toUpperCase(Locale.ROOT);
	}

	private static String transactionIdFrom(Map<String, Object> payResult) {
		Object v = payResult.get("transaction_id");
		return v == null ? "" : v.toString().trim();
	}

	private static Map<String, Object> mergeTradeKeys(Map<String, Object> channel, Trade row) {
		Map<String, Object> m = new LinkedHashMap<>(channel);
		fillIfBlank(m, "trade_id", row.getTradeId());
		fillIfBlank(m, "pay_type", row.getPayType());
		return m;
	}

	private static void fillIfBlank(Map<String, Object> m, String key, String fromTrade) {
		Object cur = m.get(key);
		if (cur == null || !StringUtils.hasText(cur.toString())) {
			m.put(key, fromTrade);
		}
	}
}
