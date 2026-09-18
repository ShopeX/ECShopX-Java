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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RolesManagementDetailService {

	private static final String ROLE_NAME_LANG_ZH_CN = "zh-CN";

	private final RolesMapper rolesMapper;
	private final ObjectMapper objectMapper;
	private final LangueProperties langueProperties;
	private final CommonLangModReadService commonLangModReadService;

	public RolesManagementDetailService(
			RolesMapper rolesMapper,
			ObjectMapper objectMapper,
			LangueProperties langueProperties,
			CommonLangModReadService commonLangModReadService) {
		this.rolesMapper = rolesMapper;
		this.objectMapper = objectMapper;
		this.langueProperties = langueProperties;
		this.commonLangModReadService = commonLangModReadService;
	}

	public Object getDataInfo(String roleIdRaw, String requestLang, Map<String, Object> jwt) {
		long companyId = requireCompanyId(jwt);

		if (roleIdRaw == null || roleIdRaw.isBlank()) {
			return Collections.emptyList();
		}
		long roleIdLong;
		try {
			roleIdLong = Long.parseLong(roleIdRaw.trim());
		} catch (NumberFormatException e) {
			return Collections.emptyList();
		}

		LambdaQueryWrapper<Roles> w = new LambdaQueryWrapper<>();
		w.eq(Roles::getCompanyId, companyId);
		w.eq(Roles::getRoleId, roleIdLong);
		Roles entity = rolesMapper.selectOne(w);

		if (entity == null) {
			return Collections.emptyList();
		}

		return buildDetailRow(entity, requestLang, companyId);
	}

	private Map<String, Object> buildDetailRow(Roles entity, String requestLang, long companyId) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("role_id", entity.getRoleId());
		row.put("company_id", entity.getCompanyId());
		row.put("distributor_id", entity.getDistributorId() != null ? entity.getDistributorId() : 0L);

		String displayName = entity.getRoleName();
		if (entity.getRoleId() != null && !langueProperties.isDefaultLang(requestLang)) {
			Map<Long, String> roleNameByLocale =
					commonLangModReadService.findRolesRoleNamesByLocale(
							companyId, List.of(entity.getRoleId()), requestLang);
			String tr = roleNameByLocale.get(entity.getRoleId());
			if (StringUtils.hasText(tr)) {
				displayName = tr;
			}
		}
		row.put("role_name", displayName);

		row.put("role_source", entity.getRoleSource());
		row.put("created", entity.getCreated());
		row.put("updated", entity.getUpdated());

		Map<Long, Map<String, String>> roleNameLangByRoleId = Map.of();
		if (entity.getRoleId() != null) {
			roleNameLangByRoleId =
					commonLangModReadService.findRolesRoleNamesAllLocales(
							companyId, List.of(entity.getRoleId()));
		}
		Map<String, String> nameLang =
				entity.getRoleId() != null
						? roleNameLangByRoleId.getOrDefault(entity.getRoleId(), Map.of())
						: Map.of();
		LinkedHashMap<String, String> roleNameLangOut = new LinkedHashMap<>(nameLang);
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

		return row;
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
}
