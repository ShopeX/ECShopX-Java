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

package cn.shopex.ecshopx.orders.service.excard;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.inventory.ItemInventoryLineContext;
import cn.shopex.ecshopx.common.order.excard.ExcardInventoryPort;
import cn.shopex.ecshopx.common.order.excard.ExcardOrderItemSnapshot;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.event.NormalOrderAddSpringEvent;
import cn.shopex.ecshopx.orders.event.OrderProcessLogSpringEvent;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ExcardNormalOrderCreateFacadeService {

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final NormalOrderNumericIdService normalOrderNumericIdService;
	private final ExcardInventoryPort excardInventoryPort;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final TaskScheduler orderRemindDelayTaskScheduler;
	private final SendPayOrdersRemindRunner sendPayOrdersRemindRunner;

	public ExcardNormalOrderCreateFacadeService(
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			OrderAssociationsMapper orderAssociationsMapper,
			NormalOrderNumericIdService normalOrderNumericIdService,
			ExcardInventoryPort excardInventoryPort,
			ApplicationEventPublisher applicationEventPublisher,
			@Qualifier("orderRemindDelayTaskScheduler") TaskScheduler orderRemindDelayTaskScheduler,
			SendPayOrdersRemindRunner sendPayOrdersRemindRunner) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.normalOrderNumericIdService = normalOrderNumericIdService;
		this.excardInventoryPort = excardInventoryPort;
		this.applicationEventPublisher = applicationEventPublisher;
		this.orderRemindDelayTaskScheduler = orderRemindDelayTaskScheduler;
		this.sendPayOrdersRemindRunner = sendPayOrdersRemindRunner;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> createThenComplete(Map<String, Object> orderParams) {
		long companyId = longVal(orderParams.get("company_id"), 0L);
		long userId = longVal(orderParams.get("user_id"), 0L);
		String mobile = stringVal(orderParams.get("mobile"));
		long userCardId = longVal(orderParams.get("user_card_id"), 0L);
		if (companyId <= 0L) {
			throw new ResourceException("企业id必填");
		}
		if (userId <= 0L) {
			throw new ResourceException("用户id必填");
		}
		if (!StringUtils.hasText(mobile)) {
			throw new ResourceException("未授权手机号，请授权");
		}
		if (userCardId <= 0L) {
			throw new ResourceException("用户兑换券id必填");
		}
		Object itemsRaw = orderParams.get("items");
		if (!(itemsRaw instanceof List<?>) || ((List<?>) itemsRaw).isEmpty()) {
			throw new ResourceException("订单商品不能为空");
		}
		List<Map<String, Object>> itemRows = new ArrayList<>();
		for (Object o : (List<?>) itemsRaw) {
			if (o instanceof Map<?, ?> m) {
				Map<String, Object> row = new LinkedHashMap<>();
				for (Map.Entry<?, ?> e : m.entrySet()) {
					if (e.getKey() != null) {
						row.put(e.getKey().toString(), e.getValue());
					}
				}
				itemRows.add(row);
			}
		}
		if (itemRows.isEmpty()) {
			throw new ResourceException("订单商品不能为空");
		}
		Map<String, Object> first = itemRows.get(0);
		long itemId = longVal(first.get("item_id"), 0L);
		int num = intVal(first.get("num"), 1);
		if (itemId <= 0L || num <= 0) {
			throw new ResourceException("订单商品不能为空");
		}
		long distributorId = longVal(orderParams.get("distributor_id"), 0L);
		boolean isTotalStore = excardInventoryPort.resolveIsTotalStore(companyId, itemId, distributorId);
		ExcardOrderItemSnapshot snap = excardInventoryPort.loadOrderItemSnapshot(companyId, itemId, distributorId);
		int lineItemFee = snap.price() * num;
		int now = (int) (System.currentTimeMillis() / 1000L);
		long orderId = normalOrderNumericIdService.generate(userId);
		String payType = stringVal(orderParams.get("pay_type"));

		NormalOrders order = new NormalOrders();
		order.setOrderId(orderId);
		order.setCompanyId(companyId);
		order.setUserId(userId);
		order.setMobile(mobile);
		order.setOrderClass("excard");
		order.setOrderType("normal");
		order.setReceiptType("ziti");
		order.setActId(userCardId);
		order.setDistributorId(distributorId);
		order.setIsDistribution(distributorId > 0L);
		order.setOrderHolder("self");
		order.setOrderSource("member");
		order.setOrderStatus("NOTPAY");
		order.setPayStatus("NOTPAY");
		order.setZitiStatus("NOTZITI");
		order.setDeliveryStatus("PENDING");
		order.setCancelStatus("NO_APPLY_CANCEL");
		order.setSelfDeliveryStatus("NOTMERCHANT");
		order.setTitle(snap.itemName());
		order.setTotalFee("0");
		order.setItemFee(String.valueOf(lineItemFee));
		order.setMarketFee(String.valueOf(snap.marketPrice() * num));
		order.setDiscountFee(lineItemFee);
		order.setPayType(payType);
		order.setCreateTime(now);
		order.setUpdateTime(now);
		order.setAutoCancelTime(String.valueOf(now + 30 * 60));

		normalOrdersMapper.insert(order);

		OrderAssociations assoc = new OrderAssociations();
		assoc.setOrderId(orderId);
		assoc.setCompanyId(companyId);
		assoc.setUserId(userId);
		assoc.setMobile(mobile);
		assoc.setTitle(snap.itemName());
		assoc.setTotalFee((long) lineItemFee);
		assoc.setOrderClass("excard");
		assoc.setOrderType("normal");
		assoc.setOrderStatus("NOTPAY");
		assoc.setIsDistribution(distributorId > 0L);
		assoc.setDeliveryStatus("PENDING");
		assoc.setCancelStatus("NO_APPLY_CANCEL");
		assoc.setCreateTime(now);
		assoc.setUpdateTime(now);
		orderAssociationsMapper.insert(assoc);

		NormalOrdersItems line = new NormalOrdersItems();
		line.setOrderId(orderId);
		line.setCompanyId(companyId);
		line.setUserId(userId);
		line.setItemId(itemId);
		line.setGoodsId(snap.goodsId());
		line.setItemBn(snap.itemBn());
		line.setGoodsBn(snap.goodsBn());
		line.setItemName(snap.itemName());
		line.setItemUnit(snap.itemUnit());
		line.setPic(snap.pic());
		line.setNum(num);
		line.setPrice(snap.price());
		line.setCostPrice(snap.costPrice());
		line.setMarketPrice(snap.marketPrice());
		line.setTotalFee(lineItemFee);
		line.setItemFee(lineItemFee);
		line.setDistributorId(distributorId);
		line.setIsTotalStore(isTotalStore);
		line.setActId(userCardId);
		line.setOrderItemType("normal");
		line.setCreateTime(now);
		line.setUpdateTime(now);
		line.setItemSpecDesc("");
		normalOrdersItemsMapper.insert(line);

		Map<String, Object> orderDataForRemind = new LinkedHashMap<>();
		orderDataForRemind.put("order_id", orderId);
		orderDataForRemind.put("company_id", companyId);
		orderDataForRemind.put("user_id", userId);
		orderDataForRemind.put("total_fee", 0);
		orderDataForRemind.put("title", snap.itemName());
		orderDataForRemind.put("wxa_appid", orderParams.get("wxa_appid"));

		boolean deducted =
				excardInventoryPort.minusItemStore(
						ItemInventoryLineContext.fromOrderLine(
								companyId, itemId, null, distributorId, isTotalStore, order.getReceiptType()),
						num);
		if (!deducted) {
			throw new ResourceException("商品库存不足");
		}

		orderRemindDelayTaskScheduler.schedule(
				() -> sendPayOrdersRemindRunner.run(orderDataForRemind), Instant.now().plusSeconds(300L));

		Map<String, Object> normalAdd = new LinkedHashMap<>();
		normalAdd.put("pay_type", payType);
		normalAdd.put("order_id", orderId);
		normalAdd.put("company_id", companyId);
		applicationEventPublisher.publishEvent(new NormalOrderAddSpringEvent(this, normalAdd));

		Map<String, Object> processLog = new LinkedHashMap<>();
		processLog.put("order_id", orderId);
		processLog.put("company_id", companyId);
		processLog.put("operator_type", "user");
		processLog.put("is_show", true);
		processLog.put("operator_id", userId);
		processLog.put("remarks", "订单核销");
		processLog.put("detail", "订单号：" + orderId + "，订单核销成功");
		processLog.put("params", orderParams);
		applicationEventPublisher.publishEvent(new OrderProcessLogSpringEvent(this, processLog));

		LambdaUpdateWrapper<NormalOrders> uw = new LambdaUpdateWrapper<>();
		uw.eq(NormalOrders::getOrderId, orderId)
				.eq(NormalOrders::getCompanyId, companyId)
				.set(NormalOrders::getZitiStatus, "DONE")
				.set(NormalOrders::getOrderStatus, "DONE")
				.set(NormalOrders::getDeliveryStatus, "DONE")
				.set(NormalOrders::getCancelStatus, "NO_APPLY_CANCEL")
				.set(NormalOrders::getDeliveryTime, now)
				.set(NormalOrders::getEndTime, (long) now)
				.set(NormalOrders::getPayStatus, "PAYED")
				.set(NormalOrders::getLeftAftersalesNum, num)
				.set(NormalOrders::getUpdateTime, now);
		normalOrdersMapper.update(null, uw);

		LambdaUpdateWrapper<OrderAssociations> aw = new LambdaUpdateWrapper<>();
		aw.eq(OrderAssociations::getOrderId, orderId)
				.eq(OrderAssociations::getCompanyId, companyId)
				.set(OrderAssociations::getOrderStatus, "DONE")
				.set(OrderAssociations::getDeliveryStatus, "DONE")
				.set(OrderAssociations::getCancelStatus, "NO_APPLY_CANCEL")
				.set(OrderAssociations::getDeliveryTime, now)
				.set(OrderAssociations::getEndTime, (long) now)
				.set(OrderAssociations::getUpdateTime, now);
		orderAssociationsMapper.update(null, aw);

		Map<String, Object> itemOut = new LinkedHashMap<>();
		itemOut.put("item_id", itemId);
		itemOut.put("item_name", snap.itemName());
		itemOut.put("pic", snap.pic());
		itemOut.put("item_spec_desc", "");
		itemOut.put("num", num);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("order_id", orderId);
		out.put("company_id", companyId);
		out.put("pay_type", payType);
		out.put("items", List.of(itemOut));
		return out;
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}

	private static long longVal(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static int intVal(Object v, int def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
