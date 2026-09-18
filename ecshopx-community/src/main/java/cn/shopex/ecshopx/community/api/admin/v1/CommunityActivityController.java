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
import cn.shopex.ecshopx.community.dto.CommunityActivityAdminListQuery;
import cn.shopex.ecshopx.community.service.CommunityActivityService;
import cn.shopex.ecshopx.community.service.admin.CommunityActivityAdminListQuerySupport;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("communityAdminV1Activity")
@RequestMapping("/api/v1/community")
public class CommunityActivityController {

	private final CommunityActivityService communityActivityService;

	public CommunityActivityController(CommunityActivityService communityActivityService) {
		this.communityActivityService = communityActivityService;
	}

	@Activated(routeAlias = "community.list")
	@GetMapping(value = "/list", name = "活动列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getList(HttpServletRequest request) {
		Object rawUd = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawUd instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = parseCompanyId(cid);
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) ud;
		CommunityActivityAdminListQuery query = CommunityActivityAdminListQuerySupport.buildQuery(jwtMap, request);

		int[] pp = parsePageParams(request);
		Map<String, Object> data = communityActivityService.getActivityList(companyId, query, pp[0], pp[1]);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "community.activity.confirm.delivery")
	@PostMapping(value = "/activity/confirm/delivery", name = "确认发货")
	public ResponseEntity<ApiResult<Map<String, Object>>> confirmDeliveryStatus(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawUd = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawUd instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = parseCompanyId(cid);

		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		String operatorType = stringVal(ud.get("operator_type"));
		boolean distributorBranch = "distributor".equals(operatorType);
		Integer distributorIdWhenBranch = null;
		if (distributorBranch) {
			Object d = merged.get("distributor_id");
			if (d == null && StringUtils.hasText(request.getParameter("distributor_id"))) {
				d = request.getParameter("distributor_id");
			}
			distributorIdWhenBranch = CommunityActivityAdminListQuerySupport.parseShopBranchDistributorIdOrNull(d);
		}

		Object rawAid = merged.get("activity_id");
		if (rawAid == null && StringUtils.hasText(request.getParameter("activity_id"))) {
			rawAid = request.getParameter("activity_id");
		}
		long activityId = requirePositiveActivityId(rawAid);

		Map<String, Object> data =
				communityActivityService.updateConfirmStatus(companyId, distributorBranch, distributorIdWhenBranch, activityId);
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

	private static long requirePositiveActivityId(Object raw) {
		if (raw == null) {
			throw new BadRequestException(
					"The given data was invalid.", Map.of("activity_id", List.of("validation.required")));
		}
		if (raw instanceof String s && !StringUtils.hasText(s.trim())) {
			throw new BadRequestException(
					"The given data was invalid.", Map.of("activity_id", List.of("validation.required")));
		}
		Long id = toPositiveLong(raw);
		if (id == null) {
			throw new BadRequestException(
					"The given data was invalid.", Map.of("activity_id", List.of("validation.integer")));
		}
		return id;
	}

	private static Long toPositiveLong(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			long v = n.longValue();
			return v > 0 ? v : null;
		}
		try {
			long v = Long.parseLong(o.toString().trim());
			return v > 0 ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long parseCompanyId(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v);
	}

	private int[] parsePageParams(HttpServletRequest request) {
		int page = 1;
		int pageSize = 20;
		String p = request.getParameter("page");
		if (p != null) {
			try {
				page = Integer.parseInt(p.trim());
			} catch (NumberFormatException e) {
				page = 1;
			}
		}
		String ps = request.getParameter("pageSize");
		if (ps != null) {
			try {
				pageSize = Integer.parseInt(ps.trim());
			} catch (NumberFormatException e) {
				pageSize = 20;
			}
		}
		if (page < 1) {
			page = 1;
		}
		if (pageSize < 1) {
			pageSize = 20;
		}
		return new int[] {page, pageSize};
	}

	@Activated(routeAlias = "community.activity.deliver")
	@PostMapping(value = "/chief/deliver", name = "店铺发货")
	public ResponseEntity<Map<String, Object>> deliver(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawUd = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawUd instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		parseCompanyId(cid);

		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		String operatorType = stringVal(ud.get("operator_type"));
		boolean distributorBranch = "distributor".equals(operatorType);

		int distributorIdParam;
		if (!distributorBranch) {
			distributorIdParam = 0;
		} else {
			Object d = merged.get("distributor_id");
			if (d == null && StringUtils.hasText(request.getParameter("distributor_id"))) {
				d = request.getParameter("distributor_id");
			}
			if (d == null || (d instanceof String s && !StringUtils.hasText(s.trim()))) {
				throw new BadRequestException(
						"The given data was invalid.",
						Map.of("distributor_id", List.of("validation.required")),
						400);
			}
			try {
				if (d instanceof Number n) {
					distributorIdParam = n.intValue();
				} else {
					distributorIdParam = Integer.parseInt(d.toString().trim());
				}
			} catch (NumberFormatException e) {
				throw new BadRequestException(
						"The given data was invalid.",
						Map.of("distributor_id", List.of("validation.integer")),
						400);
			}
		}

		Object rawAid = merged.get("activity_id");
		if (rawAid == null && StringUtils.hasText(request.getParameter("activity_id"))) {
			rawAid = request.getParameter("activity_id");
		}
		long activityId = requirePositiveActivityId(rawAid);

		communityActivityService.deliverChief(distributorIdParam, activityId);
		return ResponseEntity.ok(Map.of("status", Boolean.TRUE));
	}
}
