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
import cn.shopex.ecshopx.employeepurchase.dispatch.EmployeePurchaseEmployeeExportDispatchPublisher;
import cn.shopex.ecshopx.employeepurchase.service.dto.EmployeeAdminExportQuery;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class EmployeePurchaseEmployeeExportDispatchPublisherImpl implements EmployeePurchaseEmployeeExportDispatchPublisher {

	private final DispatchFacade dispatchFacade;

	public EmployeePurchaseEmployeeExportDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void enqueue(EmployeeAdminExportQuery query, long operatorId, boolean datapassBlock) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", "employee_purchase_employees");
		payload.put("company_id", query.companyId());
		payload.put("operator_id", operatorId);
		payload.put("datapass_block", datapassBlock ? 1 : 0);
		if (query.distributorId() != null) {
			payload.put("distributor_id", query.distributorId());
		}
		if (query.mobile() != null) {
			payload.put("mobile", query.mobile());
		}
		if (query.account() != null) {
			payload.put("account", query.account());
		}
		if (query.email() != null) {
			payload.put("email", query.email());
		}
		if (query.memberMobile() != null) {
			payload.put("member_mobile", query.memberMobile());
		}
		if (query.enterpriseId() != null) {
			payload.put("enterprise_id", query.enterpriseId());
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
