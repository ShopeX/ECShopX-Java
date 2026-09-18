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

package cn.shopex.ecshopx.employeepurchase.integration;

import cn.shopex.ecshopx.common.espier.upload.EspierUploadRelationValidatePort;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseActivityItemsSortImportRowService;
import org.springframework.stereotype.Component;

@Component
public class EmployeePurchaseUploadRelationValidatePortImpl implements EspierUploadRelationValidatePort {

	private static final String ITEMS = "employee_purchase_activity_items";
	private static final String ITEMS_SORT = "employee_purchase_activity_items_sort";

	private final EmployeePurchaseActivityItemsSortImportRowService employeePurchaseActivityItemsSortImportRowService;

	public EmployeePurchaseUploadRelationValidatePortImpl(
			EmployeePurchaseActivityItemsSortImportRowService employeePurchaseActivityItemsSortImportRowService) {
		this.employeePurchaseActivityItemsSortImportRowService = employeePurchaseActivityItemsSortImportRowService;
	}

	@Override
	public boolean supports(String fileType) {
		return ITEMS.equals(fileType) || ITEMS_SORT.equals(fileType);
	}

	@Override
	public void validate(long companyId, long relationId) {
		employeePurchaseActivityItemsSortImportRowService.requireActivityForRelation(companyId, relationId);
	}
}
