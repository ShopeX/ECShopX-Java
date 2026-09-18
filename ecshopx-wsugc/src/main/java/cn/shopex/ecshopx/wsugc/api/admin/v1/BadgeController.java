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

package cn.shopex.ecshopx.wsugc.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.wsugc.service.badge.BadgeCreateService;
import cn.shopex.ecshopx.wsugc.service.badge.BadgeDeleteService;
import cn.shopex.ecshopx.wsugc.service.badge.BadgeDetailService;
import cn.shopex.ecshopx.wsugc.service.badge.BadgeListService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422)
@RestController("wsugcAdminV1Badge")
@RequestMapping("/api/v1/ugc/badge")
public class BadgeController {

	private final BadgeCreateService badgeCreateService;
	private final BadgeDeleteService badgeDeleteService;
	private final BadgeDetailService badgeDetailService;
	private final BadgeListService badgeListService;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;

	public BadgeController(
			BadgeCreateService badgeCreateService,
			BadgeDeleteService badgeDeleteService,
			BadgeDetailService badgeDetailService,
			BadgeListService badgeListService,
			OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper) {
		this.badgeCreateService = badgeCreateService;
		this.badgeDeleteService = badgeDeleteService;
		this.badgeDetailService = badgeDetailService;
		this.badgeListService = badgeListService;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
	}

	@PostMapping(value = "/create", name = "新建角标")
	public ResponseEntity<ApiResult<Map<String, Object>>> createBadge(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Map<String, Object> data = badgeCreateService.createOrUpdate(merged, operatorJwt);

		long companyId = readLong(ud, "company_id", 1L);
		long operatorId = readLong(ud, "operator_id", 0L);
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/ugc/badge/create");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "新建角标");
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
			// 操作日志失败不影响主流程
		}

		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long readLong(Map<?, ?> ud, String key, long defaultVal) {
		Object v = ud.get(key);
		if (v == null) {
			return defaultVal;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static String clientIp(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(xff)) {
			return xff.split(",")[0].trim();
		}
		return request.getRemoteAddr() != null ? request.getRemoteAddr() : "";
	}

	private Map<String, Object> mergeInputLikeFlexibleResolver(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> input = new LinkedHashMap<>(parameterMapToMap(request));
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		return body != null ? body : new LinkedHashMap<>();
	}

	private static Map<String, Object> parameterMapToMap(HttpServletRequest request) {
		Map<String, Object> m = new LinkedHashMap<>();
		request.getParameterMap().forEach((k, v) -> {
			if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
				m.put(k, v[0]);
			}
		});
		return m;
	}

	@Activated(routeAlias = "ugc.badge.list")
	@GetMapping(value = "/list", name = "角标列表")
	public ResponseEntity<ApiResult<Object>> getBadgeList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "pageSize", required = false, defaultValue = "30") int pageSize,
			@RequestParam(value = "badge_name", required = false) String badgeName,
			@RequestParam(value = "badge_memo", required = false) String badgeMemo,
			@RequestParam(value = "is_top", required = false) String isTop,
			@RequestParam(value = "nickname", required = false) String nickname,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "sort", required = false) String sort) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		long companyId = readLong(operatorJwt, "company_id", 1L);
		String langTag = resolveAdminLangTag(operatorJwt);
		Object data = badgeListService.buildList(
				companyId, langTag, page, pageSize, badgeName, badgeMemo, isTop, nickname, mobile, sort);
		if (data instanceof List<?> list && list.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> body = (Map<String, Object>) data;
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@Activated(routeAlias = "ugc.badge.detail")
	@GetMapping(value = "/detail", name = "角标详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getBadgeDetail(
			HttpServletRequest request, @RequestParam(value = "badge_id", required = false) String badgeId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		String langTag = resolveAdminLangTag(operatorJwt);
		Map<String, Object> inner = badgeDetailService.buildResponse(badgeId, operatorJwt, langTag);
		return ResponseEntity.ok(ApiResult.ok(inner));
	}

	private static String resolveAdminLangTag(Map<String, Object> operatorJwt) {
		Object cc = operatorJwt.get("country_code");
		if (cc != null && StringUtils.hasText(cc.toString())) {
			return cc.toString().trim();
		}
		return "zh-CN";
	}

	@PostMapping(value = "/delete", name = "删除角标")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteBadge(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> data = badgeDeleteService.deleteBadges(merged);

		long companyId = readLong(ud, "company_id", 1L);
		long operatorId = readLong(ud, "operator_id", 0L);
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/ugc/badge/delete");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "删除角标");
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
			// 操作日志失败不影响主流程
		}

		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
