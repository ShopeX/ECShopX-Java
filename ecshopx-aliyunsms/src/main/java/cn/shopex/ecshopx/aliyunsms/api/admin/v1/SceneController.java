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

package cn.shopex.ecshopx.aliyunsms.api.admin.v1;

import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsSceneDetailService;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsSceneItemService;
import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsSceneListService;
import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsSceneListService.SceneListQuery;
import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsSceneSimpleListService;
import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsShopRoutePermissionService;
import cn.shopex.ecshopx.aliyunsms.web.AliyunsmsAdminFlexibleInputMerge;
import cn.shopex.ecshopx.aliyunsms.web.AliyunsmsAdminSceneDetailQueryId;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		notFound = true
)
@RestController("aliyunsmsSceneAdminV1")
@RequestMapping("/api/v1/aliyunsms")
public class SceneController {

	private final AliyunsmsSceneItemService aliyunsmsSceneItemService;
	private final AliyunsmsSceneDetailService aliyunsmsSceneDetailService;
	private final AliyunsmsSceneListService aliyunsmsSceneListService;
	private final AliyunsmsSceneSimpleListService aliyunsmsSceneSimpleListService;
	private final CompanysActivationService companysActivationService;
	private final AliyunsmsShopRoutePermissionService aliyunsmsShopRoutePermissionService;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;

	public SceneController(
			AliyunsmsSceneItemService aliyunsmsSceneItemService,
			AliyunsmsSceneDetailService aliyunsmsSceneDetailService,
			AliyunsmsSceneListService aliyunsmsSceneListService,
			AliyunsmsSceneSimpleListService aliyunsmsSceneSimpleListService,
			CompanysActivationService companysActivationService,
			AliyunsmsShopRoutePermissionService aliyunsmsShopRoutePermissionService,
			OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper) {
		this.aliyunsmsSceneItemService = aliyunsmsSceneItemService;
		this.aliyunsmsSceneDetailService = aliyunsmsSceneDetailService;
		this.aliyunsmsSceneListService = aliyunsmsSceneListService;
		this.aliyunsmsSceneSimpleListService = aliyunsmsSceneSimpleListService;
		this.companysActivationService = companysActivationService;
		this.aliyunsmsShopRoutePermissionService = aliyunsmsShopRoutePermissionService;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
	}

