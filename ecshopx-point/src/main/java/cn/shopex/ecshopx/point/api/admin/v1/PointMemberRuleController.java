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

package cn.shopex.ecshopx.point.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.point.service.PopularizeCommissionTypeReadService;
import cn.shopex.ecshopx.point.service.PointMemberRuleReadService;
import cn.shopex.ecshopx.point.service.PointMemberRuleSaveService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("pointMemberRuleAdminV1")
@RequestMapping("/api/v1")
public class PointMemberRuleController {

	private final PointMemberRuleReadService pointMemberRuleReadService;
	private final PointMemberRuleSaveService pointMemberRuleSaveService;
	private final PopularizeCommissionTypeReadService popularizeCommissionTypeReadService;

	public PointMemberRuleController(
			PointMemberRuleReadService pointMemberRuleReadService,
			PointMemberRuleSaveService pointMemberRuleSaveService,
			PopularizeCommissionTypeReadService popularizeCommissionTypeReadService) {
		this.pointMemberRuleReadService = pointMemberRuleReadService;
		this.pointMemberRuleSaveService = pointMemberRuleSaveService;
		this.popularizeCommissionTypeReadService = popularizeCommissionTypeReadService;
	}

	@Activated(routeAlias = "point.member.rule.info")
	@GetMapping(value = "/member/point/rule", name = "用户积分规则详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> info(HttpServletRequest request) {
		long companyId = parseCompanyIdFromJwt(request);
		String countryCode = request.getParameter("country_code");
		if (!StringUtils.hasText(countryCode)) {
			countryCode = "zh-CN";
		} else {
			countryCode = countryCode.trim();
		}
		Map<String, Object> data = pointMemberRuleReadService.getPointRule(companyId, countryCode);
		if (data.get("access") == null) {
			data.put("access", "order");
		}
		if (data.get("include_freight") == null) {
			data.put("include_freight", "true");
		}
		data.put(
				"popularize_commission_type",
				popularizeCommissionTypeReadService.resolvePopularizeCommissionType(companyId));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "point.member.rule.save")
	@PutMapping(value = "/member/point/rule", name = "用户积分规则设置")
	public ResponseEntity<ApiResult<Map<String, Object>>> save(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> result = pointMemberRuleSaveService.saveFromAdminRequest(request, merged);
		return ResponseEntity.ok(ApiResult.ok(result));
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

	private Map<String, Object> mergeInputLikeFlexibleResolver(HttpServletRequest request, Map<String, Object> body) {
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

	private static long parseCompanyIdFromJwt(HttpServletRequest request) {
		Object rawUd = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawUd instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		try {
			return parseLongLoose(cid);
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
	}

	private static long parseLongLoose(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
