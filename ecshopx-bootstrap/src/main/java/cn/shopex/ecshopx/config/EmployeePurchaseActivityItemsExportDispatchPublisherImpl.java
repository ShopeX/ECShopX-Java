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

package cn.shopex.ecshopx.config;

import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import cn.shopex.ecshopx.employeepurchase.dispatch.EmployeePurchaseActivityItemsExportDispatchPublisher;
import cn.shopex.ecshopx.employeepurchase.service.dto.ActivityItemsExportQuery;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class EmployeePurchaseActivityItemsExportDispatchPublisherImpl
		implements EmployeePurchaseActivityItemsExportDispatchPublisher {

	private final DispatchFacade dispatchFacade;

	public EmployeePurchaseActivityItemsExportDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void enqueue(ActivityItemsExportQuery query) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", "employee_purchase_activity_items");
		payload.put("company_id", query.companyId());
		payload.put("activity_id", query.activityId());
		payload.put("operator_id", query.operatorId());
		payload.put("distributor_id", query.distributorId());
		if (query.distributorScope() != null) {
			payload.put("distributor_scope", query.distributorScope());
		}
		if (query.mainCatId() != null) {
			payload.put("main_cat_id", query.mainCatId());
		}
		if (query.category() != null) {
			payload.put("category", query.category());
		}
		if (query.itemName() != null) {
			payload.put("item_name", query.itemName());
		}
		if (query.itemBn() != null) {
			payload.put("item_bn", query.itemBn());
		}
		if (query.shelfStatus() != null) {
			payload.put("shelf_status", query.shelfStatus());
		}
		if (query.itemIds() != null && !query.itemIds().isEmpty()) {
			payload.put("item_id", query.itemIds());
		}
		dispatchFacade.dispatchJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));
	}
}
