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

import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsShopRoutePermissionService;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsTemplateAddService;
import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsTemplateInfoService;
import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsTemplateDeleteService;
import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsTemplateListService;
import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsTemplateListService.TemplateListQuery;
import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsTemplateModifyService;
import cn.shopex.ecshopx.aliyunsms.web.AliyunsmsAdminFlexibleInputMerge;
import cn.shopex.ecshopx.aliyunsms.web.AliyunsmsAdminSignInfoQueryId;
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
import java.util.List;
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
@RestController("aliyunsmsTemplateAdminV1")
@RequestMapping("/api/v1/aliyunsms")
public class TemplateController {

	private final CompanysActivationService companysActivationService;
	private final AliyunsmsShopRoutePermissionService aliyunsmsShopRoutePermissionService;
	private final AliyunsmsTemplateAddService aliyunsmsTemplateAddService;
	private final AliyunsmsTemplateModifyService aliyunsmsTemplateModifyService;
	private final AliyunsmsTemplateInfoService aliyunsmsTemplateInfoService;
	private final AliyunsmsTemplateListService aliyunsmsTemplateListService;
	private final AliyunsmsTemplateDeleteService aliyunsmsTemplateDeleteService;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;

	public TemplateController(
			CompanysActivationService companysActivationService,
			AliyunsmsShopRoutePermissionService aliyunsmsShopRoutePermissionService,
			AliyunsmsTemplateAddService aliyunsmsTemplateAddService,
			AliyunsmsTemplateModifyService aliyunsmsTemplateModifyService,
			AliyunsmsTemplateInfoService aliyunsmsTemplateInfoService,
			AliyunsmsTemplateListService aliyunsmsTemplateListService,
			AliyunsmsTemplateDeleteService aliyunsmsTemplateDeleteService,
			OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper) {
		this.companysActivationService = companysActivationService;
		this.aliyunsmsShopRoutePermissionService = aliyunsmsShopRoutePermissionService;
		this.aliyunsmsTemplateAddService = aliyunsmsTemplateAddService;
		this.aliyunsmsTemplateModifyService = aliyunsmsTemplateModifyService;
		this.aliyunsmsTemplateInfoService = aliyunsmsTemplateInfoService;
		this.aliyunsmsTemplateListService = aliyunsmsTemplateListService;
		this.aliyunsmsTemplateDeleteService = aliyunsmsTemplateDeleteService;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
	}

