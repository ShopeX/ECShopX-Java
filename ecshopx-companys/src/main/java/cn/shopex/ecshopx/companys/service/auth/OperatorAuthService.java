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

package cn.shopex.ecshopx.companys.service.auth;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.companys.domain.DistributorWechatRel;
import cn.shopex.ecshopx.companys.domain.DistributorWorkWechatRel;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.DistributorWechatRelMapper;
import cn.shopex.ecshopx.companys.mapper.DistributorWorkWechatRelMapper;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.companys.service.DistributorWechatMobileBindService;
import cn.shopex.ecshopx.companys.service.DistributorWorkWechatMobileBindService;
import cn.shopex.ecshopx.companys.service.DistributorWechatRebindRedisService;
import cn.shopex.ecshopx.companys.service.EmployeeService;
import cn.shopex.ecshopx.companys.service.OperatorSmsVerifyService;
import cn.shopex.ecshopx.companys.service.OperatorsImageVcodeService;
import cn.shopex.ecshopx.companys.service.OperatorsOpenService;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.companys.service.auth.wechatoauth.WechatOauthLoginOutcome;
import cn.shopex.ecshopx.companys.service.auth.wechatoauth.WechatOauthLoginSuccess;
import cn.shopex.ecshopx.companys.service.auth.wechatoauth.WechatOauthLoginUnbound;
import cn.shopex.ecshopx.companys.service.auth.workwechatoauth.WorkWechatOauthLoginOutcome;
import cn.shopex.ecshopx.companys.service.auth.workwechatoauth.WorkWechatOauthLoginSuccess;
import cn.shopex.ecshopx.companys.service.auth.workwechatoauth.WorkWechatOauthLoginUnbound;
import cn.shopex.ecshopx.companys.service.shopex.ShopexOauthCertSyncService;
import cn.shopex.ecshopx.companys.service.shopex.ShopexSmsClientFacade;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.shopex.ecshopx.merchant.domain.Merchant;
import cn.shopex.ecshopx.merchant.service.MerchantQueryService;
import cn.shopex.ecshopx.shuyun.service.ShuyunOperatorsFacade;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import cn.shopex.ecshopx.thirdparty.service.prism.PrismOAuthService;
import cn.shopex.ecshopx.thirdparty.service.prism.PrismOAuthTokenResult;
import cn.shopex.ecshopx.wechat.service.OfficialAccountOAuthFacade;
import cn.shopex.ecshopx.wechat.service.OpenPlatformWoaFacade;
import cn.shopex.ecshopx.workwechat.service.DistributorWorkWechatFacade;
import cn.shopex.ecshopx.workwechat.service.WorkWechatCorpUserApiService;
import cn.shopex.ecshopx.workwechat.service.WorkWechatWebOAuthService;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

@Service
public class OperatorAuthService {

	private static final Pattern MOBILE_LOCAL_ADMIN = Pattern.compile("^1[23456789]\\d{9}$");

	private static final String WOA_MODE_AUTHORIZED = "authorized";
	private static final String WOA_MODE_DIRECT = "direct";

	private final SecureRandom secureRandom = new SecureRandom();
	private static final Pattern MOBILE_MERCHANT = Pattern.compile("^1[3456789]\\d{9}$");

	@Value("${ADMIN_LOGIN_CHECK_LEVEL:}")
	private String adminLoginCheckLevel;

	@Value("${common.product-model:platform}")
	private String productModel;

	@Value("${common.system-open-online:false}")
	private boolean systemOpenOnline;

	@Value("${common.system-is-saas:false}")
	private boolean systemIsSaas;

	private final OperatorsImageVcodeService operatorsImageVcodeService;
	private final OperatorsQueryService operatorsQueryService;
	private final CompanysActivationService companysActivationService;
	private final EmployeeService employeeService;
	private final ShopMenuService shopMenuService;
	private final MerchantQueryService merchantQueryService;
	private final PrismOAuthService prismOAuthService;
	private final OperatorsOpenService operatorsOpenService;
	private final ShopexSmsClientFacade shopexSmsClientFacade;
	private final ShopexOauthCertSyncService shopexOauthCertSyncService;
	private final ShuyunOperatorsFacade shuyunOperatorsFacade;
	private final DistributorWorkWechatFacade distributorWorkWechatFacade;
	private final DistributorWorkWechatRelMapper distributorWorkWechatRelMapper;
	private final WorkWechatWebOAuthService workWechatWebOAuthService;
	private final WorkWechatCorpUserApiService workWechatCorpUserApiService;
	private final OpenPlatformWoaFacade openPlatformWoaFacade;
	private final OfficialAccountOAuthFacade officialAccountOAuthFacade;
	private final StringRedisTemplate stringRedisTemplate;
	private final OperatorsMapper operatorsMapper;
	private final DistributorWechatRelMapper distributorWechatRelMapper;
	private final OperatorSmsVerifyService operatorSmsVerifyService;
	private final DistributorWechatRebindRedisService distributorWechatRebindRedisService;
	private final DistributorWechatMobileBindService distributorWechatMobileBindService;
	private final DistributorWorkWechatMobileBindService distributorWorkWechatMobileBindService;

