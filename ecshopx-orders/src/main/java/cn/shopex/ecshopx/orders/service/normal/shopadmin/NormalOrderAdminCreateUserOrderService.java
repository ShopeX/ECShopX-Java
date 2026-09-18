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

package cn.shopex.ecshopx.orders.service.normal.shopadmin;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.stereotype.Service;
import cn.shopex.ecshopx.orders.service.normal.create.NormalOrderCreateState;

@Service
public class NormalOrderAdminCreateUserOrderService {

	private final NormalOrderAdminCreateUserOrderParamBuilder paramBuilder;
	private final ShopadminNormalOrderCreateOrchestrator orchestrator;

	public NormalOrderAdminCreateUserOrderService(
			NormalOrderAdminCreateUserOrderParamBuilder paramBuilder,
			ShopadminNormalOrderCreateOrchestrator orchestrator) {
		this.paramBuilder = paramBuilder;
		this.orchestrator = orchestrator;
	}

	public Map<String, Object> createUserOrder(
			long companyId, long operatorId, HttpServletRequest request, Map<String, Object> mergedInput) {
		NormalOrderCreateState state = paramBuilder.build(companyId, operatorId, mergedInput, request);
		return orchestrator.create(state, request);
	}
}
