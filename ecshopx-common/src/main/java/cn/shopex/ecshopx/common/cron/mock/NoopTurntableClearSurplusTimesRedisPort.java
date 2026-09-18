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

import cn.shopex.ecshopx.common.cron.port.TurntableClearSurplusTimesRedisPort;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * 阶段 4 对拍用；bean-alias 与 plan §4 {@code turntable-clear-surplus-redis} 一致。
 */
@Slf4j
public class NoopTurntableClearSurplusTimesRedisPort implements TurntableClearSurplusTimesRedisPort {

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public void deleteEntireSurplusTimesKey(long companyId) {
		int n = callCount.incrementAndGet();
		String key = TurntableClearSurplusTimesRedisPort.SURPLUS_KEY_PREFIX + companyId;
		log.info(
				"[cron-mock][turntable-clear-surplus-redis] called#{}, op=deleteEntire, companyId={}, key={}",
				n,
				companyId,
				key);
	}
}
