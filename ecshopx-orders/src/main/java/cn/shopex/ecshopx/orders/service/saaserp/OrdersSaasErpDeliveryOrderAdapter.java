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

package cn.shopex.ecshopx.orders.service.saaserp;

import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpDeliveryOrderPort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderDetailPayloadMaps;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class OrdersSaasErpDeliveryOrderAdapter implements SaasErpDeliveryOrderPort {

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;

	public OrdersSaasErpDeliveryOrderAdapter(
			NormalOrdersMapper normalOrdersMapper, NormalOrdersItemsMapper normalOrdersItemsMapper) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
	}

	@Override
	public Optional<Map<String, Object>> load(long companyId, String orderId) {
		Long parsedOrderId = parseOrderId(orderId);
		if (parsedOrderId == null) {
			return Optional.empty();
		}
		LambdaQueryWrapper<NormalOrders> orderQuery = new LambdaQueryWrapper<>();
		orderQuery.eq(NormalOrders::getOrderId, parsedOrderId);
		if (companyId > 0) {
			orderQuery.eq(NormalOrders::getCompanyId, companyId);
		}
		orderQuery.last("LIMIT 1");
		NormalOrders tradeInfo = normalOrdersMapper.selectOne(orderQuery);
		if (tradeInfo == null) {
			return Optional.empty();
		}

		long itemCompanyId = companyId > 0 ? companyId : tradeInfo.getCompanyId();
		List<NormalOrdersItems> itemRows =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, itemCompanyId)
								.eq(NormalOrdersItems::getOrderId, parsedOrderId)
								.orderByAsc(NormalOrdersItems::getId));
		if (itemRows == null) {
			return Optional.empty();
		}

		Map<String, Object> order = new LinkedHashMap<>();
		order.put("delivery_status", tradeInfo.getDeliveryStatus());
		order.put("order_id", tradeInfo.getOrderId());
		order.put("company_id", tradeInfo.getCompanyId());
		List<Map<String, Object>> items = new ArrayList<>();
		for (NormalOrdersItems row : itemRows) {
			items.add(AdminOrderDetailPayloadMaps.itemToMap(row));
		}
		order.put("items", items);
		return Optional.of(order);
	}

	private static Long parseOrderId(String orderId) {
		if (orderId == null || orderId.isEmpty()) {
			return null;
		}
		try {
			return Long.valueOf(orderId.trim());
		} catch (NumberFormatException ex) {
			return null;
		}
	}
}
