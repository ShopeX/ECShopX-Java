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

package cn.shopex.ecshopx.companys.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.mapper.OperatorSelfDeliveryOrderGuardMapper;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OperatorStatusChangeService {

	private final OperatorsQueryService operatorsQueryService;
	private final OperatorSelfDeliveryOrderGuardMapper operatorSelfDeliveryOrderGuardMapper;
	private final OperatorsCommandService operatorsCommandService;

	public OperatorStatusChangeService(
			OperatorsQueryService operatorsQueryService,
			OperatorSelfDeliveryOrderGuardMapper operatorSelfDeliveryOrderGuardMapper,
			OperatorsCommandService operatorsCommandService) {
		this.operatorsQueryService = operatorsQueryService;
		this.operatorSelfDeliveryOrderGuardMapper = operatorSelfDeliveryOrderGuardMapper;
		this.operatorsCommandService = operatorsCommandService;
	}

	public Map<String, Object> changeOperatorStatus(
			long companyId, long currentOperatorId, long targetOperatorId, int isDisableInt) {
		if (companyId <= 0) {
			throw new ResourceException("公司id必填");
		}
		if (targetOperatorId <= 0) {
			throw new ResourceException("要修改的账号id必填");
		}

		Map<String, Object> op =
				operatorsQueryService.getInfo(
						Map.of("company_id", companyId, "operator_id", targetOperatorId));
		if (op == null || op.isEmpty()) {
			op = Map.of();
		}

		if (isDisableInt == 1) {
			String opType = op.get("operator_type") != null ? op.get("operator_type").toString() : null;
			if ("self_delivery_staff".equals(opType)) {
				long cnt =
						operatorSelfDeliveryOrderGuardMapper.countInProgressSelfDeliveryOrders(
								companyId, targetOperatorId);
				if (cnt > 0) {
					throw new ResourceException("该配送员还有未配置完的订单，所有订单配送员后可禁用！");
				}
			}
		}

		boolean disable = (isDisableInt == 1);
		return operatorsCommandService.updateDisableOnly(companyId, targetOperatorId, disable);
	}
}
