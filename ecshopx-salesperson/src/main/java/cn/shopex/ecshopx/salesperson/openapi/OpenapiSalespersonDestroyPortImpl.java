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

import cn.shopex.ecshopx.common.openapi.OpenapiSalespersonDestroyPort;
import cn.shopex.ecshopx.salesperson.service.OpenapiThirdApiV1SalespersonDestroyService;
import org.springframework.stereotype.Component;

@Component
public class OpenapiSalespersonDestroyPortImpl implements OpenapiSalespersonDestroyPort {

	private final OpenapiThirdApiV1SalespersonDestroyService destroyService;

	public OpenapiSalespersonDestroyPortImpl(OpenapiThirdApiV1SalespersonDestroyService destroyService) {
		this.destroyService = destroyService;
	}

	@Override
	public void destroySalesperson(long companyId, String employeeNumber) {
		destroyService.executeDestroySalesperson(companyId, employeeNumber);
	}
}
