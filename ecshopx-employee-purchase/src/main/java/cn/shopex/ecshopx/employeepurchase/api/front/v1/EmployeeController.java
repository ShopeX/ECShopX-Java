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

package cn.shopex.ecshopx.employeepurchase.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.employeepurchase.service.EmployeeAuthenticationService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeeCheckService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeeEmailVcodeSendService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeeInviteListService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseActivityDataService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseInviteCodeService;
import cn.shopex.ecshopx.employeepurchase.service.EmployeeRelativeBindService;
import cn.shopex.ecshopx.members.service.h5.H5BearerJwtClaimsService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("employeepurchaseEmployeeFrontV1")
@RequestMapping("/api/v1/h5app")
public class EmployeeController {

	private final EmployeeRelativeBindService employeeRelativeBindService;
	private final EmployeeAuthenticationService employeeAuthenticationService;
	private final EmployeeCheckService employeeCheckService;
	private final EmployeePurchaseActivityDataService employeePurchaseActivityDataService;
	private final EmployeeInviteListService employeeInviteListService;
	private final EmployeePurchaseInviteCodeService employeePurchaseInviteCodeService;
	private final EmployeeEmailVcodeSendService employeeEmailVcodeSendService;
	private final H5BearerJwtClaimsService h5BearerJwtClaimsService;
	private final Environment environment;

	public EmployeeController(
			EmployeeRelativeBindService employeeRelativeBindService,
			EmployeeAuthenticationService employeeAuthenticationService,
			EmployeeCheckService employeeCheckService,
			EmployeePurchaseActivityDataService employeePurchaseActivityDataService,
			EmployeeInviteListService employeeInviteListService,
			EmployeePurchaseInviteCodeService employeePurchaseInviteCodeService,
			EmployeeEmailVcodeSendService employeeEmailVcodeSendService,
			H5BearerJwtClaimsService h5BearerJwtClaimsService,
			Environment environment) {
		this.employeeRelativeBindService = employeeRelativeBindService;
		this.employeeAuthenticationService = employeeAuthenticationService;
		this.employeeCheckService = employeeCheckService;
		this.employeePurchaseActivityDataService = employeePurchaseActivityDataService;
		this.employeeInviteListService = employeeInviteListService;
		this.employeePurchaseInviteCodeService = employeePurchaseInviteCodeService;
		this.employeeEmailVcodeSendService = employeeEmailVcodeSendService;
		this.h5BearerJwtClaimsService = h5BearerJwtClaimsService;
		this.environment = environment;
	}

