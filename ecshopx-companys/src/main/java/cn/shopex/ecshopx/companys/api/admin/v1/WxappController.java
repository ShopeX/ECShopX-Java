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

package cn.shopex.ecshopx.companys.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.AdminLog;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.auth.OperatorJwtIssuerPort;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.api.admin.v1.dto.BindWechatDistributorByMobileRequest;
import cn.shopex.ecshopx.companys.api.admin.v1.dto.BindWechatDistributorByUsernameRequest;
import cn.shopex.ecshopx.companys.api.admin.v1.dto.WxLiteLoginRequest;
import cn.shopex.ecshopx.companys.api.admin.v1.dto.WxOauthLoginRequest;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.companys.service.auth.OperatorAuthService;
import cn.shopex.ecshopx.companys.service.auth.wechatoauth.OperatorWechatOauthAuthorizeUrlService;
import cn.shopex.ecshopx.companys.service.auth.wechatoauth.WechatOauthLoginOutcome;
import cn.shopex.ecshopx.companys.service.auth.wechatoauth.WechatOauthLoginSuccess;
import cn.shopex.ecshopx.companys.service.auth.wechatoauth.WechatOauthLoginUnbound;
import cn.shopex.ecshopx.companys.service.operator.OperatorWechatBindSmsCodeService;
import cn.shopex.ecshopx.companys.web.CompanysAdminRequestMerge;
import cn.shopex.ecshopx.wechat.service.DistributorWechatFacade;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@RestController("companysAdminV1Wxapp")
@RequestMapping("/api/v1/operator/wechat")
public class WxappController {

	private static final Logger log = LoggerFactory.getLogger(WxappController.class);

	private static final Pattern MOBILE_LOCAL_ADMIN = Pattern.compile("^1[23456789]\\d{9}$");

	/** Bean Validation property paths; fixed order so the first reported violation is stable when several fields fail at once. */
	private static final List<String> BIND_WECHAT_BY_MOBILE_VIOLATION_ORDER = List.of(
			"companyId",
			"mobile",
			"vcode",
			"checkToken",
			"appId",
			"appType",
			"openid",
			"unionid");

	private static final List<String> BIND_WECHAT_BY_USERNAME_VIOLATION_ORDER = List.of(
			"companyId", "appId", "appType", "openid", "unionid", "username", "password");

	private static final List<String> WX_LITE_LOGIN_VIOLATION_ORDER =
			List.of("companyId", "appId", "appType", "openid", "unionid");

	private static final List<String> WX_OAUTH_LOGIN_VIOLATION_ORDER = List.of("token", "yzm");

	private final OperatorAuthService operatorAuthService;
	private final OperatorJwtIssuerPort operatorJwtIssuerPort;
	private final Validator validator;
	private final DistributorWechatFacade distributorWechatFacade;
	private final OperatorWechatBindSmsCodeService operatorWechatBindSmsCodeService;
	private final OperatorWechatOauthAuthorizeUrlService operatorWechatOauthAuthorizeUrlService;
	private final OperatorLogsWriteService operatorLogsWriteService;

	public WxappController(
			OperatorAuthService operatorAuthService,
			OperatorJwtIssuerPort operatorJwtIssuerPort,
			Validator validator,
			DistributorWechatFacade distributorWechatFacade,
			OperatorWechatBindSmsCodeService operatorWechatBindSmsCodeService,
			OperatorWechatOauthAuthorizeUrlService operatorWechatOauthAuthorizeUrlService,
			OperatorLogsWriteService operatorLogsWriteService) {
		this.operatorAuthService = operatorAuthService;
		this.operatorJwtIssuerPort = operatorJwtIssuerPort;
		this.validator = validator;
		this.distributorWechatFacade = distributorWechatFacade;
		this.operatorWechatBindSmsCodeService = operatorWechatBindSmsCodeService;
		this.operatorWechatOauthAuthorizeUrlService = operatorWechatOauthAuthorizeUrlService;
		this.operatorLogsWriteService = operatorLogsWriteService;
	}

