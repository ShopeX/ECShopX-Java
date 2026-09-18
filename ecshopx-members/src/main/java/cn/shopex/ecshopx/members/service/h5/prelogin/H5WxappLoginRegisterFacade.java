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

package cn.shopex.ecshopx.members.service.h5.prelogin;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.members.h5.H5WxappRegisterEmployeeAuthenticator;
import cn.shopex.ecshopx.common.members.h5.H5WxappRegisterInviteBinder;
import cn.shopex.ecshopx.common.port.shuyun.ShuyunOpenPlatformWxappMemberSyncPort;
import cn.shopex.ecshopx.members.config.H5LocalProperties;
import cn.shopex.ecshopx.members.domain.MembersWhitelist;
import cn.shopex.ecshopx.members.mapper.MembersWhitelistMapper;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.members.service.admin.AdminBindUserSalespersonRelService;
import cn.shopex.ecshopx.companys.service.setting.WhitelistSettingRedisService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.shopex.ecshopx.members.dispatch.MemberRegisterJobDispatchPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class H5WxappLoginRegisterFacade {

	private final MemberAccountService memberAccountService;

	private final MembersWhitelistMapper membersWhitelistMapper;

	private final WhitelistSettingRedisService whitelistSettingRedisService;

	private final AdminBindUserSalespersonRelService adminBindUserSalespersonRelService;

	private final MemberRegisterJobDispatchPublisher memberRegisterJobDispatchPublisher;

	private final ShuyunOpenPlatformWxappMemberSyncPort openPlatformWxappMemberSyncPort;

	@Autowired
	private H5LocalProperties h5LocalProperties;

	/**
	 * 邀请码绑定为可选扩展：存在实现 Bean 时参与流程，否则跳过（不阻塞启动）。
	 */
	@Autowired
	private ObjectProvider<H5WxappRegisterInviteBinder> inviteBinderProvider;

	/**
	 * 员工认证为可选扩展：存在实现 Bean 时参与流程，否则跳过（不阻塞启动）。
	 */
	@Autowired
	private ObjectProvider<H5WxappRegisterEmployeeAuthenticator> employeeAuthenticatorProvider;

	public H5WxappLoginRegisterFacade(
			MemberAccountService memberAccountService,
			MembersWhitelistMapper membersWhitelistMapper,
			WhitelistSettingRedisService whitelistSettingRedisService,
			AdminBindUserSalespersonRelService adminBindUserSalespersonRelService,
			MemberRegisterJobDispatchPublisher memberRegisterJobDispatchPublisher,
			ShuyunOpenPlatformWxappMemberSyncPort openPlatformWxappMemberSyncPort) {
		this.memberAccountService = memberAccountService;
		this.membersWhitelistMapper = membersWhitelistMapper;
		this.whitelistSettingRedisService = whitelistSettingRedisService;
		this.adminBindUserSalespersonRelService = adminBindUserSalespersonRelService;
		this.memberRegisterJobDispatchPublisher = memberRegisterJobDispatchPublisher;
		this.openPlatformWxappMemberSyncPort = openPlatformWxappMemberSyncPort;
	}

	/**
	 * 预登录收尾：仅同步 wechat_users / 粉丝侧数据（{@code memberUserId=0} 时不写 members_associations）。
	 * 须在已完成 {@link #assertWhitelist} 的入口之后调用（例如 {@link #registerMemberForWxapp} 返回后、
	 * {@link H5WxappPreLoginService#wxappPreLogin} 末尾）；不在此重复白名单校验。
	 */
	public void syncWxappFansFromSession(Map<String, Object> params, Map<String, Object> wxSession) {
		recordWxappFansAfterMember(params, wxSession, 0L);
	}

	/**
	 * 在白名单与会员落库之后写入或更新小程序粉丝记录；{@code memberUserId > 0} 时同时写入 members_associations 绑定。
	 */
	private void recordWxappFansAfterMember(Map<String, Object> params, Map<String, Object> wxSession, long memberUserId) {
		String appid = trim(String.valueOf(params.get("appid")));
		long companyId = parseCompanyId(params);
		String openId = Objects.toString(wxSession.get("openid"), "");
		String unionId = Objects.toString(wxSession.get("unionid"), "");
		Map<String, Object> fanParams = new LinkedHashMap<>();
		fanParams.put("company_id", companyId);
		fanParams.put("open_id", openId);
		fanParams.put("unionid", unionId);
		fanParams.put("source_id", numberOrZero(params.get("source_id")));
		fanParams.put("monitor_id", numberOrZero(params.get("monitor_id")));
		fanParams.put("inviter_id", numberOrZero(params.get("inviter_id")));
		fanParams.put("source_from", stringOrDefault(params.get("source_from"), "default"));
		if (memberUserId > 0L) {
			fanParams.put("bind_member_user_id", memberUserId);
		}
		memberAccountService.createWxappFans(appid, fanParams);
	}

	/**
	 * 数云静默注册：仅当 OEM 数云且 shuyunappid 非空时入队；调用方须已处于「按手机号已有会员」且 union 关联校验通过路径。
	 */
	private void dispatchMemberRegisterJobIfEligible(
			Map<String, Object> params,
			long companyId,
			long userId,
			String mobile,
			String openId,
			String unionId) {
		if (!this.h5LocalProperties.isOemShuyun()) {
			return;
		}
		String shuyunAppId = StringUtils.trimWhitespace(Objects.toString(params.get("shuyunappid"), ""));
		if (!StringUtils.hasText(shuyunAppId)) {
			return;
		}
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("user_id", userId);
		payload.put("mobile", mobile);
		payload.put("unionid", unionId);
		payload.put("open_id", openId);
		payload.put("appid", trim(String.valueOf(params.get("appid"))));
		payload.put("source_id", numberOrZero(params.get("source_id")));
		payload.put("monitor_id", numberOrZero(params.get("monitor_id")));
		payload.put("inviter_id", numberOrZero(params.get("inviter_id")));
		payload.put("source_from", stringOrDefault(params.get("source_from"), "default"));
		payload.put("shuyunappid", shuyunAppId);
		this.memberRegisterJobDispatchPublisher.enqueueMemberRegister(payload);
	}

	/**
	 * OEM 数云下：若手机号已有会员且 wechat 关联 union 已对齐，则按需入队数云静默注册任务并结束本入口；
	 * 否则返回 false（无会员、非 OEM、或尚无 wechat 关联等），由调用方继续后续分支。
	 */
	private boolean finishIfExistingOemWxMemberShuyunResolved(
			Map<String, Object> params, Map<String, Object> wxSession, long companyId, String mobile) {
		Map<String, Object> memberRow = memberAccountService.getInfoByMobile(companyId, mobile);
		if (memberRow == null || memberRow.isEmpty()) {
			return false;
		}
		if (!this.h5LocalProperties.isOemShuyun()) {
			return false;
		}
		long userId = toLong(memberRow.get("user_id"));
		Map<String, Object> userAssociation =
				memberAccountService.getMembersAssociationByUserId(companyId, "wechat", userId);
		if (userAssociation == null || userAssociation.isEmpty()) {
			return false;
		}
		String unionId = Objects.toString(wxSession.get("unionid"), "");
		if (!unionId.equals(Objects.toString(userAssociation.get("unionid"), ""))) {
			throw new ResourceException("该手机号已注册为会员，请更换手机号！");
		}
		String openId = Objects.toString(wxSession.get("openid"), "");
		dispatchMemberRegisterJobIfEligible(params, companyId, userId, mobile, openId, unionId);
		return true;
	}

	/**
	 * OEM 数云：手机号已有会员但尚无 wechat 关联时，仅补粉丝与会员绑定并视条件入队数云任务，不走新会员自动注册。
	 */
	private boolean finishOemShuyunExistingMemberWechatBindOnly(
			Map<String, Object> params,
			Map<String, Object> wxSession,
			long companyId,
			String mobile,
			Map<String, Object> employeeAuth,
			boolean inviteLocked,
			H5WxappRegisterInviteBinder inviteBinder,
			String inviteCode) {
		if (!this.h5LocalProperties.isOemShuyun()) {
			return false;
		}
		Map<String, Object> memberRow = memberAccountService.getInfoByMobile(companyId, mobile);
		if (memberRow == null || memberRow.isEmpty()) {
			return false;
		}
		long userId = toLong(memberRow.get("user_id"));
		Map<String, Object> userAssociation =
				memberAccountService.getMembersAssociationByUserId(companyId, "wechat", userId);
		if (userAssociation != null && !userAssociation.isEmpty()) {
			return false;
		}
		recordWxappFansAfterMember(params, wxSession, userId);
		String openId = Objects.toString(wxSession.get("openid"), "");
		String unionId = Objects.toString(wxSession.get("unionid"), "");
		dispatchMemberRegisterJobIfEligible(params, companyId, userId, mobile, openId, unionId);
		applyEmployeeAuthAndInviteForWxapp(
				companyId, mobile, userId, employeeAuth, inviteLocked, inviteBinder, inviteCode);
		return true;
	}

	private void applyEmployeeAuthAndInviteForWxapp(
			long companyId,
			String mobile,
			long userId,
			Map<String, Object> employeeAuth,
			boolean inviteLocked,
			H5WxappRegisterInviteBinder inviteBinder,
			String inviteCode) {
		if (employeeAuth != null && !employeeAuth.isEmpty()) {
			H5WxappRegisterEmployeeAuthenticator authenticator = employeeAuthenticatorProvider.getIfAvailable();
			if (authenticator == null) {
				throw new ResourceException("员工认证服务未初始化");
			}
			Map<String, Object> authPayload = new HashMap<>(employeeAuth);
			authPayload.put("company_id", companyId);
			authPayload.put("user_id", userId);
			authPayload.put("member_mobile", mobile);
			authPayload.put("mobile", mobile);
			authenticator.authenticate(authPayload);
		}

		if (inviteLocked && inviteBinder != null) {
			inviteBinder.bindRelativeAndConsumeInvite(companyId, userId, mobile, inviteCode);
		}
	}

	public void bindSalespersonIfNeeded(Map<String, Object> params, long userId) {
		long sp = parseSalespersonId(params.get("salesperson_id"));
		if (sp <= 0L || userId <= 0L) {
			return;
		}
		long companyId = parseCompanyId(params);
		adminBindUserSalespersonRelService.bindUserSalespersonRel(companyId, List.of(userId), sp, null);
	}

	@Transactional(rollbackFor = Exception.class)
	public void registerMemberForWxapp(Map<String, Object> params, Map<String, Object> wxSession) {
		long companyId = 0L;
		String inviteCode = "";
		boolean inviteLocked = false;
		try {
			companyId = parseCompanyId(params);
			String mobile = Objects.toString(wxSession.get("purePhoneNumber"), "").trim();
			if (!StringUtils.hasText(mobile)) {
				throw new ResourceException("授权手机号失败");
			}

			assertWhitelist(companyId, mobile);

			if (finishIfExistingOemWxMemberShuyunResolved(params, wxSession, companyId, mobile)) {
				return;
			}

			Map<String, Object> employeeAuth = readStringObjectMap(params.get("employee_auth"));
			inviteCode = Objects.toString(params.get("invite_code"), "").trim();

			H5WxappRegisterInviteBinder inviteBinder = inviteBinderProvider.getIfAvailable();

			if (employeeAuth != null && !employeeAuth.isEmpty()) {
				Object eid = employeeAuth.get("enterprise_id");
				if (eid == null || toLong(eid) <= 0L) {
					throw new ResourceException("企业ID必填");
				}
			} else if (StringUtils.hasText(inviteCode)) {
				if (inviteBinder == null) {
					throw new ResourceException("邀请注册服务未初始化");
				}
				inviteBinder.lockInviteCode(companyId, inviteCode);
				inviteLocked = true;
			}

			if (finishOemShuyunExistingMemberWechatBindOnly(
					params, wxSession, companyId, mobile, employeeAuth, inviteLocked, inviteBinder, inviteCode)) {
				return;
			}

			String passwordPlain = randomPasswordPlain();

			Map<String, Object> registrationExtras = buildRegistrationExtrasForWxapp(params, wxSession);
			Map<String, Object> created =
					memberAccountService.createMemberLocalAutoRegister(
							companyId, mobile, passwordPlain, registrationExtras);
			long userId = toLong(created.get("user_id"));
			recordWxappFansAfterMember(params, wxSession, userId);

			applyEmployeeAuthAndInviteForWxapp(
					companyId, mobile, userId, employeeAuth, inviteLocked, inviteBinder, inviteCode);

			String openId = Objects.toString(wxSession.get("openid"), "");
			String unionId = Objects.toString(wxSession.get("unionid"), "");
			long distHint = toLong(params.get("distributor_id"));
			openPlatformWxappMemberSyncPort.syncWxappOnlineIfEnabled(
					companyId, userId, mobile, unionId, openId, distHint, true);
		} catch (ResourceException ex) {
			if (inviteLocked) {
				unlockInviteQuietly(companyId, inviteCode);
			}
			throw ex;
		} catch (Exception ex) {
			if (inviteLocked) {
				unlockInviteQuietly(companyId, inviteCode);
			}
			throw new ResourceException("注册失败，请稍后重试！");
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void registerMemberForAliapp(Map<String, Object> params, Map<String, Object> aliSession) {
		long companyId = 0L;
		String inviteCode = "";
		boolean inviteLocked = false;
		try {
			companyId = parseCompanyId(params);
			String mobile = Objects.toString(aliSession.get("purePhoneNumber"), "").trim();
			if (!StringUtils.hasText(mobile)) {
				throw new ResourceException("授权手机号失败");
			}

			assertWhitelist(companyId, mobile);

			Map<String, Object> employeeAuth = readStringObjectMap(params.get("employee_auth"));
			inviteCode = Objects.toString(params.get("invite_code"), "").trim();

			H5WxappRegisterInviteBinder inviteBinder = inviteBinderProvider.getIfAvailable();

			if (employeeAuth != null && !employeeAuth.isEmpty()) {
				Object eid = employeeAuth.get("enterprise_id");
				if (eid == null || toLong(eid) <= 0L) {
					throw new ResourceException("企业ID必填");
				}
			} else if (StringUtils.hasText(inviteCode)) {
				if (inviteBinder == null) {
					throw new ResourceException("邀请注册服务未初始化");
				}
				inviteBinder.lockInviteCode(companyId, inviteCode);
				inviteLocked = true;
			}

			String alipayUserId = Objects.toString(aliSession.get("alipay_user_id"), "");
			String alipayAppid = trim(String.valueOf(params.get("alipay_appid")));
			if (!StringUtils.hasText(alipayAppid)) {
				alipayAppid = trim(String.valueOf(params.get("appid")));
			}

			Map<String, Object> memberRow = memberAccountService.getInfoByMobile(companyId, mobile);
			boolean hasMemberByMobile = memberRow != null && !memberRow.isEmpty();
			String passwordPlain = randomPasswordPlain();
			long userId;
			if (this.h5LocalProperties.isOemShuyun()) {
				if (!hasMemberByMobile) {
					Map<String, Object> regParams = new LinkedHashMap<>();
					regParams.put("company_id", companyId);
					regParams.put("mobile", mobile);
					regParams.put("user_type", "ali");
					regParams.put("unionid", alipayUserId);
					regParams.put("alipay_appid", alipayAppid);
					regParams.put("source_id", (long) numberOrZero(params.get("source_id")));
					regParams.put("monitor_id", (long) numberOrZero(params.get("monitor_id")));
					regParams.put("inviter_id", (long) numberOrZero(params.get("inviter_id")));
					regParams.put("wxa_appid", alipayAppid);
					Object distObj = params.get("distributor_id");
					if (distObj != null) {
						regParams.put("distributor_id", distObj);
					}
					Object spObj = params.get("salesperson_id");
					if (spObj != null) {
						regParams.put("salesperson_id", spObj);
					}
					try {
						Map<String, Object> created = memberAccountService.registerShuyunMember(regParams);
						userId = toLong(created.get("user_id"));
					} catch (BadRequestException ex) {
						throw new ResourceException("注册失败，请稍后重试！");
					}
				} else {
					userId = toLong(memberRow.get("user_id"));
					memberAccountService.ensureAliMemberAssociation(companyId, userId, alipayUserId, alipayAppid);
				}
			} else {
				if (!hasMemberByMobile) {
					Map<String, Object> registrationExtras =
							buildRegistrationExtrasForAliapp(params, aliSession, alipayAppid);
					Map<String, Object> created =
							memberAccountService.createMemberLocalAutoRegister(
									companyId, mobile, passwordPlain, registrationExtras);
					userId = toLong(created.get("user_id"));
				} else {
					userId = toLong(memberRow.get("user_id"));
				}
				memberAccountService.ensureAliMemberAssociation(companyId, userId, alipayUserId, alipayAppid);
			}

			if (employeeAuth != null && !employeeAuth.isEmpty()) {
				H5WxappRegisterEmployeeAuthenticator authenticator = employeeAuthenticatorProvider.getIfAvailable();
				if (authenticator == null) {
					throw new ResourceException("员工认证服务未初始化");
				}
				Map<String, Object> authPayload = new HashMap<>(employeeAuth);
				authPayload.put("company_id", companyId);
				authPayload.put("user_id", userId);
				authPayload.put("member_mobile", mobile);
				authPayload.put("mobile", mobile);
				authenticator.authenticate(authPayload);
			}

			if (inviteLocked && inviteBinder != null) {
				inviteBinder.bindRelativeAndConsumeInvite(companyId, userId, mobile, inviteCode);
			}
		} catch (ResourceException ex) {
			if (inviteLocked) {
				unlockInviteQuietly(companyId, inviteCode);
			}
			throw ex;
		} catch (Exception ex) {
			if (inviteLocked) {
				unlockInviteQuietly(companyId, inviteCode);
			}
			throw new ResourceException("注册失败，请稍后重试！");
		}
	}

	private void unlockInviteQuietly(long companyId, String inviteCode) {
		H5WxappRegisterInviteBinder inviteBinder = inviteBinderProvider.getIfAvailable();
		if (inviteBinder == null || !StringUtils.hasText(inviteCode)) {
			return;
		}
		try {
			inviteBinder.unlockInviteCode(companyId, inviteCode);
		} catch (RuntimeException ignored) {
			// best-effort rollback of Redis lock
		}
	}

	private static Map<String, Object> readStringObjectMap(Object raw) {
		if (!(raw instanceof Map<?, ?> m) || m.isEmpty()) {
			return null;
		}
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : m.entrySet()) {
			if (e.getKey() != null) {
				out.put(String.valueOf(e.getKey()), e.getValue());
			}
		}
		return out.isEmpty() ? null : out;
	}

	private void assertWhitelist(long companyId, String purePhone) {
		Map<String, Object> cfg = this.whitelistSettingRedisService.getMergedConfig(companyId, Map.of());
		Object status = cfg.get("whitelist_status");
		boolean enabled = Boolean.TRUE.equals(status) || "true".equals(String.valueOf(status));
		if (!enabled) {
			return;
		}
		String tips = Objects.toString(cfg.get("whitelist_tips"), "登录失败，手机号不在白名单内！").trim();
		String cipher = LegacyFixedMobileEncrypt.fixedEncryptMobile(purePhone);
		Long count = this.membersWhitelistMapper.selectCount(new LambdaQueryWrapper<MembersWhitelist>()
				.eq(MembersWhitelist::getCompanyId, companyId)
				.eq(MembersWhitelist::getMobile, cipher));
		if (count == null || count <= 0L) {
			throw new ResourceException(tips.isEmpty() ? "登录失败，手机号不在白名单内！" : tips);
		}
	}

	private static Map<String, Object> buildRegistrationExtrasForWxapp(
			Map<String, Object> params, Map<String, Object> wxSession) {
		Map<String, Object> extras = new LinkedHashMap<>();
		extras.put("source_id", (long) numberOrZero(params.get("source_id")));
		extras.put("monitor_id", (long) numberOrZero(params.get("monitor_id")));
		extras.put("inviter_id", (long) numberOrZero(params.get("inviter_id")));
		Object distObj = params.get("distributor_id");
		if (distObj != null) {
			extras.put("distributor_id", distObj);
		}
		Object spObj = params.get("salesperson_id");
		if (spObj != null) {
			extras.put("salesperson_id", spObj);
		}
		String wxa =
				trim(String.valueOf(params.getOrDefault("wxa_appid", "")));
		if (!StringUtils.hasText(wxa)) {
			wxa = trim(String.valueOf(params.getOrDefault("appid", "")));
		}
		if (StringUtils.hasText(wxa)) {
			extras.put("wxa_appid", wxa);
		}
		extras.put("open_id", trim(Objects.toString(wxSession.get("openid"), "")));
		String authorizer = trim(String.valueOf(wxSession.get("authorizer_appid")));
		if (StringUtils.hasText(authorizer)) {
			extras.put("authorizer_appid", authorizer);
		}
		Object workUserid = params.get("work_userid");
		if (workUserid != null && StringUtils.hasText(String.valueOf(workUserid).trim())) {
			extras.put("work_userid", workUserid);
		}
		Object channel = params.get("channel");
		if (channel != null) {
			extras.put("channel", channel);
		}
		Object unionWx = wxSession.get("unionid");
		if (unionWx != null) {
			String unionTrim = String.valueOf(unionWx).trim();
			if (StringUtils.hasText(unionTrim)) {
				extras.put("unionid", unionTrim);
			}
		}
		return extras;
	}

	private static Map<String, Object> buildRegistrationExtrasForAliapp(
			Map<String, Object> params, Map<String, Object> aliSession, String alipayAppid) {
		Map<String, Object> extras = new LinkedHashMap<>();
		extras.put("source_id", (long) numberOrZero(params.get("source_id")));
		extras.put("monitor_id", (long) numberOrZero(params.get("monitor_id")));
		extras.put("inviter_id", (long) numberOrZero(params.get("inviter_id")));
		Object distObj = params.get("distributor_id");
		if (distObj != null) {
			extras.put("distributor_id", distObj);
		}
		Object spObj = params.get("salesperson_id");
		if (spObj != null) {
			extras.put("salesperson_id", spObj);
		}
		if (StringUtils.hasText(alipayAppid)) {
			extras.put("alipay_appid", alipayAppid);
			extras.put("wxa_appid", alipayAppid);
		}
		extras.put("open_id", trim(Objects.toString(aliSession.get("alipay_user_id"), "")));
		Object workUserid = params.get("work_userid");
		if (workUserid != null && StringUtils.hasText(String.valueOf(workUserid).trim())) {
			extras.put("work_userid", workUserid);
		}
		Object channel = params.get("channel");
		if (channel != null) {
			extras.put("channel", channel);
		}
		Object unionParam = params.get("unionid");
		if (unionParam != null && StringUtils.hasText(String.valueOf(unionParam).trim())) {
			extras.put("unionid", String.valueOf(unionParam).trim());
		}
		return extras;
	}

	private static long parseCompanyId(Map<String, Object> params) {
		return toLong(params.get("company_id"));
	}

	private static long parseSalespersonId(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : 0L;
		}
		String s = String.valueOf(o).trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0L ? v : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(o).trim());
	}

	private static String trim(String s) {
		return s == null ? "" : s.trim();
	}

	private static int numberOrZero(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String stringOrDefault(Object o, String def) {
		String s = o == null ? "" : String.valueOf(o).trim();
		return StringUtils.hasText(s) ? s : def;
	}

	private static String randomPasswordPlain() {
		String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		StringBuilder sb = new StringBuilder();
		java.util.concurrent.ThreadLocalRandom r = java.util.concurrent.ThreadLocalRandom.current();
		for (int i = 0; i < 16; i++) {
			sb.append(chars.charAt(r.nextInt(chars.length())));
		}
		return sb.toString();
	}
}
