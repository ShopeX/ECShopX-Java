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

package cn.shopex.ecshopx.community.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.espier.service.config.ConfigRequestFieldsApplicationService;
import cn.shopex.ecshopx.espier.service.config.ConfigRequestFieldsConstants;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false
)
@AdminAuth
@ShopLog
@RestController("communityAdminV1ChiefApplyFields")
@RequestMapping("/api/v1/community")
public class CommunityChiefApplyFieldsController {

	private final ConfigRequestFieldsApplicationService configRequestFieldsApplicationService;
	private final LangueProperties langueProperties;

	public CommunityChiefApplyFieldsController(
			ConfigRequestFieldsApplicationService configRequestFieldsApplicationService,
			LangueProperties langueProperties) {
		this.configRequestFieldsApplicationService = configRequestFieldsApplicationService;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "community.chief.apply_field.list")
	@GetMapping(value = "/chief/apply_fields", name = "配置字段列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> list(
			HttpServletRequest request,
			@RequestParam(value = "page", defaultValue = "1") int page,
			@RequestParam(value = "page_size", defaultValue = "10") int pageSize) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		int companyId = parseCompanyId(cid);

		String operatorType = stringVal(ud.get("operator_type"));
		int distributorId = 0;
		if ("distributor".equals(operatorType)) {
			distributorId = parseIntLoose(request.getParameter("distributor_id"), 0);
		}

		Map<String, Object> data =
				configRequestFieldsApplicationService.listPaginatedForAdmin(
						companyId,
						ConfigRequestFieldsConstants.MODULE_TYPE_CHIEF_INFO,
						distributorId,
						page,
						pageSize,
						RequestLangTag.current(langueProperties));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "community.chief.apply_field.create")
	@PostMapping(value = "/chief/apply_field", name = "创建字段")
	public ResponseEntity<ApiResult<Map<String, Object>>> create(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		int companyId = parseCompanyId(cid);

		String operatorType = stringVal(ud.get("operator_type"));
		Map<String, Object> merged = CommunityAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		int distributorId = 0;
		if ("distributor".equals(operatorType)) {
			Object d = merged.get("distributor_id");
			if (d == null && StringUtils.hasText(request.getParameter("distributor_id"))) {
				d = request.getParameter("distributor_id");
			}
			distributorId = parseIntLoose(d, 0);
		}
		LinkedHashMap<String, Object> params = new LinkedHashMap<>(merged);
		params.put("distributor_id", distributorId);

		validateChiefApplyFieldWritePayload(params);

		Map<String, Object> data =
				configRequestFieldsApplicationService.create(companyId, ConfigRequestFieldsConstants.MODULE_TYPE_CHIEF_INFO, params);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<?> handleBadRequest(BadRequestException ex) {
		int statusCode = ex.getEmbeddedStatusCode() != null ? ex.getEmbeddedStatusCode() : 422;
		return dingo422(ex.getMessage(), ex.getFieldErrors(), statusCode);
	}

	private static ResponseEntity<?> dingo422(String message, Map<String, List<String>> errors, int statusCode) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", message);
		data.put("status_code", statusCode);
		if (errors != null && !errors.isEmpty()) {
			data.put("errors", errors);
		}
		return ResponseEntity.ok(Map.of("data", data));
	}

	private static void validateChiefApplyFieldWritePayload(Map<String, Object> merged) {
		String label = stringVal(merged.get("label")).trim();
		if (!StringUtils.hasText(label)) {
			throw new BadRequestException("字段信息必填！");
		}
		merged.put("label", label);
		Object ftRaw = merged.get("field_type");
		if (ftRaw == null) {
			throw new BadRequestException("字段信息格式有误！");
		}
		int fieldType = parseIntLoose(ftRaw, -1);
		if (!ConfigRequestFieldsConstants.FIELD_TYPE_MAP.containsKey(fieldType)) {
			throw new BadRequestException("字段信息格式有误！");
		}
		String arm = stringVal(merged.get("alert_required_message")).trim();
		if (!StringUtils.hasText(arm)) {
			throw new BadRequestException("提示信息必填");
		}
		merged.put("alert_required_message", arm);
	}

	private static int parseCompanyId(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
	}

	private static int parseIntLoose(Object v, int defaultVal) {
		if (v == null) {
			return defaultVal;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v);
	}

	/**
	 * 解析查询参数 {@code switch}：未提交该键、值为 null、去空白后为空、或等于 {@code "0"} 时为 false；其余非空且不等于 {@code "0"} 时为 true。
	 */
	private static boolean parseSwitchQueryParam(HttpServletRequest request) {
		if (!request.getParameterMap().containsKey("switch")) {
			return false;
		}
		String raw = request.getParameter("switch");
		if (raw == null) {
			return false;
		}
		String s = raw.trim();
		if (s.isEmpty()) {
			return false;
		}
		return !"0".equals(s);
	}

	@PostMapping(value = "/chief/apply_field/switch/{id}", name = "配置字段开关")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateSwitch(
			HttpServletRequest request, @PathVariable("id") String id) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		int companyId = parseCompanyId(cid);

		String operatorType = stringVal(ud.get("operator_type"));
		int distributorId = 0;
		if ("distributor".equals(operatorType)) {
			distributorId = parseIntLoose(request.getParameter("distributor_id"), 0);
		}

		int rowId = parseIntLoose(id, 0);
		if (rowId <= 0) {
			return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
		}

		int type = parseIntLoose(request.getParameter("type"), 0);
		boolean sw = parseSwitchQueryParam(request);
		configRequestFieldsApplicationService.updateSwitch(companyId, rowId, type, sw, distributorId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "community.chief.apply_field.update")
	@PostMapping(value = "/chief/apply_field/{id}", name = "修改字段")
	public ResponseEntity<ApiResult<Map<String, Object>>> update(
			HttpServletRequest request,
			@PathVariable("id") String id,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		int companyId = parseCompanyId(cid);

		String operatorType = stringVal(ud.get("operator_type"));
		Map<String, Object> merged = CommunityAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		int distributorId = 0;
		if ("distributor".equals(operatorType)) {
			Object d = merged.get("distributor_id");
			if (d == null && StringUtils.hasText(request.getParameter("distributor_id"))) {
				d = request.getParameter("distributor_id");
			}
			distributorId = parseIntLoose(d, 0);
		}
		LinkedHashMap<String, Object> params = new LinkedHashMap<>(merged);
		params.put("distributor_id", distributorId);

		validateChiefApplyFieldWritePayload(params);

		int rowId = parseIntLoose(id, 0);
		Map<String, Object> data = configRequestFieldsApplicationService.updateInfo(companyId, rowId, params);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "community.chief.apply_field.delete")
	@DeleteMapping(value = "/chief/apply_field/{id}", name = "删除字段")
	public ResponseEntity<ApiResult<Map<String, Object>>> delete(
			HttpServletRequest request, @PathVariable("id") String id) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		int companyId = parseCompanyId(cid);

		String operatorType = stringVal(ud.get("operator_type"));
		int distributorId = 0;
		if ("distributor".equals(operatorType)) {
			distributorId = parseIntLoose(request.getParameter("distributor_id"), 0);
		}

		int rowId = parseIntLoose(id, 0);
		if (rowId <= 0) {
			return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
		}

		configRequestFieldsApplicationService.deleteForChiefApplyField(companyId, rowId, distributorId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}
}
