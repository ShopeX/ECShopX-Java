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

package cn.shopex.ecshopx.promotions.integration.order;

import cn.shopex.ecshopx.common.port.order.OrderCancelSeckillTicketPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 订单取消后回补限时购购买数（Redis seckill_buy_data:{companyId} hash）。
 */
@Service
public class OrderCancelSeckillTicketPortImpl implements OrderCancelSeckillTicketPort {

	private final StringRedisTemplate companysRedisTemplate;

	public OrderCancelSeckillTicketPortImpl(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.companysRedisTemplate = companysRedisTemplate;
	}

	@Override
	public void restoreUserBuysStore(long companyId, long userId, long activityId, long itemId, int qty) {
		if (qty <= 0) {
			return;
		}
		String hashKey = "seckill_buy_data:" + companyId;
		String buyStoreKey = "user_buy_store:" + activityId + ":" + userId + ":" + itemId;
		String buyTotalStore = "user_buy_total_store:" + activityId + ":" + userId;
		companysRedisTemplate.opsForHash().increment(hashKey, buyStoreKey, -qty);
		companysRedisTemplate.opsForHash().increment(hashKey, buyTotalStore, -qty);
	}
}
