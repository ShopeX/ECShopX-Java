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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.companys.domain.Roles;
import cn.shopex.ecshopx.companys.dto.CreateDataRoleRequest;
import cn.shopex.ecshopx.companys.mapper.RolesMapper;
import cn.shopex.ecshopx.companys.service.CommonLangModWriteService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataRoleCreateService {

	private static final String SAVE_LANG_TABLE = "companys_roles";
	private static final String SAVE_LANG_MODULE = "companys_roles";

	private static final List<String> LANG_STRIP_KEYS = List.of("role_name");

	private final LangueProperties langueProperties;
	private final RolesMapper rolesMapper;
	private final ObjectMapper objectMapper;
	private final CommonLangModWriteService commonLangModWriteService;

	public DataRoleCreateService(
			LangueProperties langueProperties,
			RolesMapper rolesMapper,
			ObjectMapper objectMapper,
			CommonLangModWriteService commonLangModWriteService) {
		this.langueProperties = langueProperties;
		this.rolesMapper = rolesMapper;
		this.objectMapper = objectMapper;
		this.commonLangModWriteService = commonLangModWriteService;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> create(
			CreateDataRoleRequest body,
			String roleSource,
			String requestLang,
			Map<String, Object> jwtUserData) {
		if (body == null) {
			throw new BadRequestException("请求体不能为空");
		}
		String roleName = body.getRoleName();
		if (roleName == null || roleName.isBlank()) {
			throw new BadRequestException("角色名称不能为空");
		}
		Map<String, Object> permission = body.getPermission();
		if (permission == null || permission.isEmpty()) {
			throw new BadRequestException("permission 不能为空");
		}
		if (roleSource == null || roleSource.isBlank()) {
			throw new BadRequestException("角色来源不能为空");
		}
		if (!permission.containsKey("shopmenu_alias_name")
				|| permission.get("shopmenu_alias_name") == null) {
			throw new BadRequestException("请选中角色菜单权限");
		}

		long companyId = requireCompanyId(jwtUserData);
		String operatorName = resolveOperatorName(jwtUserData);

		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("role_name", roleName);
		params.put("role_source", roleSource);
		params.put("permission", permission);
		params.put("company_id", companyId);
		params.put("operator_name", operatorName);

		String operatorType = strClaim(jwtUserData, "operator_type", "operatorType");
		if ("distributor".equalsIgnoreCase(operatorType)) {
			Long distId = longClaimOrNull(jwtUserData, "distributor_id", "distributorId");
			params.put("distributor_id", distId != null ? distId : 0L);
		}

		String permissionJson;
		try {
			permissionJson = objectMapper.writeValueAsString(permission);
		} catch (JsonProcessingException e) {
			throw new ResourceException("权限数据格式错误");
		}

		Map<String, Object> dataForMain =
				langueProperties.isDefaultLang(requestLang) ? params : stripLangFields(params);

		Roles entity = new Roles();
		applyColumnData(entity, dataForMain, permissionJson);

		int nowSec = (int) (System.currentTimeMillis() / 1000L);
		if (entity.getCreated() == null) {
			entity.setCreated(nowSec);
		}
		if (entity.getUpdated() == null) {
			entity.setUpdated(nowSec);
		}

		int rows = rolesMapper.insert(entity);
		if (rows != 1) {
			throw new ResourceException("角色保存失败");
		}

		Map<String, Object> langSource = buildLangSource(params, entity, requestLang);
		Map<String, String> langBag = buildLangBag(langSource);
		if (!langBag.isEmpty()) {
			commonLangModWriteService.saveLang(
					(int) companyId,
					langBag,
					SAVE_LANG_TABLE,
					entity.getRoleId().intValue(),
					SAVE_LANG_MODULE,
					requestLang);
		}

		return toColumnNamesData(entity);
	}

	private long requireCompanyId(Map<String, Object> jwt) {
		Long v = longClaimOrNull(jwt, "company_id", "companyId");
		if (v == null || v <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		return v;
	}

	private static String resolveOperatorName(Map<String, Object> jwt) {
		String u = strClaim(jwt, "username", "userName");
		if (!u.isBlank()) {
			return u;
		}
		return strClaim(jwt, "login_name", "loginName");
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
		return v != null ? v.toString() : "";
	}

	private static Map<String, Object> stripLangFields(Map<String, Object> params) {
		LinkedHashMap<String, Object> copy = new LinkedHashMap<>(params);
		for (String k : LANG_STRIP_KEYS) {
			copy.remove(k);
		}
		return copy;
	}

	private Map<String, Object> buildLangSource(
			Map<String, Object> params, Roles entity, String requestLang) {
		Map<String, Object> entityMap = entityToSnakeMap(entity);
		if (!langueProperties.isDefaultLang(requestLang)) {
			LinkedHashMap<String, Object> merged = new LinkedHashMap<>(entityMap);
			for (Map.Entry<String, Object> e : params.entrySet()) {
				merged.put(e.getKey(), e.getValue());
			}
			return merged;
		}
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>(params);
		for (Map.Entry<String, Object> e : entityMap.entrySet()) {
			merged.putIfAbsent(e.getKey(), e.getValue());
		}
		return merged;
	}

	private Map<String, Object> entityToSnakeMap(Roles e) {
		Map<String, Object> m = new LinkedHashMap<>();
		putIfNonNull(m, "role_id", e.getRoleId());
		putIfNonNull(m, "company_id", e.getCompanyId());
		putIfNonNull(m, "distributor_id", e.getDistributorId());
		putIfNonNull(m, "role_name", e.getRoleName());
		putIfNonNull(m, "role_source", e.getRoleSource());
		putIfNonNull(m, "permission", e.getPermission());
		putIfNonNull(m, "created", e.getCreated());
		putIfNonNull(m, "updated", e.getUpdated());
		return m;
	}

	private static void putIfNonNull(Map<String, Object> m, String k, Object v) {
		if (v != null) {
			m.put(k, v);
		}
	}

	private Map<String, String> buildLangBag(Map<String, Object> langSource) {
		Map<String, String> bag = new LinkedHashMap<>();
		if (!langSource.containsKey("role_name")) {
			return bag;
		}
		Object v = langSource.get("role_name");
		if (isEffectivelyEmpty(v)) {
			return bag;
		}
		bag.put("role_name", String.valueOf(v));
		return bag;
	}

	private static boolean isEffectivelyEmpty(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof Boolean b) {
			return !b;
		}
		if (v instanceof Number n) {
			return n.doubleValue() == 0;
		}
		if (v instanceof CharSequence cs) {
			String s = cs.toString().trim();
			return s.isEmpty() || "0".equals(s);
		}
		if (v instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (v instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		if (v instanceof Object[] a) {
			return a.length == 0;
		}
		return false;
	}

	private void applyColumnData(Roles entity, Map<String, Object> data, String permissionJson) {
		if (data.containsKey("company_id") && ValuePresence.hasEffectiveValue(data.get("company_id"))) {
			entity.setCompanyId(toLong(data.get("company_id")));
		}
		if (data.containsKey("distributor_id")) {
			Long d = toLongOrNull(data.get("distributor_id"));
			entity.setDistributorId(d != null ? d : 0L);
		} else {
			entity.setDistributorId(0L);
		}
		if (data.containsKey("role_name") && ValuePresence.hasEffectiveValue(data.get("role_name"))) {
			entity.setRoleName(String.valueOf(data.get("role_name")));
		}
		if (data.containsKey("role_source") && ValuePresence.hasEffectiveValue(data.get("role_source"))) {
			entity.setRoleSource(String.valueOf(data.get("role_source")));
		}
		entity.setPermission(permissionJson);
	}

	private static Long toLongOrNull(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		return Long.parseLong(s);
	}

	private static Long toLong(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}

	private Map<String, Object> toColumnNamesData(Roles entity) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("role_id", entity.getRoleId());
		out.put("company_id", entity.getCompanyId());
		out.put("distributor_id", entity.getDistributorId() != null ? entity.getDistributorId() : 0L);
		out.put("role_name", entity.getRoleName());
		out.put("role_source", entity.getRoleSource());
		out.put("permission", decodeJsonMaybe(entity.getPermission()));
		out.put("created", entity.getCreated());
		out.put("updated", entity.getUpdated());
		return out;
	}

	private Object decodeJsonMaybe(String raw) {
		if (raw == null || raw.isEmpty()) {
			return raw;
		}
		try {
			JsonNode node = objectMapper.readTree(raw);
			return objectMapper.convertValue(node, Object.class);
		} catch (JsonProcessingException e) {
			return raw;
		}
	}
}
