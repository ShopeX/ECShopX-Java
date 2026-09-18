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

package cn.shopex.ecshopx.companys.service.deliverystaff;

import cn.shopex.ecshopx.companys.domain.EmployeeRelRoles;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.domain.Roles;
import cn.shopex.ecshopx.companys.mapper.EmployeeRelRolesMapper;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.companys.mapper.RolesMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OperatorDeliveryStaffRoleDataService {

	private final EmployeeRelRolesMapper employeeRelRolesMapper;

	private final RolesMapper rolesMapper;

	private final ObjectMapper objectMapper;

	private final OperatorsMapper operatorsMapper;

	public OperatorDeliveryStaffRoleDataService(
			EmployeeRelRolesMapper employeeRelRolesMapper,
			RolesMapper rolesMapper,
			ObjectMapper objectMapper,
			OperatorsMapper operatorsMapper) {
		this.employeeRelRolesMapper = employeeRelRolesMapper;
		this.rolesMapper = rolesMapper;
		this.objectMapper = objectMapper;
		this.operatorsMapper = operatorsMapper;
	}

	/**
	 * For the given company and operators, loads operator–role relations and role definitions in bulk and returns,
	 * per operator id, a list of role rows (metadata and decoded permission payload). Does not attach a permission
	 * tree or other derived permission structures.
	 */
	public List<Map<String, Object>> getRoleDataList(long companyId, long operatorId) {
		LambdaQueryWrapper<Operators> w = new LambdaQueryWrapper<>();
		w.eq(Operators::getCompanyId, companyId).eq(Operators::getOperatorId, operatorId);
		Operators op = operatorsMapper.selectOne(w);
		if (op == null) {
			return List.of();
		}
		Map<Long, List<Map<String, Object>>> batch = roleDataRowsForOperators(companyId, List.of(op));
		return batch.getOrDefault(operatorId, List.of());
	}

	public Map<Long, List<Map<String, Object>>> roleDataRowsForOperators(long companyId, List<Operators> operators) {
		if (operators == null || operators.isEmpty()) {
			return Map.of();
		}
		List<String> opStrIds = new ArrayList<>();
		for (Operators op : operators) {
			if (op.getOperatorId() != null) {
				opStrIds.add(String.valueOf(op.getOperatorId()));
			}
		}
		if (opStrIds.isEmpty()) {
			return Map.of();
		}
		LambdaQueryWrapper<EmployeeRelRoles> relW = new LambdaQueryWrapper<>();
		relW.eq(EmployeeRelRoles::getCompanyId, companyId).in(EmployeeRelRoles::getOperatorId, opStrIds);
		List<EmployeeRelRoles> allRels = employeeRelRolesMapper.selectList(relW);
		Map<Long, List<String>> opToRoleIdStrs = new LinkedHashMap<>();
		for (EmployeeRelRoles rel : allRels) {
			if (rel.getOperatorId() == null || rel.getRoleId() == null) {
				continue;
			}
			long oid;
			try {
				oid = Long.parseLong(rel.getOperatorId().trim());
			} catch (NumberFormatException e) {
				continue;
			}
			opToRoleIdStrs.computeIfAbsent(oid, k -> new ArrayList<>()).add(rel.getRoleId());
		}
		Set<Long> allRoleLongIds = new HashSet<>();
		for (List<String> ids : opToRoleIdStrs.values()) {
			for (String rid : ids) {
				if (!StringUtils.hasText(rid)) {
					continue;
				}
				try {
					allRoleLongIds.add(Long.parseLong(rid.trim()));
				} catch (NumberFormatException ignored) {
					// skip
				}
			}
		}
		Map<Long, Roles> roleById = new HashMap<>();
		if (!allRoleLongIds.isEmpty()) {
			LambdaQueryWrapper<Roles> roleW = new LambdaQueryWrapper<>();
			roleW.eq(Roles::getCompanyId, companyId).in(Roles::getRoleId, allRoleLongIds);
			List<Roles> roleRows = rolesMapper.selectList(roleW);
			for (Roles r : roleRows) {
				if (r.getRoleId() != null) {
					roleById.put(r.getRoleId(), r);
				}
			}
		}
		Map<Long, List<Map<String, Object>>> out = new LinkedHashMap<>();
		for (Operators op : operators) {
			Long oid = op.getOperatorId();
			if (oid == null) {
				continue;
			}
			out.put(oid, roleDataForOperator(op, opToRoleIdStrs.getOrDefault(oid, List.of()), roleById));
		}
		return out;
	}

	private List<Map<String, Object>> roleDataForOperator(
			Operators op, List<String> relRoleIdStrs, Map<Long, Roles> roleById) {
		String opType = op.getOperatorType() != null ? op.getOperatorType() : "";
		Long distributorFilter = null;
		if ("distributor".equals(opType) && !Boolean.TRUE.equals(op.getIsDistributorMain())) {
			List<Map<String, Object>> parsed = parseDistributorIdsJson(op.getDistributorIds());
			if (parsed.isEmpty()) {
				return List.of();
			}
			Object d0 = parsed.get(0).get("distributor_id");
			if (d0 == null) {
				return List.of();
			}
			try {
				distributorFilter = Long.parseLong(d0.toString());
			} catch (NumberFormatException e) {
				return List.of();
			}
		}
		List<Long> roleLongs = new ArrayList<>();
		for (String rs : relRoleIdStrs) {
			if (!StringUtils.hasText(rs)) {
				continue;
			}
			try {
				roleLongs.add(Long.parseLong(rs.trim()));
			} catch (NumberFormatException ignored) {
				// skip
			}
		}
		Map<Long, Roles> matchedById = new LinkedHashMap<>();
		for (Long rid : roleLongs) {
			Roles r = roleById.get(rid);
			if (r == null) {
				continue;
			}
			if (distributorFilter != null) {
				long rd = r.getDistributorId() != null ? r.getDistributorId() : 0L;
				if (distributorFilter != rd) {
					continue;
				}
			}
			matchedById.putIfAbsent(rid, r);
		}
		List<Roles> matched = new ArrayList<>(matchedById.values());
		matched.sort(Comparator.comparing(Roles::getRoleId, Comparator.nullsLast(Long::compareTo)));
		List<Map<String, Object>> list = new ArrayList<>();
		for (Roles r : matched) {
			list.add(roleToRow(r));
		}
		return list;
	}

	private Map<String, Object> roleToRow(Roles r) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("role_id", r.getRoleId());
		m.put("company_id", r.getCompanyId());
		m.put("distributor_id", r.getDistributorId());
		m.put("role_name", r.getRoleName());
		m.put("role_source", r.getRoleSource());
		m.put("permission", decodePermission(r.getPermission()));
		m.put("created", r.getCreated());
		m.put("updated", r.getUpdated());
		return m;
	}

	private Object decodePermission(String json) {
		if (!StringUtils.hasText(json)) {
			return null;
		}
		try {
			return objectMapper.readValue(json.trim(), new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			return null;
		}
	}

	private List<Map<String, Object>> parseDistributorIdsJson(String json) {
		if (!StringUtils.hasText(json)) {
			return List.of();
		}
		try {
			List<Map<String, Object>> parsed =
					objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
			return parsed != null ? parsed : List.of();
		} catch (Exception e) {
			return List.of();
		}
	}
}
