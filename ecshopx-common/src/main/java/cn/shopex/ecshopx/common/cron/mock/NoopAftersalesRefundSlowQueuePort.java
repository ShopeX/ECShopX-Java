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

import cn.shopex.ecshopx.common.port.aftersales.AftersalesRefundQueueMessage;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesRefundSlowQueuePort;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * test-cron 下覆盖真实慢队列，避免向 Redis 真投递；由 CronTestMockConfig 以 {@code @Primary} 注册。
 */
@Slf4j
public class NoopAftersalesRefundSlowQueuePort implements AftersalesRefundSlowQueuePort {

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public void enqueue(AftersalesRefundQueueMessage message, int delaySeconds) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][aftersales-refund-slow] called#{}, args=refundBn={}, companyId={}, orderId={}, delaySec={}",
				n,
				message == null ? null : message.getRefundBn(),
				message == null ? null : message.getCompanyId(),
				message == null ? null : message.getOrderId(),
				delaySeconds);
	}
}
