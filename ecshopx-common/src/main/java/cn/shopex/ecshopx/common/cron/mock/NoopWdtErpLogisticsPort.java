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

import cn.shopex.ecshopx.common.port.wdterp.WdtErpLogisticsPort;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * 旺店通物流 HTTP 的 Noop，用于 test-cron；bean-alias 为 {@code wdt-logistics}。
 */
@Slf4j
public class NoopWdtErpLogisticsPort implements WdtErpLogisticsPort {

	private static final String ALIAS = "wdt-logistics";
	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public List<Map<String, Object>> getWaitSyncPage(
			long companyId, String shopNo, int pageNo, String sid, String appKey, String appSecret) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][{}] called#{}, method={}, args={}",
				ALIAS,
				n,
				"getWaitSyncPage",
				Arrays.toString(new Object[] {companyId, shopNo, pageNo, sid, appKey, appSecret}));
		return Collections.emptyList();
	}

	@Override
	public void acknowledgeSync(
			long companyId, List<Map<String, Object>> items, String sid, String appKey, String appSecret) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][{}] called#{}, method={}, args={}",
				ALIAS,
				n,
				"acknowledgeSync",
				Arrays.toString(new Object[] {companyId, items, sid, appKey, appSecret}));
	}
}
