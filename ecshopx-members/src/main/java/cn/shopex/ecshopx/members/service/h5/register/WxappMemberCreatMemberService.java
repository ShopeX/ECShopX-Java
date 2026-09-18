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

package cn.shopex.ecshopx.members.service.h5.register;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.setting.WhitelistSettingRedisService;
import cn.shopex.ecshopx.members.domain.MembersWhitelist;
import cn.shopex.ecshopx.members.mapper.MembersWhitelistMapper;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.members.service.reg.MemberRegSettingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class WxappMemberCreatMemberService {

	private static final ObjectMapper JSON = new ObjectMapper();

	private static final Pattern MOBILE_CN = Pattern.compile("^1[3456789][0-9]{9}$");

	private final MemberAccountService memberAccountService;

	private final MemberRegSettingService memberRegSettingService;

	private final MembersWhitelistMapper membersWhitelistMapper;

	private final WhitelistSettingRedisService whitelistSettingRedisService;

	public WxappMemberCreatMemberService(
			MemberAccountService memberAccountService,
			MemberRegSettingService memberRegSettingService,
			MembersWhitelistMapper membersWhitelistMapper,
			WhitelistSettingRedisService whitelistSettingRedisService) {
		this.memberAccountService = memberAccountService;
		this.memberRegSettingService = memberRegSettingService;
		this.membersWhitelistMapper = membersWhitelistMapper;
		this.whitelistSettingRedisService = whitelistSettingRedisService;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> creatMember(Map<String, Object> postData) {
		Map<String, Object> work = new LinkedHashMap<>(postData);

		String mobile = Objects.toString(work.get("mobile"), "").trim();
		if (!StringUtils.hasText(mobile)) {
			throw new ResourceException("手机号不能为空");
		}
		if (!MOBILE_CN.matcher(mobile).matches()) {
			throw new ResourceException("手机号格式不正确");
		}

		String userType = Objects.toString(work.get("user_type"), "").trim();
		if (!StringUtils.hasText(userType)) {
			throw new ResourceException("用户类型不能为空");
		}

		String apiFrom = Objects.toString(work.get("api_from"), "").trim();
		String authType = Objects.toString(work.get("auth_type"), "").trim();
		boolean h5PhoneRegister = "h5app".equals(apiFrom) && !"wxapp".equals(authType);

		if (h5PhoneRegister) {
			if (looseEmptyPassword(work.get("password"))) {
				throw new ResourceException("密码不能为空");
			}
			if (looseEmptyVcode(work.get("vcode"))) {
				throw new ResourceException("验证码不能为空");
			}
			long companyId = toLong(work.get("company_id"));
			Map<String, Object> byMobile = memberAccountService.getInfoByMobile(companyId, mobile);
			if (byMobile != null && !byMobile.isEmpty() && !isUploadMember(byMobile)) {
				throw new ResourceException("该手机号已被占用");
			}
		}

		assertWhitelist(toLong(work.get("company_id")), mobile);

		Long inviterId = resolveInviterId(work);

		if ("wxapp".equals(authType)) {
			long companyId = toLong(work.get("company_id"));
			String unionid = Objects.toString(work.get("unionid"), "").trim();
			String openId = Objects.toString(work.get("open_id"), "").trim();
			if (!StringUtils.hasText(unionid) && !StringUtils.hasText(openId)) {
				throw new ResourceException("请确认您的信息是否正确");
			}
			Map<String, Object> wxFilter = new LinkedHashMap<>();
			wxFilter.put("company_id", companyId);
			if (StringUtils.hasText(unionid)) {
				wxFilter.put("unionid", unionid);
			}
			if (StringUtils.hasText(openId)) {
				wxFilter.put("open_id", openId);
			}
			Map<String, Object> wxUser = memberAccountService.getWechatSimpleUser(wxFilter);
			if (wxUser == null || wxUser.isEmpty()) {
				throw new ResourceException("请确认您的信息是否正确");
			}
			if (inviterId == null) {
				long wxInv = toLong(wxUser.get("inviter_id"));
				work.put("inviter_id", wxInv);
				String sf = Objects.toString(wxUser.get("source_from"), "").trim();
				work.put("source_from", StringUtils.hasText(sf) ? sf : "default");
			} else {
				work.put("inviter_id", inviterId.longValue());
			}
		} else {
			long companyId = toLong(work.get("company_id"));
			String vcode = Objects.toString(work.get("vcode"), "").trim();
			String checkType = Objects.toString(work.get("check_type"), "").trim();
			if (!StringUtils.hasText(checkType)) {
				checkType = "sign";
			}
			if (!memberRegSettingService.checkSmsVcode(mobile, companyId, vcode, checkType)) {
				// throw new ResourceException("短信验证码错误");
			}
			if (inviterId != null) {
				work.put("inviter_id", inviterId);
			}
		}

		normalizeSourceFields(work);

		long companyId = toLong(work.get("company_id"));
		mergeAuthFieldsIntoPost(work, companyId);

		int inviterInt = (int) toLong(work.get("inviter_id"));
		work.put("inviter_id", inviterInt);
		String sf = Objects.toString(work.get("source_from"), "").trim();
		work.put("source_from", StringUtils.hasText(sf) ? sf : "default");

		return memberAccountService.creatMemberForH5Post(work, true);
	}

	private void normalizeSourceFields(Map<String, Object> work) {
		Object sid = work.get("source_id");
		work.put("source_id", trimNumericId(sid));
		Object mid = work.get("monitor_id");
		work.put("monitor_id", trimNumericId(mid));
		work.put("latest_source_id", work.get("source_id"));
		work.put("latest_monitor_id", work.get("monitor_id"));
	}

	private static Object trimNumericId(Object raw) {
		if (raw == null) {
			return "0";
		}
		String t = String.valueOf(raw).trim();
		return t.isEmpty() ? "0" : t;
	}

	private void mergeAuthFieldsIntoPost(Map<String, Object> work, long companyId) {
		work.put("company_id", companyId);
		putLongIfPresent(work, "user_id", work.get("user_id"));
		putTextIfPresent(work, "unionid", work.get("unionid"));
		putTextIfPresent(work, "open_id", work.get("open_id"));
		Object wxa = work.get("wxa_appid");
		if (wxa == null || !StringUtils.hasText(String.valueOf(wxa).trim())) {
			Object wxapp = work.get("wxapp_appid");
			if (wxapp != null && StringUtils.hasText(String.valueOf(wxapp).trim())) {
				work.put("wxa_appid", String.valueOf(wxapp).trim());
			}
		}
		Object authApp = work.get("authorizer_appid");
		if (authApp == null || !StringUtils.hasText(String.valueOf(authApp).trim())) {
			Object woa = work.get("woa_appid");
			if (woa != null && StringUtils.hasText(String.valueOf(woa).trim())) {
				work.put("authorizer_appid", String.valueOf(woa).trim());
			}
		}
		if (!work.containsKey("sex")) {
			work.put("sex", 0);
		}
		if (!work.containsKey("avatar")) {
			Object head = work.get("headimgurl");
			work.put("avatar", head != null ? String.valueOf(head) : "");
		}
	}

	private static void putLongIfPresent(Map<String, Object> m, String key, Object v) {
		if (v == null) {
			return;
		}
		m.put(key, toLong(v));
	}

	private static void putTextIfPresent(Map<String, Object> m, String key, Object v) {
		if (v == null) {
			return;
		}
		String t = String.valueOf(v).trim();
		if (StringUtils.hasText(t)) {
			m.put(key, t);
		}
	}

	private Long resolveInviterId(Map<String, Object> work) {
		long companyId = toLong(work.get("company_id"));
		Object uidRaw = work.get("uid");
		if (hasTruthyId(uidRaw)) {
			long uid = toLong(uidRaw);
			Map<String, Object> mi = memberAccountService.getMemberInfo(uid, companyId);
			if (mi != null && !mi.isEmpty()) {
				return uid;
			}
			return null;
		}
		Object puidRaw = work.get("puid");
		if (hasTruthyId(puidRaw)) {
			long puid = toLong(puidRaw);
			Map<String, Object> mi = memberAccountService.getMemberInfo(puid, companyId);
			if (mi != null && !mi.isEmpty()) {
				return puid;
			}
		}
		return null;
	}

	private static boolean hasTruthyId(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof String s) {
			return StringUtils.hasText(s.trim()) && !"0".equals(s.trim());
		}
		if (raw instanceof Number n) {
			return n.longValue() != 0L;
		}
		String t = String.valueOf(raw).trim();
		return StringUtils.hasText(t) && !"0".equals(t);
	}

	private void assertWhitelist(long companyId, String purePhone) {
		Map<String, Object> cfg = whitelistSettingRedisService.getMergedConfig(companyId, Collections.emptyMap());
		Object st = cfg.get("whitelist_status");
		boolean on = Boolean.TRUE.equals(st) || "true".equals(String.valueOf(st));
		if (!on) {
			return;
		}
		String tips = Objects.toString(cfg.get("whitelist_tips"), "登录失败，手机号不在白名单内！").trim();
		String cipher = LegacyFixedMobileEncrypt.fixedEncryptMobile(purePhone);
		Long cnt = membersWhitelistMapper.selectCount(new LambdaQueryWrapper<MembersWhitelist>()
				.eq(MembersWhitelist::getCompanyId, companyId)
				.eq(MembersWhitelist::getMobile, cipher));
		if (cnt == null || cnt <= 0L) {
			throw new ResourceException(tips.isEmpty() ? "登录失败，手机号不在白名单内！" : tips);
		}
	}

	private static boolean isUploadMember(Map<String, Object> memberInfo) {
		Object opRaw = memberInfo.get("other_params");
		Object decoded = decodeOtherParams(opRaw);
		if (decoded instanceof Map<?, ?> om) {
			Object v = om.get("is_upload_member");
			return Boolean.TRUE.equals(v) || "true".equalsIgnoreCase(String.valueOf(v));
		}
		return false;
	}

	private static Object decodeOtherParams(Object opRaw) {
		if (opRaw == null) {
			return Collections.emptyMap();
		}
		if (opRaw instanceof Map<?, ?> m) {
			return m;
		}
		String s = String.valueOf(opRaw).trim();
		if (!StringUtils.hasText(s) || "[]".equals(s)) {
			return Collections.emptyMap();
		}
		try {
			return JSON.readValue(s, new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			return Collections.emptyMap();
		}
	}

	private static boolean looseEmptyPassword(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof Boolean b && !b) {
			return true;
		}
		if (raw instanceof Number n && n.longValue() == 0L) {
			return true;
		}
		String s = String.valueOf(raw).trim();
		return s.isEmpty() || "0".equals(s);
	}

	private static boolean looseEmptyVcode(Object raw) {
		return looseEmptyPassword(raw);
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
