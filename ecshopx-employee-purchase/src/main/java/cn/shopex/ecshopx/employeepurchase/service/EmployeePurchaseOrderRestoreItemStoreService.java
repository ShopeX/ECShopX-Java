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

import cn.shopex.ecshopx.employeepurchase.domain.OrdersRelActivity;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.EmployeePurchasePassphraseOrderService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** 对齐 PHP NormalOrderService::restoreItemStoreAndAggregate。 */
@Service
public class EmployeePurchaseOrderRestoreItemStoreService {

	private final EmployeePurchasePassphraseOrderService employeePurchasePassphraseOrderService;
	private final ActivityItemStoreService activityItemStoreService;
	private final MemberActivityAggregateService memberActivityAggregateService;
	private final MemberActivityItemsAggregateService memberActivityItemsAggregateService;

	public EmployeePurchaseOrderRestoreItemStoreService(
			EmployeePurchasePassphraseOrderService employeePurchasePassphraseOrderService,
			ActivityItemStoreService activityItemStoreService,
			MemberActivityAggregateService memberActivityAggregateService,
			MemberActivityItemsAggregateService memberActivityItemsAggregateService) {
		this.employeePurchasePassphraseOrderService = employeePurchasePassphraseOrderService;
		this.activityItemStoreService = activityItemStoreService;
		this.memberActivityAggregateService = memberActivityAggregateService;
		this.memberActivityItemsAggregateService = memberActivityItemsAggregateService;
	}

	public void restoreItemStoreAndAggregate(
			long companyId,
			OrdersRelActivity rel,
			int itemFee,
			List<Map<String, Object>> items) {
		if (rel == null || items == null || items.isEmpty()) {
			return;
		}
		Long enterpriseId = rel.getEnterpriseId();
		Long activityId = rel.getActivityId();
		Long userId = rel.getUserId();
		if (enterpriseId == null || activityId == null || userId == null) {
			return;
		}

		employeePurchasePassphraseOrderService.releaseQuotaOnCancel(rel);

		if (!Boolean.TRUE.equals(rel.getIfShareStore())) {
			for (Map<String, Object> line : items) {
				long itemId = longVal(line.get("item_id"));
				int num = intVal(line.get("num"));
				if (itemId <= 0L || num <= 0) {
					continue;
				}
				activityItemStoreService.addActivityItemStore(companyId, activityId, itemId, num);
			}
		}

		if (itemFee > 0) {
			memberActivityAggregateService.minusAggregateFee(
					companyId, enterpriseId, activityId, userId, itemFee);
		}

		for (Map<String, Object> line : items) {
			long itemId = longVal(line.get("item_id"));
			int num = intVal(line.get("num"));
			int lineItemFee = intVal(line.get("item_fee"));
			if (lineItemFee <= 0) {
				lineItemFee = intVal(line.get("total_fee"));
			}
			if (itemId <= 0L || num <= 0) {
				continue;
			}
			memberActivityItemsAggregateService.minusItemAggregate(
					companyId, enterpriseId, activityId, userId, itemId, lineItemFee, num);
		}
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
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
}
