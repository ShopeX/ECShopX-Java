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

import cn.shopex.ecshopx.common.port.wdterp.WdtErpInventoryPort;
import cn.shopex.ecshopx.common.port.wdterp.dto.WdtInventoryWaitSyncPage;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * 旺店通库存同步 HTTP 的 Noop，用于 test-cron；bean-alias 为 {@code wdt-inventory}。
 */
@Slf4j
public class NoopWdtErpInventoryPort implements WdtErpInventoryPort {

	private static final String ALIAS = "wdt-inventory";
	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public WdtInventoryWaitSyncPage fetchWaitSyncPage(
			long companyId, int position, String sid, String appKey, String appSecret) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][{}] called#{}, method={}, args={}",
				ALIAS,
				n,
				"fetchWaitSyncPage",
				Arrays.toString(new Object[] {companyId, position, sid, appKey, appSecret}));
		return new WdtInventoryWaitSyncPage(Collections.emptyList(), position);
	}

	@Override
	public Map<String, Object> queryStore(
			long companyId, String recId, String sid, String appKey, String appSecret) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][{}] called#{}, method={}, args={}",
				ALIAS, n, "queryStore", Arrays.toString(new Object[] {companyId, recId, sid, appKey, appSecret}));
		return Collections.emptyMap();
	}

	@Override
	public void acknowledgeSuccess(
			long companyId, String recId, Map<String, Object> stockInfo, String sid, String appKey, String appSecret) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][{}] called#{}, method={}, args={}",
				ALIAS,
				n,
				"acknowledgeSuccess",
				Arrays.toString(new Object[] {companyId, recId, stockInfo, sid, appKey, appSecret}));
	}

	@Override
	public void acknowledgeFail(
			long companyId, String recId, Map<String, Object> stockInfo, String sid, String appKey, String appSecret) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][{}] called#{}, method={}, args={}",
				ALIAS, n, "acknowledgeFail", Arrays.toString(new Object[] {companyId, recId, stockInfo, sid, appKey, appSecret}));
	}
}
