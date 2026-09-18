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

import cn.shopex.ecshopx.common.dispatch.AliyunsmsQuerySendDetailJobDispatchPublisher;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * test-cron profile: replaces the real query-send-detail job publisher to avoid enqueueing and outbound
 * chains during scheduled-task tests.
 */
@Slf4j
public class NoopAliyunsmsQuerySendDetailJobDispatchPublisher implements AliyunsmsQuerySendDetailJobDispatchPublisher {

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public void publish(long companyId, long recordId, String mobilePlain, String bizId, int createdEpochSeconds) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][query-send-detail-job] called#{}, companyId={}, recordId={}, bizId={}, created={}",
				n,
				companyId,
				recordId,
				bizId,
				createdEpochSeconds);
	}
}
