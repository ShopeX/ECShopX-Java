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

import cn.shopex.ecshopx.common.cron.SalespersonStatisticsCronLogKind;
import cn.shopex.ecshopx.common.cron.SalespersonStatisticsCronLogPort;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * test-cron 下替换 Slf4j 调试日志；阶段 4 可 grep {@code [cron-mock][salesperson-stats-debug-log]}。
 */
@Slf4j
public class NoopSalespersonStatisticsCronLogPort implements SalespersonStatisticsCronLogPort {

	public static final String ALIAS = "salesperson-stats-debug-log";

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public void debugStart(SalespersonStatisticsCronLogKind kind, long companyId, long salespersonId) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][{}] called#{}, kind={}, phase={}, companyId={}, salespersonId={}, error={}",
				ALIAS,
				n,
				kind,
				"start",
				companyId,
				salespersonId,
				null);
	}

	@Override
	public void debugError(
			SalespersonStatisticsCronLogKind kind, long companyId, long salespersonId, Throwable error) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][{}] called#{}, kind={}, phase={}, companyId={}, salespersonId={}, error={}",
				ALIAS,
				n,
				kind,
				"error",
				companyId,
				salespersonId,
				error == null ? null : error.getClass().getName());
	}

	@Override
	public void debugEnd(SalespersonStatisticsCronLogKind kind, long companyId, long salespersonId) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][{}] called#{}, kind={}, phase={}, companyId={}, salespersonId={}, error={}",
				ALIAS,
				n,
				kind,
				"end",
				companyId,
				salespersonId,
				null);
	}
}
