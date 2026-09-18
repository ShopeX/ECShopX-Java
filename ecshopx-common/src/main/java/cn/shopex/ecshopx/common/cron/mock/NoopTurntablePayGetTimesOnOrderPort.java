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

import cn.shopex.ecshopx.common.cron.port.TurntablePayGetTimesOnOrderPort;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * 阶段 4 对拍用；日志 alias 与 plan §4 {@code turntable-pay-times-redis} 一致。
 */
@Slf4j
public class NoopTurntablePayGetTimesOnOrderPort implements TurntablePayGetTimesOnOrderPort {

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public void payGetTimes(long userId, long companyId, int totalFeeFen) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][turntable-pay-times-redis] called#{}, args={}",
				n,
				Arrays.toString(new Object[] {userId, companyId, totalFeeFen}));
	}
}
