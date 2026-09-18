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

package cn.shopex.ecshopx.wechat.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.wechat.service.WechatAuthBindService;
import cn.shopex.ecshopx.wechat.service.WechatAuthQueryService;
import cn.shopex.ecshopx.wechat.service.WechatPreAuthUrlService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
@RestController("wechatAdminV1Authorized")
@RequestMapping("/api/v1/wechat")
public class AuthorizedController {

	private final WechatAuthBindService wechatAuthBindService;
	private final WechatAuthQueryService wechatAuthQueryService;
	private final WechatPreAuthUrlService wechatPreAuthUrlService;

	public AuthorizedController(
			WechatAuthBindService wechatAuthBindService,
			WechatAuthQueryService wechatAuthQueryService,
			WechatPreAuthUrlService wechatPreAuthUrlService) {
		this.wechatAuthBindService = wechatAuthBindService;
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.wechatPreAuthUrlService = wechatPreAuthUrlService;
	}

	@Activated(routeAlias = "wechat.pre_auth_url")
	@GetMapping(value = "/pre_auth_url", name = "预授权URL", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, String>>> getPreAuthUrl(
			@RequestParam(value = "callback_url", required = false) String callbackUrl) {
		String url = wechatPreAuthUrlService.getPreAuthUrl(callbackUrl);
		return ResponseEntity.ok(ApiResult.ok(Map.of("url", url)));
	}

	@Activated(routeAlias = "wechat.bind")
	@PostMapping(value = "/bind", name = "回调绑定授权", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> authorizedBind(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		String authCode = merged.get("auth_code") == null ? null : String.valueOf(merged.get("auth_code")).trim();
		String authType = merged.get("auth_type") == null ? null : String.valueOf(merged.get("auth_type")).trim();
		long companyId;
		long operatorId;
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object companyIdObj = map.get("company_id");
		Object operatorIdObj = map.get("operator_id");
		if (companyIdObj == null || operatorIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
			operatorId = Long.parseLong(String.valueOf(operatorIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		Map<String, Object> data = wechatAuthBindService.authorizedBind(companyId, operatorId, authCode, authType);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "wechat.bind.direct")
	@PostMapping(value = "/directbind", name = "直连小程序", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> directBind(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		String bindType = merged.get("bind_type") == null ? null : String.valueOf(merged.get("bind_type")).trim();
		long companyId;
		long operatorId;
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object companyIdObj = map.get("company_id");
		Object operatorIdObj = map.get("operator_id");
		if (companyIdObj == null || operatorIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
			operatorId = Long.parseLong(String.valueOf(operatorIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		Map<String, Object> data = wechatAuthBindService.directBind(companyId, operatorId, merged, bindType);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "wechat.authorizerInfo")
	@GetMapping(value = "/authorizerinfo", name = "公众帐号基础信息", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getAuthorizerInfo(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		Object raw = map.get("authorizer_appid");
		String authorizerAppid = raw == null ? null : String.valueOf(raw).trim();
		if (authorizerAppid != null && authorizerAppid.isEmpty()) {
			authorizerAppid = null;
		}
		Map<String, Object> data = wechatAuthQueryService.getAuthorizerInfo(companyId, authorizerAppid);
		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
