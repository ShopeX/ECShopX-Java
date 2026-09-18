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

import cn.shopex.ecshopx.common.port.orders.CommunityActivityCancelOrderBatchPayload;
import cn.shopex.ecshopx.orders.port.CommunityActivityCancelOrdersSlowQueuePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 与发票慢队列分键，避免与 Laravel 全表 {@code slow} 混解析；同 Redis list + delayed ZSET 形态。
 */
@Component
public class CommunityActivityCancelOrdersSlowQueuePortImpl implements CommunityActivityCancelOrdersSlowQueuePort {

	public static final String KEY_LIST = "ecshopx:queue:orders:community-activity-cancel:slow";

	public static final String KEY_DELAYED = "ecshopx:queue:orders:community-activity-cancel:slow:delayed";

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public CommunityActivityCancelOrdersSlowQueuePortImpl(
			StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	@Override
	public void enqueue(CommunityActivityCancelOrderBatchPayload payload, int delaySeconds) {
		try {
			String json = objectMapper.writeValueAsString(payload);
			if (delaySeconds <= 0) {
				stringRedisTemplate.opsForList().rightPush(KEY_LIST, json);
			} else {
				double score = (double) (System.currentTimeMillis() / 1000L + delaySeconds);
				stringRedisTemplate.opsForZSet().add(KEY_DELAYED, json, score);
			}
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("community activity cancel slow queue message serialize failed", e);
		}
	}
}
