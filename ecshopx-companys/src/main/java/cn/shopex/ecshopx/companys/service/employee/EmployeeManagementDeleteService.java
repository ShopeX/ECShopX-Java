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

package cn.shopex.ecshopx.companys.service.employee;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.companys.service.EmployeeService;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmployeeManagementDeleteService {

	private final EmployeeService employeeService;

	public EmployeeManagementDeleteService(EmployeeService employeeService) {
		this.employeeService = employeeService;
	}

	public void deleteData(String operatorIdStr, Map<String, Object> jwt) {
		if (!StringUtils.hasText(operatorIdStr)) {
			throw new BadRequestException("id必填");
		}
		long targetOperatorId;
		try {
			targetOperatorId = Long.parseLong(operatorIdStr.trim());
		} catch (NumberFormatException ex) {
			throw new BadRequestException("无效的 operator_id");
		}
		long companyId = EmployeeOrchestrationSupport.requireLongClaim(jwt, "company_id", "companyId");
		long actingOperatorId = EmployeeOrchestrationSupport.requireLongClaim(jwt, "operator_id", "operatorId");
		employeeService.deleteOperatorStaff(targetOperatorId, companyId, actingOperatorId);
	}
}
