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

package cn.shopex.ecshopx.tdkset.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.tdkset.service.TdkGivenSaveService;
import cn.shopex.ecshopx.tdkset.service.TdkGivenSetMenuPermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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
		notFound = false)
@AdminAuth
@ShopLog
@RestController("tdkGivenSetAdminV1")
@RequestMapping("/api/v1")
public class TdkGivenSetController {

	private final CompanysActivationService companysActivationService;
	private final TdkGivenSetMenuPermissionService tdkGivenSetMenuPermissionService;
	private final TdkGivenSaveService tdkGivenSaveService;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;

	public TdkGivenSetController(
			CompanysActivationService companysActivationService,
			TdkGivenSetMenuPermissionService tdkGivenSetMenuPermissionService,
			TdkGivenSaveService tdkGivenSaveService,
			OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper) {
		this.companysActivationService = companysActivationService;
		this.tdkGivenSetMenuPermissionService = tdkGivenSetMenuPermissionService;
		this.tdkGivenSaveService = tdkGivenSaveService;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
	}

	@Activated(routeAlias = "pcdecoration.tdkgivenset.info")
	@GetMapping(value = "/pcdecoration/tdkgivenset/{type}", name = "获取TDK特定页面配置信息")
	public ResponseEntity<?> getInfo(
			HttpServletRequest request,
			@PathVariable("type") String type,
			@RequestParam(value = "country_code", required = false) String countryCode) {
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
		tdkGivenSetMenuPermissionService.assertTdkGivenInfo(user);

		if (!"details".equals(type) && !"list".equals(type)) {
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("message", "类型错误");
			data.put("status_code", 422);
			Map<String, Object> root = new LinkedHashMap<>();
			root.put("data", data);
			return ResponseEntity.ok(root);
		}

		Map<String, Object> payload = tdkGivenSaveService.getGivenSetInfo(type, companyId, countryCode);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@Activated(routeAlias = "pcdecoration.tdkgivenset.save")
	@PostMapping(value = "/pcdecoration/tdkgivenset/{type}", name = "TDK特定页面信息添加&修改")
	public ResponseEntity<?> save(
			HttpServletRequest request,
			@PathVariable("type") String type,
			@FlexibleBody(required = false) Map<String, Object> body) {
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
		tdkGivenSetMenuPermissionService.assertTdkGivenSave(user);

		if (!"details".equals(type) && !"list".equals(type)) {
			// 兼容历史行为：类型错误时返回 HTTP 200 + data.message / data.status_code 422
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("message", "类型错误");
			data.put("status_code", 422);
			Map<String, Object> root = new LinkedHashMap<>();
			root.put("data", data);
			return ResponseEntity.ok(root);
		}

		Map<String, Object> merged = mergeInput(request, body);
		Object titleRaw = merged.get("title");
		Object descRaw = merged.get("mate_description");
		Object mkRaw = merged.get("mate_keywords");
		Object countryObj = merged.get("country_code");
		String countryCodeRaw = countryObj == null ? null : String.valueOf(countryObj);

		String title = titleRaw == null ? "" : String.valueOf(titleRaw);
		String mateDescription = descRaw == null ? "" : String.valueOf(descRaw);
		String mk = mkRaw == null ? "" : String.valueOf(mkRaw);
		mk = mk.replace("，", ",");

		Map<String, Object> reason = new LinkedHashMap<>();
		reason.put("title", title);
		reason.put("mate_description", mateDescription);
		reason.put("mate_keywords", mk);
		reason.put("update_time", (int) (System.currentTimeMillis() / 1000L));

		tdkGivenSaveService.saveSet(type, companyId, reason, countryCodeRaw);

		long operatorId = toLong(user.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/pcdecoration/tdkgivenset/" + type);
		logCtx.put("ip", clientIp(request));
		Map<String, Object> logParams = new LinkedHashMap<>();
		logParams.put("title", title);
		logParams.put("mate_description", mateDescription);
		logParams.put("mate_keywords", mk);
		logParams.put("country_code", countryCodeRaw);
		try {
			logCtx.put("params", objectMapper.writeValueAsString(logParams));
		} catch (Exception e) {
			logCtx.put("params", logParams.toString());
		}
		logCtx.put("operator_name", "TDK特定页面信息添加&修改");
		logCtx.put("log_type", "operator");
		Object merchantId = user.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		operatorLogsWriteService.addLogs(logCtx);

		return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
	}

	private Map<String, Object> mergeInput(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> input =
					new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		return body != null ? body : new LinkedHashMap<>();
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
}