	public OperatorAuthService(
			OperatorsImageVcodeService operatorsImageVcodeService,
			OperatorsQueryService operatorsQueryService,
			CompanysActivationService companysActivationService,
			EmployeeService employeeService,
			ShopMenuService shopMenuService,
			MerchantQueryService merchantQueryService,
			PrismOAuthService prismOAuthService,
			OperatorsOpenService operatorsOpenService,
			ShopexSmsClientFacade shopexSmsClientFacade,
			ShopexOauthCertSyncService shopexOauthCertSyncService,
			ShuyunOperatorsFacade shuyunOperatorsFacade,
			DistributorWorkWechatFacade distributorWorkWechatFacade,
			DistributorWorkWechatRelMapper distributorWorkWechatRelMapper,
			WorkWechatWebOAuthService workWechatWebOAuthService,
			WorkWechatCorpUserApiService workWechatCorpUserApiService,
			OpenPlatformWoaFacade openPlatformWoaFacade,
			OfficialAccountOAuthFacade officialAccountOAuthFacade,
			@Qualifier("companysRedisTemplate") StringRedisTemplate stringRedisTemplate,
			OperatorsMapper operatorsMapper,
			DistributorWechatRelMapper distributorWechatRelMapper,
			OperatorSmsVerifyService operatorSmsVerifyService,
			DistributorWechatRebindRedisService distributorWechatRebindRedisService,
			DistributorWechatMobileBindService distributorWechatMobileBindService,
			DistributorWorkWechatMobileBindService distributorWorkWechatMobileBindService) {
		this.operatorsImageVcodeService = operatorsImageVcodeService;
		this.operatorsQueryService = operatorsQueryService;
		this.companysActivationService = companysActivationService;
		this.employeeService = employeeService;
		this.shopMenuService = shopMenuService;
		this.merchantQueryService = merchantQueryService;
		this.prismOAuthService = prismOAuthService;
		this.operatorsOpenService = operatorsOpenService;
		this.shopexSmsClientFacade = shopexSmsClientFacade;
		this.shopexOauthCertSyncService = shopexOauthCertSyncService;
		this.shuyunOperatorsFacade = shuyunOperatorsFacade;
		this.distributorWorkWechatFacade = distributorWorkWechatFacade;
		this.distributorWorkWechatRelMapper = distributorWorkWechatRelMapper;
		this.workWechatWebOAuthService = workWechatWebOAuthService;
		this.workWechatCorpUserApiService = workWechatCorpUserApiService;
		this.openPlatformWoaFacade = openPlatformWoaFacade;
		this.officialAccountOAuthFacade = officialAccountOAuthFacade;
		this.stringRedisTemplate = stringRedisTemplate;
		this.operatorsMapper = operatorsMapper;
		this.distributorWechatRelMapper = distributorWechatRelMapper;
		this.operatorSmsVerifyService = operatorSmsVerifyService;
		this.distributorWechatRebindRedisService = distributorWechatRebindRedisService;
		this.distributorWechatMobileBindService = distributorWechatMobileBindService;
		this.distributorWorkWechatMobileBindService = distributorWorkWechatMobileBindService;
	}

	public String getLoginCheckLevel() {
		return adminLoginCheckLevel == null ? "" : adminLoginCheckLevel;
	}

