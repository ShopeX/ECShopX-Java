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
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.companys.domain.Roles;
import cn.shopex.ecshopx.companys.mapper.RolesMapper;
import cn.shopex.ecshopx.companys.service.CommonLangModReadService;
import cn.shopex.ecshopx.companys.service.permission.PermissionShopMenuTreeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RolesManagementListService {

	private static final String ROLE_NAME_LANG_ZH_CN = "zh-CN";

	private final RolesMapper rolesMapper;
	private final ObjectMapper objectMapper;
	private final LangueProperties langueProperties;
	private final CommonLangModReadService commonLangModReadService;
	private final PermissionShopMenuTreeService permissionShopMenuTreeService;

	public RolesManagementListService(
			RolesMapper rolesMapper,
			ObjectMapper objectMapper,
			LangueProperties langueProperties,
			CommonLangModReadService commonLangModReadService,
			PermissionShopMenuTreeService permissionShopMenuTreeService) {
		this.rolesMapper = rolesMapper;
		this.objectMapper = objectMapper;
		this.langueProperties = langueProperties;
		this.commonLangModReadService = commonLangModReadService;
		this.permissionShopMenuTreeService = permissionShopMenuTreeService;
	}

	public Map<String, Object> getDataList(
			int page,
			int pageSize,
			String roleIdParam,
			String roleNameParam,
			String roleSourceParam,
			boolean decodePermission,
			String requestLang,
			Map<String, Object> jwt) {
		long companyId = requireCompanyId(jwt);
		String operatorType = strClaim(jwt, "operator_type", "operatorType");

		LambdaQueryWrapper<Roles> w = new LambdaQueryWrapper<>();
		w.eq(Roles::getCompanyId, companyId);

		String rs =
				roleSourceParam == null || roleSourceParam.isBlank()
						? "platform"
						: roleSourceParam.trim();
		w.eq(Roles::getRoleSource, rs);

		if (StringUtils.hasText(roleIdParam)) {
			try {
				long rid = Long.parseLong(roleIdParam.trim());
				w.eq(Roles::getRoleId, rid);
			} catch (NumberFormatException ignored) {
				// omit role_id filter when unparsable
			}
		}
		if (StringUtils.hasText(roleNameParam)) {
			w.eq(Roles::getRoleName, roleNameParam.trim());
		}

		if ("distributor".equalsIgnoreCase(operatorType)) {
			Long distId = longClaimOrNull(jwt, "distributor_id", "distributorId");
			w.eq(Roles::getDistributorId, distId != null ? distId : 0L);
		}

		w.orderByAsc(Roles::getRoleId);

		Page<Roles> p = new Page<>(page, pageSize);
		rolesMapper.selectPage(p, w);
		long total = p.getTotal();
		List<Roles> records = p.getRecords();

		List<Long> roleIds = new ArrayList<>();
		if (records != null) {
			for (Roles r : records) {
				if (r.getRoleId() != null) {
					roleIds.add(r.getRoleId());
				}
			}
		}

		Map<Long, String> roleNameByLocale = Map.of();
		if (!langueProperties.isDefaultLang(requestLang) && !roleIds.isEmpty()) {
			roleNameByLocale =
					commonLangModReadService.findRolesRoleNamesByLocale(companyId, roleIds, requestLang);
		}

		Map<Long, Map<String, String>> roleNameLangByRoleId = Map.of();
		if (!roleIds.isEmpty()) {
			roleNameLangByRoleId =
					commonLangModReadService.findRolesRoleNamesAllLocales(companyId, roleIds);
		}

		List<Map<String, Object>> list = new ArrayList<>();
		if (records != null) {
			for (Roles entity : records) {
				list.add(buildRow(entity, decodePermission, requestLang, roleNameByLocale, roleNameLangByRoleId));
			}
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", total);
		data.put("list", list);
		return data;
	}

	private Map<String, Object> buildRow(
			Roles entity,
			boolean decodePermission,
			String requestLang,
			Map<Long, String> roleNameByLocale,
			Map<Long, Map<String, String>> roleNameLangByRoleId) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("role_id", entity.getRoleId());
		row.put("company_id", entity.getCompanyId());
		row.put("distributor_id", entity.getDistributorId() != null ? entity.getDistributorId() : 0L);

		String displayName = entity.getRoleName();
		if (entity.getRoleId() != null) {
			String tr = roleNameByLocale.get(entity.getRoleId());
			if (StringUtils.hasText(tr)) {
				displayName = tr;
			}
		}
		row.put("role_name", displayName);

		row.put("role_source", entity.getRoleSource());
		row.put("created", entity.getCreated());
		row.put("updated", entity.getUpdated());

		Map<String, String> nameLang =
				entity.getRoleId() != null
						? roleNameLangByRoleId.getOrDefault(entity.getRoleId(), Map.of())
						: Map.of();
		LinkedHashMap<String, String> roleNameLangOut = new LinkedHashMap<>(nameLang);
		// When common_lang_mod has no role_name row, still expose zh-CN from DB default name.
		if (entity.getRoleId() != null) {
			String cn = roleNameLangOut.get(ROLE_NAME_LANG_ZH_CN);
			if (nameLang.isEmpty() || !StringUtils.hasText(cn)) {
				String defaultName = entity.getRoleName() != null ? entity.getRoleName() : "";
				roleNameLangOut.put(ROLE_NAME_LANG_ZH_CN, defaultName);
			}
		}
		row.put("role_name_lang", roleNameLangOut);

		Object permDecoded = decodePermissionField(entity.getPermission());
		row.put("permission", permDecoded);

		if (decodePermission) {
			maybeAttachPermissionTree(row, permDecoded, entity, requestLang);
		}

		return row;
	}

	private void maybeAttachPermissionTree(
			Map<String, Object> row, Object permDecoded, Roles entity, String requestLang) {
		if (permDecoded == null) {
			return;
		}
		if (permDecoded instanceof Map<?, ?> m && m.isEmpty()) {
			return;
		}
		if (permDecoded instanceof String s) {
			if (s.isEmpty()) {
				return;
			}
			return;
		}
		if (!(permDecoded instanceof Map<?, ?>)) {
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> perm = (Map<String, Object>) permDecoded;
		if (!perm.containsKey("shopmenu_alias_name")) {
			return;
		}
		Set<String> aliasSet = extractShopmenuAliases(perm);
		if (aliasSet.isEmpty()) {
			return;
		}
		String rs = entity.getRoleSource();
		int menuVersion = "platform".equalsIgnoreCase(rs) ? 1 : 3;
		long rowCompanyId = entity.getCompanyId() != null ? entity.getCompanyId() : 0L;
		List<Map<String, Object>> tree =
				permissionShopMenuTreeService.getRolePermissionMenuTree(
						rowCompanyId, menuVersion, aliasSet, requestLang);
		row.put("permission_tree", tree);
	}

	private Set<String> extractShopmenuAliases(Map<String, Object> perm) {
		Object v = perm.get("shopmenu_alias_name");
		Set<String> out = new LinkedHashSet<>();
		if (v instanceof Collection<?> c) {
			for (Object o : c) {
				if (o != null) {
					String s = o.toString().trim();
					if (!s.isEmpty()) {
						out.add(s);
					}
				}
			}
		} else if (v != null) {
			String s = v.toString().trim();
			if (!s.isEmpty()) {
				out.add(s);
			}
		}
		return out;
	}

	private Object decodePermissionField(String raw) {
		if (raw == null) {
			return null;
		}
		if (raw.isEmpty()) {
			return null;
		}
		try {
			JsonNode node = objectMapper.readTree(raw);
			return objectMapper.convertValue(node, Object.class);
		} catch (JsonProcessingException e) {
			return raw;
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

