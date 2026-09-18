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
import cn.shopex.ecshopx.community.service.CommunitySettingService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
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
@RestController("communityAdminV1Setting")
@RequestMapping("/api/v1/community")
public class CommunitySettingController {

	private final CommunitySettingService communitySettingService;

	public CommunitySettingController(CommunitySettingService communitySettingService) {
		this.communitySettingService = communitySettingService;
	}

	@Activated(routeAlias = "community.setting.get")
	@GetMapping(value = "/activity/setting", name = "社区团设置查询")
	public ResponseEntity<ApiResult<Map<String, Object>>> get(HttpServletRequest request) {
		Object rawUd = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawUd instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = ud.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = parseCompanyId(cid);

		boolean distributorBranch = "distributor".equals(stringVal(ud.get("operator_type")));
		Object distributorIdRaw = null;
		if (distributorBranch) {
			distributorIdRaw = request.getParameter("distributor_id");
		}

		Map<String, Object> result =
				communitySettingService.getSetting(companyId, distributorBranch, distributorIdRaw);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "community.setting.save")
	@PostMapping(value = "/activity/setting", name = "社区团设置保存")
	public ResponseEntity<ApiResult<Map<String, Object>>> save(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body)
			throws JsonProcessingException {
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
		Object distributorIdRaw = null;
		if (distributorBranch) {
			Object d = merged.get("distributor_id");
			if (d == null && StringUtils.hasText(request.getParameter("distributor_id"))) {
				d = request.getParameter("distributor_id");
			}
			distributorIdRaw = d;
		}

		validateSaveInput(merged);
		Map<String, Object> result =
				communitySettingService.saveSetting(companyId, distributorBranch, distributorIdRaw, merged);
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

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v);
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

	private static void validateSaveInput(Map<String, Object> merged) {
		if (!merged.containsKey("condition_type")) {
			throw new BadRequestException("成团条件必选");
		}
		Object ctRaw = merged.get("condition_type");
		if (!(ctRaw instanceof String)) {
			throw new BadRequestException("成团条件必选");
		}
		String ct = ((String) ctRaw).trim();
		if (!StringUtils.hasText(ct)) {
			throw new BadRequestException("成团条件必选");
		}
		if (!"num".equals(ct) && !"money".equals(ct)) {
			throw new BadRequestException("成团条件必选");
		}
		if ("money".equals(ct)) {
			validateConditionMoney(merged);
		}
		validateRebateRatioIfPresent(merged);
	}

	private static void validateConditionMoney(Map<String, Object> merged) {
		Object cm = merged.get("condition_money");
		if (cm == null) {
			return;
		}
		if (cm instanceof String s && !StringUtils.hasText(s.trim())) {
			return;
		}
		BigDecimal bd = toNonNegativeBigDecimal(cm);
		if (bd == null) {
			throw new BadRequestException("最低成团金额必填");
		}
	}

	private static BigDecimal toNonNegativeBigDecimal(Object o) {
		if (o == null) {
			return null;
		}
		try {
			BigDecimal bd;
			if (o instanceof BigDecimal b) {
				bd = b;
			} else if (o instanceof Number n) {
				bd = BigDecimal.valueOf(n.doubleValue());
			} else {
				String t = o.toString().trim();
				if (!StringUtils.hasText(t)) {
					return null;
				}
				bd = new BigDecimal(t);
			}
			return bd.compareTo(BigDecimal.ZERO) >= 0 ? bd : null;
		} catch (NumberFormatException | ArithmeticException e) {
			return null;
		}
	}

	private static void validateRebateRatioIfPresent(Map<String, Object> merged) {
		if (!merged.containsKey("rebate_ratio")) {
			return;
		}
		Object r = merged.get("rebate_ratio");
		if (r == null) {
			return;
		}
		if (r instanceof String str && !StringUtils.hasText(str.trim())) {
			return;
		}
		BigDecimal val = toBigDecimalForRatio(r);
		if (val == null
				|| val.compareTo(BigDecimal.ZERO) < 0
				|| val.compareTo(new BigDecimal("100")) > 0) {
			throw new BadRequestException("佣金比例在0～100之间");
		}
	}

	private static BigDecimal toBigDecimalForRatio(Object o) {
		if (o == null) {
			return null;
		}
		try {
			if (o instanceof BigDecimal b) {
				return b;
			}
			if (o instanceof Number n) {
				return BigDecimal.valueOf(n.doubleValue());
			}
			String t = o.toString().trim();
			if (!StringUtils.hasText(t)) {
				return null;
			}
			return new BigDecimal(t);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
