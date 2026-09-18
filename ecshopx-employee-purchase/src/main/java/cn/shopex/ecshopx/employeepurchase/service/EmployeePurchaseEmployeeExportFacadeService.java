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

import cn.shopex.ecshopx.employeepurchase.dispatch.EmployeePurchaseEmployeeExportDispatchPublisher;
import cn.shopex.ecshopx.employeepurchase.service.dto.EmployeeAdminExportQuery;
import org.springframework.stereotype.Service;

@Service
public class EmployeePurchaseEmployeeExportFacadeService {

	private final EmployeePurchaseEmployeeExportDispatchPublisher dispatchPublisher;

	public EmployeePurchaseEmployeeExportFacadeService(
			EmployeePurchaseEmployeeExportDispatchPublisher dispatchPublisher) {
		this.dispatchPublisher = dispatchPublisher;
	}

	public void submitExport(EmployeeAdminExportQuery query, long operatorId, boolean datapassBlock) {
		dispatchPublisher.enqueue(query, operatorId, datapassBlock);
	}
}
