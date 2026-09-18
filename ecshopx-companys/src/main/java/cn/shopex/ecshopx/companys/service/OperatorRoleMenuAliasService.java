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

import cn.shopex.ecshopx.companys.domain.EmployeeRelRoles;
import cn.shopex.ecshopx.companys.domain.Roles;
import cn.shopex.ecshopx.companys.mapper.EmployeeRelRolesMapper;
import cn.shopex.ecshopx.companys.mapper.RolesMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 汇总操作员角色绑定的菜单别名（shop_menu.alias_name），用于接口权限校验。
 */
@Service
public class OperatorRoleMenuAliasService {

	private final EmployeeRelRolesMapper employeeRelRolesMapper;
	private final RolesMapper rolesMapper;
	private final ObjectMapper objectMapper;

	public OperatorRoleMenuAliasService(
			EmployeeRelRolesMapper employeeRelRolesMapper,
			RolesMapper rolesMapper,
			ObjectMapper objectMapper) {
		this.employeeRelRolesMapper = employeeRelRolesMapper;
		this.rolesMapper = rolesMapper;
		this.objectMapper = objectMapper;
	}

	/**
	 * @return 无角色关联时返回 null；有关联时返回去重后的别名列表（可能为空列表）
	 */
	public List<String> listShopMenuAliases(long companyId, long operatorId) {
		LambdaQueryWrapper<EmployeeRelRoles> w = new LambdaQueryWrapper<>();
		w.eq(EmployeeRelRoles::getCompanyId, companyId).eq(EmployeeRelRoles::getOperatorId, String.valueOf(operatorId));
		List<EmployeeRelRoles> rels = employeeRelRolesMapper.selectList(w);
		if (rels == null || rels.isEmpty()) {
			return null;
		}
		Set<String> roleIds = new LinkedHashSet<>();
		for (EmployeeRelRoles r : rels) {
			if (r.getRoleId() != null && !r.getRoleId().isEmpty()) {
				roleIds.add(r.getRoleId());
			}
		}
		if (roleIds.isEmpty()) {
			return List.of();
		}
		Set<String> aliases = new LinkedHashSet<>();
		for (String rid : roleIds) {
			Long roleIdLong;
			try {
				roleIdLong = Long.parseLong(rid);
			} catch (NumberFormatException e) {
				continue;
			}
			Roles role = rolesMapper.selectById(roleIdLong);
			if (role == null || role.getCompanyId() == null || role.getCompanyId() != companyId) {
				continue;
			}
			mergeAliasesFromPermissionJson(role.getPermission(), aliases);
		}
		return new ArrayList<>(aliases);
	}

	private void mergeAliasesFromPermissionJson(String permissionJson, Set<String> out) {
		if (permissionJson == null || permissionJson.isBlank()) {
			return;
		}
		try {
			JsonNode root = objectMapper.readTree(permissionJson);
			JsonNode arr = root.get("shopmenu_alias_name");
			if (arr == null || !arr.isArray()) {
				return;
			}
			for (JsonNode n : arr) {
				if (n != null && n.isTextual()) {
					String s = n.asText();
					if (s != null && !s.isEmpty()) {
						out.add(s);
					}
				}
			}
		} catch (Exception ignored) {
			// 权限 JSON 异常时视为无菜单别名
		}
	}
}