	public WorkWechatOauthLoginOutcome workWeChatOauthLogin(Map<String, Object> params) {
		checkImgCode(params);
		String companyIdStr = stringParam(params.get("company_id")).trim();
		long companyId;
		try {
			companyId = Long.parseLong(companyIdStr);
		} catch (NumberFormatException e) {
			throw new BadRequestException("缺少参数，登录失败");
		}
		if (companyId <= 0L) {
			throw new BadRequestException("缺少参数，登录失败");
		}
		Map<String, Object> config = distributorWorkWechatFacade.getWorkConfig(companyId, "dianwu");
		String workUserid =
				workWechatWebOAuthService.resolveWorkUserIdFromCode(
						config, stringParam(params.get("code")).trim());
		workWechatCorpUserApiService.getUserForOperatorWorkWechatOauth(companyId, workUserid);
		LambdaQueryWrapper<DistributorWorkWechatRel> relQ = new LambdaQueryWrapper<>();
		relQ.eq(DistributorWorkWechatRel::getCompanyId, companyId)
				.eq(DistributorWorkWechatRel::getWorkUserid, workUserid)
				.last("LIMIT 1");
		DistributorWorkWechatRel rel = distributorWorkWechatRelMapper.selectOne(relQ);
		if (rel != null && rel.getOperatorId() != null && rel.getOperatorId() > 0L) {
			Map<String, Object> opFilter = new HashMap<>();
			opFilter.put("company_id", companyId);
			opFilter.put("operator_id", rel.getOperatorId());
			Map<String, Object> operator = operatorsQueryService.getInfo(opFilter);
			if (operator != null && !operator.isEmpty()) {
				operator.put("menu_type", getCompanyMenuType(operator.get("company_id")));
				operator.put("logintype", "oauthworkwechat");
				Map<String, Object> jwtClaims = companysActivationService.getLoginToken(operator);
				jwtClaims.put(
						"menu_type",
						operator.get("menu_type") != null ? operator.get("menu_type") : productModel);
				return new WorkWechatOauthLoginSuccess(jwtClaims);
			}
		}
		String code = stringParam(params.get("code")).trim();
		String checkToken =
				DigestUtils.md5DigestAsHex((code + randomAlphanumeric(5)).getBytes(StandardCharsets.UTF_8));
		distributorWorkWechatFacade.setReBindMobileEncrypt(companyId, workUserid, checkToken);
		return new WorkWechatOauthLoginUnbound(String.valueOf(companyId), workUserid, checkToken);
	}

	public WechatOauthLoginOutcome wechatOauthLogin(Map<String, Object> params) {
		checkImgCode(params);
		WechatBindSnapshot snap = resolveWechatOauthBinding(params);
		if (snap instanceof WechatBindSnapshot.Unbound u) {
			String code = stringParam(params.get("code")).trim();
			String randomFive = randomAlphanumeric(5);
			String checkToken =
					DigestUtils.md5DigestAsHex((code + randomFive).getBytes(StandardCharsets.UTF_8));
			distributorWechatRebindRedisService.setCheckToken(
					u.companyId(), u.appId(), u.openid(), u.unionid(), checkToken);
			return new WechatOauthLoginUnbound(
					u.companyId(), u.appId(), "wx", u.openid(), u.unionid(), checkToken);
		}
		if (snap instanceof WechatBindSnapshot.Bound b) {
			Map<String, Object> jwtClaims = companysActivationService.getLoginToken(b.operatorMap());
			Object menuType = b.operatorMap().get("menu_type");
			jwtClaims.put("menu_type", menuType != null ? menuType : productModel);
			return new WechatOauthLoginSuccess(jwtClaims);
		}
		throw new IllegalStateException("wechat oauth binding");
	}

	public Map<String, Object> retrieveByCredentials(Map<String, Object> params) {
		checkImgCode(params);
		String logintype =
				params.get("logintype") != null && !params.get("logintype").toString().isEmpty()
						? params.get("logintype").toString()
						: "localadmin";
		Map<String, Object> operator;
		switch (logintype) {
			case "localadmin":
				operator = credentialsByLocalAdminAccount(params);
				break;
			case "dealer":
			case "staff":
			case "distributor":
			case "supplier":
				operator = credentialsByStaffAccount(params);
				break;
			case "oauthadmin":
				operator = credentialsByShopexOauthCode(params);
				break;
			case "shuyunadmin":
				operator = credentialsByShuyunCode(params);
				if (operator.get("logintype") != null) {
					logintype = operator.get("logintype").toString();
				}
				break;
			case "oauthworkwechat":
				operator = credentialsByWorkWechatOauth(params);
				break;
			case "workwechatbind":
				operator = credentialsByBindWorkWechat(params);
				break;
			case "oauthwechat":
				operator = loadBoundOperatorForWechatOauth(params);
				break;
			case "wechatbinddistributor":
				operator = credentialsByBindWechatDistributor(params);
				break;
			case "wechatbinddistributorbyusername":
				operator = credentialsByBindWechatDistributorByUsername(params);
				break;
			case "wechatbinddistributorbylite":
				operator = credentialsByBindWechatDistributorByLite(params);
				break;
			case "merchant":
				operator = credentialsByMerchantAccount(params);
				break;
			case "admin":
			default:
				operator = credentialsByLocalAdminAccount(params);
				break;
		}
		operator.put("logintype", logintype);
		Map<String, Object> newOperator = companysActivationService.getLoginToken(operator);
		Object menuType = operator.get("menu_type");
		newOperator.put("menu_type", menuType != null ? menuType : productModel);
		return newOperator;
	}

