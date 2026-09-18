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
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.merchant.port.MerchantShopRoutePermissionPort;
import cn.shopex.ecshopx.merchant.port.ShopOperatorCompanyActivation;
import cn.shopex.ecshopx.merchant.port.ShopOperatorDatapassApplyPort;
import cn.shopex.ecshopx.merchant.port.ShopOperatorLogsWrite;
import cn.shopex.ecshopx.merchant.service.MerchantDataMasking;
import cn.shopex.ecshopx.merchant.service.MerchantSettlementApplyDetailQueryService;
import cn.shopex.ecshopx.merchant.service.MerchantSettlementApplyListParamValidator;
import cn.shopex.ecshopx.merchant.service.MerchantSettlementApplyListQueryInput;
import cn.shopex.ecshopx.merchant.service.MerchantSettlementApplyListQueryService;
import cn.shopex.ecshopx.merchant.service.MerchantSettlementApplyListRequestParamsResolver;
import cn.shopex.ecshopx.merchant.service.MerchantSettlementApplyAuditParamValidator;
import cn.shopex.ecshopx.merchant.service.MerchantSettlementApplyAuditService;
import cn.shopex.ecshopx.merchant.web.MerchantDatapassBlockSupport;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@DingoResponse(resource = DingoResponse.ResourceStyle.DINGO)
@RestController("merchantAdminV1MerchantSettlementApply")
@RequestMapping("/api/v1/merchant/settlement/apply")
public class MerchantSettlementApplyController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private static final List<String> AUDIT_PARAM_KEYS =
			List.of("id", "audit_status", "audit_memo", "audit_goods");

	private final ObjectMapper objectMapper;
	private final ShopOperatorCompanyActivation shopOperatorCompanyActivation;
	private final ShopOperatorLogsWrite shopOperatorLogsWrite;
	private final MerchantShopRoutePermissionPort merchantShopRoutePermissionPort;
	private final MerchantSettlementApplyListRequestParamsResolver merchantSettlementApplyListRequestParamsResolver;
	private final MerchantSettlementApplyListParamValidator merchantSettlementApplyListParamValidator;
	private final MerchantSettlementApplyListQueryService merchantSettlementApplyListQueryService;
	private final MerchantSettlementApplyAuditParamValidator merchantSettlementApplyAuditParamValidator;
	private final MerchantSettlementApplyAuditService merchantSettlementApplyAuditService;
	private final ShopOperatorDatapassApplyPort shopOperatorDatapassApplyPort;
	private final MerchantSettlementApplyDetailQueryService merchantSettlementApplyDetailQueryService;

	public MerchantSettlementApplyController(
			ObjectMapper objectMapper,
			ShopOperatorCompanyActivation shopOperatorCompanyActivation,
			ShopOperatorLogsWrite shopOperatorLogsWrite,
			MerchantShopRoutePermissionPort merchantShopRoutePermissionPort,
			MerchantSettlementApplyListRequestParamsResolver merchantSettlementApplyListRequestParamsResolver,
			MerchantSettlementApplyListParamValidator merchantSettlementApplyListParamValidator,
			MerchantSettlementApplyListQueryService merchantSettlementApplyListQueryService,
			MerchantSettlementApplyAuditParamValidator merchantSettlementApplyAuditParamValidator,
			MerchantSettlementApplyAuditService merchantSettlementApplyAuditService,
			ShopOperatorDatapassApplyPort shopOperatorDatapassApplyPort,
			MerchantSettlementApplyDetailQueryService merchantSettlementApplyDetailQueryService) {
		this.objectMapper = objectMapper;
		this.shopOperatorCompanyActivation = shopOperatorCompanyActivation;
		this.shopOperatorLogsWrite = shopOperatorLogsWrite;
		this.merchantShopRoutePermissionPort = merchantShopRoutePermissionPort;
		this.merchantSettlementApplyListRequestParamsResolver = merchantSettlementApplyListRequestParamsResolver;
		this.merchantSettlementApplyListParamValidator = merchantSettlementApplyListParamValidator;
		this.merchantSettlementApplyListQueryService = merchantSettlementApplyListQueryService;
		this.merchantSettlementApplyAuditParamValidator = merchantSettlementApplyAuditParamValidator;
		this.merchantSettlementApplyAuditService = merchantSettlementApplyAuditService;
		this.shopOperatorDatapassApplyPort = shopOperatorDatapassApplyPort;
		this.merchantSettlementApplyDetailQueryService = merchantSettlementApplyDetailQueryService;
	}

	@Activated(routeAlias = "merchant.settlement.apply.list")
	@GetMapping(value = "/list", name = "商户入驻申请列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getList(HttpServletRequest request) {
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
		merchantShopRoutePermissionPort.assertMerchantSettlementApplyListAllowed(user);

		Map<String, Object> params = merchantSettlementApplyListRequestParamsResolver.resolve(request);
		MerchantSettlementApplyListQueryInput input = merchantSettlementApplyListParamValidator.validateAndExtract(params);
		Map<String, Object> body = merchantSettlementApplyListQueryService.query(companyId, input);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@DataPass
	@Activated(routeAlias = "merchant.settlement.apply.detail")
	@GetMapping(value = "/{id}", name = "商户入驻申请详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDetail(
			HttpServletRequest request,
			@PathVariable("id") String id,
			@RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
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
		long applyId;
		try {
			applyId = Long.parseLong(tid);
		} catch (NumberFormatException ex) {
			throw new ResourceException("入驻申请查询失败");
		}
		if (applyId <= 0) {
			throw new ResourceException("入驻申请查询失败");
		}

		merchantShopRoutePermissionPort.assertMerchantSettlementApplyDetailAllowed(user);
		shopOperatorDatapassApplyPort.apply(request, user, "merchant.settlement.apply.detail");
		Map<String, Object> result =
				merchantSettlementApplyDetailQueryService.getDetail(applyId, acceptLanguage);
		Object datapassEcho = MerchantDatapassBlockSupport.resolveEchoValue(request);
		result.put("datapass_block", datapassEcho);
		if (MerchantDatapassBlockSupport.shouldMask(datapassEcho)) {
			MerchantDataMasking.applyDetail(result);
		}
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "merchant.settlement.apply.audit")
	@PostMapping(value = "/audit", name = "审核商户入驻申请")
	public ResponseEntity<ApiResult<Map<String, Object>>> auditData(
			HttpServletRequest request,
			@RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage)
			throws IOException {
		Map<String, Object> merged = collectAuditParams(request);

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

		Map<String, Object> params = merchantSettlementApplyAuditParamValidator.validate(merged);
		params.put("company_id", companyId);

		merchantSettlementApplyAuditService.settlementApplyAudit(params, acceptLanguage);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/merchant/settlement/apply/audit");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(params));
		} catch (Exception e) {
			logCtx.put("params", params.toString());
		}
		logCtx.put("operator_name", "审核商户入驻申请");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			shopOperatorLogsWrite.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private Map<String, Object> collectAuditParams(HttpServletRequest request) throws IOException {
		Map<String, Object> merged = new LinkedHashMap<>();
		for (String key : AUDIT_PARAM_KEYS) {
			String[] vals = request.getParameterValues(key);
			if (vals == null || vals.length == 0) {
				vals = request.getParameterValues(key + "[]");
			}
			if (vals != null && vals.length > 0) {
				if (vals.length == 1) {
					merged.put(key, vals[0]);
				} else {
					merged.put(key, new ArrayList<>(Arrays.asList(vals)));
				}
			}
		}
		String ct = request.getContentType();
		if (ct != null) {
			String ctl = ct.toLowerCase(Locale.ROOT);
			if (ctl.contains("application/json")) {
				byte[] buf = StreamUtils.copyToByteArray(request.getInputStream());
				if (buf.length > 0) {
					Map<String, Object> jsonMap = objectMapper.readValue(buf, new TypeReference<Map<String, Object>>() {});
					for (String k : AUDIT_PARAM_KEYS) {
						if (jsonMap.containsKey(k)) {
							merged.put(k, jsonMap.get(k));
						}
					}
				}
			}
		}
		return merged;
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
