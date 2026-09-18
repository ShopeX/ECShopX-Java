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

import cn.shopex.ecshopx.common.cron.CompanysRecordStatisticsRedisPort;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * test-cron 下替换真实 Redis 端口；阶段 4 可 grep {@code [cron-mock][companys-record-stats-redis]}。
 */
@Slf4j
public class NoopCompanysRecordStatisticsRedisPort implements CompanysRecordStatisticsRedisPort {

	public static final String ALIAS = "companys-record-stats-redis";

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public long scard(String key) {
		int n = callCount.incrementAndGet();
		log.info("[cron-mock][{}] called#{}, op={}, args={}", ALIAS, n, "scard", new Object[] {key});
		return 0L;
	}

	@Override
	public void expireAt(String key, long epochSeconds) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][{}] called#{}, op={}, args={}", ALIAS, n, "expireAt", new Object[] {key, epochSeconds});
	}

	@Override
	public Map<String, String> hgetall(String key) {
		int n = callCount.incrementAndGet();
		log.info("[cron-mock][{}] called#{}, op={}, args={}", ALIAS, n, "hgetall", new Object[] {key});
		return Collections.emptyMap();
	}

	@Override
	public String get(String key) {
		int n = callCount.incrementAndGet();
		log.info("[cron-mock][{}] called#{}, op=get, args={}", ALIAS, n, new Object[] {key});
		return null;
	}
}
