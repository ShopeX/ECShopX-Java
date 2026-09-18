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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.orders.domain.OrdersDelivery;
import cn.shopex.ecshopx.orders.mapper.OrdersDeliveryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxappOrderDetailDeliveryInfoService {

	private final OrdersDeliveryMapper ordersDeliveryMapper;

	public WxappOrderDetailDeliveryInfoService(OrdersDeliveryMapper ordersDeliveryMapper) {
		this.ordersDeliveryMapper = ordersDeliveryMapper;
	}

	public void appendDeliveryInfo(long companyId, long orderIdNum, Map<String, Object> result) {
		@SuppressWarnings("unchecked")
		Map<String, Object> orderInfo = (Map<String, Object>) result.get("orderInfo");
		if (orderInfo == null) {
			return;
		}
		List<OrdersDelivery> rows =
				ordersDeliveryMapper.selectList(
						new LambdaQueryWrapper<OrdersDelivery>()
								.eq(OrdersDelivery::getCompanyId, companyId)
								.eq(OrdersDelivery::getOrderId, orderIdNum)
								.orderByAsc(OrdersDelivery::getOrdersDeliveryId));
		List<Map<String, Object>> deliveryInfo = new ArrayList<>();
		for (OrdersDelivery v : rows) {
			Map<String, Object> line = new LinkedHashMap<>();
			line.put("delivery_code", v.getDeliveryCode());
			line.put("delivery_corp", v.getDeliveryCorp());
			line.put("delivery_corp_name", v.getDeliveryCorpName());
			line.put("delivery_time", v.getDeliveryTime());
			deliveryInfo.add(line);
		}
		orderInfo.put("delivery_info", deliveryInfo);
	}
}
