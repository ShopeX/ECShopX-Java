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

package cn.shopex.ecshopx.orders.service;

import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.ServiceOrders;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.ServiceOrdersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupsServiceOrderService {

	private static final int PAGE_SIZE = 20;

	private final ServiceOrdersMapper serviceOrdersMapper;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;

	/**
	 * 自动取消超时未支付的服务类拼团订单（order_class = 'groups'）。
	 * 每分钟执行，先统计满足条件的总数再逐页批量取消，并发布流程日志事件。
	 *
	 * @return 本次共取消的订单数
	 */
	public int scheduleCancelOrders() {
		long threshold = System.currentTimeMillis() / 1000L + 60;

		long totalCount = serviceOrdersMapper.selectCount(
				new LambdaQueryWrapper<ServiceOrders>()
						.apply("CAST(auto_cancel_time AS UNSIGNED) < {0}", threshold)
						.eq(ServiceOrders::getOrderStatus, "NOTPAY")
						.eq(ServiceOrders::getOrderClass, "groups"));

		if (totalCount == 0) {
			return 0;
		}

		int totalPage = (int) Math.ceil((double) totalCount / PAGE_SIZE);
		int cancelledCount = 0;

		for (int i = 1; i <= totalPage; i++) {
			List<ServiceOrders> result = serviceOrdersMapper.selectList(
					new LambdaQueryWrapper<ServiceOrders>()
							.apply("CAST(auto_cancel_time AS UNSIGNED) < {0}", threshold)
							.eq(ServiceOrders::getOrderStatus, "NOTPAY")
							.eq(ServiceOrders::getOrderClass, "groups")
							.last("LIMIT " + PAGE_SIZE));

			List<Long> orderIds = result.stream()
					.map(ServiceOrders::getOrderId)
					.collect(Collectors.toList());

			if (orderIds.isEmpty()) {
				continue;
			}

			serviceOrdersMapper.update(null,
					new LambdaUpdateWrapper<ServiceOrders>()
							.in(ServiceOrders::getOrderId, orderIds)
							.set(ServiceOrders::getOrderStatus, "CANCEL"));

			orderAssociationsMapper.update(null,
					new LambdaUpdateWrapper<OrderAssociations>()
							.in(OrderAssociations::getOrderId, orderIds)
							.set(OrderAssociations::getOrderStatus, "CANCEL"));

			for (ServiceOrders order : result) {
				orderProcessLogPublishPort.publish(buildScheduleCancelServiceGroupsOrderProcessLogEntities(order));
			}

			cancelledCount += result.size();
		}

		return cancelledCount;
	}

	private Map<String, Object> buildScheduleCancelServiceGroupsOrderProcessLogEntities(ServiceOrders order) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("order_id", order.getOrderId());
		m.put("company_id", order.getCompanyId());
		m.put("operator_type", "system");
		m.put("operator_id", 0L);
		m.put("remarks", "订单取消");
		m.put("detail", "订单单号：" + order.getOrderId() + "，取消订单退款");
		m.put("supplier_id", 0L);
		m.put("is_show", true);
		return m;
	}
}
