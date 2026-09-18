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
import cn.shopex.ecshopx.common.port.order.PointsmallPartialCancelItemStorePort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.orderlog.PointsmallScheduleCancelOrderProcessLogEntities;
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointsmallNormalOrderService {

	private static final int PAGE_SIZE = 20;

	private final NormalOrdersMapper normalOrdersMapper;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final PointsmallPartialCancelItemStorePort pointsmallPartialCancelItemStorePort;
	private final PointMemberAddPointService pointMemberAddPointService;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;

	/**
	 * 自动取消超时未支付的积分商城单（仅 {@code order_class=pointsmall}，不含 {@code order_type} 条件）。
	 *
	 * @return 本次任务累计置为取消的订单行数
	 */
	public int scheduleCancelOrders() {
		long nowEpoch = System.currentTimeMillis() / 1000L;
		long autoCancelThreshold = nowEpoch + 60;

		long totalCount = normalOrdersMapper.selectCount(baseCancelQuery(autoCancelThreshold));
		if (totalCount == 0) {
			return 0;
		}

		int totalPage = (int) Math.ceil((double) totalCount / PAGE_SIZE);
		int cancelledCount = 0;

		for (int i = 1; i <= totalPage; i++) {
			List<NormalOrders> result = normalOrdersMapper.selectList(
					baseCancelQuery(autoCancelThreshold).last("LIMIT " + PAGE_SIZE));

			List<Long> orderIds = result.stream()
					.map(NormalOrders::getOrderId)
					.collect(Collectors.toList());

			if (orderIds.isEmpty()) {
				continue;
			}

			normalOrdersMapper.update(null,
					new LambdaUpdateWrapper<NormalOrders>()
							.in(NormalOrders::getOrderId, orderIds)
							.set(NormalOrders::getOrderStatus, "CANCEL"));
			orderAssociationsMapper.update(null,
					new LambdaUpdateWrapper<OrderAssociations>()
							.in(OrderAssociations::getOrderId, orderIds)
							.set(OrderAssociations::getOrderStatus, "CANCEL"));

			List<NormalOrdersItems> orderItems = normalOrdersItemsMapper.selectList(
					new LambdaQueryWrapper<NormalOrdersItems>()
							.in(NormalOrdersItems::getOrderId, orderIds));

			for (NormalOrders order : result) {
				long userId = order.getUserId() != null ? order.getUserId() : 0L;
				long companyId = order.getCompanyId() != null ? order.getCompanyId() : 0L;
				int points = order.getPoint() != null ? order.getPoint() : 0;
				pointMemberAddPointService.addPointForOrderCancelReturn(userId, companyId, points, order.getOrderId());
				orderProcessLogPublishPort.publish(
						PointsmallScheduleCancelOrderProcessLogEntities.buildForScheduleCancel(order));
			}

			for (NormalOrdersItems row : orderItems) {
				Long itemId = row.getItemId();
				if (itemId == null || itemId <= 0L) {
					continue;
				}
				int num = row.getNum() != null ? row.getNum() : 0;
				long companyId = row.getCompanyId() != null ? row.getCompanyId() : 0L;
				boolean ok = pointsmallPartialCancelItemStorePort.minusItemStore(companyId, itemId, -num, true);
				if (!ok) {
					log.warn("[scheduleCancelOrders-pointsmall] minusItemStore failed, orderId={}, itemId={}",
							row.getOrderId(), itemId);
				}
			}

			cancelledCount += result.size();
		}

		return cancelledCount;
	}

	private LambdaQueryWrapper<NormalOrders> baseCancelQuery(long autoCancelThreshold) {
		return new LambdaQueryWrapper<NormalOrders>()
				.apply("CAST(auto_cancel_time AS UNSIGNED) < {0}", autoCancelThreshold)
				.gt(NormalOrders::getAutoCancelTime, "0")
				.eq(NormalOrders::getOrderStatus, "NOTPAY")
				.eq(NormalOrders::getOrderClass, "pointsmall");
	}
}