	private void checkImgCode(Map<String, Object> params) {
		if (!"img_code".equals(adminLoginCheckLevel)) {
			return;
		}
		String token = params.get("token") != null ? params.get("token").toString() : "";
		String yzm = params.get("yzm") != null ? params.get("yzm").toString() : "";
		if (token.isBlank()) {
			throw new BadRequestException("请输入token");
		}
		if (yzm.isBlank()) {
			throw new BadRequestException("请输入vcode");
		}
		if (!operatorsImageVcodeService.checkImageVcode(token, yzm, "login")) {
			throw new ResourceException("图形验证码错误");
		}
	}

	private Map<String, Object> credentialsByLocalAdminAccount(Map<String, Object> params) {
		String username = params.get("username") != null ? params.get("username").toString() : "";
		String password = params.get("password") != null ? params.get("password").toString() : "";
		String redisKey = "admin_login_failed_times:" + username;
		bumpFailedOrThrow(redisKey);
		if ("admin".equals(username)) {
			Map<String, Object> filter = new HashMap<>();
			filter.put("login_name", "admin");
			filter.put("operator_type", "admin");
			Map<String, Object> operator = operatorsQueryService.getInfo(filter);
			if (operator == null || operator.isEmpty()) {
				throw new ResourceException("账号不存在", 403);
			}
			String hash = operator.get("password") != null ? operator.get("password").toString() : "";
			if (!BCrypt.checkpw(password, hash)) {
				throw new ResourceException("账号密码错误，请重新登录", 403);
			}
			stringRedisTemplate.delete(redisKey);
			Object companyId = operator.get("company_id");
			String menuTypeStr = getCompanyMenuType(companyId);
			operator.put("menu_type", menuTypeStr);
			return operator;
		}
		Map<String, Object> filter = new HashMap<>();
		filter.put("operator_type", "admin");
		if (MOBILE_LOCAL_ADMIN.matcher(username).matches()) {
			filter.put("mobile", username);
		} else {
			filter.put("login_name", username);
		}
		Map<String, Object> operator;
		try {
			operator = operatorsQueryService.getInfo(filter);
		} catch (Exception e) {
			throw new ResourceException("账号不存在", 403);
		}
		if (operator == null || operator.isEmpty()) {
			throw new ResourceException("账号不存在", 403);
		}
		String hash = operator.get("password") != null ? operator.get("password").toString() : "";
		if (!BCrypt.checkpw(password, hash)) {
			throw new ResourceException("账号密码错误，请重新登录", 403);
		}
		stringRedisTemplate.delete(redisKey);
		Object companyId = operator.get("company_id");
		operator.put("menu_type", getCompanyMenuType(companyId));
		return operator;
	}

	private Map<String, Object> credentialsByStaffAccount(Map<String, Object> params) {
		String username = params.get("username") != null ? params.get("username").toString() : "";
		String logintype = params.get("logintype") != null ? params.get("logintype").toString() : "staff";
		String redisKey = logintype + "_login_failed_times:" + username;
		bumpFailedOrThrow(redisKey);
		Map<String, Object> operator = employeeService.employeeLogin(params);
		stringRedisTemplate.delete(redisKey);
		if (operator.get("company_id") != null) {
			operator.put("menu_type", getCompanyMenuType(operator.get("company_id")));
		}
		return operator;
	}

	private Map<String, Object> credentialsByMerchantAccount(Map<String, Object> params) {
		String username = params.get("username") != null ? params.get("username").toString() : "";
		String password = params.get("password") != null ? params.get("password").toString() : "";
		String redisKey = "merchant_login_failed_times:" + username;
		bumpFailedOrThrow(redisKey);
		Map<String, Object> filter = new HashMap<>();
		filter.put("operator_type", "merchant");
		if (MOBILE_MERCHANT.matcher(username).matches()) {
			filter.put("mobile", username);
		} else {
			filter.put("login_name", username);
		}
		Map<String, Object> operator = operatorsQueryService.getInfo(filter);
		if (operator == null || operator.isEmpty()) {
			throw new UnauthorizedException("账号不存在");
		}
		Object merchantIdObj = operator.get("merchant_id");
		if (merchantIdObj != null && toLong(merchantIdObj) > 0) {
			Merchant m = merchantQueryService.getInfo(toLong(operator.get("company_id")), toLong(merchantIdObj), false);
			if (m == null || m.isDisabled()) {
				throw new UnauthorizedException("账号不存在");
			}
		}
		String hash = operator.get("password") != null ? operator.get("password").toString() : "";
		if (!BCrypt.checkpw(password, hash)) {
			throw new UnauthorizedException("账号密码错误，请重新登录");
		}
		stringRedisTemplate.delete(redisKey);
		operator.put("menu_type", getCompanyMenuType(operator.get("company_id")));
		return operator;
	}

