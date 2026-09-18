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

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.employeepurchase.domain.Employees;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeesMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class EmployeeDetailService {

	private final EmployeesMapper employeesMapper;

	public EmployeeDetailService(EmployeesMapper employeesMapper) {
		this.employeesMapper = employeesMapper;
	}

	public Object getInfoData(String employeeIdRaw, Map<String, Object> operatorJwt) {
		long companyId = readCompanyId(operatorJwt);
		LambdaQueryWrapper<Employees> w = buildInfoLookupWrapper(companyId, employeeIdRaw);
		Employees row = employeesMapper.selectOne(w.last("LIMIT 1"));
		if (row == null) {
			return Collections.emptyList();
		}
		return toColumnNamesMap(row);
	}

	private static LambdaQueryWrapper<Employees> buildInfoLookupWrapper(long companyId, String employeeIdRaw) {
		LambdaQueryWrapper<Employees> w =
				new LambdaQueryWrapper<Employees>().eq(Employees::getCompanyId, companyId);
		if (employeeIdRaw == null) {
			return w.eq(Employees::getId, -1L);
		}
		String trimmed = employeeIdRaw.trim();
		if (trimmed.isEmpty()) {
			return w.eq(Employees::getId, -1L);
		}
		try {
			long id = Long.parseLong(trimmed);
			if (id <= 0) {
				return w.eq(Employees::getId, -1L);
			}
			return w.eq(Employees::getId, id);
		} catch (NumberFormatException e) {
			return w.apply("CAST(id AS CHAR) = {0}", trimmed);
		}
	}

	private static Map<String, Object> toColumnNamesMap(Employees e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("company_id", e.getCompanyId());
		m.put("distributor_id", e.getDistributorId());
		m.put("operator_id", e.getOperatorId());
		m.put("name", e.getName());
		m.put("mobile", e.getMobile());
		m.put("account", e.getAccount());
		m.put("email", e.getEmail());
		m.put("enterprise_id", e.getEnterpriseId());
		m.put("auth_code", e.getAuthCode());
		m.put("user_id", e.getUserId());
		m.put("member_mobile", e.getMemberMobile());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		m.put("disabled", e.getDisabled());
		return m;
	}

	private static long readCompanyId(Map<String, Object> operatorJwt) {
		Object co = operatorJwt.get("company_id");
		if (co == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(co);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}
		return companyId;
	}

	private static long toLong(Object co) {
		if (co instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(co.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
