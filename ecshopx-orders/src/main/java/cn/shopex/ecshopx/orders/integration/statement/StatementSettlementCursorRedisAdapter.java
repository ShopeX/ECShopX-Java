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

package cn.shopex.ecshopx.orders.integration.statement;

import cn.shopex.ecshopx.common.cron.statement.StatementSettlementCursorRedisPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 生产环境使用 {@link StringRedisTemplate} 读写结算游标，键与 PHP 一致。
 */
@Component
@Profile("!test-cron")
@RequiredArgsConstructor
public class StatementSettlementCursorRedisAdapter implements StatementSettlementCursorRedisPort {

	private final StringRedisTemplate stringRedisTemplate;

	@Override
	public Optional<String> getDistributorLastEnd(long companyId, long distributorId) {
		String key = "generate_statements_last_end_time:" + companyId + "_" + distributorId;
		String v = stringRedisTemplate.opsForValue().get(key);
		return Optional.ofNullable(v);
	}

	@Override
	public void setDistributorLastEnd(long companyId, long distributorId, String epochOrPayload) {
		String key = "generate_statements_last_end_time:" + companyId + "_" + distributorId;
		stringRedisTemplate.opsForValue().set(key, epochOrPayload);
	}

	@Override
	public Optional<String> getSupplierLastEnd(long companyId, long supplierTableId) {
		String key = "supplier_statements_last_end_time:" + companyId + "_" + supplierTableId;
		String v = stringRedisTemplate.opsForValue().get(key);
		return Optional.ofNullable(v);
	}

	@Override
	public void setSupplierLastEnd(long companyId, long supplierTableId, String payload) {
		String key = "supplier_statements_last_end_time:" + companyId + "_" + supplierTableId;
		stringRedisTemplate.opsForValue().set(key, payload);
	}
}
