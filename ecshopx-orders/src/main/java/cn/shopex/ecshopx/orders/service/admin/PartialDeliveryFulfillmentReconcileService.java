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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.aftersales.service.AftersalesDetailAggregateService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 部分发货订单中，当全部明细已发货或已取消（{@code delivery_item_num + cancel_item_num >= num}）时，
 * 将主单从 {@code PAYED + PARTAIL} 推进到 {@code WAIT_BUYER_CONFIRM + DONE}，对齐 PHP
 * {@code AbstractNormalOrder::partailCancelOrder} 收尾逻辑。
 */
@Service
public class PartialDeliveryFulfillmentReconcileService {

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;
	private final AftersalesDetailAggregateService aftersalesDetailAggregateService;

	public PartialDeliveryFulfillmentReconcileService(
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			OrderAssociationsMapper orderAssociationsMapper,
			OrderValiditySettingRedisReadService orderValiditySettingRedisReadService,
			AftersalesDetailAggregateService aftersalesDetailAggregateService) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.orderValiditySettingRedisReadService = orderValiditySettingRedisReadService;
		this.aftersalesDetailAggregateService = aftersalesDetailAggregateService;
	}

	public void reconcileIfFullyProcessed(long companyId, long orderId) {
		if (companyId <= 0L || orderId <= 0L) {
			return;
		}
		NormalOrders order = loadOrder(companyId, orderId);
		if (order == null) {
			return;
		}
		if (!"PAYED".equalsIgnoreCase(safe(order.getOrderStatus()))
				|| !"PARTAIL".equalsIgnoreCase(safe(order.getDeliveryStatus()))) {
			return;
		}
		List<NormalOrdersItems> items = loadItems(companyId, orderId);
		if (items.isEmpty()) {
			return;
		}
		if (!allItemsProcessed(items) || totalDelivered(items) <= 0) {
			return;
		}

		int finishDays = readOrderFinishDays(companyId);
		int now = (int) (System.currentTimeMillis() / 1000L);
		int autoFinish = now + finishDays * 24 * 3600;
		int leftAftersales = computeLeftAftersalesNum(companyId, orderId, items);

		LambdaUpdateWrapper<NormalOrders> ou = new LambdaUpdateWrapper<>();
		ou.eq(NormalOrders::getCompanyId, companyId)
				.eq(NormalOrders::getOrderId, orderId)
				.set(NormalOrders::getOrderStatus, "WAIT_BUYER_CONFIRM")
				.set(NormalOrders::getDeliveryStatus, "DONE")
				.set(NormalOrders::getAutoFinishTime, String.valueOf(autoFinish))
				.set(NormalOrders::getLeftAftersalesNum, leftAftersales)
				.set(NormalOrders::getUpdateTime, now);
		normalOrdersMapper.update(null, ou);

		LambdaUpdateWrapper<OrderAssociations> au = new LambdaUpdateWrapper<>();
		au.eq(OrderAssociations::getCompanyId, companyId)
				.eq(OrderAssociations::getOrderId, orderId)
				.set(OrderAssociations::getOrderStatus, "WAIT_BUYER_CONFIRM")
				.set(OrderAssociations::getDeliveryStatus, "DONE")
				.set(OrderAssociations::getUpdateTime, now);
		orderAssociationsMapper.update(null, au);
	}

	public boolean hasPendingShippableItems(long companyId, long orderId) {
		return hasPendingShippableItems(loadItems(companyId, orderId));
	}

	public boolean hasPendingPartialCancelItems(long companyId, long orderId) {
		return hasPendingPartialCancelItems(loadItems(companyId, orderId));
	}

	public static boolean hasPendingShippableItems(List<NormalOrdersItems> items) {
		if (items == null || items.isEmpty()) {
			return false;
		}
		for (NormalOrdersItems item : items) {
			if (pendingShipQty(item) > 0) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasPendingPartialCancelItems(List<NormalOrdersItems> items) {
		if (items == null || items.isEmpty()) {
			return false;
		}
		for (NormalOrdersItems item : items) {
			if (pendingPartialCancelQty(item) > 0) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasPendingShippableItemsFromMaps(List<Map<String, Object>> items) {
		if (items == null || items.isEmpty()) {
			return false;
		}
		for (Map<String, Object> item : items) {
			int num = intVal(item.get("num"));
			int delivered = intVal(item.get("delivery_item_num"));
			int cancelled = intVal(item.get("cancel_item_num"));
			if (num - delivered - cancelled > 0) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasPendingPartialCancelItemsFromMaps(List<Map<String, Object>> items) {
		return hasPendingShippableItemsFromMaps(items);
	}

	public static boolean allItemsProcessedFromMaps(List<Map<String, Object>> items) {
		if (items == null || items.isEmpty()) {
			return false;
		}
		for (Map<String, Object> item : items) {
			int num = intVal(item.get("num"));
			int delivered = intVal(item.get("delivery_item_num"));
			int cancelled = intVal(item.get("cancel_item_num"));
			if (delivered + cancelled < num) {
				return false;
			}
		}
		return true;
	}

	public static int totalDeliveredFromMaps(List<Map<String, Object>> items) {
		int total = 0;
		if (items == null) {
			return 0;
		}
		for (Map<String, Object> item : items) {
			total += intVal(item.get("delivery_item_num"));
		}
		return total;
	}

	private int computeLeftAftersalesNum(long companyId, long orderId, List<NormalOrdersItems> items) {
		String orderIdStr = String.valueOf(orderId);
		int now = (int) (System.currentTimeMillis() / 1000L);
		int sum = 0;
		for (NormalOrdersItems item : items) {
			if (item.getId() == null) {
				continue;
			}
			int delivered = nz(item.getDeliveryItemNum());
			if (delivered <= 0) {
				continue;
			}
			int applied =
					aftersalesDetailAggregateService.getAppliedNum(companyId, orderIdStr, item.getId());
			int cancelNum = nz(item.getCancelItemNum());
			int left = delivered + cancelNum - applied;
			if (left <= 0) {
				continue;
			}
			int autoClose = nz(item.getAutoCloseAftersalesTime());
			if (autoClose > 0 && autoClose < now) {
				continue;
			}
			sum += left;
		}
		return sum;
	}

	private static boolean allItemsProcessed(List<NormalOrdersItems> items) {
		for (NormalOrdersItems item : items) {
			int num = nz(item.getNum());
			if (nz(item.getDeliveryItemNum()) + nz(item.getCancelItemNum()) < num) {
				return false;
			}
		}
		return true;
	}

	private static int totalDelivered(List<NormalOrdersItems> items) {
		int total = 0;
		for (NormalOrdersItems item : items) {
			total += nz(item.getDeliveryItemNum());
		}
		return total;
	}

	private static int pendingShipQty(NormalOrdersItems item) {
		return Math.max(0, nz(item.getNum()) - nz(item.getDeliveryItemNum()) - nz(item.getCancelItemNum()));
	}

	private static int pendingPartialCancelQty(NormalOrdersItems item) {
		return Math.max(0, nz(item.getNum()) - nz(item.getDeliveryItemNum()) - nz(item.getCancelItemNum()));
	}

	private List<NormalOrdersItems> loadItems(long companyId, long orderId) {
		return normalOrdersItemsMapper.selectList(
				new LambdaQueryWrapper<NormalOrdersItems>()
						.eq(NormalOrdersItems::getCompanyId, companyId)
						.eq(NormalOrdersItems::getOrderId, orderId));
	}

	private NormalOrders loadOrder(long companyId, long orderId) {
		return normalOrdersMapper.selectOne(
				new LambdaQueryWrapper<NormalOrders>()
						.eq(NormalOrders::getCompanyId, companyId)
						.eq(NormalOrders::getOrderId, orderId)
						.last("LIMIT 1"));
	}

	private int readOrderFinishDays(long companyId) {
		Map<String, Object> setting = orderValiditySettingRedisReadService.readPlatformSetting(companyId);
		Object ot = setting.get("order_finish_time");
		if (ot instanceof Number n) {
			return Math.max(1, n.intValue());
		}
		if (ot != null) {
			try {
				return Math.max(1, Integer.parseInt(String.valueOf(ot).trim()));
			} catch (NumberFormatException ignored) {
				return 7;
			}
		}
		return 7;
	}

	private static int nz(Integer v) {
		return v == null ? 0 : v;
	}

	private static int intVal(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String safe(String s) {
		return s == null ? "" : s.trim();
	}
}
