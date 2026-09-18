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

import cn.shopex.ecshopx.common.port.order.OrderCancelMarketingJoinCountPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 订单取消后减少营销活动报名人数（Redis MarketingUserJoinNum:{companyId}:{activityId} hash hincrby user_{userId} -1）。
 */
@Service
public class OrderCancelMarketingJoinCountPortImpl implements OrderCancelMarketingJoinCountPort {

	private final StringRedisTemplate companysRedisTemplate;

	public OrderCancelMarketingJoinCountPortImpl(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.companysRedisTemplate = companysRedisTemplate;
	}

	@Override
	public void lessJoinCount(long companyId, long userId, long activityId) {
		String key = "MarketingUserJoinNum:" + companyId + ":" + activityId;
		String hashField = "user_" + userId;
		companysRedisTemplate.opsForHash().increment(key, hashField, -1L);
	}
}
