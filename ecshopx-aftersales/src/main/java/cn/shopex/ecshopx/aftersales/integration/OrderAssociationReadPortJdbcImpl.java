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

import cn.shopex.ecshopx.common.port.order.OrderAssociationReadPort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderAssociationReadPortJdbcImpl implements OrderAssociationReadPort {

	private final JdbcTemplate jdbcTemplate;

	public OrderAssociationReadPortJdbcImpl(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Optional<Map<String, Object>> getAssociation(long companyId, long orderId) {
		List<Map<String, Object>> rows =
				jdbcTemplate.queryForList(
						"SELECT order_id, company_id, user_id, order_type, order_class, order_status, shop_id "
								+ "FROM orders_associations WHERE company_id = ? AND order_id = ? LIMIT 1",
						companyId,
						orderId);
		if (rows.isEmpty()) {
			return Optional.empty();
		}
		Map<String, Object> src = rows.get(0);
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("order_id", src.get("order_id"));
		m.put("company_id", src.get("company_id"));
		m.put("user_id", src.get("user_id"));
		m.put("order_type", src.get("order_type"));
		m.put("order_class", src.get("order_class"));
		m.put("order_status", src.get("order_status"));
		m.put("shop_id", src.get("shop_id"));
		return Optional.of(m);
	}
}
