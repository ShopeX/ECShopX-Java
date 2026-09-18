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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.domain.Enterprises;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterprisesMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmployeePurchaseEmployeesImportRowService {

	private final EnterprisesMapper enterprisesMapper;
	private final EmployeeCreateService employeeCreateService;

	public EmployeePurchaseEmployeesImportRowService(
			EnterprisesMapper enterprisesMapper, EmployeeCreateService employeeCreateService) {
		this.enterprisesMapper = enterprisesMapper;
		this.employeeCreateService = employeeCreateService;
	}

	public void acceptRow(long companyId, long operatorId, long distributorId, Map<String, Object> row) {
		String enterpriseSn = trim(row.get("enterprise_sn"));
		if (!StringUtils.hasText(enterpriseSn)) {
			throw new BadRequestException("企业编码不能为空");
		}
		String name = trim(row.get("name"));
		if (!StringUtils.hasText(name)) {
			throw new BadRequestException("姓名必填");
		}

		long rowDist = parseLongLoose(row.get("distributor_id"));
		long dist = rowDist > 0L ? rowDist : distributorId;

		Enterprises enterprise =
				enterprisesMapper.selectOne(
						new LambdaQueryWrapper<Enterprises>()
								.eq(Enterprises::getCompanyId, companyId)
								.eq(Enterprises::getEnterpriseSn, enterpriseSn)
								.eq(Enterprises::getDistributorId, (int) Math.min(Integer.MAX_VALUE, Math.max(0, dist)))
								.last("LIMIT 1"));
		if (enterprise == null) {
			throw new ResourceException("企业不存在");
		}
		if (Boolean.FALSE.equals(enterprise.getIsEmployeeCheckEnabled())) {
			throw new ResourceException("该企业无需导入白名单");
		}

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("enterprise_id", enterprise.getId());
		merged.put("name", name);
		if (row.get("mobile") != null) {
			merged.put("mobile", trim(row.get("mobile")));
		}
		if (row.get("account") != null) {
			merged.put("account", trim(row.get("account")));
		}
		if (row.get("auth_code") != null) {
			merged.put("auth_code", trim(row.get("auth_code")));
		}
		if (row.get("email") != null) {
			merged.put("email", trim(row.get("email")));
		}

		LinkedHashMap<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("company_id", companyId);
		jwt.put("distributor_id", dist);
		jwt.put("operator_id", (int) Math.min(Integer.MAX_VALUE, Math.max(0, operatorId)));

		employeeCreateService.create(merged, jwt);
	}

	private static String trim(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}

	private static long parseLongLoose(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
