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

import cn.shopex.ecshopx.common.cron.statement.StatementSettlementCursorRedisPort;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * 阶段 4 test-cron 下用的内存 Redis 端口，保留同进程 read-after-write 语义。
 */
@Slf4j
public class NoopStatementSettlementCursorRedis implements StatementSettlementCursorRedisPort {

	private static final String ALIAS = "statement-cursor-redis";
	private final AtomicInteger callCount = new AtomicInteger();
	private final Map<String, String> store = new ConcurrentHashMap<>();

	@Override
	public Optional<String> getDistributorLastEnd(long companyId, long distributorId) {
		return get(keyDistributor(companyId, distributorId));
	}

	@Override
	public void setDistributorLastEnd(long companyId, long distributorId, String epochOrPayload) {
		set(keyDistributor(companyId, distributorId), epochOrPayload);
	}

	@Override
	public Optional<String> getSupplierLastEnd(long companyId, long supplierTableId) {
		return get(keySupplier(companyId, supplierTableId));
	}

	@Override
	public void setSupplierLastEnd(long companyId, long supplierTableId, String payload) {
		set(keySupplier(companyId, supplierTableId), payload);
	}

	private Optional<String> get(String key) {
		int n = callCount.incrementAndGet();
		String op = "get";
		String v = store.get(key);
		log.info(
				"[cron-mock][{}] called#{}, op={}, key={}, hit={}", ALIAS, n, op, key, v != null);
		return Optional.ofNullable(v);
	}

	private void set(String key, String value) {
		int n = callCount.incrementAndGet();
		String op = "set";
		store.put(key, value);
		log.info(
				"[cron-mock][{}] called#{}, op={}, key={}, value={}", ALIAS, n, op, key, value);
	}

	private static String keyDistributor(long companyId, long distributorId) {
		return "generate_statements_last_end_time:" + companyId + "_" + distributorId;
	}

	private static String keySupplier(long companyId, long supplierTableId) {
		return "supplier_statements_last_end_time:" + companyId + "_" + supplierTableId;
	}
}
