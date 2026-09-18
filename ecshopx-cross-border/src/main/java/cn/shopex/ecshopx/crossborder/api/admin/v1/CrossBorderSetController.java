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

package cn.shopex.ecshopx.crossborder.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.crossborder.api.admin.v1.dto.CrossBorderSetInfoDto;
import cn.shopex.ecshopx.crossborder.service.CrossBorderSetInfoQueryService;
import cn.shopex.ecshopx.crossborder.service.CrossBorderSetSaveService;
import cn.shopex.ecshopx.crossborder.service.CrossBorderShopRoutePermissionService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422
)
@RestController("crossborderSetAdminV1")
@RequestMapping("/api/v1")
public class CrossBorderSetController {

	private final CrossBorderSetSaveService crossBorderSetSaveService;
	private final CrossBorderSetInfoQueryService crossBorderSetInfoQueryService;
	private final CompanysActivationService companysActivationService;
	private final CrossBorderShopRoutePermissionService crossBorderShopRoutePermissionService;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;

	public CrossBorderSetController(
			CrossBorderSetSaveService crossBorderSetSaveService,
			CrossBorderSetInfoQueryService crossBorderSetInfoQueryService,
			CompanysActivationService companysActivationService,
			CrossBorderShopRoutePermissionService crossBorderShopRoutePermissionService,
			OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper) {
		this.crossBorderSetSaveService = crossBorderSetSaveService;
		this.crossBorderSetInfoQueryService = crossBorderSetInfoQueryService;
		this.companysActivationService = companysActivationService;
		this.crossBorderShopRoutePermissionService = crossBorderShopRoutePermissionService;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
	}

	@Activated(routeAlias = "crossborder.set.info")
	@GetMapping(value = "/crossborder/set", name = "获取设置信息")
	public ResponseEntity<?> getInfo(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = toLong(ud.get("company_id"));
		companysActivationService.assertShopOperatorCompanyActive(companyId);
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}
		crossBorderShopRoutePermissionService.assertRouteAllowed(
				user, CrossBorderShopRoutePermissionService.ROUTE_ALIAS_CROSSBORDER_SET_GETINFO);
		Optional<CrossBorderSetInfoDto> opt = crossBorderSetInfoQueryService.findByCompanyId(companyId);
		if (opt.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		return ResponseEntity.ok(ApiResult.ok(opt.get()));
	}

	@Activated(routeAlias = "crossborder.set.save")
	@PostMapping(value = "/crossborder/set", name = "保存设置信息")
	public ResponseEntity<?> Save(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}
		long companyId = toLong(ud.get("company_id"));
		companysActivationService.assertShopOperatorCompanyActive(companyId);
		crossBorderShopRoutePermissionService.assertRouteAllowed(
				user, CrossBorderShopRoutePermissionService.ROUTE_ALIAS_CROSSBORDER_SET_SAVE);

		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		crossBorderSetSaveService.save(companyId, merged);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("status", Boolean.TRUE);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/crossborder/set");
		logCtx.put("ip", clientIp(request));
		Map<String, Object> logParams = new LinkedHashMap<>();
		logParams.put("tax_rate", merged.get("tax_rate"));
		logParams.put("quota_tip", merged.get("quota_tip"));
		logParams.put("crossborder_show", merged.get("crossborder_show"));
		logParams.put("logistics", merged.get("logistics"));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(logParams));
		} catch (Exception e) {
			logCtx.put("params", logParams.toString());
		}
		logCtx.put("operator_name", "保存设置信息");
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

		return ResponseEntity.ok(ApiResult.ok(payload));
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
}
