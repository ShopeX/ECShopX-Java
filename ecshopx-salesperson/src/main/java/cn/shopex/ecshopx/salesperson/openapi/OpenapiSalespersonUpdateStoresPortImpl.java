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

package cn.shopex.ecshopx.salesperson.openapi;

import cn.shopex.ecshopx.common.openapi.OpenapiSalespersonUpdateStoresPort;
import cn.shopex.ecshopx.salesperson.service.OpenapiThirdApiV1SalespersonUpdateStoresService;
import org.springframework.stereotype.Component;

@Component
public class OpenapiSalespersonUpdateStoresPortImpl implements OpenapiSalespersonUpdateStoresPort {

	private final OpenapiThirdApiV1SalespersonUpdateStoresService updateStoresService;

	public OpenapiSalespersonUpdateStoresPortImpl(
			OpenapiThirdApiV1SalespersonUpdateStoresService updateStoresService) {
		this.updateStoresService = updateStoresService;
	}

	@Override
	public void updateSalespersonStores(long companyId, String employeeNumber, String storeBn) {
		updateStoresService.executeUpdateSalespersonStores(companyId, employeeNumber, storeBn);
	}
}
