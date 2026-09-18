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

package cn.shopex.ecshopx.distribution.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.distribution.domain.BasicConfig;
import cn.shopex.ecshopx.distribution.repository.BasicConfigWriteRepository;
import cn.shopex.ecshopx.distribution.service.BasicConfigSaveParamValidator;
import cn.shopex.ecshopx.distribution.service.BasicConfigSaveService;
import cn.shopex.ecshopx.distribution.service.DistributorMenuPermissionService;
import cn.shopex.ecshopx.distribution.support.BasicConfigColumnNamesDataMapper;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("distributionAdminV1BasicConfig")
@RequestMapping("/api/v1/distribution")
public class BasicConfigController {

	private final BasicConfigSaveService basicConfigSaveService;
	private final BasicConfigWriteRepository basicConfigWriteRepository;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;
	private final CompanysActivationService companysActivationService;
	private final DistributorMenuPermissionService distributorMenuPermissionService;

	public BasicConfigController(
			BasicConfigSaveService basicConfigSaveService,
			BasicConfigWriteRepository basicConfigWriteRepository,
			OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper,
			CompanysActivationService companysActivationService,
			DistributorMenuPermissionService distributorMenuPermissionService) {
		this.basicConfigSaveService = basicConfigSaveService;
		this.basicConfigWriteRepository = basicConfigWriteRepository;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
		this.companysActivationService = companysActivationService;
		this.distributorMenuPermissionService = distributorMenuPermissionService;
	}

	@Activated(routeAlias = "distribution.basic_config.save")
	@PostMapping(value = "/basic_config", name = "保存分销基础配置")
	public ResponseEntity<?> saveBasicConfig(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}
		long companyId = toLong(ud.get("company_id"));
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Object opTypeJwt = ud.get("operator_type");
		if (opTypeJwt != null) {
			merged.put("operator_type", opTypeJwt.toString());
		}
		Object cid = ud.get("company_id");
		if (cid != null) {
			merged.put("company_id", cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString()));
		}
		Object mid = ud.get("merchant_id");
		if (mid != null) {
			merged.put("merchant_id", mid instanceof Number n ? n.longValue() : Long.parseLong(mid.toString()));
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		distributorMenuPermissionService.assertRouteAllowed(
				user, DistributorMenuPermissionService.ROUTE_ALIAS_DISTRIBUTION_BASIC_CONFIG_SAVE);
		try {
			BasicConfigSaveParamValidator.validateRequiredReturns(merged);
			Map<String, Object> data = basicConfigSaveService.saveBasicConfig(companyId, merged);

			long operatorId = toLong(ud.get("operator_id"));
			Map<String, Object> logCtx = new LinkedHashMap<>();
			logCtx.put("company_id", companyId);
			logCtx.put("operator_id", (int) operatorId);
			logCtx.put("request_uri", "/api/v1/distribution/basic_config");
			logCtx.put("ip", clientIp(request));
			try {
				logCtx.put("params", objectMapper.writeValueAsString(merged));
			} catch (Exception e) {
				logCtx.put("params", merged.toString());
			}
			logCtx.put("operator_name", "保存分销基础配置");
			logCtx.put("log_type", "operator");
			Object merchantId = ud.get("merchant_id");
			if (merchantId != null) {
				logCtx.put(
						"merchant_id",
						merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
			}
			operatorLogsWriteService.addLogs(logCtx);

			return ResponseEntity.ok(ApiResult.ok(data));
		} catch (ResourceException ex) {
			String msg = ex.getMessage();
			if (BasicConfigSaveParamValidator.RETURN_NAME_REQUIRED.equals(msg)
					|| BasicConfigSaveParamValidator.RETURN_ADDRESS_REQUIRED.equals(msg)
					|| BasicConfigSaveParamValidator.RETURN_PHONE_REQUIRED.equals(msg)) {
				LinkedHashMap<String, Object> data = new LinkedHashMap<>();
				data.put("message", msg);
				data.put("status_code", 422);
				LinkedHashMap<String, Object> root = new LinkedHashMap<>();
				root.put("data", data);
				return ResponseEntity.ok(root);
			}
			throw ex;
		}
	}

	@Activated(routeAlias = "distributor.basic_config.get")
	@GetMapping(value = "/basic_config", name = "获取分销基础配置")
	public ResponseEntity<ApiResult<?>> getBasicConfig(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}
		long companyId = toLong(ud.get("company_id"));
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Object opTypeJwt = ud.get("operator_type");
		if (opTypeJwt != null) {
			merged.put("operator_type", opTypeJwt.toString());
		}
		Object cid = ud.get("company_id");
		if (cid != null) {
			merged.put("company_id", cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString()));
		}
		Object mid = ud.get("merchant_id");
		if (mid != null) {
			merged.put("merchant_id", mid instanceof Number n ? n.longValue() : Long.parseLong(mid.toString()));
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		distributorMenuPermissionService.assertRouteAllowed(
				user, DistributorMenuPermissionService.ROUTE_ALIAS_DISTRIBUTOR_BASIC_CONFIG_GET);

		BasicConfig row = basicConfigWriteRepository.getInfoByCompanyId(companyId);
		if (row == null) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		return ResponseEntity.ok(ApiResult.ok(BasicConfigColumnNamesDataMapper.toColumnNamesData(row)));
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