	@Activated(routeAlias = "aliyunsms.tmpl.getList")
	@GetMapping(value = "/template/list", name = "aliyunsms.tmpl.getList")
	public ResponseEntity<ApiResult<Map<String, Object>>> getList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) Integer pageIgnored,
			@RequestParam(value = "pageSize", required = false) Integer pageSizeIgnored,
			@RequestParam(value = "template_name", required = false) String templateNameIgnored,
			@RequestParam(value = "status", required = false) String statusIgnored,
			@RequestParam(value = "template_type", required = false) String templateTypeIgnored,
			@RequestParam(value = "scene_id", required = false) String sceneIdIgnored) {
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
		aliyunsmsShopRoutePermissionService.assertTemplateGetList(user);

		TemplateListQuery q = new TemplateListQuery();
		q.setPage(parsePageRawForTaskList(request.getParameter("page")));
		q.setPageSize(resolveTemplateListPageSize(request));
		applyTemplateNameToTemplateListQuery(request, q);
		var pm = request.getParameterMap();
		q.setStatusKeyPresent(pm.containsKey("status"));
		q.setStatusValue(request.getParameter("status"));
		applyTemplateTypeToTemplateListQuery(request, q);
		applySceneIdToTemplateListQuery(request, q);

		Map<String, Object> data = aliyunsmsTemplateListService.getList(companyId, q);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "aliyunsms.tmpl.getInfo")
	@GetMapping(value = "/template/info", name = "aliyunsms.tmpl.getInfo")
	public ResponseEntity<ApiResult<Object>> getInfo(HttpServletRequest request) {
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
		aliyunsmsShopRoutePermissionService.assertTemplateGetInfo(user);

		Optional<Long> idOpt = AliyunsmsAdminSignInfoQueryId.resolveSignInfoPrimaryKey(request);
		Long id = idOpt.orElse(null);
		Map<String, Object> data = aliyunsmsTemplateInfoService.getInfo(companyId, id);
		if (data.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(List.of()));
		}
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "aliyunsms.tmpl.add")
	@PostMapping(value = "/template/add", name = "新增模板")
	public ResponseEntity<ApiResult<Map<String, Object>>> addTemplate(
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
		aliyunsmsShopRoutePermissionService.assertTemplateAdd(user);

		Map<String, Object> merged = AliyunsmsAdminFlexibleInputMerge.merge(request, body);
		validateAddTemplateMerged(merged);

		String templateName = stringValue(merged.get("template_name"));
		int templateNameCp = templateName.codePointCount(0, templateName.length());
		if (templateNameCp < 1 || templateNameCp > 30) {
			throw new ResourceException("模板名称有效长度1-30个字符");
		}
		String templateContent = stringValue(merged.get("template_content"));
		int contentCp = templateContent.codePointCount(0, templateContent.length());
		if (contentCp < 1 || contentCp > 500) {
			throw new ResourceException("模板内容有效长度1-500个字符");
		}
		String remark = stringValue(merged.get("remark"));
		int remarkCp = remark.codePointCount(0, remark.length());
		if (remarkCp < 1 || remarkCp > 100) {
			throw new ResourceException("申请说明有效长度1-100个字符");
		}
		int templateTypeInt = parseTemplateType(merged.get("template_type"));
		int sceneId =
				parseRequiredMinInt(
						merged,
						"scene_id",
						1,
						"短信场景必填",
						"场景id必须是大于等于1的整数");
		String relatedSignName = stringValue(merged.get("related_sign_name")).trim();

		aliyunsmsTemplateAddService.addTemplate(
				companyId, templateName, templateTypeInt, remark, templateContent, sceneId, relatedSignName);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/aliyunsms/template/add");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "新增模板");
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

	@Activated(routeAlias = "aliyunsms.tmpl.modify")
	@PostMapping(value = "/template/modify", name = "修改模板")
	public ResponseEntity<ApiResult<Map<String, Object>>> modifyTemplate(
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
		aliyunsmsShopRoutePermissionService.assertTemplateModify(user);

		Map<String, Object> merged = AliyunsmsAdminFlexibleInputMerge.merge(request, body);
		int idInt = parseRequiredIntField(merged, "id", "id必填");
		long templateId = idInt;
		validateModifyTemplateMerged(merged);

		String templateName = stringValue(merged.get("template_name"));
		int templateNameCp = templateName.codePointCount(0, templateName.length());
		if (templateNameCp < 1 || templateNameCp > 30) {
			throw new ResourceException("模板名称有效长度1-30个字符");
		}
		String templateContent = stringValue(merged.get("template_content"));
		int contentCp = templateContent.codePointCount(0, templateContent.length());
		if (contentCp < 1 || contentCp > 500) {
			throw new ResourceException("模板内容有效长度1-500个字符");
		}
		String remark = stringValue(merged.get("remark"));
		int remarkCp = remark.codePointCount(0, remark.length());
		if (remarkCp < 1 || remarkCp > 100) {
			throw new ResourceException("申请说明有效长度1-100个字符");
		}
		int templateTypeInt = parseTemplateType(merged.get("template_type"));
		int sceneId =
				parseRequiredMinInt(
						merged,
						"scene_id",
						1,
						"短信场景必填",
						"场景id必须是大于等于1的整数");
		String relatedSignName = stringValue(merged.get("related_sign_name")).trim();

		aliyunsmsTemplateModifyService.modifyTemplate(
				companyId, templateId, templateName, templateTypeInt, remark, templateContent, sceneId, relatedSignName);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/aliyunsms/template/modify");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "修改模板");
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

	@Activated(routeAlias = "aliyunsms.tmpl.delete")
	@DeleteMapping(value = "/template/delete/{id}", name = "aliyunsms.tmpl.delete")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteTemplate(
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
		aliyunsmsShopRoutePermissionService.assertTemplateDelete(user);

		long templateId = parseTemplateDeleteId(id);
		aliyunsmsTemplateDeleteService.deleteTemplate(companyId, templateId);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/aliyunsms/template/delete/" + templateId);
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(Map.of("id", templateId)));
		} catch (Exception e) {
			logCtx.put("params", Map.of("id", templateId).toString());
		}
		logCtx.put("operator_name", "删除模板");
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

	private static long parseTemplateDeleteId(String id) {
		if (id == null) {
			return Long.MIN_VALUE;
		}
		String t = id.trim();
		if (t.isEmpty()) {
			return Long.MIN_VALUE;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return Long.MIN_VALUE;
		}
	}

	private static void validateModifyTemplateMerged(Map<String, Object> merged) {
		validateAddTemplateMerged(merged);
	}

	private static void validateAddTemplateMerged(Map<String, Object> merged) {
		requireNonEmptyTrimmed(merged, "template_name", "模板名称必填");
		if (!merged.containsKey("template_type") || merged.get("template_type") == null) {
			throw new ResourceException("模板类型有误");
		}
		requireNonEmptyTrimmed(merged, "remark", "申请说明必填");
		requireNonEmptyTrimmed(merged, "template_content", "模板内容必填");
		if (!merged.containsKey("scene_id") || merged.get("scene_id") == null) {
			throw new ResourceException("短信场景必填");
		}
		if (stringValue(merged.get("scene_id")).isEmpty()) {
			throw new ResourceException("短信场景必填");
		}
		requireNonEmptyTrimmed(merged, "related_sign_name", "关联签名必填");
	}

	private static void requireNonEmptyTrimmed(Map<String, Object> merged, String key, String message) {
		if (!merged.containsKey(key)) {
			throw new ResourceException(message);
		}
		Object v = merged.get(key);
		if (v == null) {
			throw new ResourceException(message);
		}
		if (stringValue(v).isEmpty()) {
			throw new ResourceException(message);
		}
	}

	private static int parseTemplateType(Object v) {
		if (v instanceof Boolean) {
			throw new ResourceException("模板类型有误");
		}
		if (v instanceof Integer i) {
			return checkTemplateTypeRange(i);
		}
		if (v instanceof Long l) {
			if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE) {
				throw new ResourceException("模板类型有误");
			}
			return checkTemplateTypeRange(l.intValue());
		}
		if (v instanceof Byte b) {
			return checkTemplateTypeRange(b.intValue());
		}
		if (v instanceof Short s) {
			return checkTemplateTypeRange(s.intValue());
		}
		if (v instanceof Number n) {
			double d = n.doubleValue();
			if (Double.isNaN(d) || Double.isInfinite(d) || d != Math.rint(d)) {
				throw new ResourceException("模板类型有误");
			}
			if (d > Integer.MAX_VALUE || d < Integer.MIN_VALUE) {
				throw new ResourceException("模板类型有误");
			}
			return checkTemplateTypeRange((int) d);
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				throw new ResourceException("模板类型有误");
			}
			if (t.indexOf('.') >= 0 || t.indexOf('e') >= 0 || t.indexOf('E') >= 0) {
				throw new ResourceException("模板类型有误");
			}
			int parsed;
			try {
				parsed = Integer.parseInt(t);
			} catch (NumberFormatException e) {
				throw new ResourceException("模板类型有误");
			}
			return checkTemplateTypeRange(parsed);
		}
		throw new ResourceException("模板类型有误");
	}

	private static int checkTemplateTypeRange(int n) {
		if (n < 0 || n > 2) {
			throw new ResourceException("模板类型有误");
		}
		return n;
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

	private static String stringValue(Object o) {
		if (o == null) {
			return "";
		}
		if (o instanceof String s) {
			return s.trim();
		}
		return String.valueOf(o).trim();
	}

	private static String clientIp(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(xff)) {
			return xff.split(",")[0].trim();
		}
		return request.getRemoteAddr() != null ? request.getRemoteAddr() : "";
	}

	/**
	 * Resolves page size for template list: only {@code pageSize} (default 10) is recognized.
	 * {@code pagSize} / {@code page_size} are ignored for backward compatibility.
	 */
	private static int resolveTemplateListPageSize(HttpServletRequest request) {
		var pm = request.getParameterMap();
		if (pm.containsKey("pageSize")) {
			return parsePositiveIntOrDefault(request.getParameter("pageSize"), 10);
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

	private static int parsePageRawForTaskList(String p) {
		if (p == null) {
			return 1;
		}
		try {
			return Integer.parseInt(p.trim());
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private static void applyTemplateNameToTemplateListQuery(HttpServletRequest request, TemplateListQuery q) {
		var pm = request.getParameterMap();
		if (!pm.containsKey("template_name")) {
			q.setTemplateNameContainsActive(false);
			return;
		}
		String raw = request.getParameter("template_name");
		if (raw == null || !StringUtils.hasText(raw) || "0".equals(raw.trim())) {
			q.setTemplateNameContainsActive(false);
			return;
		}
		q.setTemplateNameContainsActive(true);
		q.setTemplateNameContains(raw);
	}

	private static void applyTemplateTypeToTemplateListQuery(HttpServletRequest request, TemplateListQuery q) {
		var pm = request.getParameterMap();
		if (!pm.containsKey("template_type")) {
			q.setTemplateTypeFilterActive(false);
			return;
		}
		String raw = request.getParameter("template_type");
		if (raw == null || !StringUtils.hasText(raw) || "0".equals(raw.trim())) {
			q.setTemplateTypeFilterActive(false);
			return;
		}
		q.setTemplateTypeFilterActive(true);
		q.setTemplateTypeRaw(raw.trim());
	}

	private static void applySceneIdToTemplateListQuery(HttpServletRequest request, TemplateListQuery q) {
		var pm = request.getParameterMap();
		if (!pm.containsKey("scene_id")) {
			q.setSceneIdFilterActive(false);
			return;
		}
		String raw = request.getParameter("scene_id");
		if (raw == null || !StringUtils.hasText(raw) || "0".equals(raw.trim())) {
			q.setSceneIdFilterActive(false);
			return;
		}
		int coerced;
		try {
			coerced = Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			q.setSceneIdFilterActive(false);
			return;
		}
		if (coerced <= 0) {
			q.setSceneIdFilterActive(false);
			return;
		}
		q.setSceneIdFilterActive(true);
		q.setSceneIdInt(coerced);
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

	private static int parseRequiredMinInt(
			Map<String, Object> merged, String key, int min, String emptyMessage, String belowMinMessage) {
		int v = parseRequiredIntField(merged, key, emptyMessage);
		if (v < min) {
			throw new BadRequestException(belowMinMessage);
		}
		return v;
	}
}
