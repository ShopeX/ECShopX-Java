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

package cn.shopex.ecshopx.members.service.email;

import cn.shopex.ecshopx.common.dispatch.SendMemberEmailActivationJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.setting.WhitelistSettingRedisService;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersWhitelist;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.mapper.MembersWhitelistMapper;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import cn.shopex.ecshopx.members.service.admin.AdminMemberCreateMemberService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * C3 邮箱注册编排，对齐 PHP {@code MemberService::registerMemberWithEmail}：
 * 密码策略 → 邮件配置就绪 → 合成手机号 → 查重 → 白名单 → 建会员 → 激活 base 解析 → 异步入队激活邮件。
 */
@Service
public class MemberEmailRegisterService {

	private static final Logger log = LoggerFactory.getLogger(MemberEmailRegisterService.class);

	private final MemberEmailVerificationService memberEmailVerificationService;
	private final MemberPasswordPolicyService memberPasswordPolicyService;
	private final MemberEmailRegistrationValidator memberEmailRegistrationValidator;
	private final MemberSyntheticMobileService memberSyntheticMobileService;
	private final MembersMapper membersMapper;
	private final MembersWhitelistMapper membersWhitelistMapper;
	private final WhitelistSettingRedisService whitelistSettingRedisService;
	private final AdminMemberCreateMemberService adminMemberCreateMemberService;
	private final MailSettingActivationBaseUrlResolver mailSettingActivationBaseUrlResolver;
	private final SendMemberEmailActivationJobDispatchPublisher sendMemberEmailActivationJobDispatchPublisher;
	private final String commonH5BaseUrl;

	public MemberEmailRegisterService(
			MemberEmailVerificationService memberEmailVerificationService,
			MemberPasswordPolicyService memberPasswordPolicyService,
			MemberEmailRegistrationValidator memberEmailRegistrationValidator,
			MemberSyntheticMobileService memberSyntheticMobileService,
			MembersMapper membersMapper,
			MembersWhitelistMapper membersWhitelistMapper,
			WhitelistSettingRedisService whitelistSettingRedisService,
			AdminMemberCreateMemberService adminMemberCreateMemberService,
			MailSettingActivationBaseUrlResolver mailSettingActivationBaseUrlResolver,
			SendMemberEmailActivationJobDispatchPublisher sendMemberEmailActivationJobDispatchPublisher,
			@Value("${common.h5-base-url:}") String commonH5BaseUrl) {
		this.memberEmailVerificationService = memberEmailVerificationService;
		this.memberPasswordPolicyService = memberPasswordPolicyService;
		this.memberEmailRegistrationValidator = memberEmailRegistrationValidator;
		this.memberSyntheticMobileService = memberSyntheticMobileService;
		this.membersMapper = membersMapper;
		this.membersWhitelistMapper = membersWhitelistMapper;
		this.whitelistSettingRedisService = whitelistSettingRedisService;
		this.adminMemberCreateMemberService = adminMemberCreateMemberService;
		this.mailSettingActivationBaseUrlResolver = mailSettingActivationBaseUrlResolver;
		this.sendMemberEmailActivationJobDispatchPublisher = sendMemberEmailActivationJobDispatchPublisher;
		this.commonH5BaseUrl = commonH5BaseUrl;
	}

