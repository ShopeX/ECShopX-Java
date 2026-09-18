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

package cn.shopex.ecshopx.orders.service.normal;

import cn.shopex.ecshopx.orders.domain.OrderProfit;
import cn.shopex.ecshopx.orders.mapper.OrderProfitMapper;
import cn.shopex.ecshopx.orders.service.distribution.DistributionPlanLimitTimeRedisReadService;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OrderProfitPlanCloseTimeWriteService {

	private final OrderProfitMapper orderProfitMapper;
	private final OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;
	private final DistributionPlanLimitTimeRedisReadService distributionPlanLimitTimeRedisReadService;

	public OrderProfitPlanCloseTimeWriteService(
			OrderProfitMapper orderProfitMapper,
			OrderValiditySettingRedisReadService orderValiditySettingRedisReadService,
			DistributionPlanLimitTimeRedisReadService distributionPlanLimitTimeRedisReadService) {
		this.orderProfitMapper = orderProfitMapper;
		this.orderValiditySettingRedisReadService = orderValiditySettingRedisReadService;
		this.distributionPlanLimitTimeRedisReadService = distributionPlanLimitTimeRedisReadService;
	}

	public void orderProfitPlanCloseTime(long companyId, long orderId) {
		Map<String, Object> setting = orderValiditySettingRedisReadService.readPlatformSetting(companyId);
		int latestAftersale = intVal(setting.get("latest_aftersale_time"));
		int planLimit = distributionPlanLimitTimeRedisReadService.readPlanLimitTimeDays(companyId);
		int day = latestAftersale + planLimit;
		int planCloseTime = (int) (Instant.now().getEpochSecond() + 86400L * day);

		OrderProfit row =
				orderProfitMapper.selectOne(
						new LambdaQueryWrapper<OrderProfit>()
								.eq(OrderProfit::getOrderId, orderId)
								.eq(OrderProfit::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (row == null) {
			return;
		}
		LambdaUpdateWrapper<OrderProfit> uw = new LambdaUpdateWrapper<>();
		uw.eq(OrderProfit::getOrderId, orderId)
				.eq(OrderProfit::getCompanyId, companyId)
				.set(OrderProfit::getPlanCloseTime, (long) planCloseTime);
		orderProfitMapper.update(null, uw);
	}

	private static int intVal(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			String s = v.toString().trim();
			if (s.isEmpty()) {
				return 0;
			}
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
