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

package cn.shopex.ecshopx.orders.service.doumenintl;

import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.payment.OrdersTradePaymentCallbackService;
import cn.shopex.ecshopx.payment.integration.doumenintl.DoumenIntlNotifyOrderSidePort;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DoumenIntlNotifyOrderSidePortImpl implements DoumenIntlNotifyOrderSidePort {

	private final TradeMapper tradeMapper;
	private final OrdersTradePaymentCallbackService ordersTradePaymentCallbackService;

	public DoumenIntlNotifyOrderSidePortImpl(
			TradeMapper tradeMapper,
			OrdersTradePaymentCallbackService ordersTradePaymentCallbackService) {
		this.tradeMapper = tradeMapper;
		this.ordersTradePaymentCallbackService = ordersTradePaymentCallbackService;
	}

	@Override
	public TradeBrief findTrade(String tradeId) {
		if (!StringUtils.hasText(tradeId)) {
			return null;
		}
		Trade trade = tradeMapper.selectById(tradeId);
		if (trade == null) {
			return null;
		}
		long companyId = 0L;
		if (StringUtils.hasText(trade.getCompanyId())) {
			try {
				companyId = Long.parseLong(trade.getCompanyId().trim());
			} catch (NumberFormatException ignored) {
				companyId = 0L;
			}
		}
		return new TradeBrief(companyId, trade.getTradeState() == null ? "" : trade.getTradeState());
	}

	@Override
	public void applyTradePayment(String tradeId, String status, Map<String, Object> options) {
		ordersTradePaymentCallbackService.applyTradePaymentNotify(tradeId, status, options);
	}
}
