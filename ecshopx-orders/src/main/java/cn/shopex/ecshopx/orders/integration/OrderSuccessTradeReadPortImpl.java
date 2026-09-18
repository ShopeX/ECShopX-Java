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

import cn.shopex.ecshopx.common.port.order.OrderSuccessTradeReadPort;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class OrderSuccessTradeReadPortImpl implements OrderSuccessTradeReadPort {

	private final TradeMapper tradeMapper;

	public OrderSuccessTradeReadPortImpl(TradeMapper tradeMapper) {
		this.tradeMapper = tradeMapper;
	}

	@Override
	public Optional<Map<String, Object>> primarySuccessTrade(long companyId, long orderId) {
		String cid = String.valueOf(companyId);
		String oid = String.valueOf(orderId);
		LambdaQueryWrapper<Trade> base = new LambdaQueryWrapper<>();
		base.eq(Trade::getCompanyId, cid).eq(Trade::getOrderId, oid).eq(Trade::getTradeState, "SUCCESS");
		long cnt = tradeMapper.selectCount(base);
		if (cnt <= 0) {
			return Optional.empty();
		}
		LambdaQueryWrapper<Trade> qw = new LambdaQueryWrapper<>();
		qw.eq(Trade::getCompanyId, cid).eq(Trade::getOrderId, oid).eq(Trade::getTradeState, "SUCCESS");
		if (cnt > 1) {
			qw.ne(Trade::getPayType, "point");
		}
		qw.last("LIMIT 1");
		Trade t = tradeMapper.selectOne(qw);
		if (t == null) {
			return Optional.empty();
		}
		return Optional.of(tradeToMap(t));
	}

	private static Map<String, Object> tradeToMap(Trade t) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("trade_id", t.getTradeId());
		m.put("pay_type", t.getPayType());
		m.put("fee_type", t.getFeeType());
		m.put("cur_fee_type", t.getCurFeeType());
		m.put("cur_fee_rate", t.getCurFeeRate());
		m.put("cur_fee_symbol", t.getCurFeeSymbol());
		m.put("pay_fee", t.getPayFee());
		m.put("merchant_id", t.getMerchantId());
		return m;
	}
}
