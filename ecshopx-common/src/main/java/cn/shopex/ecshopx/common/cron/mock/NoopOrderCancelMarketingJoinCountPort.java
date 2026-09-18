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

package cn.shopex.ecshopx.common.cron.mock;

import cn.shopex.ecshopx.common.port.order.OrderCancelMarketingJoinCountPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * OrderCancelMarketingJoinCountPort 的 Noop 实现；阶段 4 Tester 通过 grep [cron-mock][market-join-count] 日志做等价性断言。
 * 仅在 spring.profiles.active 包含 test-cron 时经 @Profile("test-cron") 装配。
 */
@Slf4j
@Primary
@Profile("test-cron")
@Component
public class NoopOrderCancelMarketingJoinCountPort implements OrderCancelMarketingJoinCountPort {

	private final AtomicInteger callCount = new AtomicInteger(0);

	@Override
	public void lessJoinCount(long companyId, long userId, long activityId) {
		int n = callCount.incrementAndGet();
		log.info("[cron-mock][market-join-count] called#{}, args={}",
				n, Arrays.toString(new Object[]{companyId, userId, activityId}));
	}
}
