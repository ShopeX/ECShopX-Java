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

package cn.shopex.ecshopx.orders.integration;

import cn.shopex.ecshopx.common.port.order.TradeByIdReadPort;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TradeByIdReadPortImpl implements TradeByIdReadPort {

	private final TradeMapper tradeMapper;

	public TradeByIdReadPortImpl(TradeMapper tradeMapper) {
		this.tradeMapper = tradeMapper;
	}

	@Override
	public Optional<Map<String, Object>> getByTradeId(String tradeId, long companyId) {
		if (!StringUtils.hasText(tradeId)) {
			return Optional.empty();
		}
		LambdaQueryWrapper<Trade> w =
				new LambdaQueryWrapper<Trade>().eq(Trade::getTradeId, tradeId.trim()).last("LIMIT 1");
		if (companyId > 0L) {
			w.eq(Trade::getCompanyId, String.valueOf(companyId));
		}
		Trade trade = tradeMapper.selectOne(w);
		if (trade == null) {
			return Optional.empty();
		}
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("trade_id", trade.getTradeId());
		m.put("trade_source_type", trade.getTradeSourceType() == null ? "" : trade.getTradeSourceType());
		m.put("company_id", trade.getCompanyId());
		m.put("order_id", trade.getOrderId());
		return Optional.of(m);
	}
}
