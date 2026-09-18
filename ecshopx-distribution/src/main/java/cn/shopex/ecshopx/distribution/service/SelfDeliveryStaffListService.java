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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.companys.service.employee.EmployeeManagementListService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SelfDeliveryStaffListService {

	private final EmployeeManagementListService employeeManagementListService;

	public SelfDeliveryStaffListService(EmployeeManagementListService employeeManagementListService) {
		this.employeeManagementListService = employeeManagementListService;
	}

	public Map<String, Object> getSelfDeliveryList(
			long companyId, List<Long> selfDeliveryOperatorIds, String requestLangTag) {
		if (companyId <= 0L) {
			throw new BadRequestException("参数 company_id 错误");
		}
		return employeeManagementListService.listSelfDeliveryStaffForH5Wxapp(
				companyId, selfDeliveryOperatorIds, requestLangTag);
	}
}
