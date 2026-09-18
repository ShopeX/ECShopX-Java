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

package cn.shopex.ecshopx.salesperson.integration;

import cn.shopex.ecshopx.common.salesperson.port.OpenapiSalespersonCompleteNewUserPort;
import cn.shopex.ecshopx.salesperson.service.OpenapiSalespersonCompleteNewUserService;
import org.springframework.stereotype.Component;

@Component
public class OpenapiSalespersonCompleteNewUserPortImpl implements OpenapiSalespersonCompleteNewUserPort {

	private final OpenapiSalespersonCompleteNewUserService completeNewUserService;

	public OpenapiSalespersonCompleteNewUserPortImpl(
			OpenapiSalespersonCompleteNewUserService completeNewUserService) {
		this.completeNewUserService = completeNewUserService;
	}

	@Override
	public void completeNewUser(long companyId, long salespersonId, long userId) {
		completeNewUserService.completeNewUser(companyId, salespersonId, userId);
	}
}
