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

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class NewCustomerEffectivePaidOrderQuery {

	private static final String COUNT_SQL =
			"""
			SELECT COUNT(1)
			FROM orders_normal_orders o
			WHERE o.company_id = ?
			  AND o.user_id = ?
			  AND o.pay_status = 'PAYED'
			  AND (o.cancel_status IS NULL OR o.cancel_status <> 'SUCCESS')
			  AND (
			    CAST(IFNULL(o.total_fee, '0') AS UNSIGNED) = 0
			    OR CAST(IFNULL(o.total_fee, '0') AS UNSIGNED) > IFNULL((
			      SELECT SUM(CAST(IFNULL(r.refunded_fee, 0) AS UNSIGNED))
			      FROM aftersales_refund r
			      WHERE r.company_id = o.company_id
			        AND r.order_id = o.order_id
			        AND r.refund_status = 'SUCCESS'
			    ), 0)
			  )
			""";

	private static final String USER_BY_ORDER_SQL =
			"""
			SELECT user_id
			FROM orders_normal_orders
			WHERE company_id = ? AND order_id = ?
			LIMIT 1
			""";

	private final JdbcTemplate jdbcTemplate;

	public NewCustomerEffectivePaidOrderQuery(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public long countEffectivePaid(long companyId, long userId) {
		Long n = jdbcTemplate.queryForObject(COUNT_SQL, Long.class, companyId, userId);
		return n == null ? 0L : n;
	}

	public Long findUserIdByOrder(long companyId, long orderId) {
		return jdbcTemplate.query(
				USER_BY_ORDER_SQL,
				rs -> {
					if (!rs.next()) {
						return null;
					}
					long uid = rs.getLong("user_id");
					return uid > 0L ? uid : null;
				},
				companyId,
				orderId);
	}
}
