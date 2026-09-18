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

package cn.shopex.ecshopx.orders.service.normal;

import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrdersRelChinaumspayDivision;
import cn.shopex.ecshopx.orders.mapper.OrdersRelChinaumspayDivisionMapper;
import org.springframework.stereotype.Service;

@Service
public class OrdersRelChinaumspayDivisionWriteService {

	private final OrdersRelChinaumspayDivisionMapper ordersRelChinaumspayDivisionMapper;

	public OrdersRelChinaumspayDivisionWriteService(OrdersRelChinaumspayDivisionMapper ordersRelChinaumspayDivisionMapper) {
		this.ordersRelChinaumspayDivisionMapper = ordersRelChinaumspayDivisionMapper;
	}

	public void addRelChinaumsPayDivision(long companyId, NormalOrders order) {
		if (companyId <= 0L || order == null || order.getOrderId() == null || order.getOrderId() <= 0L) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		OrdersRelChinaumspayDivision row = new OrdersRelChinaumspayDivision();
		row.setOrderId(order.getOrderId());
		row.setCompanyId(companyId);
		row.setStatus("0");
		row.setCreateTime(now);
		row.setUpdateTime(now);
		ordersRelChinaumspayDivisionMapper.insert(row);
	}
}