	@GetMapping("/wxapp/employee/email/vcode")
	@FrontNoAuth
	public ResponseEntity<ApiResult<Map<String, Object>>> sendEmailVcode(
			HttpServletRequest request,
			@RequestParam(value = "email", required = false) String email,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") String distributorIdRaw,
			@RequestParam(value = "enterprise_id", required = false, defaultValue = "0") String enterpriseIdRaw) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && !s.isBlank()) {
			try {
				companyId = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
		} else {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}

		int dist;
		try {
			dist = Integer.parseInt(distributorIdRaw == null ? "0" : distributorIdRaw.trim());
		} catch (NumberFormatException e) {
			dist = 0;
		}
		long ent;
		try {
			ent = Long.parseLong(enterpriseIdRaw == null ? "0" : enterpriseIdRaw.trim());
		} catch (NumberFormatException e) {
			ent = 0L;
		}

		boolean ok = employeeEmailVcodeSendService.sendEmailVcode(companyId, email == null ? "" : email, dist, ent);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", ok);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping("/wxapp/employee/check")
	@FrontNoAuth
	public ResponseEntity<ApiResult<Map<String, Object>>> employeeCheck(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);

		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && !s.isBlank()) {
			try {
				companyId = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
		} else {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}

		Object rawAuthType = merged.get("auth_type");
		String authTypeStr = rawAuthType == null ? "" : rawAuthType.toString().trim();
		if (!StringUtils.hasText(authTypeStr)) {
			throw new ResourceException("验证方式必填");
		}

		replaceMemberMobilePlaceholderFromH5Claims(request, merged);

		merged.put("company_id", companyId);

		Map<String, Object> data = employeeCheckService.doEmployeeCheck(companyId, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping("/wxapp/employee/auth")
	public ResponseEntity<ApiResult<Map<String, Object>>> authentication(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);

		Map<String, Object> claims = h5BearerJwtClaimsService
				.verifyAndExtractClaims(request)
				.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		long companyId = parseLongFlexible(claims.get("company_id"), 1L);
		long userId = resolveH5AuthenticatedUserId(request, claims, merged);

		Object mob = claims.get("mobile");
		String memberMobile = mob == null ? "0" : mob.toString();

		replaceMemberMobilePlaceholder(merged, memberMobile);

		merged.put("company_id", companyId);
		merged.put("user_id", userId);
		merged.put("member_mobile", memberMobile);

		long enterpriseId = requirePositiveLongField(merged.get("enterprise_id"), "企业ID必填");
		merged.put("enterprise_id", enterpriseId);

		String authType = requireNonBlankTrimmedForResource(merged.get("auth_type"), "认证方式必填");
		merged.put("auth_type", authType);

		String authLower = authType.toLowerCase(Locale.ROOT);
		if ("mobile".equals(authLower) || "account".equals(authLower)) {
			requireNonBlankTrimmedForResource(merged.get("employee_id"), "员工ID必填");
		}
		if ("email".equalsIgnoreCase(authType)) {
			requireNonBlankTrimmedForResource(merged.get("email"), "邮箱必填");
		}
		if ("qr_code".equalsIgnoreCase(authType)) {
			requireNonBlankTrimmedForResource(merged.get("mobile"), "手机号必填");
		}

		employeeAuthenticationService.authenticate(merged);

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping("/wxapp/employee/activitydata")
	public ResponseEntity<ApiResult<Map<String, Object>>> getActivityData(
			HttpServletRequest request,
			@RequestParam(value = "activity_id", required = false) String activityIdRaw,
			@RequestParam(value = "enterprise_id", required = false) String enterpriseIdRaw) {
		Map<String, Object> claims = h5BearerJwtClaimsService
				.verifyAndExtractClaims(request)
				.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		long companyId = parseLongFlexible(claims.get("company_id"), 1L);
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, null);
		long userId = resolveH5AuthenticatedUserId(request, claims, merged);

		if (activityIdRaw == null || !StringUtils.hasText(activityIdRaw.trim())) {
			throw new ResourceException("活动ID必填");
		}
		if (enterpriseIdRaw == null || !StringUtils.hasText(enterpriseIdRaw.trim())) {
			throw new ResourceException("企业ID必填");
		}

		long activityId;
		try {
			activityId = Long.parseLong(activityIdRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("活动ID格式错误");
		}
		long enterpriseId;
		try {
			enterpriseId = Long.parseLong(enterpriseIdRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("企业ID格式错误");
		}

		Object mobileClaim = claims.get("mobile");
		String memberMobile = mobileClaim == null ? "" : mobileClaim.toString().trim();
		Map<String, Object> data =
				employeePurchaseActivityDataService.buildActivityData(
						companyId, userId, activityId, enterpriseId, memberMobile);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping("/wxapp/employee/invitelist")
	public ResponseEntity<ApiResult<Map<String, Object>>> getInviteList(
			HttpServletRequest request,
			@RequestParam(value = "activity_id", required = false) String activityIdRaw,
			@RequestParam(value = "enterprise_id", required = false) String enterpriseIdRaw,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "pageSize", required = false) String pageSizeRaw) {
		Map<String, Object> claims = h5BearerJwtClaimsService
				.verifyAndExtractClaims(request)
				.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		long companyId = parseLongFlexible(claims.get("company_id"), 1L);
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, null);
		long userId = resolveH5AuthenticatedUserId(request, claims, merged);

		if (activityIdRaw == null || !StringUtils.hasText(activityIdRaw.trim())) {
			throw new BadRequestException("活动ID必填");
		}
		if (enterpriseIdRaw == null || !StringUtils.hasText(enterpriseIdRaw.trim())) {
			throw new BadRequestException("企业ID必填");
		}

		long activityId;
		try {
			activityId = Long.parseLong(activityIdRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("活动ID格式错误");
		}
		long enterpriseId;
		try {
			enterpriseId = Long.parseLong(enterpriseIdRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("企业ID格式错误");
		}

		Map<String, Object> data =
				employeeInviteListService.buildInviteList(
						companyId, userId, activityId, enterpriseId, pageRaw, pageSizeRaw);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping("/wxapp/employee/invitecode")
	public ResponseEntity<ApiResult<Map<String, Object>>> getInviteCode(
			HttpServletRequest request,
			@RequestParam(value = "activity_id", required = false) String activityIdRaw,
			@RequestParam(value = "enterprise_id", required = false) String enterpriseIdRaw) {
		Map<String, Object> claims = h5BearerJwtClaimsService
				.verifyAndExtractClaims(request)
				.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		long companyId = parseLongFlexible(claims.get("company_id"), 1L);
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, null);
		long userId = resolveH5AuthenticatedUserId(request, claims, merged);

		if (activityIdRaw == null || !StringUtils.hasText(activityIdRaw.trim())) {
			throw new ResourceException("活动ID必填");
		}
		if (enterpriseIdRaw == null || !StringUtils.hasText(enterpriseIdRaw.trim())) {
			throw new ResourceException("企业ID必填");
		}

		long activityId;
		try {
			activityId = Long.parseLong(activityIdRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("活动ID格式错误");
		}
		long enterpriseId;
		try {
			enterpriseId = Long.parseLong(enterpriseIdRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("企业ID格式错误");
		}

		String code =
				employeePurchaseInviteCodeService.generateInviteCode(companyId, enterpriseId, activityId, userId);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("invite_code", code);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping("/wxapp/employee/relative/bind")
	public ResponseEntity<ApiResult<Map<String, Object>>> bindRelative(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> claims = h5BearerJwtClaimsService
				.verifyAndExtractClaims(request)
				.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		long companyId = parseLongFlexible(claims.get("company_id"), 1L);
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		long userId = resolveH5AuthenticatedUserId(request, claims, merged);

		Object mob = claims.get("mobile");
		String memberMobile = mob == null ? "0" : mob.toString();

		Object rawInvite = merged.get("invite_code");
		String inviteCode = rawInvite == null ? "" : rawInvite.toString().trim();
		if (!StringUtils.hasText(inviteCode)) {
			throw new ResourceException("邀请码必填");
		}

		employeeRelativeBindService.executeBindWithInviteLock(companyId, userId, memberMobile, inviteCode);

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private long resolveH5AuthenticatedUserId(
			HttpServletRequest request, Map<String, Object> claims, Map<String, Object> mergedInput) {
		long userId = 0L;
		Object claimUid = claims.get("user_id");
		if (claimUid != null && authUserIdTruthy(claimUid)) {
			userId = parseLongFlexible(claimUid, 0L);
		}
		boolean local = this.environment.acceptsProfiles(Profiles.of("local"));
		if (userId <= 0L && local) {
			if (mergedInput != null && mergedInput.containsKey("user_id")) {
				userId = parseLongFlexible(mergedInput.get("user_id"), 0L);
			} else {
				String q = request.getParameter("user_id");
				if (StringUtils.hasText(q)) {
					userId = parseLongFlexible(q, 0L);
				}
			}
		}
		return userId;
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

	private static boolean isAccountDisabled(Object v) {
		if (v == null) {
			return false;
		}
		if (Boolean.TRUE.equals(v)) {
			return true;
		}
		String s = v.toString().trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}

	private static boolean authUserIdTruthy(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Number n) {
			return n.longValue() > 0L;
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			return false;
		}
		try {
			return Long.parseLong(s) > 0L;
		} catch (NumberFormatException e) {
			return true;
		}
	}

	private static long parseLongFlexible(Object raw, long defaultVal) {
		if (raw == null) {
			return defaultVal;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static void replaceMemberMobilePlaceholderFromH5Claims(
			HttpServletRequest request, Map<String, Object> merged) {
		if (!merged.containsKey("mobile")) {
			return;
		}
		Object raw = merged.get("mobile");
		if (raw == null) {
			return;
		}
		String s;
		if (raw instanceof CharSequence) {
			s = raw.toString().trim();
		} else if (raw instanceof Number) {
			s = raw.toString().trim();
		} else {
			s = String.valueOf(raw).trim();
		}
		if (!"member_mobile".equals(s)) {
			return;
		}
		Object claims = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		String replacement;
		if (claims instanceof Map<?, ?> map) {
			replacement =
					Optional.ofNullable(map.get("mobile")).map(Object::toString).orElse("");
		} else {
			replacement = "";
		}
		merged.put("mobile", replacement);
	}

	private static void replaceMemberMobilePlaceholder(Map<String, Object> merged, String memberMobileFromClaims) {
		if (!merged.containsKey("mobile")) {
			return;
		}
		Object raw = merged.get("mobile");
		if (raw == null) {
			return;
		}
		String s;
		if (raw instanceof CharSequence) {
			s = raw.toString().trim();
		} else if (raw instanceof Number) {
			s = raw.toString().trim();
		} else {
			s = String.valueOf(raw).trim();
		}
		if ("member_mobile".equals(s)) {
			merged.put("mobile", memberMobileFromClaims);
		}
	}

	private static long requirePositiveLongField(Object raw, String errMsg) {
		if (raw == null) {
			throw new ResourceException(errMsg);
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			if (v <= 0L) {
				throw new ResourceException(errMsg);
			}
			return v;
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new ResourceException(errMsg);
		}
		try {
			long v = Long.parseLong(s);
			if (v <= 0L) {
				throw new ResourceException(errMsg);
			}
			return v;
		} catch (NumberFormatException e) {
			throw new ResourceException(errMsg);
		}
	}

	private static String requireNonBlankTrimmedForResource(Object raw, String errMsg) {
		if (raw == null) {
			throw new ResourceException(errMsg);
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new ResourceException(errMsg);
		}
		return s;
	}
}
