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

import cn.shopex.ecshopx.common.port.order.PointsmallPartialCancelItemStorePort;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * PointsmallPartialCancelItemStorePort 的 Noop 实现；阶段 4 通过 {@code [cron-mock][pointsmall-item-store]} 日志做断言。
 * 经 {@code CronTestMockConfig} 在 {@code test-cron} profile 下以 {@code @Bean @Primary} 注册。
 */
@Slf4j
public class NoopPointsmallPartialCancelItemStorePort implements PointsmallPartialCancelItemStorePort {

	private final AtomicInteger callCount = new AtomicInteger(0);

	@Override
	public boolean minusItemStore(long companyId, long itemId, int num, boolean isTotalStore) {
		int n = callCount.incrementAndGet();
		log.info("[cron-mock][pointsmall-item-store] called#{}, args={}", n,
				Arrays.toString(new Object[]{companyId, itemId, num, isTotalStore}));
		return true;
	}
}
