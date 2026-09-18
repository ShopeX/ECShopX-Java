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

import cn.shopex.ecshopx.common.port.order.OrderCancelSeckillTicketPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.orderlog.SeckillScheduleCancelOrderProcessLogEntities;
import cn.shopex.ecshopx.point.service.PointMemberCancelOrderReturnPointsService;
import cn.shopex.ecshopx.point.service.PointMemberMinusOrderUppointsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillNormalOrderService {

	private static final int PAGE_SIZE = 20;

	private final NormalOrdersMapper normalOrdersMapper;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final OrderCancelSeckillTicketPort orderCancelSeckillTicketPort;
	private final PointMemberCancelOrderReturnPointsService pointMemberCancelOrderReturnPointsService;
	private final PointMemberMinusOrderUppointsService pointMemberMinusOrderUppointsService;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;

	/**
	 * 自动取消超时未支付的秒杀实体普通单（order_class=seckill, order_type=normal）。
	 * @return 本次累计取消的订单行数
	 */
	public int scheduleCancelOrders() {
		log.info("[scheduleCancelOrders-seckill] 自动取消秒杀未支付订单");

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

			for (NormalOrdersItems row : orderItems) {
				Long actId = row.getActId();
				Long itemId = row.getItemId();
				if (actId == null || itemId == null) {
					continue;
				}
				long companyId = row.getCompanyId() != null ? row.getCompanyId() : 0L;
				long userId = row.getUserId() != null ? row.getUserId() : 0L;
				int qty = row.getNum() != null ? row.getNum() : 0;
				orderCancelSeckillTicketPort.restoreUserBuysStore(companyId, userId, actId, itemId, qty);
			}

			for (NormalOrders orderData : result) {
				Map<String, Object> orderDataMap = buildOrderDataMap(orderData);
				pointMemberCancelOrderReturnPointsService.cancelOrderReturnBackPoints(orderDataMap);
				if (orderData.getUppointUse() != null && orderData.getUppointUse() > 0) {
					pointMemberMinusOrderUppointsService.minusOrderUppoints(orderDataMap);
				}
				orderProcessLogPublishPort.publish(SeckillScheduleCancelOrderProcessLogEntities.buildForScheduleCancel(orderData));
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
				.eq(NormalOrders::getOrderClass, "seckill")
				.eq(NormalOrders::getOrderType, "normal");
	}

	private Map<String, Object> buildOrderDataMap(NormalOrders order) {
		Map<String, Object> data = new HashMap<>();
		data.put("order_id", order.getOrderId());
		data.put("company_id", order.getCompanyId());
		data.put("user_id", order.getUserId());
		data.put("point_use", order.getPointUse() != null ? order.getPointUse() : 0);
		data.put("uppoint_use", order.getUppointUse() != null ? order.getUppointUse() : 0);
		data.put("pay_type", order.getPayType() != null ? order.getPayType() : "");
		return data;
	}
}
