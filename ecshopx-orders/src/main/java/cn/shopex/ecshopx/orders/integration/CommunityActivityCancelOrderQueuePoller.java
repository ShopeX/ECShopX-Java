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
import cn.shopex.ecshopx.common.port.orders.CommunityActivityCancelOrderRow;
import cn.shopex.ecshopx.orders.service.CommunityActivityCancelOrderExecutor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 消费 {@link CommunityActivityCancelOrdersSlowQueuePortImpl} 入队的 JSON；与全站 Laravel slow 分键，仅处理本业务负载。
 */
@Component
@Profile("!test-cron")
public class CommunityActivityCancelOrderQueuePoller {

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;
	private final CommunityActivityCancelOrderExecutor executor;

	public CommunityActivityCancelOrderQueuePoller(
			StringRedisTemplate stringRedisTemplate,
			ObjectMapper objectMapper,
			CommunityActivityCancelOrderExecutor executor) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
		this.executor = executor;
	}

	@Scheduled(fixedDelayString = "${ecshopx.orders.community-cancel.queue.poll-ms:2000}")
	public void pollReadyList() {
		for (int i = 0; i < 20; i++) {
			String json = stringRedisTemplate.opsForList().leftPop(CommunityActivityCancelOrdersSlowQueuePortImpl.KEY_LIST);
			if (json == null) {
				return;
			}
			dispatchOnePayload(json);
		}
	}

	private void dispatchOnePayload(String json) {
		CommunityActivityCancelOrderBatchPayload payload;
		try {
			payload = objectMapper.readValue(json, CommunityActivityCancelOrderBatchPayload.class);
		} catch (JsonProcessingException e) {
			return;
		}
		List<CommunityActivityCancelOrderRow> rows = payload.getRows();
		if (rows == null) {
			return;
		}
		for (CommunityActivityCancelOrderRow row : rows) {
			executor.execute(row);
		}
	}
}
