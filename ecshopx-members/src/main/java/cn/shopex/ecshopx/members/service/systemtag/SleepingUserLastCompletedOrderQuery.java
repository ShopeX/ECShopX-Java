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

package cn.shopex.ecshopx.members.service.systemtag;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SleepingUserLastCompletedOrderQuery {

	private static final String MAX_END_SQL =
			"""
			SELECT MAX(o.end_time)
			FROM orders_normal_orders o
			WHERE o.company_id = ?
			  AND o.user_id = ?
			  AND o.order_status = 'DONE'
			  AND o.end_time IS NOT NULL
			  AND o.end_time > 0
			""";

	private static final String LIST_BEFORE_SQL =
			"""
			SELECT o.user_id
			FROM orders_normal_orders o
			WHERE o.company_id = ?
			  AND o.order_status = 'DONE'
			  AND o.end_time IS NOT NULL
			  AND o.end_time > 0
			  AND o.user_id IS NOT NULL
			  AND o.user_id > 0
			GROUP BY o.user_id
			HAVING MAX(o.end_time) < ?
			ORDER BY o.user_id
			LIMIT ? OFFSET ?
			""";

	private final JdbcTemplate jdbcTemplate;

	public SleepingUserLastCompletedOrderQuery(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Long findLastCompletedEndTime(long companyId, long userId) {
		Long v = jdbcTemplate.queryForObject(MAX_END_SQL, Long.class, companyId, userId);
		if (v == null || v <= 0L) {
			return null;
		}
		return v;
	}

	public List<Long> listUserIdsLastCompletedBefore(
			long companyId, long beforeEpochSec, long offset, int limit) {
		if (limit <= 0) {
			return List.of();
		}
		long safeOffset = Math.max(0L, offset);
		List<Long> rows =
				jdbcTemplate.query(
						LIST_BEFORE_SQL,
						(rs, rowNum) -> rs.getLong("user_id"),
						companyId,
						beforeEpochSec,
						limit,
						safeOffset);
		return rows == null ? List.of() : rows;
	}
}