	private Map<String, Object> credentialsByShopexOauthCode(Map<String, Object> params) {
		String code = params.get("code") != null ? params.get("code").toString() : "";
		if (code.isBlank()) {
			throw new ResourceException("登录账号异常");
		}
		PrismOAuthTokenResult token = prismOAuthService.exchangeAuthorizationCode(code);
		Map<String, String> prismData = token.data();
		String shopexid = prismData.get("shopexid");
		Map<String, Object> filter = new HashMap<>();
		filter.put("mobile", shopexid);
		filter.put("operator_type", "admin");
		Map<String, Object> operator = operatorsQueryService.getInfo(filter);
		if (operator == null || operator.isEmpty()) {
			if (systemIsSaas && !systemOpenOnline) {
				throw new ResourceException("账号未开通，请联系客服");
			}
			Map<String, Object> operatorData = new HashMap<>();
			operatorData.put("eid", prismData.get("eid"));
			operatorData.put("mobile", shopexid);
			operatorData.put("passport_uid", prismData.get("passport_uid"));
			operatorData.put("password", params.get("password") != null ? params.get("password").toString() : "");
			Object pmObj = params.get("product_model");
			String pm =
					pmObj != null && StringUtils.hasText(pmObj.toString())
							? pmObj.toString().trim()
							: productModel;
			operatorData.put("menu_type", pm);
			String issueId = prismData.get("issue_id");
			if (StringUtils.hasText(issueId)) {
				operatorData.put("issue_id", issueId.trim());
			}
			String email = prismData.get("email");
			if (StringUtils.hasText(email)) {
				operatorData.put("email", email.trim());
			}
			operatorsOpenService.open(operatorData);
			operator = operatorsQueryService.getInfo(filter);
		}
		if (operator == null || operator.isEmpty()) {
			throw new ResourceException("登录账号异常");
		}
		long companyId = toLong(operator.get("company_id"));
		String passportUid = prismData.get("passport_uid");
		String access = token.accessToken();
		String refresh = token.refreshToken() != null ? token.refreshToken() : "";
		shopexSmsClientFacade.setAccessToken(companyId, passportUid, access, token.expiresAtEpochSeconds());
		shopexSmsClientFacade.setRefreshToken(companyId, passportUid, refresh, token.refreshExpiresAtEpochSeconds());
		shopexOauthCertSyncService.syncAfterOauthLogin(companyId, passportUid, access);
		operator.put("menu_type", getCompanyMenuType(operator.get("company_id")));
		return operator;
	}

	private Map<String, Object> credentialsByShuyunCode(Map<String, Object> params) {
		boolean hasCodeKey = params.containsKey("code");
		String codeString =
				hasCodeKey && params.get("code") != null ? params.get("code").toString() : null;
		return shuyunOperatorsFacade.resolveOperatorData(codeString, hasCodeKey);
	}

	private Map<String, Object> credentialsByWorkWechatOauth(Map<String, Object> params) {
		throw new ResourceException("企业微信 OAuth 登录请走 workWeChatOauthLogin");
	}

