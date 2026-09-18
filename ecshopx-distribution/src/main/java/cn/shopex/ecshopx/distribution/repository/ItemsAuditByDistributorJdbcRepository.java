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

package cn.shopex.ecshopx.distribution.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Batch-updates {@code items.audit_status} for a distributor without depending on ecshopx-goods (avoids a Maven cycle).
 */
@Repository
public class ItemsAuditByDistributorJdbcRepository {

	private final JdbcTemplate jdbcTemplate;

	public ItemsAuditByDistributorJdbcRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void updateAuditStatusApprovedByDistributorId(long companyId, long distributorId) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		jdbcTemplate.update(
				"UPDATE items SET audit_status = ?, updated = ? WHERE company_id = ? AND distributor_id = ?",
				"approved",
				now,
				companyId,
				(int) distributorId);
	}
}
