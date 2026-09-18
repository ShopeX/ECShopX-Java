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

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.AdminLog;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtBlacklistPort;
import cn.shopex.ecshopx.common.auth.OperatorJwtIssuerPort;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.api.admin.v1.dto.OperatorOauthLoginRequest;
import cn.shopex.ecshopx.companys.service.OperatorBasicUserService;
import cn.shopex.ecshopx.companys.service.OperatorGetUserDataService;
import cn.shopex.ecshopx.companys.service.OperatorDistributorSelectionService;
import cn.shopex.ecshopx.companys.service.OperatorLogsQueryService;
import cn.shopex.ecshopx.companys.service.OperatorStatusChangeService;
import cn.shopex.ecshopx.companys.service.OperatorsCommandService;
import cn.shopex.ecshopx.companys.service.OperatorsImageVcodeService;
import cn.shopex.ecshopx.companys.service.OperatorsYdleadsService;
import cn.shopex.ecshopx.companys.service.auth.OperatorAuthService;
import cn.shopex.ecshopx.companys.service.auth.OperatorForgetPasswordSmsSendService;
import cn.shopex.ecshopx.companys.service.auth.OperatorPasswordResetService;
import cn.shopex.ecshopx.companys.service.auth.OperatorShopexOAuthAuthorizeUrlService;
import cn.shopex.ecshopx.companys.service.auth.OperatorTokenRefreshService;
import cn.shopex.ecshopx.companys.service.datapass.OperatorDataPassApplyService;
import cn.shopex.ecshopx.companys.service.datapass.OperatorDataPassApproveService;
import cn.shopex.ecshopx.companys.service.datapass.OperatorDataPassDetailPort;
import cn.shopex.ecshopx.companys.service.datapass.OperatorDataPassListService;
import cn.shopex.ecshopx.companys.service.datapass.OperatorDataPassLogQueryService;
import cn.shopex.ecshopx.companys.config.CommonApiTokenProperties;
import cn.shopex.ecshopx.companys.web.CompanysAdminRequestMerge;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("companysAdminV1Operators")
@RequestMapping("/api/v1")
public class OperatorsController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private static final Pattern RANGE_REGEX = Pattern.compile("^\\d{1,2}:\\d{2}-\\d{1,2}:\\d{2}$");
	private static final Pattern OPERATOR_SELF_PASSWORD_COMPLEXITY =
			Pattern.compile(
					"^(?!^[0-9]+$)(?!^[a-z]+$)(?!^[A-Z]+$)(?!^[^A-z0-9]+$)^[^\\s\\x{4e00}-\\x{9fa5}]{8,}$",
					Pattern.UNICODE_CHARACTER_CLASS);
	private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("uuuu-MM-dd");

	private final OperatorDataPassApplyService operatorDataPassApplyService;
	private final OperatorDataPassApproveService operatorDataPassApproveService;
	private final OperatorDataPassListService operatorDataPassListService;
	private final OperatorDataPassDetailPort operatorDataPassDetailPort;
	private final OperatorsYdleadsService operatorsYdleadsService;
	private final OperatorAuthService operatorAuthService;
	private final OperatorJwtIssuerPort operatorJwtIssuerPort;
	private final OperatorJwtBlacklistPort operatorJwtBlacklistPort;
	private final OperatorPasswordResetService operatorPasswordResetService;
	private final OperatorDistributorSelectionService operatorDistributorSelectionService;
	private final OperatorForgetPasswordSmsSendService operatorForgetPasswordSmsSendService;
	private final OperatorsImageVcodeService operatorsImageVcodeService;
	private final OperatorStatusChangeService operatorStatusChangeService;
	private final OperatorsCommandService operatorsCommandService;
	private final OperatorLogsQueryService operatorLogsQueryService;
	private final OperatorDataPassLogQueryService operatorDataPassLogQueryService;
	private final OperatorShopexOAuthAuthorizeUrlService operatorShopexOAuthAuthorizeUrlService;
	private final OperatorBasicUserService operatorBasicUserService;
	private final OperatorGetUserDataService operatorGetUserDataService;
	private final CommonApiTokenProperties commonApiTokenProperties;
	private final OperatorTokenRefreshService operatorTokenRefreshService;

	public OperatorsController(
			OperatorDataPassApplyService operatorDataPassApplyService,
			OperatorDataPassApproveService operatorDataPassApproveService,
			OperatorDataPassListService operatorDataPassListService,
			OperatorDataPassDetailPort operatorDataPassDetailPort,
			OperatorsYdleadsService operatorsYdleadsService,
			OperatorAuthService operatorAuthService,
			OperatorJwtIssuerPort operatorJwtIssuerPort,
			OperatorJwtBlacklistPort operatorJwtBlacklistPort,
			OperatorPasswordResetService operatorPasswordResetService,
			OperatorDistributorSelectionService operatorDistributorSelectionService,
			OperatorForgetPasswordSmsSendService operatorForgetPasswordSmsSendService,
			OperatorsImageVcodeService operatorsImageVcodeService,
			OperatorStatusChangeService operatorStatusChangeService,
			OperatorsCommandService operatorsCommandService,
			OperatorLogsQueryService operatorLogsQueryService,
			OperatorDataPassLogQueryService operatorDataPassLogQueryService,
			OperatorShopexOAuthAuthorizeUrlService operatorShopexOAuthAuthorizeUrlService,
			OperatorBasicUserService operatorBasicUserService,
			OperatorGetUserDataService operatorGetUserDataService,
			CommonApiTokenProperties commonApiTokenProperties,
			OperatorTokenRefreshService operatorTokenRefreshService) {
		this.operatorDataPassApplyService = operatorDataPassApplyService;
		this.operatorDataPassApproveService = operatorDataPassApproveService;
		this.operatorDataPassListService = operatorDataPassListService;
		this.operatorDataPassDetailPort = operatorDataPassDetailPort;
		this.operatorsYdleadsService = operatorsYdleadsService;
		this.operatorAuthService = operatorAuthService;
		this.operatorJwtIssuerPort = operatorJwtIssuerPort;
		this.operatorJwtBlacklistPort = operatorJwtBlacklistPort;
		this.operatorPasswordResetService = operatorPasswordResetService;
		this.operatorDistributorSelectionService = operatorDistributorSelectionService;
		this.operatorForgetPasswordSmsSendService = operatorForgetPasswordSmsSendService;
		this.operatorsImageVcodeService = operatorsImageVcodeService;
		this.operatorStatusChangeService = operatorStatusChangeService;
		this.operatorsCommandService = operatorsCommandService;
		this.operatorLogsQueryService = operatorLogsQueryService;
		this.operatorDataPassLogQueryService = operatorDataPassLogQueryService;
		this.operatorShopexOAuthAuthorizeUrlService = operatorShopexOAuthAuthorizeUrlService;
		this.operatorBasicUserService = operatorBasicUserService;
		this.operatorGetUserDataService = operatorGetUserDataService;
		this.commonApiTokenProperties = commonApiTokenProperties;
		this.operatorTokenRefreshService = operatorTokenRefreshService;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@GetMapping(value = "/operator/credential", name = "登陆获取证书")
	public ResponseEntity<ApiResult<Map<String, Object>>> getCredentials(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> input =
				CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body == null ? Map.of() : body);
		String apiTokenParam = stringVal(input.get("token"));
		String configured = commonApiTokenProperties.getApiToken();
		if (!StringUtils.hasText(configured)
				|| !StringUtils.hasText(apiTokenParam)
				|| !Objects.equals(apiTokenParam, configured)) {
			throw new BadRequestException("无权访问该API,签名错误");
		}
		Map<String, Object> newOperator = operatorAuthService.retrieveByCredentials(input);
		Map<String, Object> data = new LinkedHashMap<>(newOperator);
		data.remove("merchant_id");
		data.remove("shop_ids");
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@GetMapping(value = "/operator/basic", name = "获取账号基本信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> getBasicUserById(
			@RequestParam(value = "id", required = false) String id,
			@RequestParam(value = "token", required = false) String token) {
		return ResponseEntity.ok(ApiResult.ok(operatorBasicUserService.getBasicUserById(id, token)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@GetMapping(value = "/operator/images/code", name = "获取图片验证码")
	public ResponseEntity<ApiResult<Map<String, String>>> getImageVcode(
			@RequestParam(value = "type", required = false) String type) {
		Map<String, String> data = operatorsImageVcodeService.generateImageVcode(type);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@PostMapping(value = "/operator/sms/code", name = "获取手机短信验证码")
	public ResponseEntity<ApiResult<Map<String, Object>>> getSmsCode(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> input = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body == null ? Map.of() : body);
		String mobile = stringVal(input.get("mobile"));
		String token = stringVal(input.get("token"));
		String yzm = stringVal(input.get("yzm"));
		operatorForgetPasswordSmsSendService.sendForgetSmsCode(mobile, token, yzm);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@PostMapping(value = "/operator/resetpassword", name = "重置密码")
	public ResponseEntity<ApiResult<Map<String, Object>>> resetPassword(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> input = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		String account = stringVal(input.get("account"));
		String code = stringVal(input.get("code"));
		String newpassword = stringVal(input.get("newpassword"));
		operatorPasswordResetService.resetPassword(account, code, newpassword);
		return ResponseEntity.ok(ApiResult.ok(Map.of("message", "密码修改成功，请重新登录")));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@GetMapping(value = "/operator/app/image/code", name = "App图片验证码")
	public ResponseEntity<ApiResult<Map<String, String>>> getAppImageVcode(
			@RequestParam(value = "type", required = false) String type) {
		Map<String, String> data = operatorsImageVcodeService.generateAppImageVcode(type);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@ShopLog
	@AdminAuth
	@Activated(routeAlias = "company.get.operatorlogs")
	@GetMapping(value = "/company/operatorlogs", name = "操作日志")
	public ResponseEntity<ApiResult<Map<String, Object>>> getCompanysLogs(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readPositiveLong(jwt, "company_id", "无权访问该API,非法访问！");
		String operatorType = jwt.get("operator_type") == null ? "" : jwt.get("operator_type").toString();
		long merchantRaw = readMerchantIdOrZero(jwt);
		Long jwtMerchantId = merchantRaw == 0L ? null : merchantRaw;
		int p = (page == null || page < 1) ? 1 : page;
		int ps = (pageSize == null || pageSize < 1) ? 20 : pageSize;
		boolean listFilterHasNonemptyMerchantId = "merchant".equals(operatorType) && jwtMerchantId != null;
		Map<String, Object> data =
				operatorLogsQueryService.getCompanysLogs(
						companyId, jwtMerchantId, operatorType, p, ps, listFilterHasNonemptyMerchantId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@ShopLog
	@AdminAuth
	@Activated(routeAlias = "operator.update.data")
	@PutMapping(value = "/operator/updatedata", name = "更改用户名和头像")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateUserData(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> input =
				CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body == null ? Map.of() : body);
		String pwd = stringVal(input.get("pwd"));
		String repwd = stringVal(input.get("repwd"));
		Object logintype = input.get("logintype");

		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readPositiveLong(jwt, "company_id", "无权访问该API,非法访问！");
		long operatorId = readPositiveLong(jwt, "operator_id", "无权访问该API,非法访问！");

		boolean pwdTruthy = pwd != null;
		boolean skipPwd = "admin".equals(logintype == null ? null : logintype.toString());
		String plainPasswordForService = null;
		if (pwdTruthy && !skipPwd) {
			if (!OPERATOR_SELF_PASSWORD_COMPLEXITY.matcher(pwd).matches()) {
				throw new BadRequestException("密码至少8位以上，至少由数字、字母或特殊字符中两种及以上方式组成");
			}
			if (repwd == null || !pwd.equals(repwd)) {
				throw new BadRequestException("密码不一致");
			}
			plainPasswordForService = pwd;
		}

		LinkedHashMap<String, Object> operatorUpdatePayload = new LinkedHashMap<>();
		operatorUpdatePayload.put("username", input.get("username"));
		operatorUpdatePayload.put("head_portrait", input.get("head_portrait"));
		if (plainPasswordForService != null) {
			operatorUpdatePayload.put("password", plainPasswordForService);
		}

		Map<String, Object> status =
				operatorsCommandService.updateOperator(operatorId, companyId, operatorUpdatePayload);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", status)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@ShopLog
	@AdminAuth
	@Activated(routeAlias = "operator.select.distributor")
	@PostMapping(value = "/operator/select/distributor", name = "店铺端选择店铺")
	public ResponseEntity<ApiResult<Map<String, Object>>> shopLoginSelectShopId(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> input = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		Object setDistributorIdRaw = input.get("set_distributor_id");

		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readPositiveLong(jwt, "company_id", "无权访问该API,非法访问！");
		int operatorId = Math.toIntExact(readPositiveLong(jwt, "operator_id", "无权访问该API,非法访问！"));

		operatorDistributorSelectionService.persistSelection(operatorId, companyId, setDistributorIdRaw);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@ShopLog
	@AdminAuth
	@Activated(routeAlias = "operator.status.change")
	@PutMapping(value = "/operator/changestatus", name = "修改账号状态")
	public ResponseEntity<ApiResult<Map<String, Object>>> changeOperatorStatus(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> input =
				CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body == null ? Map.of() : body);
		int isDisableInt = parseRequiredIsDisableInt(input.get("is_disable"));
		long targetOperatorId = parseRequiredOperatorId(input.get("operator_id"));

		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readPositiveLong(jwt, "company_id", "无权访问该API,非法访问！");
		long currentOperatorId = readPositiveLong(jwt, "operator_id", "无权访问该API,非法访问！");
		if (currentOperatorId == targetOperatorId) {
			throw new ResourceException("不能操作本人禁用状态");
		}

		Map<String, Object> status =
				operatorStatusChangeService.changeOperatorStatus(
						companyId, currentOperatorId, targetOperatorId, isDisableInt);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", status)));
	}

	@ShopLog
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@AdminAuth
	@Activated(routeAlias = "companys.ydleads.create")
	@PostMapping(value = "/ydleads/create", name = "云店留资创建")
	public ResponseEntity<ApiResult<Map<String, Object>>> createYdleads(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> input = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		String goodsName = stringVal(input.get("goods_name"));
		if (goodsName == null) {
			throw new BadRequestException("订购套餐必填");
		}
		String callName = stringVal(input.get("call_name"));
		if (callName == null) {
			throw new BadRequestException("称呼必填");
		}
		String sex = stringVal(input.get("sex"));
		if (sex == null) {
			throw new BadRequestException("性别必填");
		}
		String mobile = stringVal(input.get("mobile"));
		if (mobile == null) {
			throw new BadRequestException("手机号码必填");
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readPositiveLong(jwt, "company_id", "无权访问该API,非法访问！");

		operatorsYdleadsService.createYdleadsData(
				companyId,
				Map.of(
						"goods_name", goodsName,
						"call_name", callName,
						"sex", sex,
						"mobile", mobile));
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@AdminLog
	@PostMapping(value = "/operator/oauth/login", name = "operator oauth登录")
	public ResponseEntity<ApiResult<Map<String, String>>> login(@FlexibleBody OperatorOauthLoginRequest body) {
		OperatorOauthLoginRequest b = body != null ? body : new OperatorOauthLoginRequest();
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		if (StringUtils.hasText(b.getCode())) {
			params.put("code", b.getCode());
		}
		if (StringUtils.hasText(b.getLogintype())) {
			params.put("logintype", b.getLogintype());
		}
		if (StringUtils.hasText(b.getProductModel())) {
			params.put("product_model", b.getProductModel());
		}
		Map<String, Object> newOperator = operatorAuthService.retrieveByCredentials(params);
		String jwt = operatorJwtIssuerPort.issueToken(newOperator);
		return ResponseEntity.ok(ApiResult.ok(Map.of("token", jwt)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@AdminLog
	@PostMapping(value = "/operator/shuyun/login", name = "数云登录")
	public ResponseEntity<ApiResult<Map<String, String>>> shuyunLogin(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> input =
				CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body == null ? Map.of() : body);
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("logintype", "shuyunadmin");

		if (input.containsKey("code")) {
			params.put("code", input.get("code"));
		}
		params.put("token", input.get("token"));
		params.put("yzm", input.get("yzm"));

		Map<String, Object> newOperator = operatorAuthService.retrieveByCredentials(params);
		String jwt = operatorJwtIssuerPort.issueToken(newOperator);
		if (jwt == null || jwt.isBlank()) {
			throw new UnauthorizedException("登录失败");
		}
		return ResponseEntity.ok(ApiResult.ok(Map.of("token", jwt)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@GetMapping(value = "/token/refresh", name = "刷新token")
	public ResponseEntity<ApiResult<Map<String, Boolean>>> tokenRefresh(HttpServletRequest request) {
		String newJwt = operatorTokenRefreshService.tokenRefresh(request);
		return ResponseEntity.ok()
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + newJwt)
				.body(ApiResult.ok(Map.of("result", Boolean.TRUE)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@GetMapping(value = "/token/invalidate", name = "作废token")
	public ResponseEntity<ApiResult<Map<String, Boolean>>> tokenInvalidate(HttpServletRequest request) {
		String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (auth == null || !auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		String compactJwt = auth.substring(7).trim();
		if (compactJwt.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> userData = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (userData == null || userData.isEmpty()) {
			throw new UnauthorizedException("登录验证错误");
		}
		Object expObj = userData.get("exp");
		long expirationEpochSeconds;
		if (expObj instanceof Date d) {
			expirationEpochSeconds = d.getTime() / 1000L;
		} else if (expObj instanceof Number n) {
			expirationEpochSeconds = n.longValue();
		} else {
			throw new UnauthorizedException("登录验证错误");
		}
		operatorJwtBlacklistPort.addToBlacklist(compactJwt, expirationEpochSeconds);
		return ResponseEntity.ok(ApiResult.ok(Map.of("result", Boolean.TRUE)));
	}

	@ShopLog
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@AdminAuth
	@Activated(routeAlias = "companys.datapass.apply")
	@PostMapping(value = "/datapass", name = "申请查看敏感数据")
	public ResponseEntity<ApiResult<Map<String, Object>>> applyDataPass(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> b = body == null ? Map.of() : body;
		String startRaw = stringVal(b.get("start_time"));
		String endRaw = stringVal(b.get("end_time"));
		if (startRaw == null || startRaw.isEmpty() || endRaw == null || endRaw.isEmpty()) {
			throw new BadRequestException("请输入日期");
		}
		ZoneId zone = ZoneId.systemDefault();
		int startSec;
		int endSec;
		try {
			startSec = Math.toIntExact(LocalDate.parse(startRaw, YMD).atStartOfDay(zone).toEpochSecond());
			endSec = Math.toIntExact(LocalDate.parse(endRaw, YMD).atStartOfDay(zone).toEpochSecond());
		} catch (DateTimeParseException | ArithmeticException e) {
			throw new BadRequestException("请输入日期");
		}

		Object dateTypeObj = b.get("date_type");
		if (dateTypeObj == null || (dateTypeObj instanceof String s && s.isEmpty())) {
			throw new BadRequestException("日期类型必填");
		}
		int dateType;
		try {
			dateType = Integer.parseInt(dateTypeObj.toString().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("日期类型必填");
		}
		if (dateType < 0 || dateType > 1) {
			throw new BadRequestException("日期类型必填");
		}

		String rangeRaw = stringVal(b.get("range"));
		String normalizedRange = "";
		if (rangeRaw != null) {
			if (!RANGE_REGEX.matcher(rangeRaw).matches()) {
				throw new BadRequestException("时间范围错误");
			}
			normalizedRange = normalizeRangeSegments(rangeRaw);
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		Object typeObj = jwt.get("operator_type");
		if (typeObj != null && "admin".equalsIgnoreCase(typeObj.toString().trim())) {
			throw new ResourceException("账号错误");
		}

		String reasonStr = b.get("reason") == null ? "" : b.get("reason").toString();

		long companyId = readPositiveLong(jwt, "company_id", "无权访问该API,非法访问！");
		int operatorId = Math.toIntExact(readPositiveLong(jwt, "operator_id", "无权访问该API,非法访问！"));

		String conflict =
				operatorDataPassApplyService.apply(companyId, operatorId, startSec, endSec, dateType, normalizedRange, reasonStr);
		if (conflict == null) {
			return ResponseEntity.ok(ApiResult.ok(Map.of("status", true, "message", "")));
		}
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", false, "message", conflict)));
	}

	private static String stringVal(Object o) {
		if (o == null) {
			return null;
		}
		String s = o.toString().trim();
		return s.isEmpty() ? null : s;
	}

	private static String normalizeRangeSegments(String rangeRaw) {
		String[] rs = rangeRaw.split("-", 2);
		if (rs.length != 2) {
			throw new BadRequestException("时间范围错误");
		}
		String[] out = new String[2];
		for (int k = 0; k < 2; k++) {
			String r = rs[k];
			String[] hi = r.split(":", 2);
			if (hi.length != 2) {
				throw new BadRequestException("时间范围格式错误");
			}
			int h;
			int minute;
			try {
				h = Integer.parseInt(hi[0].trim());
				minute = Integer.parseInt(hi[1].trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("时间范围格式错误");
			}
			if (h < 0 || h > 24 || minute < 0 || minute >= 60) {
				throw new BadRequestException("时间范围格式错误");
			}
			out[k] = String.format("%02d", h) + ":" + minute;
		}
		return out[0] + "-" + out[1];
	}

	private static long readPositiveLong(Map<String, Object> jwt, String key, String unauthorizedMsg) {
		Object v = jwt.get(key);
		if (v == null) {
			throw new UnauthorizedException(unauthorizedMsg);
		}
		try {
			long id = v instanceof Number n ? n.longValue() : Long.parseLong(v.toString().trim());
			if (id <= 0L) {
				throw new UnauthorizedException(unauthorizedMsg);
			}
			return id;
		} catch (NumberFormatException e) {
			throw new UnauthorizedException(unauthorizedMsg);
		}
	}

	private static long readMerchantIdOrZero(Map<String, Object> jwt) {
		Object v = jwt.get("merchant_id");
		if (v == null) {
			return 0L;
		}
		try {
			return v instanceof Number n ? n.longValue() : Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int parseRequiredIsDisableInt(Object raw) {
		if (raw == null) {
			throw new BadRequestException("状态不能为空");
		}
		if (raw instanceof Number n) {
			long lv = n.longValue();
			if (lv == 0L || lv == 1L) {
				return (int) lv;
			}
			throw new BadRequestException("状态不能为空");
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				throw new BadRequestException("状态不能为空");
			}
			if ("0".equals(t)) {
				return 0;
			}
			if ("1".equals(t)) {
				return 1;
			}
			throw new BadRequestException("状态不能为空");
		}
		throw new BadRequestException("状态不能为空");
	}

	private static long parseRequiredOperatorId(Object raw) {
		if (raw == null) {
			throw new BadRequestException("账号id不能为空");
		}
		try {
			long v = raw instanceof Number n ? n.longValue() : Long.parseLong(raw.toString().trim());
			if (v < 1L) {
				throw new BadRequestException("账号id不能为空");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("账号id不能为空");
		}
	}

	@ShopLog
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@AdminAuth
	@Activated(routeAlias = "companys.datapass.approve")
	@PutMapping(value = { "/datapass/apply/{id}", "/datapass/open/{id}", "/datapass/close/{id}" }, name = "敏感数据申请审核")
	public ResponseEntity<ApiResult<Map<String, Object>>> approveDataPass(
			HttpServletRequest request,
			@PathVariable("id") String id,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> input =
				CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body == null ? Map.of() : body);
		String status = stringVal(input.get("status"));
		String remarks = stringVal(input.get("remarks"));
		String isClosed =
				input.containsKey("is_closed") && input.get("is_closed") != null
						? input.get("is_closed").toString()
						: null;

		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readPositiveLong(jwt, "company_id", "无权访问该API,非法访问！");

		long passId;
		try {
			passId = Long.parseLong(id.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("数据不存在");
		}

		operatorDataPassApproveService.approveDataPass(companyId, passId, status, remarks, isClosed);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@ShopLog
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@AdminAuth
	@Activated(routeAlias = "companys.datapass.list")
	@GetMapping(value = "/datapass", name = "敏感数据申请列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> listDataPass(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "params", required = false) String params,
			@RequestParam(value = "login_name", required = false) String loginName,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "start_time", required = false) Integer startTime,
			@RequestParam(value = "end_time", required = false) Integer endTime) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readPositiveLong(jwt, "company_id", "无权访问该API,非法访问！");
		long operatorId = readPositiveLong(jwt, "operator_id", "无权访问该API,非法访问！");
		long merchantId = readMerchantIdOrZero(jwt);
		Object typeObj = jwt.get("operator_type");
		String operatorType = typeObj == null ? "" : typeObj.toString();

		Map<String, Object> data = operatorDataPassListService.listDataPass(
				companyId,
				merchantId,
				operatorId,
				operatorType,
				page,
				pageSize,
				params,
				loginName,
				status,
				startTime,
				endTime);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@ShopLog
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@AdminAuth
	@Activated(routeAlias = "companys.datapass.detail")
	@GetMapping(value = "/datapass/{id}", name = "敏感数据申请详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> fetchDataPassDetail(
			HttpServletRequest request, @PathVariable("id") String id) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		String callerOperatorType = jwt.get("operator_type") == null ? "" : jwt.get("operator_type").toString().trim();

		long passId;
		try {
			passId = Long.parseLong(id.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("数据不存在");
		}

		Map<String, Object> data = operatorDataPassDetailPort.fetchDataPassDetail(passId, callerOperatorType);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@ShopLog
	@AdminAuth
	@Activated(routeAlias = "companys.datapass.log.list")
	@GetMapping(value = "/datapasslog", produces = MediaType.APPLICATION_JSON_VALUE, name = "敏感数据日志")
	public ApiResult<Map<String, Object>> listDataPassLog(
			HttpServletRequest request,
			@RequestParam(value = "operator_id", required = false) String operatorIdRaw,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readPositiveLong(jwt, "company_id", "无权访问该API,非法访问！");

		if (operatorIdRaw == null || !StringUtils.hasText(operatorIdRaw.trim())) {
			throw new BadRequestException("操作员id必填");
		}
		int operatorIdInt;
		try {
			operatorIdInt = Integer.parseInt(operatorIdRaw.trim());
			if (operatorIdInt <= 0) {
				throw new BadRequestException("操作员id必填");
			}
		} catch (NumberFormatException e) {
			throw new BadRequestException("操作员id必填");
		}

		int p = page != null && page > 0 ? page : 1;
		int ps = pageSize != null && pageSize > 0 ? pageSize : 1000;

		Map<String, Object> data =
				operatorDataPassLogQueryService.listDataPassLog(companyId, operatorIdInt, p, ps);
		return ApiResult.ok(data);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@ShopLog
	@AdminAuth
	@GetMapping(value = "/operator/getinfo", produces = MediaType.APPLICATION_JSON_VALUE, name = "获取操作员信息")
	public ApiResult<Map<String, Object>> getUserData(
			HttpServletRequest request,
			@RequestParam(value = "operator_id", required = false) String ignoredOperatorId,
			@RequestParam(value = "is_app", required = false) String isAppRaw) {
		@SuppressWarnings("unchecked")
		Map<String, Object> jwt = (Map<String, Object>) request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (jwt == null || jwt.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> data = operatorGetUserDataService.getUserData(request, isAppRaw);
		return ApiResult.ok(data);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@GetMapping(value = "/operator/authorizeurl", name = "获取oauth登录链接")
	public ResponseEntity<ApiResult<Map<String, String>>> getOuthorizeurl() {
		String url = operatorShopexOAuthAuthorizeUrlService.buildAuthorizeUrl();
		return ResponseEntity.ok(ApiResult.ok(Map.of("url", url)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@GetMapping(value = "/operator/oauth/logout", name = "获取oauth登出链接")
	public ResponseEntity<ApiResult<Map<String, String>>> getOauthLogouturl() {
		String url = operatorShopexOAuthAuthorizeUrlService.buildOauthLogoutUrl();
		return ResponseEntity.ok(ApiResult.ok(Map.of("url", url)));
	}
}
