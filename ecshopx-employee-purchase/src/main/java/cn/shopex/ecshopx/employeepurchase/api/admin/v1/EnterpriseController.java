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

package cn.shopex.ecshopx.employeepurchase.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.employeepurchase.service.EnterpriseCreateService;
import cn.shopex.ecshopx.employeepurchase.service.EnterpriseDeleteService;
import cn.shopex.ecshopx.employeepurchase.service.EnterpriseInfoService;
import cn.shopex.ecshopx.employeepurchase.service.EnterpriseListService;
import cn.shopex.ecshopx.employeepurchase.service.EnterpriseQrcodeService;
import cn.shopex.ecshopx.employeepurchase.service.EnterpriseSendTestEmailService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
@RestController("employeepurchaseEnterpriseAdminV1")
@RequestMapping("/api/v1")
public class EnterpriseController {

	private static final String[] ENTERPRISE_UPDATE_FIELD_KEYS = {
		"name",
		"enterprise_sn",
		"logo",
		"auth_type",
		"sort",
		"relay_host",
		"smtp_port",
		"email_user",
		"email_password",
		"email_suffix",
		"qr_code_bg_image",
		"is_employee_check_enabled"
	};

	private final EnterpriseCreateService enterpriseCreateService;
	private final EnterpriseSendTestEmailService enterpriseSendTestEmailService;
	private final EnterpriseListService enterpriseListService;
	private final EnterpriseQrcodeService enterpriseQrcodeService;
	private final EnterpriseInfoService enterpriseInfoService;
	private final EnterpriseDeleteService enterpriseDeleteService;

	public EnterpriseController(
			EnterpriseCreateService enterpriseCreateService,
			EnterpriseSendTestEmailService enterpriseSendTestEmailService,
			EnterpriseListService enterpriseListService,
			EnterpriseQrcodeService enterpriseQrcodeService,
			EnterpriseInfoService enterpriseInfoService,
			EnterpriseDeleteService enterpriseDeleteService) {
		this.enterpriseCreateService = enterpriseCreateService;
		this.enterpriseSendTestEmailService = enterpriseSendTestEmailService;
		this.enterpriseListService = enterpriseListService;
		this.enterpriseQrcodeService = enterpriseQrcodeService;
		this.enterpriseInfoService = enterpriseInfoService;
		this.enterpriseDeleteService = enterpriseDeleteService;
	}