	/**
	 * 注册并返回 {@code {activation_email_queued: true}} 附加结果。
	 *
	 * @param params keys: email, password, wxa_appid, authorizer_appid, sex, username, avatar,
	 *     unionid, open_id, inviter_id, source_from, source_id, monitor_id,
	 *     activation_base_url（激活 base 第三回退）, client_ip, device_id
	 */
	public Map<String, Object> registerByEmail(long companyId, Map<String, Object> params) {
		String email = memberEmailVerificationService.normalizeEmail(asString(params.get("email")));
		String password = asString(params.get("password"));

		memberPasswordPolicyService.validateOrFail(password);
		memberEmailRegistrationValidator.assertReadyForMemberEmailRegistration(companyId);

		String mobile = memberSyntheticMobileService.allocateUnique(companyId);

		Members dup = membersMapper.selectOne(new LambdaQueryWrapper<Members>()
				.eq(Members::getCompanyId, companyId)
				.eq(Members::getLoginEmail, email)
				.last("LIMIT 1"));
		if (dup != null) {
			throw new ResourceException("该邮箱已注册");
		}

		assertWhitelistValid(companyId, mobile);

		Map<String, Object> createParams = buildCreateParams(companyId, params, email, password, mobile);
		Map<String, Object> result =
				adminMemberCreateMemberService.createMemberWithEmail(companyId, createParams);

		String baseUrl = resolveActivationBaseUrl(companyId, asString(params.get("activation_base_url")));
		if (baseUrl.isEmpty()) {
			throw new ResourceException("请配置激活页基础地址（activation_base_url）");
		}

		String clientIp = asString(params.get("client_ip"));
		String deviceId = asString(params.get("device_id"));
		try {
			sendMemberEmailActivationJobDispatchPublisher.publish(
					companyId, email, clientIp, deviceId, baseUrl);
		} catch (RuntimeException e) {
			log.error(
					"activation email queue dispatch failed after register companyId={} email={}",
					companyId,
					email,
					e);
			throw new ResourceException(
					"激活邮件排队失败，请稍后在发送邮箱邮件接口选择 purpose=activate 重发激活链接。");
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>(result);
		out.put("activation_email_queued", true);
		return out;
	}

	private Map<String, Object> buildCreateParams(
			long companyId, Map<String, Object> params, String email, String password, String mobile) {
		String apiFrom = asString(params.get("api_from"));
		String sourceFrom = asString(params.get("source_from"));
		LinkedHashMap<String, Object> createParams = new LinkedHashMap<>();
		createParams.put("mobile", mobile);
		createParams.put("region_mobile", mobile);
		createParams.put("mobile_country_code", "86");
		createParams.put("company_id", companyId);
		createParams.put("wxa_appid", asString(params.get("wxa_appid")));
		createParams.put("authorizer_appid", asString(params.get("authorizer_appid")));
		createParams.put("sex", intOrZero(params.get("sex")));
		createParams.put("username", asString(params.get("username")));
		createParams.put("avatar", asString(params.get("avatar")));
		createParams.put("email", email);
		createParams.put("login_email", email);
		createParams.put("email_verified_at", null);
		createParams.put("password", password);
		createParams.put("api_from", apiFrom.isEmpty() ? "h5app" : apiFrom);
		createParams.put("auth_type", "local");
		createParams.put("user_type", "local");
		createParams.put("unionid", asString(params.get("unionid")));
		createParams.put("open_id", asString(params.get("open_id")));
		createParams.put("inviter_id", longOrZero(params.get("inviter_id")));
		createParams.put("source_from", sourceFrom.isEmpty() ? "default" : sourceFrom);
		createParams.put("source_id", longOrZero(params.get("source_id")));
		createParams.put("monitor_id", longOrZero(params.get("monitor_id")));
		return createParams;
	}

	/**
	 * 激活 base 地址回退顺序（对齐 PHP 注册流程）：{@code mailSetting:{companyId}} 的 H5 激活域名 →
	 * {@code common.h5_base_url} → 请求参数 {@code activation_base_url}。
	 */
	private String resolveActivationBaseUrl(long companyId, String requestBase) {
		String base = mailSettingActivationBaseUrlResolver.getH5ActivationBaseUrl(companyId);
		if (base.isEmpty()) {
			base = commonH5BaseUrl == null ? "" : commonH5BaseUrl.trim();
		}
		if (base.isEmpty()) {
			base = requestBase == null ? "" : requestBase.trim();
		}
		return base;
	}

	private void assertWhitelistValid(long companyId, String mobile) {
		Map<String, Object> setting = whitelistSettingRedisService.getMergedConfig(companyId, Map.of());
		Object status = setting.get("whitelist_status");
		boolean enabled =
				(status instanceof Boolean b && b) || "true".equals(String.valueOf(status));
		if (!enabled) {
			return;
		}
		Long count = membersWhitelistMapper.selectCount(new LambdaQueryWrapper<MembersWhitelist>()
				.eq(MembersWhitelist::getCompanyId, companyId)
				.eq(MembersWhitelist::getMobile, LegacyFixedMobileEncrypt.fixedEncryptMobile(mobile)));
		if (count == null || count == 0L) {
			String tips = setting.get("whitelist_tips") == null
					? "登录失败，手机号不在白名单内！"
					: String.valueOf(setting.get("whitelist_tips"));
			throw new ResourceException(tips);
		}
	}

	private static int intOrZero(Object value) {
		if (value == null) {
			return 0;
		}
		if (value instanceof Number n) {
			return n.intValue();
		}
		String s = String.valueOf(value).trim();
		if (s.isEmpty()) {
			return 0;
		}
		try {
			return (int) Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long longOrZero(Object value) {
		if (value == null) {
			return 0L;
		}
		if (value instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(value).trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String asString(Object value) {
		return value == null ? "" : String.valueOf(value);
	}
}