	@AdminLog
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = false,
			notFound = false)
	@PostMapping(value = "/oauth/login", name = "微信oauth登录")
	public ResponseEntity<ApiResult<Map<String, Object>>> login(@FlexibleBody WxOauthLoginRequest body) {
		validateWxOauthLogin(body);
		Map<String, Object> params = new HashMap<>();
		params.put("code", body.getCode() == null ? "" : body.getCode());
		params.put("company_id", body.getCompanyId() == null ? "" : body.getCompanyId());
		params.put("logintype", "oauthwechat");
		if (StringUtils.hasText(body.getToken())) {
			params.put("token", body.getToken());
		}
		if (StringUtils.hasText(body.getYzm())) {
			params.put("yzm", body.getYzm());
		}
		WechatOauthLoginOutcome outcome = operatorAuthService.wechatOauthLogin(params);
		if (outcome instanceof WechatOauthLoginUnbound un) {
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("status", "unbound");
			data.put("company_id", String.valueOf(un.companyId()));
			data.put("app_id", un.appId());
			data.put("app_type", un.appType());
			data.put("openid", un.openid());
			data.put("unionid", un.unionid());
			return ResponseEntity.ok(ApiResult.ok(data));
		}
		if (outcome instanceof WechatOauthLoginSuccess success) {
			String jwt = operatorJwtIssuerPort.issueToken(success.jwtClaims());
			if (!StringUtils.hasText(jwt)) {
				throw new UnauthorizedException("登录失败");
			}
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("status", "success");
			data.put("token", jwt);
			data.put("company_id", "");
			data.put("app_id", "");
			data.put("app_type", "wx");
			data.put("openid", "");
			data.put("unionid", "");
			return ResponseEntity.ok(ApiResult.ok(data));
		}
		throw new UnauthorizedException("登录失败");
	}

	@AdminLog
	@PostMapping(value = "/lite/login", name = "小程序登录")
	public ResponseEntity<ApiResult<Map<String, String>>> wxLiteLogin(@FlexibleBody WxLiteLoginRequest body) {
		validateWxLiteLogin(body);
		Map<String, Object> params = new HashMap<>();
		params.put("company_id", body.getCompanyId().trim());
		params.put("app_id", body.getAppId().trim());
		params.put("app_type", body.getAppType().trim());
		params.put("openid", body.getOpenid().trim());
		params.put("unionid", body.getUnionid().trim());
		params.put("logintype", "wechatbinddistributorbylite");
		Map<String, Object> newOp = operatorAuthService.retrieveByCredentials(params);
		String jwt = operatorJwtIssuerPort.issueToken(newOp);
		return ResponseEntity.ok(ApiResult.ok(Map.of("token", jwt)));
	}

	@AdminLog
	@PostMapping(value = "/bind_mobile", name = "绑定微信手机号")
	public ResponseEntity<ApiResult<Map<String, String>>> bindMobile(@FlexibleBody BindWechatDistributorByMobileRequest body) {
		validateBindWechatDistributorByMobile(body);
		if (!MOBILE_LOCAL_ADMIN.matcher(body.getMobile().trim()).matches()) {
			throw new BadRequestException("请输入合法手机号码");
		}
		Map<String, Object> params = new HashMap<>();
		params.put("company_id", body.getCompanyId());
		params.put("app_id", body.getAppId());
		params.put("app_type", body.getAppType());
		params.put("openid", body.getOpenid());
		params.put("unionid", body.getUnionid());
		params.put("mobile", body.getMobile());
		params.put("vcode", body.getVcode());
		params.put("check_token", body.getCheckToken());
		params.put("logintype", "wechatbinddistributor");
		Map<String, Object> newOp = operatorAuthService.retrieveByCredentials(params);
		String jwt = operatorJwtIssuerPort.issueToken(newOp);
		return ResponseEntity.ok(ApiResult.ok(Map.of("token", jwt)));
	}

	@AdminLog
	@PostMapping(value = "/bind_account", name = "绑定账号")
	public ResponseEntity<ApiResult<Map<String, String>>> bindAccountLogin(
			@FlexibleBody BindWechatDistributorByUsernameRequest body) {
		validateBindWechatDistributorByUsername(body);
		Map<String, Object> params = new HashMap<>();
		params.put("company_id", body.getCompanyId());
		params.put("app_id", body.getAppId());
		params.put("app_type", body.getAppType());
		params.put("openid", body.getOpenid());
		params.put("unionid", body.getUnionid());
		params.put("username", body.getUsername());
		params.put("password", body.getPassword());
		params.put("logintype", "wechatbinddistributorbyusername");
		Map<String, Object> newOp = operatorAuthService.retrieveByCredentials(params);
		String jwt = operatorJwtIssuerPort.issueToken(newOp);
		return ResponseEntity.ok(ApiResult.ok(Map.of("token", jwt)));
	}

	private void validateWxLiteLogin(WxLiteLoginRequest body) {
		Set<ConstraintViolation<WxLiteLoginRequest>> violations = validator.validate(body);
		if (violations.isEmpty()) {
			return;
		}
		throw ValidationViolationOrder.buildOrderedConstraintViolationException(
				violations, WX_LITE_LOGIN_VIOLATION_ORDER);
	}

	private void validateWxOauthLogin(WxOauthLoginRequest body) {
		if (body == null) {
			throw new BadRequestException("缺少参数，登录失败", 400);
		}
		String code = body.getCode();
		String companyId = body.getCompanyId();
		if (code == null || code.isEmpty() || companyId == null || companyId.isEmpty()) {
			throw new BadRequestException("缺少参数，登录失败", 400);
		}
		final Set<ConstraintViolation<WxOauthLoginRequest>> violations;
		try {
			violations = validator.validate(body);
		} catch (IllegalArgumentException ex) {
			// Hibernate Validator: HV000116 The object to be validated must not be null
			if (ex.getMessage() != null && ex.getMessage().contains("HV000116")) {
				throw new BadRequestException("缺少参数，登录失败", 400);
			}
			throw ex;
		}
		if (violations.isEmpty()) {
			return;
		}
		throw ValidationViolationOrder.buildOrderedConstraintViolationException(
				violations, WX_OAUTH_LOGIN_VIOLATION_ORDER, 400);
	}

	private void validateBindWechatDistributorByMobile(BindWechatDistributorByMobileRequest body) {
		if (body == null) {
			throw new BadRequestException("请求体不能为空");
		}
		final Set<ConstraintViolation<BindWechatDistributorByMobileRequest>> violations;
		try {
			violations = validator.validate(body);
		} catch (IllegalArgumentException ex) {
			// Hibernate Validator: HV000116 The object to be validated must not be null
			if (ex.getMessage() != null && ex.getMessage().contains("HV000116")) {
				throw new BadRequestException("请求体不能为空");
			}
			throw ex;
		}
		if (violations.isEmpty()) {
			return;
		}
		throw ValidationViolationOrder.buildOrderedConstraintViolationException(
				violations, BIND_WECHAT_BY_MOBILE_VIOLATION_ORDER);
	}

	private void validateBindWechatDistributorByUsername(BindWechatDistributorByUsernameRequest body) {
		Set<ConstraintViolation<BindWechatDistributorByUsernameRequest>> violations = validator.validate(body);
		if (violations.isEmpty()) {
			return;
		}
		throw ValidationViolationOrder.buildOrderedConstraintViolationException(
				violations, BIND_WECHAT_BY_USERNAME_VIOLATION_ORDER);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = false,
			notFound = false)
	@GetMapping(value = "/authorizeurl", name = "获取微信oauth登录链接")
	public ResponseEntity<ApiResult<Map<String, String>>> getWechatOuthorizeurl(
			@RequestParam(name = "company_id", required = false) String companyId, HttpServletRequest request) {
		if (isMissingCompanyId(companyId)) {
			throw new BadRequestException("缺少参数", 400);
		}
		String url = operatorWechatOauthAuthorizeUrlService.getWechatOuthorizeurl(companyId);
		String safeUrl = url == null ? "" : url;
		ResponseEntity<ApiResult<Map<String, String>>> resp =
				ResponseEntity.ok(ApiResult.ok(Map.of("url", safeUrl)));
		try {
			Map<String, Object> ctx = new LinkedHashMap<>();
			ctx.put("company_id", 0L);
			ctx.put("operator_id", 0);
			ctx.put("merchant_id", 0L);
			ctx.put("operator_name", "登录失败");
			ctx.put("request_uri", operatorLoginLogRequestPath(request));
			ctx.put("ip", clientIpForOperatorLog(request));
			ctx.put("log_type", "login");
			operatorLogsWriteService.addLogs(ctx);
		} catch (Exception ex) {
			log.info("getWechatOuthorizeurl addLogs failed", ex);
		}
		return resp;
	}

	/**
	 * Whether the {@code company_id} query is treated as absent for this route: {@code null}, empty, or the literal
	 * {@code "0"}. Not trimmed so values such as {@code " 0 "} are not folded into the missing branch.
	 */
	private static boolean isMissingCompanyId(String companyId) {
		return companyId == null || companyId.isEmpty() || "0".equals(companyId);
	}

	private static String operatorLoginLogRequestPath(HttpServletRequest request) {
		String raw = request.getRequestURI();
		if (raw == null) {
			raw = "";
		}
		int q = raw.indexOf('?');
		if (q >= 0) {
			raw = raw.substring(0, q);
		}
		String cp = request.getContextPath();
		if (StringUtils.hasText(cp) && raw.startsWith(cp)) {
			return raw.substring(cp.length());
		}
		return raw;
	}

	private static String clientIpForOperatorLog(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(xff)) {
			int comma = xff.indexOf(',');
			String first = comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
			if (StringUtils.hasText(first)) {
				return first;
			}
		}
		String addr = request.getRemoteAddr();
		return addr != null ? addr : "";
	}

	@AdminLog
	@PostMapping(value = "/sms/code", name = "发送手机短信验证码")
	public ResponseEntity<ApiResult<Map<String, Object>>> getSmsCode(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> input = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		operatorWechatBindSmsCodeService.sendWechatBindSmsCode(input);
		return ResponseEntity.ok(ApiResult.ok(Map.<String, Object>of("status", true)));
	}

	@Activated(routeAlias = "wechat.distributor.js.config")
	@PostMapping(value = "/distributor/js/config", name = "前台获取JsSDK")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDistributorJsConfig(
			@FlexibleBody(required = false) Map<String, Object> body) {
		if (body == null) {
			throw new BadRequestException("当前页面url必填");
		}
		Object urlRaw = body.get("url");
		if (urlRaw == null) {
			throw new BadRequestException("当前页面url必填");
		}
		String url = String.valueOf(urlRaw).trim();
		if (!StringUtils.hasText(url)) {
			throw new BadRequestException("当前页面url必填");
		}
		long companyId = parseRequiredCompanyId(body.get("company_id"));
		Map<String, Object> result = distributorWechatFacade.buildDistributorJsConfig(companyId, url);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	private static long parseRequiredCompanyId(Object raw) {
		if (raw == null) {
			throw new BadRequestException("当前页面companyId必填");
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			if (v <= 0L) {
				throw new BadRequestException("当前页面companyId必填");
			}
			return v;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty() || "0".equals(t)) {
				throw new BadRequestException("当前页面companyId必填");
			}
			try {
				long v = Long.parseLong(t);
				if (v <= 0L) {
					throw new BadRequestException("当前页面companyId必填");
				}
				return v;
			} catch (NumberFormatException e) {
				throw new BadRequestException("companyId 格式错误");
			}
		}
		throw new BadRequestException("companyId 格式错误");
	}
}
