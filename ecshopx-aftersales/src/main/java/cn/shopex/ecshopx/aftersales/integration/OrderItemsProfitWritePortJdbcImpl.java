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

package cn.shopex.ecshopx.aftersales.integration;

import cn.shopex.ecshopx.common.port.order.OrderItemsProfitWritePort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderItemsProfitWritePortJdbcImpl implements OrderItemsProfitWritePort {

	private final JdbcTemplate jdbcTemplate;

	public OrderItemsProfitWritePortJdbcImpl(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void resetOrderProfitStatusByItem(long companyId, long orderId, long itemId) {
		jdbcTemplate.update(
				"UPDATE orders_items_rel_profit SET order_profit_status = 0 WHERE company_id = ? AND order_id = ? AND item_id = ?",
				companyId,
				orderId,
				itemId);
	}
}
