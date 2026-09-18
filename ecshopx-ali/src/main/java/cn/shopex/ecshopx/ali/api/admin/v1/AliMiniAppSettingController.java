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

package cn.shopex.ecshopx.ali.api.admin.v1;

import cn.shopex.ecshopx.ali.service.minisetting.AliMiniAppMenuPermissionService;
import cn.shopex.ecshopx.ali.service.minisetting.AliMiniAppSettingInfoService;
import cn.shopex.ecshopx.ali.service.minisetting.AliMiniAppSettingSaveService;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@DingoResponse(resource = DingoResponse.ResourceStyle.DINGO_FIXED)
@RestController("aliminiappSettingAdminV1")
@RequestMapping("/api/v1/aliminiapp")
public class AliMiniAppSettingController {

	private static final List<String> SAVE_BODY_WHITELIST = List.of(
			"setting_id",
			"authorizer_appid",
			"merchant_private_key",
			"api_sign_method",
			"alipay_cert_path",
			"alipay_root_cert_path",
			"merchant_cert_path",
			"alipay_public_key",
			"notify_url",
			"encrypt_key");

	private static final Set<String> API_SIGN_METHOD_ALLOWED = Set.of("key", "cert");

	private final CompanysActivationService companysActivationService;
	private final AliMiniAppMenuPermissionService aliMiniAppMenuPermissionService;
	private final AliMiniAppSettingInfoService aliMiniAppSettingInfoService;
	private final AliMiniAppSettingSaveService aliMiniAppSettingSaveService;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;

	public AliMiniAppSettingController(
			CompanysActivationService companysActivationService,
			AliMiniAppMenuPermissionService aliMiniAppMenuPermissionService,
			AliMiniAppSettingInfoService aliMiniAppSettingInfoService,
			AliMiniAppSettingSaveService aliMiniAppSettingSaveService,
			OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper) {
		this.companysActivationService = companysActivationService;
		this.aliMiniAppMenuPermissionService = aliMiniAppMenuPermissionService;
		this.aliMiniAppSettingInfoService = aliMiniAppSettingInfoService;
		this.aliMiniAppSettingSaveService = aliMiniAppSettingSaveService;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
	}

	@Activated(routeAlias = "aliminiapp.setting.info")
	@GetMapping(value = "/setting/info", name = "获取支付宝小程序设置")
	public ResponseEntity<ApiResult<Map<String, Object>>> actionInfo(HttpServletRequest request) {
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
		aliMiniAppMenuPermissionService.assertSettingInfo(user);

		Map<String, Object> data = aliMiniAppSettingInfoService.getInfoByCompanyId(companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "aliminiapp.setting.save")
	@PostMapping(value = "/setting/save", name = "保存支付宝小程序设置")
	public ResponseEntity<ApiResult<Map<String, Object>>> actionSave(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
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
		aliMiniAppMenuPermissionService.assertSettingSave(user);

		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> filtered = whitelistSaveParams(merged);
		filtered.put("company_id", companyId);

		validateSaveParams(filtered);

		aliMiniAppSettingSaveService.save(companyId, filtered);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/aliminiapp/setting/save");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(filtered));
		} catch (Exception e) {
			logCtx.put("params", filtered.toString());
		}
		logCtx.put("operator_name", "保存支付宝小程序设置");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		operatorLogsWriteService.addLogs(logCtx);

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	private static void validateSaveParams(Map<String, Object> merged) {
		if (!isNonBlankParam(merged.get("authorizer_appid"))) {
			throw new ResourceException("authorizer_appid 必填");
		}
		if (!isNonBlankParam(merged.get("merchant_private_key"))) {
			throw new ResourceException("merchant_private_key 必填");
		}
		if (!isNonBlankParam(merged.get("api_sign_method"))) {
			throw new ResourceException("api_sign_method 必填");
		}
		String method = String.valueOf(merged.get("api_sign_method")).trim();
		if (!API_SIGN_METHOD_ALLOWED.contains(method)) {
			throw new ResourceException("api_sign_method 参数错误，只能为【key,cert】中的一种");
		}
	}

	private static Map<String, Object> whitelistSaveParams(Map<String, Object> merged) {
		Map<String, Object> out = new LinkedHashMap<>();
		for (String k : SAVE_BODY_WHITELIST) {
			if (merged.containsKey(k)) {
				out.put(k, merged.get(k));
			}
		}
		return out;
	}

	private Map<String, Object> mergeInputLikeFlexibleResolver(HttpServletRequest request, Map<String, Object> body) {
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

	private static boolean isNonBlankParam(Object v) {
		if (v == null) {
			return false;
		}
		String s = String.valueOf(v);
		return StringUtils.hasText(s);
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