	private Map<String, Object> credentialsByBindWorkWechat(Map<String, Object> params) {
		String companyIdRaw = stringParam(params.get("company_id"));
		String workUserid = stringParam(params.get("work_userid")).trim();
		String mobile = stringParam(params.get("mobile")).trim();
		String vcode = stringParam(params.get("vcode"));
		String checkToken = stringParam(params.get("check_token"));
		if (workUserid.isEmpty()) {
			throw new BadRequestException("work_userid必填");
		}
		if (mobile.isEmpty()) {
			throw new BadRequestException("请输入手机号码");
		}
		if (vcode.isEmpty()) {
			throw new BadRequestException("请输入短信验证码");
		}
		if (checkToken.isEmpty()) {
			throw new BadRequestException("check_token不能为空");
		}
		long companyId;
		try {
			companyId = Long.parseLong(companyIdRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("company_id必填");
		}
		if (!operatorSmsVerifyService.checkVerifyCode(mobile, "login", vcode)) {
			throw new ResourceException("短信验证码错误");
		}
		if (!distributorWorkWechatFacade.checkReBindMobile(companyId, workUserid, checkToken)) {
			throw new ResourceException("校验信息失效，请重新绑定");
		}
		LambdaQueryWrapper<Operators> opQ = new LambdaQueryWrapper<>();
		opQ.eq(Operators::getCompanyId, companyId)
				.eq(Operators::getMobile, mobile)
				.eq(Operators::getOperatorType, "staff")
				.last("LIMIT 1");
		Operators staff = operatorsMapper.selectOne(opQ);
		if (staff == null) {
			throw new ResourceException("需关联云店手机号");
		}
		long operatorId = staff.getOperatorId();
		distributorWorkWechatMobileBindService.upsertWorkWechatRelForMobileBind(companyId, workUserid, operatorId);
		distributorWorkWechatFacade.delReBindKey(companyId, workUserid);
		Map<String, Object> operator = toOperatorMap(staff);
		operator.put("menu_type", getCompanyMenuType(operator.get("company_id")));
		return operator;
	}

	private Map<String, Object> loadBoundOperatorForWechatOauth(Map<String, Object> params) {
		WechatBindSnapshot snap = resolveWechatOauthBinding(params);
		if (snap instanceof WechatBindSnapshot.Unbound) {
			throw new ResourceException("账号未绑定");
		}
		if (snap instanceof WechatBindSnapshot.Bound b) {
			return b.operatorMap();
		}
		throw new IllegalStateException("wechat oauth binding");
	}

	private WechatBindSnapshot resolveWechatOauthBinding(Map<String, Object> params) {
		String cidStr = stringParam(params.get("company_id")).trim();
		long companyId;
		if (!StringUtils.hasText(cidStr)) {
			companyId = 0L;
		} else {
			try {
				companyId = Long.parseLong(cidStr);
			} catch (NumberFormatException e) {
				companyId = 0L;
			}
		}
		Map<String, Object> woa = openPlatformWoaFacade.getWoaApp(companyId, "weixin", "touch");
		String woaAppid;
		Object modeObj = woa.get("mode");
		String mode = modeObj != null ? modeObj.toString() : "";
		if (WOA_MODE_AUTHORIZED.equals(mode)) {
			Object aid = woa.get("authorizerAppid");
			woaAppid = aid != null ? aid.toString().trim() : "";
		} else if (WOA_MODE_DIRECT.equals(mode)) {
			woaAppid = stringParam(woa.get("app_id")).trim();
		} else {
			throw new ResourceException("公众号信息有误！");
		}
		if (!StringUtils.hasText(woaAppid)) {
			throw new ResourceException("公众号信息有误！");
		}
		String code = stringParam(params.get("code"));
		Map<String, Object> wxUser = officialAccountOAuthFacade.getUserInfoByCode(woa, code);
		String openid = stringParam(wxUser.get("openid")).trim();
		if (!StringUtils.hasText(openid)) {
			throw new ResourceException("该账号不在店务应用可见范围内");
		}
		String unionid = stringParam(wxUser.get("unionid")).trim();
		LambdaQueryWrapper<DistributorWechatRel> relQ = new LambdaQueryWrapper<>();
		relQ.eq(DistributorWechatRel::getCompanyId, companyId)
				.eq(DistributorWechatRel::getAppId, woaAppid)
				.eq(DistributorWechatRel::getAppType, "wx")
				.eq(DistributorWechatRel::getOpenid, openid)
				.eq(DistributorWechatRel::getUnionid, unionid)
				.last("LIMIT 1");
		DistributorWechatRel rel = distributorWechatRelMapper.selectOne(relQ);
		if (rel == null || rel.getOperatorId() == null || rel.getOperatorId() <= 0L) {
			return new WechatBindSnapshot.Unbound(companyId, woaAppid, openid, unionid);
		}
		LambdaQueryWrapper<Operators> opQ = new LambdaQueryWrapper<>();
		opQ.eq(Operators::getCompanyId, companyId)
				.eq(Operators::getOperatorId, rel.getOperatorId())
				.last("LIMIT 1");
		Operators opRow = operatorsMapper.selectOne(opQ);
		if (opRow == null) {
			return new WechatBindSnapshot.Unbound(companyId, woaAppid, openid, unionid);
		}
		Map<String, Object> operator = toOperatorMap(opRow);
		operator.put("menu_type", getCompanyMenuType(operator.get("company_id")));
		return new WechatBindSnapshot.Bound(operator);
	}

	private String randomAlphanumeric(int len) {
		String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			sb.append(alphabet.charAt(secureRandom.nextInt(alphabet.length())));
		}
		return sb.toString();
	}

	private sealed interface WechatBindSnapshot permits WechatBindSnapshot.Bound, WechatBindSnapshot.Unbound {

