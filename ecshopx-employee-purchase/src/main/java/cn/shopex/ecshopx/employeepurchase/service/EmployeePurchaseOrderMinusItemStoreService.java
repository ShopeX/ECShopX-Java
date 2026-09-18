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

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.inventory.ItemInventoryLineContext;
import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.EmployeePurchasePassphraseOrderService;
import cn.shopex.ecshopx.employeepurchase.support.PurchaseModeSupport;
import cn.shopex.ecshopx.goods.service.items.ItemInventoryOrchestratorService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** 对齐 PHP EmployeePurchaseBundle/Services/NormalOrderService::minusItemStore。 */
@Service
public class EmployeePurchaseOrderMinusItemStoreService {

	private final EmployeePurchasePassphraseOrderService employeePurchasePassphraseOrderService;
	private final EmployeePurchaseActivityDataService employeePurchaseActivityDataService;
	private final ActivityItemStoreService activityItemStoreService;
	private final MemberActivityAggregateService memberActivityAggregateService;
	private final MemberActivityItemsAggregateService memberActivityItemsAggregateService;
	private final ItemInventoryOrchestratorService itemInventoryOrchestratorService;

	public EmployeePurchaseOrderMinusItemStoreService(
			EmployeePurchasePassphraseOrderService employeePurchasePassphraseOrderService,
			EmployeePurchaseActivityDataService employeePurchaseActivityDataService,
			ActivityItemStoreService activityItemStoreService,
			MemberActivityAggregateService memberActivityAggregateService,
			MemberActivityItemsAggregateService memberActivityItemsAggregateService,
			ItemInventoryOrchestratorService itemInventoryOrchestratorService) {
		this.employeePurchasePassphraseOrderService = employeePurchasePassphraseOrderService;
		this.employeePurchaseActivityDataService = employeePurchaseActivityDataService;
		this.activityItemStoreService = activityItemStoreService;
		this.memberActivityAggregateService = memberActivityAggregateService;
		this.memberActivityItemsAggregateService = memberActivityItemsAggregateService;
		this.itemInventoryOrchestratorService = itemInventoryOrchestratorService;
	}

	public void applyAfterOrderInsert(NormalOrderCreateParams p) {
		employeePurchasePassphraseOrderService.persistRelAndScheduleQuotaConsume(p);
		minusItemStore(p);
	}

	@SuppressWarnings("unchecked")
	public void minusItemStore(NormalOrderCreateParams p) {
		Map<String, Object> pr = p.getParams();
		if (!"normal_employee_purchase".equals(stringVal(pr.get("order_type")))) {
			return;
		}
		Map<String, Object> od = p.getOrderData();
		if (od == null) {
			return;
		}
		long companyId = longVal(pr.get("company_id"), longVal(od.get("company_id"), 0L));
		long enterpriseId = longVal(pr.get("enterprise_id"), 0L);
		long activityId = longVal(pr.get("activity_id"), 0L);
		long userId = longVal(pr.get("user_id"), longVal(od.get("user_id"), 0L));
		if (companyId <= 0L || enterpriseId <= 0L || activityId <= 0L || userId <= 0L) {
			return;
		}

		Activities activity =
				employeePurchaseActivityDataService.requireActivityWithEnterpriseOrThrow(
						companyId, activityId, enterpriseId);

		Object itemsRaw = od.get("items");
		if (!(itemsRaw instanceof List<?> rawList) || rawList.isEmpty()) {
			return;
		}

		if (!Boolean.TRUE.equals(activity.getIfShareStore())) {
			for (Object o : rawList) {
				if (!(o instanceof Map<?, ?> row)) {
					continue;
				}
				Map<String, Object> item = (Map<String, Object>) row;
				long itemId = longVal(item.get("item_id"), 0L);
				int num = intVal(item.get("num"), 0);
				if (itemId <= 0L || num <= 0) {
					continue;
				}
				activityItemStoreService.minusActivityItemStore(companyId, activityId, itemId, num);
			}
		} else {
			minusGenericItemStore(od, companyId, rawList);
		}

		int deductFee;
		if (PurchaseModeSupport.isPrepaidPoint(activity)) {
			deductFee = intVal(od.get("prepaid_payable_fee"), 0);
			if (deductFee <= 0) {
				long totalFee = longVal(od.get("total_fee"), 0L);
				if (totalFee <= 0L) {
					totalFee = longVal(od.get("item_fee"), 0L) + longVal(od.get("freight_fee"), 0L);
				}
				deductFee = (int) Math.min(Math.max(totalFee, 0L), Integer.MAX_VALUE);
				od.put("prepaid_payable_fee", deductFee);
			}
		} else {
			deductFee = intVal(od.get("item_fee"), 0);
		}
		memberActivityAggregateService.addAggregateFee(companyId, enterpriseId, activityId, userId, deductFee);

		for (Object o : rawList) {
			if (!(o instanceof Map<?, ?> row)) {
				continue;
			}
			Map<String, Object> item = (Map<String, Object>) row;
			long itemId = longVal(item.get("item_id"), 0L);
			int num = intVal(item.get("num"), 0);
			int lineItemFee = intVal(item.get("item_fee"), 0);
			if (itemId <= 0L || num <= 0) {
				continue;
			}
			memberActivityItemsAggregateService.addItemAggregate(
					companyId, enterpriseId, activityId, userId, itemId, lineItemFee, num);
		}
	}

	@SuppressWarnings("unchecked")
	private void minusGenericItemStore(Map<String, Object> od, long companyId, List<?> rawList) {
		String receiptType = stringVal(od.get("receipt_type"));
		for (Object o : rawList) {
			if (!(o instanceof Map<?, ?> row)) {
				continue;
			}
			Map<String, Object> line = (Map<String, Object>) row;
			long itemId = longVal(line.get("item_id"), 0L);
			int num = intVal(line.get("num"), 0);
			if (itemId <= 0L || num <= 0) {
				continue;
			}
			long distributorId = longVal(line.get("distributor_id"), 0L);
			boolean isTotalStore = !Boolean.FALSE.equals(line.get("is_total_store"));
			long supplierId = longVal(line.get("supplier_id"), 0L);
			boolean ok =
					itemInventoryOrchestratorService.minusItemStore(
							ItemInventoryLineContext.of(
									companyId, itemId, supplierId, distributorId, isTotalStore, receiptType, 0L),
							num);
			if (!ok) {
				throw new ResourceException("商品库存不足");
			}
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