	@Activated(routeAlias = "aliyunsms.scene.getList")
	@GetMapping(value = "/scene/list", name = "aliyunsms.scene.getList")
	public ResponseEntity<ApiResult<Map<String, Object>>> getList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) Integer pageIgnored,
			@RequestParam(value = "pageSize", required = false) Integer pageSizeIgnored,
			@RequestParam(value = "page_size", required = false) Integer pageSizeUnderscoreIgnored,
			@RequestParam(value = "scene_name", required = false) String sceneNameIgnored) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		aliyunsmsShopRoutePermissionService.assertSceneGetList(user);

		SceneListQuery q = new SceneListQuery();
		q.setPage(parsePage(request.getParameter("page")));
		q.setPageSize(resolvePageSize(request));
		applySceneNameToQuery(request, q);

		Map<String, Object> data = aliyunsmsSceneListService.getList(companyId, q);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "aliyunsms.scene.getList")
	@GetMapping(value = "/scene/simpleList", name = "aliyunsms.scene.getList")
	public ResponseEntity<ApiResult<Map<String, Object>>> getSimpleList(
			HttpServletRequest request,
			@RequestParam(value = "template_type", required = false) String templateTypeIgnored) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		aliyunsmsShopRoutePermissionService.assertSceneGetList(user);

		boolean templateTypeKeyPresent = request.getParameterMap().containsKey("template_type");
		String templateTypeRaw = request.getParameter("template_type");

		Map<String, Object> data =
				aliyunsmsSceneSimpleListService.getSimpleList(companyId, templateTypeKeyPresent, templateTypeRaw);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "aliyunsms.scene.getDetail")
	@GetMapping(value = "/scene/detail", name = "模板页场景明细")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDetail(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		aliyunsmsShopRoutePermissionService.assertSceneGetDetail(user);

		Optional<Long> pkOpt = AliyunsmsAdminSceneDetailQueryId.resolveSceneDetailPrimaryKey(request);
		Long pk = pkOpt.orElseThrow(
				() -> new ResourceException(
						"The identifier id is missing for a query of AliyunsmsBundle\\Entities\\Scene", 500));
		Map<String, Object> body = aliyunsmsSceneDetailService.getDetail(pk);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@Activated(routeAlias = "aliyunsms.scene.addItem")
	@PostMapping(value = "/scene/addItem", name = "添加场景实例")
	public ResponseEntity<ApiResult<Map<String, Object>>> addItem(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		aliyunsmsShopRoutePermissionService.assertSceneAddItem(user);

		Map<String, Object> merged = AliyunsmsAdminFlexibleInputMerge.merge(request, body);
		int sceneId = parseRequiredIntField(merged, "scene_id", "请选择场景");
		int signId = parseRequiredIntField(merged, "sign_id", "请选择签名");
		int templateId = parseRequiredIntField(merged, "template_id", "请选择模板");

		aliyunsmsSceneItemService.addItem(companyId, sceneId, signId, templateId);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/aliyunsms/scene/addItem");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "添加场景实例");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "aliyunsms.scene.enableItem")
	@GetMapping(value = "/scene/enableItem", name = "启用场景实例")
	public ResponseEntity<ApiResult<Map<String, Object>>> enableItem(
			HttpServletRequest request,
			@RequestParam(value = "id", required = false) String idParam) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		aliyunsmsShopRoutePermissionService.assertSceneEnableItem(user);

		Long itemId = parseOptionalItemId(idParam);
		aliyunsmsSceneItemService.enableItem(companyId, itemId);

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "aliyunsms.scene.disableItem")
	@GetMapping(value = "/scene/disableItem", name = "停用场景实例")
	public ResponseEntity<ApiResult<Map<String, Object>>> disableItem(
			HttpServletRequest request,
			@RequestParam(value = "id", required = false) String idParam) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		aliyunsmsShopRoutePermissionService.assertSceneDisableItem(user);

		Long itemId = parseOptionalItemId(idParam);
		aliyunsmsSceneItemService.disableItem(companyId, itemId);

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "aliyunsms.scene.deleteItem")
	@DeleteMapping(value = "/scene/deleteItem/{id}", name = "移除场景实例")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteItem(
			HttpServletRequest request, @PathVariable("id") String id) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		aliyunsmsShopRoutePermissionService.assertSceneDeleteItem(user);

		long itemId = parseRequiredDeleteItemId(id);
		aliyunsmsSceneItemService.deleteItem(companyId, itemId);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/aliyunsms/scene/deleteItem/" + itemId);
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(Map.of("id", itemId)));
		} catch (Exception e) {
			logCtx.put("params", Map.of("id", itemId).toString());
		}
		logCtx.put("operator_name", "移除场景实例");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}

	private static String clientIp(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(xff)) {
			return xff.split(",")[0].trim();
		}
		return request.getRemoteAddr() != null ? request.getRemoteAddr() : "";
	}

	/**
	 * Parses path {@code id} for deleteItem. Empty / zero throw {@code id必填}.
	 * Non-numeric segments return {@link Long#MIN_VALUE} (sentinel) so that delete affects 0 rows and returns success.
	 */
	private static long parseRequiredDeleteItemId(String id) {
		if (id == null) {
			throw new ResourceException("id必填");
		}
		String t = id.trim();
		if (t.isEmpty()) {
			throw new ResourceException("id必填");
		}
		if ("0".equals(t)) {
			throw new ResourceException("id必填");
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return Long.MIN_VALUE;
		}
	}

	private static Long parseOptionalItemId(String idParam) {
		if (idParam == null) {
			return null;
		}
		String t = idParam.trim();
		if (t.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static void applySceneNameToQuery(HttpServletRequest request, SceneListQuery q) {
		var pm = request.getParameterMap();
		if (!pm.containsKey("scene_name")) {
			q.setSceneNameFilterActive(false);
			return;
		}
		String raw = request.getParameter("scene_name");
		if (raw == null || raw.isEmpty() || "0".equals(raw)) {
			q.setSceneNameFilterActive(false);
			return;
		}
		q.setSceneNameFilterActive(true);
		q.setSceneNameContains(raw);
	}

	private static int resolvePageSize(HttpServletRequest request) {
		var pm = request.getParameterMap();
		if (pm.containsKey("pageSize")) {
			return parsePositiveIntOrDefault(request.getParameter("pageSize"), 10);
		}
		if (pm.containsKey("page_size")) {
			return parsePositiveIntOrDefault(request.getParameter("page_size"), 10);
		}
		return 10;
	}

	private static int parsePositiveIntOrDefault(String raw, int def) {
		if (raw == null) {
			return def;
		}
		try {
			int v = Integer.parseInt(raw.trim());
			return v > 0 ? v : def;
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static int parsePage(String p) {
		if (p == null) {
			return 1;
		}
		try {
			int v = Integer.parseInt(p.trim());
			return v > 0 ? v : 1;
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private static int parseRequiredIntField(Map<String, Object> merged, String key, String message) {
		if (!merged.containsKey(key)) {
			throw new BadRequestException(message);
		}
		Object v = merged.get(key);
		if (v == null) {
			throw new BadRequestException(message);
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			throw new BadRequestException(message);
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException(message);
		}
	}
}
