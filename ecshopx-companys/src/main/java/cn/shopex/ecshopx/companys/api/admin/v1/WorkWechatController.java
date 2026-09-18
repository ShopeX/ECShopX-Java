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
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.auth.OperatorJwtIssuerPort;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.api.admin.v1.dto.BindWorkWechatMobileRequest;
import cn.shopex.ecshopx.companys.api.admin.v1.dto.WorkWechatOauthLoginRequest;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.companys.service.auth.OperatorAuthService;
import cn.shopex.ecshopx.companys.service.auth.workwechatoauth.OperatorWorkWechatOauthAuthorizeUrlService;
import cn.shopex.ecshopx.companys.service.auth.workwechatoauth.WorkWechatOauthLoginOutcome;
import cn.shopex.ecshopx.companys.service.auth.workwechatoauth.WorkWechatOauthLoginSuccess;
import cn.shopex.ecshopx.companys.service.auth.workwechatoauth.WorkWechatOauthLoginUnbound;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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
@RestController("companysAdminV1WorkWechat")
@RequestMapping("/api/v1/operator/workwechat")
public class WorkWechatController {

	private static final Logger log = LoggerFactory.getLogger(WorkWechatController.class);

	private static final Pattern MOBILE_WORKWECHAT_BIND = Pattern.compile("^1[3456789]\\d{9}$");

	/**
	 * Ordered Bean Validation property-path names for {@link ValidationViolationOrder}: pick the first violation in
	 * this sequence instead of relying on {@link Set} iteration order. {@link #validateWorkWechatOauthLogin} passes
	 * {@code embeddedStatusCode = 400}; {@link #validateBindWorkWechatMobile} uses the two-argument overload default
	 * ({@code 422}).
	 */
	private static final List<String> BIND_WORKWECHAT_MOBILE_VIOLATION_ORDER = List.of(
			"companyId", "workUserid", "checkToken", "mobile", "vcode");

	private static final List<String> WORKWECHAT_OAUTH_LOGIN_VIOLATION_ORDER =
			List.of("code", "companyId", "token", "yzm");

	private final OperatorAuthService operatorAuthService;
	private final OperatorJwtIssuerPort operatorJwtIssuerPort;
	private final Validator validator;
	private final OperatorWorkWechatOauthAuthorizeUrlService operatorWorkWechatOauthAuthorizeUrlService;
	private final OperatorLogsWriteService operatorLogsWriteService;

	public WorkWechatController(
			OperatorAuthService operatorAuthService,
			OperatorJwtIssuerPort operatorJwtIssuerPort,
			Validator validator,
			OperatorWorkWechatOauthAuthorizeUrlService operatorWorkWechatOauthAuthorizeUrlService,
			OperatorLogsWriteService operatorLogsWriteService) {
		this.operatorAuthService = operatorAuthService;
		this.operatorJwtIssuerPort = operatorJwtIssuerPort;
		this.validator = validator;
		this.operatorWorkWechatOauthAuthorizeUrlService = operatorWorkWechatOauthAuthorizeUrlService;
		this.operatorLogsWriteService = operatorLogsWriteService;
	}

