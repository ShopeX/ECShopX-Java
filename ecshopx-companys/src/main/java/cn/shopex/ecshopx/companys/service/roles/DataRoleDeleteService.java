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

package cn.shopex.ecshopx.companys.service.roles;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.companys.domain.EmployeeRelRoles;
import cn.shopex.ecshopx.companys.domain.Roles;
import cn.shopex.ecshopx.companys.mapper.EmployeeRelRolesMapper;
import cn.shopex.ecshopx.companys.mapper.RolesMapper;
import cn.shopex.ecshopx.companys.service.CommonLangModWriteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DataRoleDeleteService {

	private static final String TABLE_LANG = "companys_roles";
	private static final String MODULE_LANG = "companys_roles";

	private final RolesMapper rolesMapper;
	private final EmployeeRelRolesMapper employeeRelRolesMapper;
	private final CommonLangModWriteService commonLangModWriteService;
	private final LangueProperties langueProperties;

	public DataRoleDeleteService(
			RolesMapper rolesMapper,
			EmployeeRelRolesMapper employeeRelRolesMapper,
			CommonLangModWriteService commonLangModWriteService,
			LangueProperties langueProperties) {
		this.rolesMapper = rolesMapper;
		this.employeeRelRolesMapper = employeeRelRolesMapper;
		this.commonLangModWriteService = commonLangModWriteService;
		this.langueProperties = langueProperties;
	}

	private void deleteAllLangForRole(long companyId, long dataId) {
		List<String> langs = langueProperties.getList();
		if (langs == null) {
			return;
		}
		for (String lang : langs) {
			if (StringUtils.hasText(lang)) {
				commonLangModWriteService.deleteLang((int) companyId, TABLE_LANG, dataId, MODULE_LANG, lang);
			}
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteDataRole(String roleIdPath, Map<String, Object> jwt) {
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}

		String p = roleIdPath == null ? "" : roleIdPath.trim();
		if (p.isEmpty()) {
			throw new ResourceException("删除的数据不存在");
		}
		long roleId;
		try {
			roleId = Long.parseLong(p);
		} catch (NumberFormatException e) {
			throw new ResourceException("删除的数据不存在");
		}
		if (roleId <= 0L) {
			throw new ResourceException("删除的数据不存在");
		}

		long companyId = requireCompanyId(jwt);
		String operatorType = strClaim(jwt, "operator_type", "operatorType");

		Long distributorFilter = null;
		if ("distributor".equalsIgnoreCase(operatorType)) {
			Long distId = longClaimOrNull(jwt, "distributor_id", "distributorId");
			if (distId != null && distId > 0L) {
				distributorFilter = distId;
			}
		}

		LambdaQueryWrapper<Roles> baseWrapper = new LambdaQueryWrapper<>();
		baseWrapper.eq(Roles::getCompanyId, companyId).eq(Roles::getRoleId, roleId);
		if (distributorFilter != null) {
			baseWrapper.eq(Roles::getDistributorId, distributorFilter);
		}

		LambdaQueryWrapper<EmployeeRelRoles> relW = new LambdaQueryWrapper<>();
		relW.eq(EmployeeRelRoles::getCompanyId, companyId)
				.eq(EmployeeRelRoles::getRoleId, String.valueOf(roleId));
		long relCount = employeeRelRolesMapper.selectCount(relW);
		if (relCount > 0) {
			throw new ResourceException("该角色有账号关联，不可删除");
		}

		int pageNo = 1;
		final int pageSize = 500;
		boolean firstPage = true;
		while (true) {
			Page<Roles> page = new Page<>(pageNo, pageSize, false);
			Page<Roles> result = rolesMapper.selectPage(page, baseWrapper);
			List<Roles> records = result.getRecords();
			if (records.isEmpty()) {
				if (firstPage) {
					throw new ResourceException("删除的数据不存在");
				}
				break;
			}
			firstPage = false;
			for (Roles row : records) {
				if (row.getRoleId() == null) {
					throw new ResourceException("删除的数据不存在");
				}
				deleteAllLangForRole(companyId, row.getRoleId());
			}
			if (records.size() < pageSize) {
				break;
			}
			pageNo++;
		}

		int removed = rolesMapper.delete(baseWrapper);
		if (removed == 0) {
			throw new ResourceException("删除的数据不存在");
		}
	}

	private long requireCompanyId(Map<String, Object> jwt) {
		Long v = longClaimOrNull(jwt, "company_id", "companyId");
		if (v == null || v <= 0L) {
			throw new ResourceException("登录验证错误");
		}
		return v;
	}

	private static Long longClaimOrNull(Map<String, Object> jwt, String snake, String camel) {
		Object v = jwt.get(snake);
		if (v == null) {
			v = jwt.get(camel);
		}
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String strClaim(Map<String, Object> jwt, String snake, String camel) {
		Object v = jwt.get(snake);
		if (v == null) {
			v = jwt.get(camel);
		}
		return v != null ? v.toString().trim() : "";
	}
}