		record Bound(Map<String, Object> operatorMap) implements WechatBindSnapshot {}

		record Unbound(long companyId, String appId, String openid, String unionid) implements WechatBindSnapshot {}
	}

	private Map<String, Object> credentialsByBindWechatDistributor(Map<String, Object> params) {
		String companyIdRaw = stringParam(params.get("company_id"));
		String appId = stringParam(params.get("app_id"));
		String appType = stringParam(params.get("app_type"));
		String openid = stringParam(params.get("openid"));
		String unionid = stringParam(params.get("unionid"));
		String mobile = stringParam(params.get("mobile")).trim();
		String vcode = stringParam(params.get("vcode"));
		String checkToken = stringParam(params.get("check_token"));
		long companyId;
		try {
			companyId = Long.parseLong(companyIdRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("company_id必填");
		}
		if (!operatorSmsVerifyService.checkVerifyCode(mobile, "login", vcode)) {
			throw new ResourceException("短信验证码错误");
		}
		if (!distributorWechatRebindRedisService.checkReBindMobile(companyId, appId, openid, unionid, checkToken)) {
			throw new ResourceException("校验信息失效，请重新绑定");
		}
		LambdaQueryWrapper<Operators> opQ = new LambdaQueryWrapper<>();
		opQ.eq(Operators::getCompanyId, companyId)
				.eq(Operators::getMobile, mobile)
				.eq(Operators::getOperatorType, "distributor")
				.last("LIMIT 1");
		Operators distributor = operatorsMapper.selectOne(opQ);
		if (distributor == null) {
			throw new ResourceException("需关联云店手机号");
		}
		long operatorId = distributor.getOperatorId();
		distributorWechatMobileBindService.upsertWechatRelForMobileBind(
				companyId, appId, appType, openid, unionid, operatorId);
		distributorWechatRebindRedisService.delReBindKey(companyId, appId, openid, unionid);
		Map<String, Object> operator = toOperatorMap(distributor);
		operator.put("menu_type", getCompanyMenuType(operator.get("company_id")));
		return operator;
	}

	private Map<String, Object> credentialsByBindWechatDistributorByUsername(Map<String, Object> params) {
		String companyIdRaw = stringParam(params.get("company_id"));
		String appId = stringParam(params.get("app_id"));
		String appType = stringParam(params.get("app_type"));
		String openid = stringParam(params.get("openid"));
		String unionid = stringParam(params.get("unionid"));
		String username = stringParam(params.get("username"));
		long companyId;
		try {
			companyId = Long.parseLong(companyIdRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("company_id必填");
		}
		LambdaQueryWrapper<Operators> opQ = new LambdaQueryWrapper<>();
		opQ.eq(Operators::getCompanyId, companyId)
				.eq(Operators::getUsername, username)
				.eq(Operators::getOperatorType, "distributor")
				.last("LIMIT 1");
		Operators distributor = operatorsMapper.selectOne(opQ);
		if (distributor == null) {
			throw new ResourceException("账号不存在");
		}
		long operatorId = distributor.getOperatorId();
		LambdaQueryWrapper<DistributorWechatRel> boundQ = new LambdaQueryWrapper<>();
		boundQ.eq(DistributorWechatRel::getCompanyId, companyId)
				.eq(DistributorWechatRel::getAppType, appType)
				.eq(DistributorWechatRel::getOperatorId, operatorId)
				.last("LIMIT 1");
		if (distributorWechatRelMapper.selectOne(boundQ) != null) {
			throw new ResourceException("该账号已绑定，请更换对应的微信账号");
		}
		long boundTime = System.currentTimeMillis() / 1000L;
		LambdaQueryWrapper<DistributorWechatRel> wxQ = new LambdaQueryWrapper<>();
		wxQ.eq(DistributorWechatRel::getCompanyId, companyId)
				.eq(DistributorWechatRel::getAppId, appId)
				.eq(DistributorWechatRel::getAppType, appType)
				.eq(DistributorWechatRel::getOpenid, openid)
				.eq(DistributorWechatRel::getUnionid, unionid)
				.last("LIMIT 1");
		DistributorWechatRel wxRow = distributorWechatRelMapper.selectOne(wxQ);
		if (wxRow != null) {
			wxRow.setOperatorId(operatorId);
			wxRow.setBoundTime(boundTime);
			distributorWechatRelMapper.updateById(wxRow);
		} else {
			DistributorWechatRel ins = new DistributorWechatRel();
			ins.setCompanyId(companyId);
			ins.setAppId(appId);
			ins.setAppType(appType);
			ins.setOpenid(openid);
			ins.setUnionid(unionid);
			ins.setOperatorId(operatorId);
			ins.setBoundTime(boundTime);
			distributorWechatRelMapper.insert(ins);
		}
		Map<String, Object> operator = toOperatorMap(distributor);
		operator.put("menu_type", getCompanyMenuType(operator.get("company_id")));
		return operator;
	}

	private static String stringParam(Object o) {
		return o != null ? o.toString() : "";
	}

	private static Map<String, Object> toOperatorMap(Operators op) {
		Map<String, Object> m = new HashMap<>();
		m.put("operator_id", op.getOperatorId());
		m.put("mobile", op.getMobile());
		m.put("login_name", op.getLoginName());
		m.put("operator_type", op.getOperatorType());
		m.put("password", op.getPassword());
		m.put("company_id", op.getCompanyId());
		m.put("is_disable", op.getIsDisable());
		m.put("merchant_id", op.getMerchantId());
		m.put("distributor_ids", op.getDistributorIds());
		m.put("shop_ids", op.getShopIds());
		m.put("username", op.getUsername());
		m.put("head_portrait", op.getHeadPortrait());
		m.put("regionauth_id", op.getRegionauthId());
		m.put("contact", op.getContact());
		m.put("split_ledger_info", op.getSplitLedgerInfo());
		m.put("adapay_open_account_time", op.getAdapayOpenAccountTime());
		m.put("dealer_parent_id", op.getDealerParentId());
		m.put("is_dealer_main", op.getIsDealerMain());
		m.put("is_merchant_main", op.getIsMerchantMain());
		m.put("is_distributor_main", op.getIsDistributorMain());
		m.put("passport_uid", op.getPassportUid());
		m.put("shopex_bind_account", op.getShopexBindAccount());
		m.put("eid", op.getEid());
		m.put("created", op.getCreated());
		m.put("updated", op.getUpdated());
		return m;
	}

	private Map<String, Object> credentialsByBindWechatDistributorByLite(Map<String, Object> params) {
		String companyIdRaw = stringParam(params.get("company_id"));
		String appId = stringParam(params.get("app_id"));
		String appType = stringParam(params.get("app_type"));
		String openid = stringParam(params.get("openid"));
		String unionid = stringParam(params.get("unionid"));
		long companyId;
		try {
			companyId = Long.parseLong(companyIdRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("company_id必填");
		}
		LambdaQueryWrapper<DistributorWechatRel> relQ = new LambdaQueryWrapper<>();
		relQ.eq(DistributorWechatRel::getCompanyId, companyId)
				.eq(DistributorWechatRel::getAppId, appId)
				.eq(DistributorWechatRel::getAppType, appType)
				.eq(DistributorWechatRel::getOpenid, openid)
				.eq(DistributorWechatRel::getUnionid, unionid)
				.last("LIMIT 1");
		DistributorWechatRel rel = distributorWechatRelMapper.selectOne(relQ);
		if (rel == null) {
			throw new ResourceException("请先绑定账号");
		}
		Long boundOpId = rel.getOperatorId();
		if (boundOpId == null || boundOpId <= 0L) {
			throw new ResourceException("请先绑定账号");
		}
		String productKey = shopMenuService.resolveProductModelKeyForCompany(companyId);
		String operatorType = "platform".equalsIgnoreCase(productKey) ? "distributor" : "staff";
		LambdaQueryWrapper<Operators> opQ = new LambdaQueryWrapper<>();
		opQ.eq(Operators::getCompanyId, companyId)
				.eq(Operators::getOperatorId, boundOpId)
				.eq(Operators::getOperatorType, operatorType)
				.last("LIMIT 1");
		Operators op = operatorsMapper.selectOne(opQ);
		if (op == null) {
			throw new ResourceException("账号不存在");
		}
		Map<String, Object> operator = toOperatorMap(op);
		operator.put("menu_type", getCompanyMenuType(operator.get("company_id")));
		return operator;
	}

	private void bumpFailedOrThrow(String redisKey) {
		Long n = stringRedisTemplate.opsForValue().increment(redisKey);
		if (n != null && n > 5) {
			throw new ForbiddenException("失败次数过多，请30分钟后重试");
		}
		stringRedisTemplate.expire(redisKey, 1800, TimeUnit.SECONDS);
	}

	private String getCompanyMenuType(Object companyId) {
		if (companyId == null) {
			return productModel;
		}
		long cid = toLong(companyId);
		Map<String, Object> info = shopMenuService.getMenuTypeByCompanyId(cid);
		Object str = info.get("menu_type_str");
		return str != null ? str.toString() : productModel;
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}
}
