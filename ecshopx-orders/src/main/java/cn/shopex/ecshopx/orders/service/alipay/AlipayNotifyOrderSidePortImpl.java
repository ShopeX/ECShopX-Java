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

package cn.shopex.ecshopx.orders.service.alipay;

import cn.shopex.ecshopx.orders.domain.DistributionDistributorPeek;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.DistributionDistributorPeekMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.payment.OrdersTradePaymentCallbackService;
import cn.shopex.ecshopx.payment.integration.alipay.AlipayNotifyOrderSidePort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AlipayNotifyOrderSidePortImpl implements AlipayNotifyOrderSidePort {

	private final TradeMapper tradeMapper;
	private final DistributionDistributorPeekMapper distributionDistributorPeekMapper;
	private final OrdersTradePaymentCallbackService ordersTradePaymentCallbackService;

	public AlipayNotifyOrderSidePortImpl(
			TradeMapper tradeMapper,
			DistributionDistributorPeekMapper distributionDistributorPeekMapper,
			OrdersTradePaymentCallbackService ordersTradePaymentCallbackService) {
		this.tradeMapper = tradeMapper;
		this.distributionDistributorPeekMapper = distributionDistributorPeekMapper;
		this.ordersTradePaymentCallbackService = ordersTradePaymentCallbackService;
	}

	@Override
	public long resolveDistributorIdForAlipayRedis(long companyId, String outTradeNo) {
		if (!StringUtils.hasText(outTradeNo)) {
			return 0L;
		}
		Trade trade = tradeMapper.selectById(outTradeNo);
		if (trade == null) {
			return 0L;
		}
		String distStr = trade.getDistributorId();
		if (!StringUtils.hasText(distStr) || "0".equals(distStr.trim())) {
			return 0L;
		}
		long distributorId;
		try {
			distributorId = Long.parseLong(distStr.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
		if (distributorId <= 0) {
			return 0L;
		}
		DistributionDistributorPeek peek = distributionDistributorPeekMapper.selectOne(
				new LambdaQueryWrapper<DistributionDistributorPeek>()
						.eq(DistributionDistributorPeek::getCompanyId, companyId)
						.eq(DistributionDistributorPeek::getDistributorId, distributorId)
						.last("LIMIT 1"));
		if (peek == null || peek.getPaymentSubject() == null || peek.getPaymentSubject() != 1) {
			return 0L;
		}
		return distributorId;
	}

	@Override
	public void applyTradePaymentAfterAlipay(String outTradeNo, String status, Map<String, Object> options) {
		ordersTradePaymentCallbackService.applyTradePaymentNotify(outTradeNo, status, options);
	}
}
