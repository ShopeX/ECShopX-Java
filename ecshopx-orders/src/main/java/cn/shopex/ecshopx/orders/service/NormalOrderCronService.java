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

import cn.shopex.ecshopx.common.port.order.EmployeePurchaseOrderCancelRestorePort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.inventory.ItemInventoryLineContext;
import cn.shopex.ecshopx.common.port.order.OrderCancelItemStoreRestorePort;
import cn.shopex.ecshopx.common.port.order.OrderCancelMarketingJoinCountPort;
import cn.shopex.ecshopx.common.port.order.OrderCancelSeckillTicketPort;
import cn.shopex.ecshopx.common.dispatch.ConsumptionOrderJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.FinishOrderJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.NormalOrderCancelDispatchPublisher;
import cn.shopex.ecshopx.common.port.order.LimitedTimeSaleQuotaFen;
import cn.shopex.ecshopx.common.port.order.PartialCancelOrderItemRow;
import cn.shopex.ecshopx.common.port.order.PartialCancelPromotionRestorePort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OfflinePayment;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.OrderPromotions;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OfflinePaymentMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.OrderPromotionsMapper;
import cn.shopex.ecshopx.orders.service.admin.NormalOrderCancelDiscountRestoreService;
import cn.shopex.ecshopx.orders.service.invoice.OrderInvoiceCancelOnOrderCancelService;
import cn.shopex.ecshopx.point.service.PointMemberCancelOrderReturnPointsService;
import cn.shopex.ecshopx.point.service.PointMemberMinusOrderUppointsService;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NormalOrderCronService {

	private static final int PAGE_SIZE = 20;

	private final NormalOrdersMapper normalOrdersMapper;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final OrderPromotionsMapper orderPromotionsMapper;
	private final SupplierOrderMapper supplierOrderMapper;
	private final OrderInvoiceCancelOnOrderCancelService orderInvoiceCancelOnOrderCancelService;
	private final PointMemberCancelOrderReturnPointsService pointMemberCancelOrderReturnPointsService;
	private final PointMemberMinusOrderUppointsService pointMemberMinusOrderUppointsService;
	private final EmployeePurchaseOrderCancelRestorePort employeePurchaseOrderCancelRestorePort;
	private final PartialCancelPromotionRestorePort partialCancelPromotionRestorePort;
	private final OrderCancelMarketingJoinCountPort orderCancelMarketingJoinCountPort;
	private final OrderCancelItemStoreRestorePort orderCancelItemStoreRestorePort;
	private final OrderCancelSeckillTicketPort orderCancelSeckillTicketPort;
	private final NormalOrderCancelDiscountRestoreService normalOrderCancelDiscountRestoreService;
	private final OfflinePaymentMapper offlinePaymentMapper;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;
	private final NormalOrderCancelDispatchPublisher normalOrderCancelDispatchPublisher;
	private final ConsumptionOrderJobDispatchPublisher consumptionOrderJobDispatchPublisher;
	private final FinishOrderJobDispatchPublisher finishOrderJobDispatchPublisher;

	@Value("${ecshopx.request-field.oem-shuyun:false}")
	private boolean oemShuyun;

	/**
	 * 售后期满消费累加：OEM（数云）场景下跳过；否则每 tick 向 slow 队列投递一次作业，由 worker
	 * 执行批处理。返回值 OEM 为 0；已发起投递为 1（与 {@link #scheduleFinishOrders()} 语义一致），不代表本
	 * tick 已标记订单数。
	 */
	public int scheduleConsumptionOrders() {
		if (oemShuyun) {
			log.info("[scheduleConsumptionOrders] oem-shuyun enabled, skip batch");
			return 0;
		}
		consumptionOrderJobDispatchPublisher.publish(Map.of("orderType", "normal", "pageSize", "100"));
		return 1;
	}

	/**
	 * 自动确认收货：每次调度将完成单批处理作业投递到异步队列，由 worker 执行等价于队列句柄的批处理逻辑。
	 *
	 * @return 每 tick 固定为 1，表示已发起一次投递；具体完成单数量在 worker 侧统计
	 */
	public int scheduleFinishOrders() {
		finishOrderJobDispatchPublisher.publish(Collections.emptyMap());
		return 1;
	}

	/**
	 * 自动取消超时未支付的普通订单。
	 * 每分钟执行，先统计总页数再逐页批量取消+处理售后逻辑。
	 * @return 本次共取消的订单数
	 */
	public int scheduleCancelOrders() {
		log.info("[scheduleCancelOrders] 自动取消订单");

		long nowEpoch = System.currentTimeMillis() / 1000L;
		long autoCancelThreshold = nowEpoch + 60;

		long totalCount = normalOrdersMapper.selectCount(
				new LambdaQueryWrapper<NormalOrders>()
						.apply("CAST(auto_cancel_time AS UNSIGNED) < {0}", autoCancelThreshold)
						.gt(NormalOrders::getAutoCancelTime, "0")
						.eq(NormalOrders::getOrderStatus, "NOTPAY")
						.in(NormalOrders::getOrderClass, List.of("normal", "shopguide", "employee_purchase"))
						.notIn(NormalOrders::getPayType, List.of("offline_pay", "point")));

		if (totalCount == 0) {
			return 0;
		}

		int totalPage = (int) Math.ceil((double) totalCount / PAGE_SIZE);
		int cancelledCount = 0;

		for (int i = 1; i <= totalPage; i++) {
			List<NormalOrders> result = normalOrdersMapper.selectList(
					new LambdaQueryWrapper<NormalOrders>()
							.apply("CAST(auto_cancel_time AS UNSIGNED) < {0}", autoCancelThreshold)
							.gt(NormalOrders::getAutoCancelTime, "0")
							.eq(NormalOrders::getOrderStatus, "NOTPAY")
							.in(NormalOrders::getOrderClass, List.of("normal", "shopguide", "employee_purchase"))
							.notIn(NormalOrders::getPayType, List.of("offline_pay", "point"))
							.last("LIMIT " + PAGE_SIZE));

			List<Long> orderIds = result.stream()
					.map(NormalOrders::getOrderId)
					.collect(Collectors.toList());

			if (orderIds.isEmpty()) {
				continue;
			}

			// 步骤 5-3-a: 批量更新主单和关联单状态为 CANCEL
			normalOrdersMapper.update(null,
					new LambdaUpdateWrapper<NormalOrders>()
							.in(NormalOrders::getOrderId, orderIds)
							.set(NormalOrders::getOrderStatus, "CANCEL"));
			orderAssociationsMapper.update(null,
					new LambdaUpdateWrapper<OrderAssociations>()
							.in(OrderAssociations::getOrderId, orderIds)
							.set(OrderAssociations::getOrderStatus, "CANCEL"));

			// 步骤 5-3-b: 查询订单明细
			List<NormalOrdersItems> orderItems = normalOrdersItemsMapper.selectList(
					new LambdaQueryWrapper<NormalOrdersItems>()
							.in(NormalOrdersItems::getOrderId, orderIds));

			// 步骤 5-3-c: 查询促销关联，按 activity_type 分类
			List<OrderPromotions> promotions = orderPromotionsMapper.selectList(
					new LambdaQueryWrapper<OrderPromotions>()
							.in(OrderPromotions::getMoid, orderIds));

			// limitedTimeSaleItemIds: 有限时特惠促销的商品，用于按单回补件数+金额额度
			Set<Long> limitedTimeSaleItemIds = new HashSet<>();
			// marketingActivityMap: userId -> (orderId -> activityId)
			Map<Long, Map<Long, Long>> marketingActivityMap = new HashMap<>();

			for (OrderPromotions p : promotions) {
				if ("limited_time_sale".equals(p.getActivityType()) && p.getItemId() != null) {
					limitedTimeSaleItemIds.add(p.getItemId());
				} else if ("marketing_activity".equals(p.getActivityType())
						|| "register_promotion".equals(p.getActivityType())) {
					if (p.getUserId() != null && p.getMoid() != null && p.getActivityId() != null) {
						marketingActivityMap
								.computeIfAbsent(p.getUserId(), k -> new HashMap<>())
								.put(p.getMoid(), p.getActivityId());
					}
				}
			}

			// 步骤 5-3-d: 逐单处理售后逻辑
			for (NormalOrders order : result) {
				long orderId = order.getOrderId();
				long companyId = order.getCompanyId();

				// 发票取消
				orderInvoiceCancelOnOrderCancelService.updateInvoiceStatusCancel(companyId, orderId, "cancel");

				// 供应商单取消
				long supplierCount = supplierOrderMapper.selectCount(
						new LambdaQueryWrapper<SupplierOrder>()
								.eq(SupplierOrder::getOrderId, orderId));
				if (supplierCount > 0) {
					supplierOrderMapper.update(null,
							new LambdaUpdateWrapper<SupplierOrder>()
									.eq(SupplierOrder::getOrderId, orderId)
									.set(SupplierOrder::getOrderStatus, "CANCEL"));
				}

				// 营销活动人数减一（Redis）
				Map<Long, Long> orderMarketingMap = marketingActivityMap.getOrDefault(order.getUserId(), Map.of());
				Long activityId = orderMarketingMap.get(orderId);
				if (activityId != null) {
					orderCancelMarketingJoinCountPort.lessJoinCount(companyId, order.getUserId(), activityId);
				}

				// 积分回退
				if (order.getPointUse() != null && order.getPointUse() > 0
						&& !"point".equals(order.getPayType())) {
					Map<String, Object> pointData = buildOrderDataMap(order);
					pointMemberCancelOrderReturnPointsService.cancelOrderReturnBackPoints(pointData);
				}

				// 上分回退
				if (order.getUppointUse() != null && order.getUppointUse() > 0) {
					Map<String, Object> upointData = buildOrderDataMap(order);
					upointData.put("uppoint_use", order.getUppointUse());
					pointMemberMinusOrderUppointsService.minusOrderUppoints(upointData);
				}

				// 流程日志：经统一投递端口 fan-out 为异步 listener
				orderProcessLogPublishPort.publish(buildScheduleCancelNormalOrderProcessLogEntities(order));

				publishNormalOrderCancelMarketingDispatch(orderId, companyId);

				// 内购活动恢复
				if ("employee_purchase".equals(order.getOrderClass())) {
					employeePurchaseOrderCancelRestorePort.restoreOnOrderCancel(orderId, companyId);
				}
			}

			// 步骤 5-3-e: 逐商品处理库存/限时特惠额度/限购
			Map<Long, String> receiptTypeByOrderId = result.stream()
					.collect(Collectors.toMap(NormalOrders::getOrderId, o -> o.getReceiptType() != null ? o.getReceiptType() : "", (a, b) -> a));
			restoreLimitedTimeSaleQuotasOnCancel(result, orderItems, limitedTimeSaleItemIds);
			for (NormalOrdersItems item : orderItems) {
				// 商品库存回补（DB + Redis）
				if (item.getItemId() != null && item.getNum() != null && item.getNum() > 0) {
					long orderId = item.getOrderId() != null ? item.getOrderId() : 0L;
					String receiptType = receiptTypeByOrderId.getOrDefault(orderId, "");
					orderCancelItemStoreRestorePort.restoreStore(
							ItemInventoryLineContext.fromOrderLine(
									item.getCompanyId(),
									item.getItemId(),
									item.getSupplierId(),
									item.getDistributorId(),
									item.getIsTotalStore(),
									receiptType),
							item.getNum());
				}

				// 限购恢复
				if (item.getItemId() != null && item.getNum() != null && item.getNum() > 0) {
					partialCancelPromotionRestorePort.reduceLimitPerson(
							item.getCompanyId(),
							item.getUserId() != null ? item.getUserId() : 0L,
							item.getItemId(),
							item.getNum());
				}
			}

			// 步骤 5-3-f: 优惠券/定向促销恢复
			for (NormalOrders order : result) {
				normalOrderCancelDiscountRestoreService.restoreOnAutoCancel(order);
			}

			cancelledCount += result.size();
		}

		return cancelledCount;
	}

	/**
	 * 自动取消超时未支付的线下转账订单（pay_type=offline_pay）。
	 * 仅处理 check_status IS NULL 或 check_status != 0 的订单，排除待审核状态。
	 * @return 本次共取消的订单数
	 */
	public int scheduleOfflinePayCancelOrders() {
		log.info("[scheduleOfflinePayCancelOrders] 自动取消线下支付超时未支付订单");

		long autoCancelThreshold = System.currentTimeMillis() / 1000L + 60;

		long totalCount = normalOrdersMapper.countOfflinePayCancelable(autoCancelThreshold);

		if (totalCount == 0) {
			return 0;
		}

		int totalPage = (int) Math.ceil((double) totalCount / PAGE_SIZE);
		int cancelledCount = 0;

		for (int i = 1; i <= totalPage; i++) {
			List<NormalOrders> result = normalOrdersMapper.selectOfflinePayCancelable(autoCancelThreshold, PAGE_SIZE);

			List<Long> orderIds = result.stream()
					.map(NormalOrders::getOrderId)
					.collect(Collectors.toList());

			if (orderIds.isEmpty()) {
				continue;
			}

			// 4-A-1: 批量更新主单和关联单状态为 CANCEL
			normalOrdersMapper.update(null,
					new LambdaUpdateWrapper<NormalOrders>()
							.in(NormalOrders::getOrderId, orderIds)
							.set(NormalOrders::getOrderStatus, "CANCEL"));
			orderAssociationsMapper.update(null,
					new LambdaUpdateWrapper<OrderAssociations>()
							.in(OrderAssociations::getOrderId, orderIds)
							.set(OrderAssociations::getOrderStatus, "CANCEL"));

			// 4-A-2: 查询订单明细和促销关联，按 activity_type 分类
			List<NormalOrdersItems> orderItems = normalOrdersItemsMapper.selectList(
					new LambdaQueryWrapper<NormalOrdersItems>()
							.in(NormalOrdersItems::getOrderId, orderIds));

			List<OrderPromotions> promotions = orderPromotionsMapper.selectList(
					new LambdaQueryWrapper<OrderPromotions>()
							.in(OrderPromotions::getMoid, orderIds));

			Map<Long, OrderPromotions> limitedTimeSaleMap = new HashMap<>();
			Map<Long, Map<Long, Long>> marketingActivityMap = new HashMap<>();

			for (OrderPromotions p : promotions) {
				if ("limited_time_sale".equals(p.getActivityType()) && p.getItemId() != null) {
					limitedTimeSaleMap.putIfAbsent(p.getItemId(), p);
				} else if ("marketing_activity".equals(p.getActivityType())
						|| "register_promotion".equals(p.getActivityType())) {
					if (p.getUserId() != null && p.getMoid() != null && p.getActivityId() != null) {
						marketingActivityMap
								.computeIfAbsent(p.getUserId(), k -> new HashMap<>())
								.put(p.getMoid(), p.getActivityId());
					}
				}
			}

			// 4-A-3/4/5/6: 逐单处理售后逻辑
			for (NormalOrders order : result) {
				long orderId = order.getOrderId();
				long companyId = order.getCompanyId();

				// 4-A-4-a: 供应商单取消
				long supplierCount = supplierOrderMapper.selectCount(
						new LambdaQueryWrapper<SupplierOrder>()
								.eq(SupplierOrder::getOrderId, orderId));
				if (supplierCount > 0) {
					supplierOrderMapper.update(null,
							new LambdaUpdateWrapper<SupplierOrder>()
									.eq(SupplierOrder::getOrderId, orderId)
									.set(SupplierOrder::getOrderStatus, "CANCEL"));
				}

				// 4-A-4-b: 积分回退
				if (order.getPointUse() != null && order.getPointUse() > 0
						&& !"point".equals(order.getPayType())) {
					Map<String, Object> pointData = buildOrderDataMap(order);
					pointMemberCancelOrderReturnPointsService.cancelOrderReturnBackPoints(pointData);
				}

				// 4-A-4-c: 上分回退（仅 Redis）
				if (order.getUppointUse() != null && order.getUppointUse() > 0) {
					Map<String, Object> upointData = buildOrderDataMap(order);
					upointData.put("uppoint_use", order.getUppointUse());
					pointMemberMinusOrderUppointsService.minusOrderUppoints(upointData);
				}

				// 4-A-3: 营销活动人数减一（Redis）
				Map<Long, Long> orderMarketingMap = marketingActivityMap.getOrDefault(order.getUserId(), Map.of());
				Long activityId = orderMarketingMap.get(orderId);
				if (activityId != null) {
					orderCancelMarketingJoinCountPort.lessJoinCount(companyId, order.getUserId(), activityId);
				}

				// 4-A-5-a: 流程日志经统一投递端口 fan-out 为异步 listener
				orderProcessLogPublishPort.publish(buildScheduleCancelNormalOrderProcessLogEntities(order));

				publishNormalOrderCancelMarketingDispatch(orderId, companyId);

				// 4-A-6: 内购活动恢复
				if ("employee_purchase".equals(order.getOrderClass())) {
					employeePurchaseOrderCancelRestorePort.restoreOnOrderCancel(orderId, companyId);
				}
			}

			// 4-A-2-a/b/c: 逐商品处理限时特惠额度/库存/限购
			Map<Long, String> receiptTypeByOrderIdOffline = result.stream()
					.collect(Collectors.toMap(NormalOrders::getOrderId, o -> o.getReceiptType() != null ? o.getReceiptType() : "", (a, b) -> a));
			restoreLimitedTimeSaleQuotasOnCancel(result, orderItems, limitedTimeSaleMap.keySet());
			for (NormalOrdersItems item : orderItems) {
				if (item.getItemId() != null && item.getNum() != null && item.getNum() > 0) {
					long orderId = item.getOrderId() != null ? item.getOrderId() : 0L;
					String receiptType = receiptTypeByOrderIdOffline.getOrDefault(orderId, "");
					orderCancelItemStoreRestorePort.restoreStore(
							ItemInventoryLineContext.fromOrderLine(
									item.getCompanyId(),
									item.getItemId(),
									item.getSupplierId(),
									item.getDistributorId(),
									item.getIsTotalStore(),
									receiptType),
							item.getNum());
				}

				if (item.getItemId() != null && item.getNum() != null && item.getNum() > 0) {
					partialCancelPromotionRestorePort.reduceLimitPerson(
							item.getCompanyId(),
							item.getUserId() != null ? item.getUserId() : 0L,
							item.getItemId(),
							item.getNum());
				}
			}

			// 4-A-7: 优惠券/定向促销恢复
			for (NormalOrders order : result) {
				normalOrderCancelDiscountRestoreService.restoreOnAutoCancel(order);
			}

			// 4-A-8: 更新 offline_payment check_status → 9（有 try-catch，失败不影响其他订单）
			for (NormalOrders order : result) {
				try {
					offlinePaymentMapper.update(null,
							new LambdaUpdateWrapper<OfflinePayment>()
									.eq(OfflinePayment::getOrderId, order.getOrderId())
									.set(OfflinePayment::getCheckStatus, 9));
				} catch (Exception e) {
					log.debug("[scheduleOfflinePayCancelOrders] updateOfflinePayment failed, orderId={}",
							order.getOrderId(), e);
				}
			}

			cancelledCount += result.size();
		}

		return cancelledCount;
	}

	private Map<String, Object> buildScheduleCancelNormalOrderProcessLogEntities(NormalOrders order) {
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

	private void publishNormalOrderCancelMarketingDispatch(long orderId, long companyId) {
		Map<String, Object> payload = new HashMap<>();
		payload.put("order_id", orderId);
		payload.put("company_id", companyId);
		payload.put("source", "normal_order_cron_schedule_cancel");
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					normalOrderCancelDispatchPublisher.publish(payload);
				}
			});
		} else {
			normalOrderCancelDispatchPublisher.publish(payload);
		}
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

	/**
	 * 定时取消回补限时特惠已购件数与金额额度（对齐人工取消 {@code restoreLimitedTimeSaleUserBuys}，按活动价）。
	 */
	private void restoreLimitedTimeSaleQuotasOnCancel(
			List<NormalOrders> orders, List<NormalOrdersItems> orderItems, Set<Long> limitedTimeSaleItemIds) {
		if (limitedTimeSaleItemIds == null
				|| limitedTimeSaleItemIds.isEmpty()
				|| orderItems == null
				|| orderItems.isEmpty()
				|| orders == null
				|| orders.isEmpty()) {
			return;
		}
		Map<Long, Long> companyByOrderId =
				orders.stream()
						.filter(o -> o.getOrderId() != null && o.getCompanyId() != null)
						.collect(
								Collectors.toMap(
										NormalOrders::getOrderId, NormalOrders::getCompanyId, (a, b) -> a));
		Map<Long, List<PartialCancelOrderItemRow>> rowsByOrderId = new LinkedHashMap<>();
		for (NormalOrdersItems item : orderItems) {
			if (item.getItemId() == null || item.getNum() == null || item.getNum() <= 0) {
				continue;
			}
			if (!limitedTimeSaleItemIds.contains(item.getItemId())) {
				continue;
			}
			long orderId = item.getOrderId() != null ? item.getOrderId() : 0L;
			if (orderId <= 0L) {
				continue;
			}
			int price = item.getPrice() == null ? 0 : item.getPrice();
			int discountFee = item.getDiscountFee() == null ? 0 : item.getDiscountFee();
			int unitFen = LimitedTimeSaleQuotaFen.unitFen(price, item.getNum(), discountFee);
			rowsByOrderId
					.computeIfAbsent(orderId, k -> new ArrayList<>())
					.add(new PartialCancelOrderItemRow(item.getItemId(), item.getNum(), unitFen));
		}
		for (Map.Entry<Long, List<PartialCancelOrderItemRow>> e : rowsByOrderId.entrySet()) {
			long orderId = e.getKey();
			long companyId = companyByOrderId.getOrDefault(orderId, 0L);
			if (companyId <= 0L) {
				continue;
			}
			partialCancelPromotionRestorePort.restoreLimitedTimeSaleUserBuys(companyId, orderId, e.getValue());
		}
	}
}
