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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.domain.Employees;
import cn.shopex.ecshopx.employeepurchase.domain.EnterpriseEmailBox;
import cn.shopex.ecshopx.employeepurchase.domain.Enterprises;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterpriseEmailBoxMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterprisesMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import java.util.Optional;
import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnterpriseDeleteService {

	private final EmployeesMapper employeesMapper;
	private final EnterprisesMapper enterprisesMapper;
	private final EnterpriseEmailBoxMapper enterpriseEmailBoxMapper;

	public EnterpriseDeleteService(
			EmployeesMapper employeesMapper,
			EnterprisesMapper enterprisesMapper,
			EnterpriseEmailBoxMapper enterpriseEmailBoxMapper) {
		this.employeesMapper = employeesMapper;
		this.enterprisesMapper = enterprisesMapper;
		this.enterpriseEmailBoxMapper = enterpriseEmailBoxMapper;
	}

	public Optional<EnterpriseDeleteTarget> resolveForDelete(
			String enterpriseIdRaw, Map<String, Object> operatorJwt) {
		long companyId = readCompanyId(operatorJwt);

		long cnt = employeesMapper.selectCount(buildEmployeesCountByEnterpriseIdWrapper(enterpriseIdRaw));
		if (cnt > 0) {
			throw new ResourceException("请先删除该企业的员工");
		}

		String trimmed = enterpriseIdRaw == null ? "" : enterpriseIdRaw.trim();
		if (trimmed.isEmpty()) {
			return Optional.empty();
		}
		long parsedId;
		try {
			parsedId = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			return Optional.empty();
		}

		Enterprises row =
				enterprisesMapper.selectOne(
						new LambdaQueryWrapper<Enterprises>()
								.eq(Enterprises::getId, parsedId)
								.eq(Enterprises::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (row == null) {
			return Optional.empty();
		}
		return Optional.of(new EnterpriseDeleteTarget(companyId, row.getId()));
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteEnterpriseAndEmailInTx(long companyId, long enterpriseId) {
		try {
			enterprisesMapper.delete(
					new LambdaQueryWrapper<Enterprises>()
							.eq(Enterprises::getId, enterpriseId)
							.eq(Enterprises::getCompanyId, companyId));
			enterpriseEmailBoxMapper.delete(
					new LambdaQueryWrapper<EnterpriseEmailBox>()
							.eq(EnterpriseEmailBox::getCompanyId, companyId)
							.eq(EnterpriseEmailBox::getEnterpriseId, enterpriseId));
		} catch (DataAccessException e) {
			throw new ResourceException(e.getMessage() != null ? e.getMessage() : "删除失败");
		} catch (PersistenceException e) {
			throw new ResourceException(e.getMessage() != null ? e.getMessage() : "删除失败");
		}
	}

	private static LambdaQueryWrapper<Employees> buildEmployeesCountByEnterpriseIdWrapper(
			String enterpriseIdRaw) {
		LambdaQueryWrapper<Employees> w = new LambdaQueryWrapper<>();
		if (enterpriseIdRaw == null) {
			return w.eq(Employees::getEnterpriseId, -1L);
		}
		String trimmed = enterpriseIdRaw.trim();
		if (trimmed.isEmpty()) {
			return w.eq(Employees::getEnterpriseId, -1L);
		}
		try {
			long id = Long.parseLong(trimmed);
			if (id <= 0) {
				return w.eq(Employees::getEnterpriseId, -1L);
			}
			return w.eq(Employees::getEnterpriseId, id);
		} catch (NumberFormatException e) {
			return w.apply("CAST(enterprise_id AS CHAR) = {0}", trimmed);
		}
	}

	private static long readCompanyId(Map<String, Object> operatorJwt) {
		Object co = operatorJwt.get("company_id");
		if (co == null) {
			throw new ForbiddenException("未激活");
		}
		long cid = toLong(co);
		if (cid <= 0) {
			throw new ForbiddenException("未激活");
		}
		return cid;
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
