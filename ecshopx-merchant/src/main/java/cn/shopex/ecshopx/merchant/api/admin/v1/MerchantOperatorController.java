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

package cn.shopex.ecshopx.merchant.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.dispatch.MerchantResetPasswordNoticeDispatchPublisher;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.merchant.port.MerchantOperatorPasswordResetPort;
import cn.shopex.ecshopx.merchant.port.MerchantOperatorPasswordResetResult;
import cn.shopex.ecshopx.merchant.port.MerchantShopRoutePermissionPort;
import cn.shopex.ecshopx.merchant.port.OperatorAccountPasswordUpdatePort;
import cn.shopex.ecshopx.merchant.port.ShopOperatorCompanyActivation;
import cn.shopex.ecshopx.merchant.port.ShopOperatorDatapassApplyPort;
import cn.shopex.ecshopx.merchant.port.ShopOperatorLogsWrite;
import cn.shopex.ecshopx.merchant.service.MerchantDataMasking;
import cn.shopex.ecshopx.merchant.service.MerchantListParamValidator;
import cn.shopex.ecshopx.merchant.service.MerchantListRequestParamsResolver;
import cn.shopex.ecshopx.merchant.service.MerchantOperatorListParamValidator;
import cn.shopex.ecshopx.merchant.service.MerchantOperatorListQueryService;
import cn.shopex.ecshopx.merchant.web.MerchantDatapassBlockSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@DingoResponse(resource = DingoResponse.ResourceStyle.DINGO)
@RestController("merchantAdminV1MerchantOperator")
@RequestMapping("/api/v1/merchant")
public class MerchantOperatorController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final ObjectMapper objectMapper;
	private final ShopOperatorCompanyActivation shopOperatorCompanyActivation;
	private final MerchantShopRoutePermissionPort merchantShopRoutePermissionPort;
	private final ShopOperatorDatapassApplyPort shopOperatorDatapassApplyPort;
	private final MerchantListRequestParamsResolver merchantListRequestParamsResolver;
	private final MerchantOperatorListParamValidator merchantOperatorListParamValidator;
	private final MerchantOperatorListQueryService merchantOperatorListQueryService;
	private final OperatorAccountPasswordUpdatePort operatorAccountPasswordUpdatePort;
	private final MerchantOperatorPasswordResetPort merchantOperatorPasswordResetPort;
	private final MerchantResetPasswordNoticeDispatchPublisher merchantResetPasswordNoticeDispatchPublisher;
	private final ShopOperatorLogsWrite shopOperatorLogsWrite;

	public MerchantOperatorController(
			ObjectMapper objectMapper,
			ShopOperatorCompanyActivation shopOperatorCompanyActivation,
			MerchantShopRoutePermissionPort merchantShopRoutePermissionPort,
			ShopOperatorDatapassApplyPort shopOperatorDatapassApplyPort,
			MerchantListRequestParamsResolver merchantListRequestParamsResolver,
			MerchantOperatorListParamValidator merchantOperatorListParamValidator,
			MerchantOperatorListQueryService merchantOperatorListQueryService,
			OperatorAccountPasswordUpdatePort operatorAccountPasswordUpdatePort,
			MerchantOperatorPasswordResetPort merchantOperatorPasswordResetPort,
			MerchantResetPasswordNoticeDispatchPublisher merchantResetPasswordNoticeDispatchPublisher,
			ShopOperatorLogsWrite shopOperatorLogsWrite) {
		this.objectMapper = objectMapper;
		this.shopOperatorCompanyActivation = shopOperatorCompanyActivation;
		this.merchantShopRoutePermissionPort = merchantShopRoutePermissionPort;
		this.shopOperatorDatapassApplyPort = shopOperatorDatapassApplyPort;
		this.merchantListRequestParamsResolver = merchantListRequestParamsResolver;
		this.merchantOperatorListParamValidator = merchantOperatorListParamValidator;
		this.merchantOperatorListQueryService = merchantOperatorListQueryService;
		this.operatorAccountPasswordUpdatePort = operatorAccountPasswordUpdatePort;
		this.merchantOperatorPasswordResetPort = merchantOperatorPasswordResetPort;
		this.merchantResetPasswordNoticeDispatchPublisher = merchantResetPasswordNoticeDispatchPublisher;
		this.shopOperatorLogsWrite = shopOperatorLogsWrite;
	}

	@DataPass
	@Activated(routeAlias = "merchant.operator.list")
	@GetMapping(value = "/operator", name = "商户账号列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getOperatorList(
			HttpServletRequest request,
			@SuppressWarnings("unused") @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false)
					String acceptLanguage)
			throws IOException {
		Object rawJwt = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> ud)) {
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
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		merchantShopRoutePermissionPort.assertMerchantOperatorListAllowed(user);
		shopOperatorDatapassApplyPort.apply(request, user, "merchant.operator.list");

		Map<String, Object> params = merchantListRequestParamsResolver.resolveToMapForOperator(request);
		merchantOperatorListParamValidator.validateFromMap(params);

		Object ccRaw = MerchantListParamValidator.scalarFrom(params.get("country_code"));
		String langTag = "zh-CN";
		if (ccRaw != null) {
			String t = ccRaw.toString().trim();
			if (StringUtils.hasText(t)) {
				langTag = t;
			}
		}

		Map<String, Object> body = merchantOperatorListQueryService.query(companyId, user, params, langTag);
		Object datapassEcho = MerchantDatapassBlockSupport.resolveEchoValue(request);
		body.put("datapass_block", datapassEcho);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) body.get("list");
		MerchantDataMasking.applyOperatorMobileMaskIfNeeded(list, MerchantDatapassBlockSupport.shouldMask(datapassEcho));

		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@Activated(routeAlias = "merchant.operator.save")
	@PostMapping(value = "/operator", name = "商户修改密码")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateOperatorAccount(HttpServletRequest request) {
		Object rawJwt = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> ud)) {
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
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		Map<String, String> p = readOperatorAccountParams(request);
		String operatorIdRaw = p.get("operator_id");
		String passwordRaw = p.get("password");

		if (operatorIdRaw == null || operatorIdRaw.isBlank()) {
			throw new ResourceException("operator_id必填");
		}
		if (passwordRaw == null || passwordRaw.isBlank()) {
			throw new ResourceException("密码必须6-16位");
		}
		if (passwordRaw.length() < 6 || passwordRaw.length() > 16) {
			throw new ResourceException("密码必须6-16位");
		}
		if (!passwordRaw.matches("(?i)^[_0-9a-z]{6,16}$")) {
			throw new ResourceException("密码格式不正确");
		}

		String tid = operatorIdRaw.trim();
		long operatorId;
		try {
			operatorId = Long.parseLong(tid);
		} catch (NumberFormatException ex) {
			throw new ResourceException("未查询到更新数据");
		}
		if (operatorId <= 0) {
			throw new ResourceException("未查询到更新数据");
		}

		operatorAccountPasswordUpdatePort.updatePasswordByOperatorId(operatorId, passwordRaw);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "merchant.operator.update")
	@PutMapping(value = "/operator/{id}", name = "平台重置商户密码")
	public ResponseEntity<ApiResult<Map<String, Object>>> resetOperatorAccount(
			@PathVariable("id") String id, HttpServletRequest request) {
		Object rawJwt = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> ud)) {
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
		shopOperatorCompanyActivation.assertShopOperatorCompanyActive(companyId);

		String tid = id == null ? "" : id.trim();
		long operatorId;
		try {
			operatorId = Long.parseLong(tid);
		} catch (NumberFormatException ex) {
			throw new ResourceException("该账号不存在");
		}
		if (operatorId <= 0) {
			throw new ResourceException("该账号不存在");
		}

		MerchantOperatorPasswordResetResult result =
				merchantOperatorPasswordResetPort.resetForMerchantConsole(companyId, operatorId);
		merchantResetPasswordNoticeDispatchPublisher.publish(
				result.companyId(), result.mobile(), result.plainPassword());

		long actingOperatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) actingOperatorId);
		logCtx.put("request_uri", "/api/v1/merchant/operator/" + tid);
		logCtx.put("ip", clientIp(request));
		Map<String, Object> paramsMap = Map.of("id", operatorId);
		try {
			logCtx.put("params", objectMapper.writeValueAsString(paramsMap));
		} catch (Exception e) {
			logCtx.put("params", paramsMap.toString());
		}
		logCtx.put("operator_name", "平台重置商户密码");
		logCtx.put("log_type", "operator");
		Object merchantIdObj = ud.get("merchant_id");
		if (merchantIdObj != null) {
			logCtx.put(
					"merchant_id",
					merchantIdObj instanceof Number n ? n.longValue() : Long.parseLong(merchantIdObj.toString()));
		}
		try {
			shopOperatorLogsWrite.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private Map<String, String> readOperatorAccountParams(HttpServletRequest request) {
		Object op = firstParameterValue(request, "operator_id");
		Object pwd = firstParameterValue(request, "password");

		String ct = request.getContentType();
		if (ct != null && ct.toLowerCase(Locale.ROOT).contains("application/json")) {
			try {
				byte[] buf = StreamUtils.copyToByteArray(request.getInputStream());
				if (buf.length == 0) {
					return operatorAccountParamMap(op, pwd);
				}
				JsonNode root = objectMapper.readTree(buf);
				if (!root.isObject()) {
					throw new ResourceException("请求体须为JSON对象");
				}
				if (root.has("operator_id")) {
					op = jsonNodeToOverlayValue(root.get("operator_id"));
				}
				if (root.has("password")) {
					pwd = jsonNodeToOverlayValue(root.get("password"));
				}
			} catch (ResourceException e) {
				throw e;
			} catch (IOException e) {
				throw new ResourceException("请求体JSON格式错误");
			}
		}
		return operatorAccountParamMap(op, pwd);
	}

	private static String firstParameterValue(HttpServletRequest request, String key) {
		String[] vals = request.getParameterValues(key);
		if (vals != null && vals.length > 0) {
			return vals[0];
		}
		return request.getParameter(key);
	}

	private Object jsonNodeToOverlayValue(JsonNode n) {
		if (n.isNull()) {
			return null;
		}
		if (n.isBoolean()) {
			return n.booleanValue();
		}
		if (n.isNumber()) {
			if (n.isIntegralNumber()) {
				return n.longValue();
			}
			return n.doubleValue();
		}
		if (n.isTextual()) {
			return n.asText();
		}
		return objectMapper.convertValue(n, Object.class);
	}

	private static Map<String, String> operatorAccountParamMap(Object operatorIdVal, Object passwordVal) {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("operator_id", mergedValueToString(operatorIdVal));
		m.put("password", mergedValueToString(passwordVal));
		return m;
	}

	private static String mergedValueToString(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof String s) {
			return s;
		}
		if (v instanceof Boolean b) {
			return b ? "true" : "false";
		}
		if (v instanceof Number n) {
			return n.toString();
		}
		return String.valueOf(v);
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
