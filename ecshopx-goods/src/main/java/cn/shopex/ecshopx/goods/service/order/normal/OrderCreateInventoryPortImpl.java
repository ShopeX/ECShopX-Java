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

package cn.shopex.ecshopx.goods.service.order.normal;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.inventory.ItemInventoryLineContext;
import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.order.normal.OrderCreateInventoryPort;
import cn.shopex.ecshopx.common.port.order.PointsmallPartialCancelItemStorePort;
import cn.shopex.ecshopx.goods.service.items.ItemInventoryOrchestratorService;
import cn.shopex.ecshopx.promotions.service.LimitPersonBuyService;
import cn.shopex.ecshopx.promotions.service.SeckillUserBuysStoreService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OrderCreateInventoryPortImpl implements OrderCreateInventoryPort {

	private final ItemInventoryOrchestratorService itemInventoryOrchestratorService;
	private final PointsmallPartialCancelItemStorePort pointsmallPartialCancelItemStorePort;
	private final LimitPersonBuyService limitPersonBuyService;
	private final SeckillUserBuysStoreService seckillUserBuysStoreService;

	public OrderCreateInventoryPortImpl(
			ItemInventoryOrchestratorService itemInventoryOrchestratorService,
			PointsmallPartialCancelItemStorePort pointsmallPartialCancelItemStorePort,
			LimitPersonBuyService limitPersonBuyService,
			SeckillUserBuysStoreService seckillUserBuysStoreService) {
		this.itemInventoryOrchestratorService = itemInventoryOrchestratorService;
		this.pointsmallPartialCancelItemStorePort = pointsmallPartialCancelItemStorePort;
		this.limitPersonBuyService = limitPersonBuyService;
		this.seckillUserBuysStoreService = seckillUserBuysStoreService;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void minusStores(NormalOrderCreateParams p) {
		Map<String, Object> pr = p.getParams();
		if ("normal_employee_purchase".equals(stringVal(pr.get("order_type")))) {
			return;
		}
		Map<String, Object> od = p.getOrderData();
		long companyId = longVal(od.get("company_id"), 0L);
		String receiptType = stringVal(od.get("receipt_type"));
		boolean pointsmallOrder = "pointsmall".equals(stringVal(od.get("order_class")));
		Object itemsRaw = od.get("items");
		if (!(itemsRaw instanceof List<?> list)) {
			return;
		}
		for (Object o : list) {
			if (!(o instanceof Map<?, ?> row)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> line = (Map<String, Object>) row;
			long itemId = longVal(line.get("item_id"), 0L);
			int num = intVal(line.get("num"), 0);
			if (itemId <= 0L || num <= 0) {
				continue;
			}
			long distributorId = longVal(line.get("distributor_id"), 0L);
			boolean isTotalStore = !Boolean.FALSE.equals(line.get("is_total_store"));
			long supplierId = longVal(line.get("supplier_id"), 0L);
			boolean ok = pointsmallOrder
					? pointsmallPartialCancelItemStorePort.minusItemStore(companyId, itemId, num, isTotalStore)
					: itemInventoryOrchestratorService.minusItemStore(
							ItemInventoryLineContext.of(
									companyId, itemId, supplierId, distributorId, isTotalStore, receiptType, 0L),
							num);
			if (!ok) {
				throw new ResourceException("商品库存不足");
			}
		}
		recordLimitedBuyPurchases(od);
		recordLimitedTimeSalePurchases(od);
	}

	/** 对齐 PHP AbstractNormalOrder 扣库存后 createLimitPerson。 */
	@SuppressWarnings("unchecked")
	private void recordLimitedBuyPurchases(Map<String, Object> od) {
		Object promoRaw = od.get("items_promotion");
		if (!(promoRaw instanceof List<?> promos) || promos.isEmpty()) {
			return;
		}
		Map<Long, Integer> numByItem = new HashMap<>();
		Object itemsRaw = od.get("items");
		if (itemsRaw instanceof List<?> items) {
			for (Object o : items) {
				if (!(o instanceof Map<?, ?> row)) {
					continue;
				}
				long itemId = longVal(row.get("item_id"), 0L);
				int num = intVal(row.get("num"), 0);
				if (itemId > 0L && num > 0) {
					numByItem.merge(itemId, num, Integer::sum);
				}
			}
		}
		long orderDistributorId = longVal(od.get("distributor_id"), 0L);
		for (Object o : promos) {
			if (!(o instanceof Map<?, ?> promoRawMap)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> promo = (Map<String, Object>) promoRawMap;
			if (!"limited_buy".equals(stringVal(promo.get("activity_type")))) {
				continue;
			}
			long itemId = longVal(promo.get("item_id"), 0L);
			int num = numByItem.getOrDefault(itemId, 0);
			if (itemId <= 0L || num <= 0) {
				continue;
			}
			int day = 0;
			Object ruleRaw = promo.get("activity_rule");
			if (ruleRaw instanceof Map<?, ?> ruleMap) {
				day = intVal(ruleMap.get("day"), 0);
			} else if (ruleRaw instanceof List<?> ruleList && !ruleList.isEmpty() && ruleList.get(0) instanceof Map<?, ?> first) {
				day = intVal(first.get("day"), 0);
			}
			long distributorId = longVal(promo.get("shop_id"), orderDistributorId);
			Map<String, Object> limitParams = new HashMap<>();
			limitParams.put("limit_id", longVal(promo.get("activity_id"), 0L));
			limitParams.put("user_id", longVal(promo.get("user_id"), longVal(od.get("user_id"), 0L)));
			limitParams.put("item_id", itemId);
			limitParams.put("company_id", longVal(promo.get("company_id"), longVal(od.get("company_id"), 0L)));
			limitParams.put("number", (long) num);
			limitParams.put("day", day);
			limitParams.put("distributor_id", distributorId);
			limitPersonBuyService.createLimitPerson(limitParams);
		}
	}

	@SuppressWarnings("unchecked")
	private void recordLimitedTimeSalePurchases(Map<String, Object> od) {
		Object promoRaw = od.get("items_promotion");
		if (!(promoRaw instanceof List<?> promos) || promos.isEmpty()) {
			return;
		}
		Map<Long, Map<String, Object>> itemOrder = new HashMap<>();
		Object itemsRaw = od.get("items");
		if (itemsRaw instanceof List<?> items) {
			for (Object o : items) {
				if (!(o instanceof Map<?, ?> row)) {
					continue;
				}
				long itemId = longVal(row.get("item_id"), 0L);
				if (itemId > 0L) {
					itemOrder.put(itemId, (Map<String, Object>) row);
				}
			}
		}
		for (Object o : promos) {
			if (!(o instanceof Map<?, ?> promoRawMap)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> promo = (Map<String, Object>) promoRawMap;
			if (!"limited_time_sale".equals(stringVal(promo.get("activity_type")))) {
				continue;
			}
			long itemId = longVal(promo.get("item_id"), 0L);
			Map<String, Object> item = itemOrder.get(itemId);
			if (item == null) {
				continue;
			}
			int store = intVal(item.get("num"), 0);
			if (store <= 0) {
				continue;
			}
			int activityPrice = intVal(item.get("activity_price"), 0);
			if (activityPrice <= 0) {
				activityPrice = intVal(item.get("price"), 0);
			}
			long price = (long) activityPrice * (long) store;
			long activityId = longVal(promo.get("activity_id"), 0L);
			long companyId = longVal(promo.get("company_id"), longVal(od.get("company_id"), 0L));
			long userId = longVal(promo.get("user_id"), longVal(od.get("user_id"), 0L));
			seckillUserBuysStoreService.setUserBuysStore(activityId, companyId, userId, itemId, store, price);
		}
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
