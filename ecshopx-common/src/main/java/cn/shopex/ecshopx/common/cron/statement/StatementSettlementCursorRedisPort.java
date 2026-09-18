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

package cn.shopex.ecshopx.common.cron.statement;

import java.util.Optional;

/**
 * 结算单调度游标在 Redis 中的读写端口；与 PHP
 * {@code generate_statements_last_end_time:*} / {@code supplier_statements_last_end_time:*} 键规约一致。
 */
public interface StatementSettlementCursorRedisPort {

	Optional<String> getDistributorLastEnd(long companyId, long distributorId);

	void setDistributorLastEnd(long companyId, long distributorId, String epochOrPayload);

	Optional<String> getSupplierLastEnd(long companyId, long supplierTableId);

	void setSupplierLastEnd(long companyId, long supplierTableId, String payload);
}