	@AdminLog
	@PostMapping(value = "/oauth/login", name = "企业微信oauth登录")
	public ResponseEntity<ApiResult<Map<String, String>>> login(@FlexibleBody WorkWechatOauthLoginRequest body) {
		validateWorkWechatOauthLogin(body);
		Map<String, Object> params = new HashMap<>();
		params.put("code", body.getCode() == null ? "" : body.getCode());
		params.put("company_id", body.getCompanyId() == null ? "" : body.getCompanyId());
		if (StringUtils.hasText(body.getToken())) {
			params.put("token", body.getToken());
		}
		if (StringUtils.hasText(body.getYzm())) {
			params.put("yzm", body.getYzm());
		}
		WorkWechatOauthLoginOutcome outcome = operatorAuthService.workWeChatOauthLogin(params);
		if (outcome instanceof WorkWechatOauthLoginUnbound un) {
			Map<String, String> data = new LinkedHashMap<>();
			data.put("status", "unbound");
			data.put("token", "");
			data.put("company_id", un.companyId());
			data.put("work_userid", un.workUserid());
			data.put("check_token", un.checkToken());
			return ResponseEntity.ok(ApiResult.ok(data));
		}
		if (outcome instanceof WorkWechatOauthLoginSuccess success) {
			String jwt = operatorJwtIssuerPort.issueToken(success.jwtClaims());
			if (!StringUtils.hasText(jwt)) {
				throw new UnauthorizedException("登录失败");
			}
			Map<String, String> data = new LinkedHashMap<>();
			data.put("status", "success");
			data.put("token", jwt);
			data.put("company_id", "");
			data.put("work_userid", "");
			data.put("check_token", "");
			return ResponseEntity.ok(ApiResult.ok(data));
		}
		throw new UnauthorizedException("登录失败");
	}

	@AdminLog
	@PostMapping(value = "/bind_mobile", name = "绑定企业微信手机号")
	public ResponseEntity<ApiResult<Map<String, String>>> bindMobile(@FlexibleBody BindWorkWechatMobileRequest body) {
		validateBindWorkWechatMobile(body);
		if (!MOBILE_WORKWECHAT_BIND.matcher(body.getMobile().trim()).matches()) {
			throw new BadRequestException("请输入合法手机号码");
		}
		Map<String, Object> params = new HashMap<>();
		params.put("company_id", body.getCompanyId());
		params.put("work_userid", body.getWorkUserid());
		params.put("mobile", body.getMobile());
		params.put("vcode", body.getVcode());
		params.put("check_token", body.getCheckToken());
		params.put("logintype", "workwechatbind");
		Map<String, Object> newOp = operatorAuthService.retrieveByCredentials(params);
		String jwt = operatorJwtIssuerPort.issueToken(newOp);
		return ResponseEntity.ok(ApiResult.ok(Map.of("token", jwt)));
	}

	private void validateBindWorkWechatMobile(BindWorkWechatMobileRequest body) {
		Set<ConstraintViolation<BindWorkWechatMobileRequest>> violations = validator.validate(body);
		if (violations.isEmpty()) {
			return;
		}
		throw ValidationViolationOrder.buildOrderedConstraintViolationException(
				violations, BIND_WORKWECHAT_MOBILE_VIOLATION_ORDER);
	}

	private void validateWorkWechatOauthLogin(WorkWechatOauthLoginRequest body) {
		if (body == null) {
			throw new BadRequestException("缺少参数，登录失败", 400);
		}
		final Set<ConstraintViolation<WorkWechatOauthLoginRequest>> violations;
		try {
			violations = validator.validate(body);
		} catch (IllegalArgumentException ex) {
			if (ex.getMessage() != null && ex.getMessage().contains("HV000116")) {
				throw new BadRequestException("缺少参数，登录失败", 400);
			}
			throw ex;
		}
		if (violations.isEmpty()) {
			return;
		}
		throw ValidationViolationOrder.buildOrderedConstraintViolationException(
				violations, WORKWECHAT_OAUTH_LOGIN_VIOLATION_ORDER, 400);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = true)
	@GetMapping(value = "/authorizeurl", name = "获取企业微信oauth登录链接")
	public ResponseEntity<ApiResult<Map<String, String>>> getWorkwechatOuthorizeurl(
			@RequestParam(name = "company_id", required = false) String companyId, HttpServletRequest request) {
		if (isMissingCompanyId(companyId)) {
			throw new BadRequestException("缺少参数", 400);
		}
		String url = operatorWorkWechatOauthAuthorizeUrlService.getWorkwechatOuthorizeurl(companyId);
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
			log.info("getWorkwechatOuthorizeurl addLogs failed", ex);
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
}
