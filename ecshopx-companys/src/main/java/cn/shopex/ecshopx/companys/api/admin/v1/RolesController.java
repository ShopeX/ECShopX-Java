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

package cn.shopex.ecshopx.companys.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.companys.dto.CreateDataRoleRequest;
import cn.shopex.ecshopx.companys.dto.UpdateDataRoleRequest;
import cn.shopex.ecshopx.companys.service.roles.DataRoleCreateService;
import cn.shopex.ecshopx.companys.service.roles.DataRoleDeleteService;
import cn.shopex.ecshopx.companys.service.roles.DataRoleUpdateService;
import cn.shopex.ecshopx.companys.service.roles.RolesManagementDetailService;
import cn.shopex.ecshopx.companys.service.roles.RolesManagementListService;
import cn.shopex.ecshopx.companys.service.roles.RolesPermissionQueryService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("companysAdminV1Roles")
@RequestMapping("/api/v1")
public class RolesController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final DataRoleCreateService dataRoleCreateService;
	private final DataRoleDeleteService dataRoleDeleteService;
	private final DataRoleUpdateService dataRoleUpdateService;
	private final LangueProperties langueProperties;
	private final RolesPermissionQueryService rolesPermissionQueryService;
	private final RolesManagementListService rolesManagementListService;
	private final RolesManagementDetailService rolesManagementDetailService;

	public RolesController(
			DataRoleCreateService dataRoleCreateService,
			DataRoleDeleteService dataRoleDeleteService,
			DataRoleUpdateService dataRoleUpdateService,
			LangueProperties langueProperties,
			RolesPermissionQueryService rolesPermissionQueryService,
			RolesManagementListService rolesManagementListService,
			RolesManagementDetailService rolesManagementDetailService) {
		this.dataRoleCreateService = dataRoleCreateService;
		this.dataRoleDeleteService = dataRoleDeleteService;
		this.dataRoleUpdateService = dataRoleUpdateService;
		this.langueProperties = langueProperties;
		this.rolesPermissionQueryService = rolesPermissionQueryService;
		this.rolesManagementListService = rolesManagementListService;
		this.rolesManagementDetailService = rolesManagementDetailService;
	}

	@GetMapping(value = "/permission", name = "获取权限详情")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getPermission(
			@RequestParam(name = "version", required = false) String version, HttpServletRequest request) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		String requestLang = RequestLangTag.current(langueProperties);
		List<Map<String, Object>> data = rolesPermissionQueryService.getPermission(version, jwt, requestLang);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "roles.create")
	@PostMapping(value = "/roles/management", name = "创建企业员工角色")
	public ResponseEntity<ApiResult<Map<String, Object>>> createDataRole(
			HttpServletRequest request,
			@FlexibleBody CreateDataRoleRequest body,
			@RequestParam(name = "role_source", required = false) String roleSourceParam) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		String roleSource =
				resolveRoleSource(body != null ? body.getRoleSource() : null, roleSourceParam);
		String requestLang = RequestLangTag.current(langueProperties);
		Map<String, Object> data = dataRoleCreateService.create(body, roleSource, requestLang, jwt);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "roles.list")
	@GetMapping(value = "/roles/management", name = "获取角色列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDataList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false, defaultValue = "1") String pageRaw,
			@RequestParam(name = "pageSize", required = false, defaultValue = "1000") String pageSizeRaw,
			@RequestParam(name = "role_id", required = false) String roleId,
			@RequestParam(name = "role_name", required = false) String roleName,
			@RequestParam(name = "role_source", required = false, defaultValue = "platform") String roleSource,
			@RequestParam(name = "is_decode_permission", required = false) String isDecodePermissionRaw) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		int page = Math.max(1, parseIntLoose(pageRaw, 1));
		int pageSize = clamp(parseIntLoose(pageSizeRaw, 1000), 1, 1000);
		boolean decodePermission = resolveDecodePermission(isDecodePermissionRaw);
		String requestLang = RequestLangTag.current(langueProperties);
		Map<String, Object> data =
				rolesManagementListService.getDataList(
						page,
						pageSize,
						roleId,
						roleName,
						roleSource,
						decodePermission,
						requestLang,
						jwt);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static int parseIntLoose(String raw, int defaultVal) {
		if (raw == null || raw.isBlank()) {
			return defaultVal;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static int clamp(int v, int min, int max) {
		return Math.max(min, Math.min(max, v));
	}

	private static boolean resolveDecodePermission(String raw) {
		if (raw == null) {
			return true;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return false;
		}
		if ("0".equals(t)) {
			return false;
		}
		return true;
	}

	private static String resolveRoleSource(String bodyRoleSource, String queryRoleSource) {
		if (bodyRoleSource != null && !bodyRoleSource.isBlank()) {
			return bodyRoleSource.trim();
		}
		if (queryRoleSource != null && !queryRoleSource.isBlank()) {
			return queryRoleSource.trim();
		}
		return "platform";
	}

	@Activated(routeAlias = "roles.info")
	@GetMapping(value = "/roles/management/{role_id}", name = "获取角色详情")
	public ResponseEntity<ApiResult<Object>> getDataInfo(
			HttpServletRequest request, @PathVariable("role_id") String roleId) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		String requestLang = RequestLangTag.current(langueProperties);
		Object body = rolesManagementDetailService.getDataInfo(roleId, requestLang, jwt);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@Activated(routeAlias = "roles.update")
	@PatchMapping(value = "/roles/management/{role_id}", name = "更新企业员工角色")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> updateDataRole(
			HttpServletRequest request,
			@PathVariable("role_id") String roleId,
			@RequestParam(name = "role_id", required = false) String queryRoleId,
			@FlexibleBody UpdateDataRoleRequest body,
			@RequestParam(name = "role_source", required = false) String roleSourceParam) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		String roleSource =
				resolveRoleSource(body != null ? body.getRoleSource() : null, roleSourceParam);
		String requestLang = RequestLangTag.current(langueProperties);
		List<Map<String, Object>> data =
				dataRoleUpdateService.update(roleId, queryRoleId, body, roleSource, requestLang, jwt);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "roles.delete")
	@DeleteMapping(value = "/roles/management/{role_id}", name = "删除角色")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteDataRole(
			HttpServletRequest request, @PathVariable("role_id") String roleId) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		dataRoleDeleteService.deleteDataRole(roleId, jwt);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}
}
