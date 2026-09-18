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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappOrderDetailTradePrefixSupport {

	private final TradeMapper tradeMapper;

	public WxappOrderDetailTradePrefixSupport(TradeMapper tradeMapper) {
		this.tradeMapper = tradeMapper;
	}

	public String resolveCanonicalOrderId(String tdPrefixedOrderId) {
		if (!StringUtils.hasText(tdPrefixedOrderId) || tdPrefixedOrderId.length() <= 2) {
			throw new BadRequestException("支付单不存在");
		}
		String tradePk = tdPrefixedOrderId.substring(2).trim();
		if (!StringUtils.hasText(tradePk)) {
			throw new BadRequestException("支付单不存在");
		}
		Trade row = tradeMapper.selectById(tradePk);
		if (row == null) {
			throw new BadRequestException("支付单不存在");
		}
		if (!StringUtils.hasText(row.getOrderId())) {
			return null;
		}
		return row.getOrderId().trim();
	}
}
