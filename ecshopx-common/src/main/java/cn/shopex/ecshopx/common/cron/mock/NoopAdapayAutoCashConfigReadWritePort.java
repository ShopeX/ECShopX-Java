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

import cn.shopex.ecshopx.common.port.adapay.AdapayAutoCashConfigReadWritePort;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * 阶段 4 不连业务 Redis 时覆盖真实端口；与 {@code [cron-mock][adapay-auto-cash-config]} 约定格式打点。
 */
@Slf4j
public class NoopAdapayAutoCashConfigReadWritePort implements AdapayAutoCashConfigReadWritePort {

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public Map<String, Object> getAutoCashConfig(long companyId) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][adapay-auto-cash-config] called#{}, op=get, companyId={}",
				n, companyId);
		return Collections.emptyMap();
	}

	@Override
	public void putAutoCashConfig(long companyId, Map<String, Object> config) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][adapay-auto-cash-config] called#{}, op=put, companyId={}, config={}",
				n,
				companyId,
				Arrays.toString(new Object[] {config}));
	}
}
