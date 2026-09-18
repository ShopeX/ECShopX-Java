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

import cn.shopex.ecshopx.employeepurchase.dispatch.EmployeePurchaseActivityItemsExportDispatchPublisher;
import cn.shopex.ecshopx.employeepurchase.service.dto.ActivityItemsExportQuery;
import cn.shopex.ecshopx.employeepurchase.service.export.EmployeePurchaseActivityItemsCsvExportService;
import org.springframework.stereotype.Service;

@Service
public class EmployeePurchaseActivityItemsExportFacadeService {

	private final EmployeePurchaseActivityItemsCsvExportService csvExportService;
	private final EmployeePurchaseActivityItemsExportDispatchPublisher dispatchPublisher;

	public EmployeePurchaseActivityItemsExportFacadeService(
			EmployeePurchaseActivityItemsCsvExportService csvExportService,
			EmployeePurchaseActivityItemsExportDispatchPublisher dispatchPublisher) {
		this.csvExportService = csvExportService;
		this.dispatchPublisher = dispatchPublisher;
	}

	public void submitExport(ActivityItemsExportQuery query) {
		csvExportService.validateExportable(query);
		dispatchPublisher.enqueue(query);
	}
}
