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

package cn.shopex.ecshopx.aftersales.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Atomic deduct of {@code orders_normal_orders.left_aftersales_num}. */
public final class NormalOrderLeftAftersalesNumDeduct {

	static final String SQL =
			"UPDATE orders_normal_orders SET left_aftersales_num = left_aftersales_num - ?"
					+ " WHERE company_id = ? AND order_id = ? AND left_aftersales_num >= ?";

	private NormalOrderLeftAftersalesNumDeduct() {}

	public static void deductOrThrow(
			JdbcTemplate jdbcTemplate, long companyId, long orderId, int applyNum) {
		int affected = jdbcTemplate.update(SQL, applyNum, companyId, orderId, applyNum);
		if (affected == 0) {
			throw new ResourceException("可申请售后数量不足");
		}
	}
}
