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

import cn.shopex.ecshopx.common.inventory.ItemInventoryLineContext;
import cn.shopex.ecshopx.common.port.order.OrderCancelItemStoreRestorePort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.order.PartialCancelPromotionRestorePort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.orderlog.GroupsNormalScheduleCancelOrderProcessLogEntities;
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
public class GroupsOrderService {

	private static final int PAGE_SIZE = 20;

	private final NormalOrdersMapper normalOrdersMapper;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final OrderCancelItemStoreRestorePort orderCancelItemStoreRestorePort;
	private final PartialCancelPromotionRestorePort partialCancelPromotionRestorePort;
	private final PointMemberCancelOrderReturnPointsService pointMemberCancelOrderReturnPointsService;
	private final PointMemberMinusOrderUppointsService pointMemberMinusOrderUppointsService;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;

	/**
	 * 自动取消超时未支付的拼团订单（order_class = 'groups'）。
	 * 每分钟执行，先统计满足条件的总数再逐页批量取消，同步恢复商品库存、拼团活动库存、积分，并发布流程日志事件。
	 *
	 * @return 本次共取消的订单数
	 */
	public int scheduleCancelOrders() {
		long threshold = System.currentTimeMillis() / 1000L + 60;

		long totalCount = normalOrdersMapper.selectCount(
				new LambdaQueryWrapper<NormalOrders>()
						.apply("CAST(auto_cancel_time AS UNSIGNED) < {0}", threshold)
						.eq(NormalOrders::getOrderStatus, "NOTPAY")
						.eq(NormalOrders::getOrderClass, "groups"));

		if (totalCount == 0) {
			return 0;
		}

		int totalPage = (int) Math.ceil((double) totalCount / PAGE_SIZE);
		int cancelledCount = 0;

		for (int i = 1; i <= totalPage; i++) {
			List<NormalOrders> result = normalOrdersMapper.selectList(
					new LambdaQueryWrapper<NormalOrders>()
							.apply("CAST(auto_cancel_time AS UNSIGNED) < {0}", threshold)
							.eq(NormalOrders::getOrderStatus, "NOTPAY")
							.eq(NormalOrders::getOrderClass, "groups")
							.last("LIMIT " + PAGE_SIZE));

			List<Long> orderIds = result.stream()
					.map(NormalOrders::getOrderId)
					.collect(Collectors.toList());

			if (orderIds.isEmpty()) {
				continue;
			}

			// 3-3: 查询订单明细（先于状态更新，确保能取到 NOTPAY 状态下的明细）
			List<NormalOrdersItems> orderItems = normalOrdersItemsMapper.selectList(
					new LambdaQueryWrapper<NormalOrdersItems>()
							.in(NormalOrdersItems::getOrderId, orderIds));

			// 3-4-A: 批量更新主单状态为 CANCEL
			normalOrdersMapper.update(null,
					new LambdaUpdateWrapper<NormalOrders>()
							.in(NormalOrders::getOrderId, orderIds)
							.set(NormalOrders::getOrderStatus, "CANCEL"));

			// 3-4-B: 批量更新关联单状态为 CANCEL
			orderAssociationsMapper.update(null,
					new LambdaUpdateWrapper<OrderAssociations>()
							.in(OrderAssociations::getOrderId, orderIds)
							.set(OrderAssociations::getOrderStatus, "CANCEL"));

			// 3-4-C: 逐 item 恢复商品库存与拼团活动库存
			Map<Long, String> receiptTypeByOrderId = result.stream()
					.collect(Collectors.toMap(NormalOrders::getOrderId, o -> o.getReceiptType() != null ? o.getReceiptType() : "", (a, b) -> a));
			for (NormalOrdersItems item : orderItems) {
				if (item.getItemId() == null || item.getNum() == null || item.getNum() <= 0) {
					continue;
				}

				// C-1: 恢复商品库存
				long orderIdForItem = item.getOrderId() != null ? item.getOrderId() : 0L;
				String receiptType = receiptTypeByOrderId.getOrDefault(orderIdForItem, "");
				orderCancelItemStoreRestorePort.restoreStore(
						ItemInventoryLineContext.fromOrderLine(
								item.getCompanyId(),
								item.getItemId(),
								item.getSupplierId(),
								item.getDistributorId(),
								item.getIsTotalStore(),
								receiptType),
						item.getNum());

				// C-2 + C-3: 恢复拼团活动库存（impl 内部查 promotion_groups_team_member → actId → Redis + DB）
				long orderUserId = item.getUserId() != null ? item.getUserId() : 0L;
				long orderId = item.getOrderId() != null ? item.getOrderId() : 0L;
				partialCancelPromotionRestorePort.restoreGroupItemStore(
						item.getCompanyId(), orderId, orderUserId, item.getItemId(), item.getNum());
			}

			// 3-4-D: 逐 order 处理积分与流程日志
			for (NormalOrders order : result) {
				// D-1: 返还积分（条件：point_use > 0 && pay_type != 'point'）
				if (order.getPointUse() != null && order.getPointUse() > 0
						&& !"point".equals(order.getPayType())) {
					Map<String, Object> pointData = buildOrderDataMap(order);
					pointMemberCancelOrderReturnPointsService.cancelOrderReturnBackPoints(pointData);
				}

				// D-2: 回退上分（条件：uppoint_use > 0，仅写 Redis）
				if (order.getUppointUse() != null && order.getUppointUse() > 0) {
					Map<String, Object> upointData = buildOrderDataMap(order);
					upointData.put("uppoint_use", order.getUppointUse());
					pointMemberMinusOrderUppointsService.minusOrderUppoints(upointData);
				}

				// D-3: 发布流程日志（经统一派发总线，由 OrderProcessLogDispatchListener 落库）
				orderProcessLogPublishPort.publish(
						GroupsNormalScheduleCancelOrderProcessLogEntities.buildForScheduleCancel(order));
			}

			cancelledCount += result.size();
		}

		return cancelledCount;
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