	@Activated(routeAlias = "employeepurchase.enterprise.add")
	@PostMapping(value = "/enterprise", name = "添加企业")
	public ResponseEntity<ApiResult<Map<String, Object>>> create(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> data = enterpriseCreateService.create(merged, operatorJwt);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.enterprise.update")
	@PutMapping(value = "/enterprise/{enterpriseId}", name = "更新企业")
	public ResponseEntity<ApiResult<Map<String, Object>>> update(
			HttpServletRequest request,
			@PathVariable("enterpriseId") String enterpriseId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		LinkedHashMap<String, Object> whitelist = new LinkedHashMap<>();
		for (String k : ENTERPRISE_UPDATE_FIELD_KEYS) {
			if (merged.containsKey(k)) {
				whitelist.put(k, merged.get(k));
			}
		}
		Map<String, Object> data = enterpriseCreateService.updateEnterprise(enterpriseId, whitelist, operatorJwt);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.enterprise.delete")
	@DeleteMapping(value = "/enterprise/{enterpriseId}", name = "删企业")
	public ResponseEntity<ApiResult<Map<String, Object>>> delete(
			@PathVariable("enterpriseId") String enterpriseId, HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		var target = enterpriseDeleteService.resolveForDelete(enterpriseId, operatorJwt);
		target.ifPresent(
				t -> enterpriseDeleteService.deleteEnterpriseAndEmailInTx(t.companyId(), t.enterpriseId()));
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "employeepurchase.enterprise.list")
	@GetMapping(value = "/enterprise", name = "获取企业列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getEnterprisesList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "enterprise_sn", required = false) String enterpriseSn,
			@RequestParam(value = "auth_type", required = false) String authType,
			@RequestParam(value = "distributor_id", required = false) String distributorId,
			@RequestParam(value = "is_employee_check_enabled", required = false) String isEmployeeCheckEnabled) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		if (page == null || pageSize == null) {
			throw new BadRequestException("分页参数错误");
		}
		Map<String, String[]> parameterMap = request.getParameterMap();
		Map<String, Object> data =
				enterpriseListService.getEnterprisesList(
						page,
						pageSize,
						name,
						enterpriseSn,
						authType,
						distributorId,
						parameterMap.containsKey("disabled"),
						request.getParameter("disabled"),
						resolveEnterpriseIdFilter(request),
						parameterMap.containsKey("is_employee_check_enabled"),
						isEmployeeCheckEnabled,
						operatorJwt);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	/**
	 * Supports PHP-style {@code enterprise_id[]=1&enterprise_id[]=2} and repeated
	 * {@code enterprise_id=1&enterprise_id=2} (or a single value).
	 *
	 * @return {@code null} when the filter is absent; otherwise a (possibly empty) id list
	 */
	private static List<Long> resolveEnterpriseIdFilter(HttpServletRequest request) {
		Map<String, String[]> pm = request.getParameterMap();
		String[] raw;
		if (pm.containsKey("enterprise_id[]")) {
			raw = request.getParameterValues("enterprise_id[]");
		} else if (pm.containsKey("enterprise_id")) {
			raw = request.getParameterValues("enterprise_id");
		} else {
			return null;
		}
		if (raw == null || raw.length == 0) {
			return List.of();
		}
		Set<Long> ids = new LinkedHashSet<>();
		for (String s : raw) {
			if (!StringUtils.hasText(s)) {
				continue;
			}
			try {
				ids.add(Long.parseLong(s.trim()));
			} catch (NumberFormatException e) {
				throw new BadRequestException("enterprise_id 无效");
			}
		}
		return new ArrayList<>(ids);
	}

	@Activated(routeAlias = "employeepurchase.enterprise.get")
	@GetMapping(value = "/enterprise/{enterpriseId}", name = "获取企业详情")
	public ResponseEntity<ApiResult<Object>> getEnterpriseInfo(
			HttpServletRequest request, @PathVariable("enterpriseId") String enterpriseId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object payload = enterpriseInfoService.getEnterpriseInfo(enterpriseId, operatorJwt);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@Activated(routeAlias = "employeepurchase.enterprise.qrcode.get")
	@GetMapping(value = "/enterprise/qrcode/{enterpriseId}", name = "获取企业小程序码")
	public ResponseEntity<ApiResult<Map<String, Object>>> getEnterpriseQrcode(
			HttpServletRequest request, @PathVariable("enterpriseId") String enterpriseId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Map<String, Object> data = enterpriseQrcodeService.getEnterpriseQrcode(enterpriseId, operatorJwt);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.enterprise.status.update")
	@PostMapping(value = "/enterprise/status", name = "更新企业状态")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateStatus(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		LinkedHashMap<String, Object> whitelist = new LinkedHashMap<>();
		whitelist.put("enterprise_id", merged.get("enterprise_id"));
		whitelist.put("disabled", merged.get("disabled"));
		enterpriseCreateService.updateStatus(whitelist, operatorJwt);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping(value = "/enterprise/sort", name = "更新排序")
	public ResponseEntity<ApiResult<Map<String, Object>>> setSort(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		LinkedHashMap<String, Object> whitelist = new LinkedHashMap<>();
		whitelist.put("enterprise_id", merged.get("enterprise_id"));
		whitelist.put("sort", merged.get("sort"));
		enterpriseCreateService.setSort(whitelist, operatorJwt);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.enterprise.sendtestemail")
	@PostMapping(value = "/enterprise/sendtestemail", name = "发送测试邮件")
	public ResponseEntity<ApiResult<Map<String, Object>>> sendTestemail(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> data = enterpriseSendTestEmailService.sendTestEmail(merged, operatorJwt);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static Map<String, Object> mergeInputLikeFlexibleResolver(
			HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> input = new LinkedHashMap<>(parameterMapToMapLikeResolver(request));
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		return body != null ? body : new LinkedHashMap<>();
	}

	private static Map<String, Object> parameterMapToMapLikeResolver(HttpServletRequest request) {
		Map<String, Object> m = new LinkedHashMap<>();
		request.getParameterMap().forEach((k, v) -> {
			if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
				m.put(k, v[0]);
			}
		});
		return m;
	}
}
