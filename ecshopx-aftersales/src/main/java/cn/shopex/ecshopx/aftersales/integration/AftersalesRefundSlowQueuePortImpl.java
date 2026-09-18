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

package cn.shopex.ecshopx.aftersales.integration;

import cn.shopex.ecshopx.common.port.aftersales.AftersalesRefundQueueMessage;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesRefundSlowQueuePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 使用 Redis 列表/延迟集投递慢队列，键名以 {@value #KEY_LIST}/{@value #KEY_DELAYED} 为基线，可与运维约定覆盖。
 */
@Component
@Profile("!test-cron")
public class AftersalesRefundSlowQueuePortImpl implements AftersalesRefundSlowQueuePort {

	/** 与历史上 Laravel 风格 slow 可观察名对齐，可配 {@code ecshopx.aftersales.slow-queue.list-key} 后续扩展。 */
	public static final String KEY_LIST = "ecshopx:queue:slow";

	public static final String KEY_DELAYED = "ecshopx:queue:slow:delayed";

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public AftersalesRefundSlowQueuePortImpl(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	@Override
	public void enqueue(AftersalesRefundQueueMessage message, int delaySeconds) {
		try {
			String json = objectMapper.writeValueAsString(message);
			if (delaySeconds <= 0) {
				stringRedisTemplate.opsForList().rightPush(KEY_LIST, json);
			} else {
				double score = (double) (System.currentTimeMillis() / 1000L + delaySeconds);
				stringRedisTemplate.opsForZSet().add(KEY_DELAYED, json, score);
			}
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("refund slow queue message serialize failed", e);
		}
	}
}
