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

package cn.shopex.ecshopx.orders.service;

import cn.shopex.ecshopx.common.port.order.OrderTradeMiniProgramRecipientLookupPort;
import cn.shopex.ecshopx.common.port.order.OrderTradeMiniProgramRecipientLookupPort.MiniProgramTradeRecipient;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class OrderTradeMiniProgramRecipientLookupService implements OrderTradeMiniProgramRecipientLookupPort {

	private final TradeMapper tradeMapper;

	@Override
	public Optional<MiniProgramTradeRecipient> findRecipient(long companyId, long orderId) {
		String cid = String.valueOf(companyId);
		String oid = String.valueOf(orderId);
		Trade row =
				tradeMapper.selectOne(
						new LambdaQueryWrapper<Trade>()
								.eq(Trade::getCompanyId, cid)
								.eq(Trade::getOrderId, oid)
								.eq(Trade::getTradeState, "SUCCESS")
								.last("LIMIT 1"));
		if (row == null) {
			row =
					tradeMapper.selectOne(
							new LambdaQueryWrapper<Trade>()
									.eq(Trade::getCompanyId, cid)
									.eq(Trade::getOrderId, oid)
									.last("LIMIT 1"));
		}
		if (row == null) {
			return Optional.empty();
		}
		String wxa = row.getWxaAppid();
		String open = row.getOpenId();
		if (!StringUtils.hasText(wxa) || !StringUtils.hasText(open)) {
			return Optional.empty();
		}
		return Optional.of(new MiniProgramTradeRecipient(wxa.trim(), open.trim()));
	}
}
