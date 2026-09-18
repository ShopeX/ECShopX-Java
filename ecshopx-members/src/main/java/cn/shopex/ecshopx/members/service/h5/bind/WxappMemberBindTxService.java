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

package cn.shopex.ecshopx.members.service.h5.bind;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.members.service.reg.MemberRegSettingService;
import cn.shopex.ecshopx.members.service.trustlogin.SocialTrustLoginTypes;
import cn.shopex.ecshopx.members.domain.Members;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class WxappMemberBindTxService {

	private static final Logger log = LoggerFactory.getLogger(WxappMemberBindTxService.class);

	private static final Pattern MOBILE_CN = Pattern.compile("^1[3456789][0-9]{9}$");

	/** Alphanumeric only (letters and digits); combined length enforced separately. */
	private static final Pattern PASSWORD_ALPHA_NUM = Pattern.compile("^[a-zA-Z0-9]+$");

	/** Numeric string of 6–16 digits inclusive. */
	private static final Pattern PASSWORD_DIGITS_6_16 = Pattern.compile("^\\d{6,16}$");

	private final MemberAccountService memberAccountService;

	private final MemberRegSettingService memberRegSettingService;

	public WxappMemberBindTxService(
			MemberAccountService memberAccountService,
			MemberRegSettingService memberRegSettingService) {
		this.memberAccountService = memberAccountService;
		this.memberRegSettingService = memberRegSettingService;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> bindInTransaction(Map<String, Object> params) {
		try {
			validateParams(params);
			String userType = stringVal(params.get("user_type")).trim();
			if (SocialTrustLoginTypes.isSocial(userType)) {
				return bindSocialMember(params, userType);
			}

			long companyId = parsePositiveLong(params.get("company_id"));
			String mobile = stringVal(params.get("username")).trim();
			String unionId = stringVal(params.get("union_id")).trim();
			String checkType = stringVal(params.get("check_type")).trim();
			String vcode = stringVal(params.get("vcode")).trim();
			String password = params.get("password") == null ? "" : stringVal(params.get("password")).trim();

			if (StringUtils.hasText(checkType)) {
				if (!memberRegSettingService.checkSmsVcode(mobile, companyId, vcode, checkType)) {
					throw new ResourceException("短信验证码错误");
				}
			}

			Map<String, Object> wxFilter = new HashMap<>();
			wxFilter.put("company_id", companyId);
			wxFilter.put("unionid", unionId);
			Map<String, Object> wx = memberAccountService.getWechatUserInfo(wxFilter);
			if (wx.isEmpty()) {
				throw new ResourceException("用户的微信信息有误！");
			}

			String unionidForAssoc = stringVal(wx.get("unionid")).trim();
			String openIdForToken = stringVal(wx.get("open_id")).trim();

			Members existing = memberAccountService.findMemberByCompanyAndMobile(companyId, mobile);
			if (existing == null) {
				Map<String, Object> created =
						memberAccountService.createMemberForWxappBind(
								companyId, mobile, password, wx, params);
				Map<String, Object> out = new HashMap<>();
				out.put("user_id", created.get("user_id"));
				out.put("company_id", companyId);
				out.put("is_new", 1);
				out.put("unionid", unionidForAssoc);
				out.put("open_id", openIdForToken);
				out.put("mobile", mobile);
				return out;
			}

			if (StringUtils.hasText(password)
					&& !memberAccountService.passwordMatches(password, existing.getPassword())) {
				throw new ResourceException("用户名或密码错误");
			}

			long userId = existing.getUserId();
			Map<String, Object> memberRow = memberAccountService.getMemberInfo(userId, companyId);
			if (memberRow == null || memberRow.isEmpty()) {
				throw new ResourceException("未知错误！");
			}
			memberAccountService.createMemberWechatAssociation(companyId, userId, unionidForAssoc);

			Map<String, Object> out = new HashMap<>();
			out.put("user_id", userId);
			out.put("company_id", companyId);
			out.put("is_new", 0);
			out.put("unionid", unionidForAssoc);
			out.put("open_id", openIdForToken);
			out.put("mobile", mobile);
			return out;
		} catch (ResourceException e) {
			throw e;
		} catch (Throwable t) {
			log.info("user_bind_member_error: {}", t.getMessage(), t);
			throw new ResourceException("未知错误！");
		}
	}

	private Map<String, Object> bindSocialMember(Map<String, Object> params, String userType) {
		long companyId = parsePositiveLong(params.get("company_id"));
		String mobile = stringVal(params.get("username")).trim();
		String unionid = stringVal(params.get("union_id")).trim();
		String checkType = stringVal(params.get("check_type")).trim();
		String vcode = stringVal(params.get("vcode")).trim();
		String password = params.get("password") == null ? "" : stringVal(params.get("password")).trim();

		if (StringUtils.hasText(checkType)) {
			if (!memberRegSettingService.checkSmsVcode(mobile, companyId, vcode, checkType)) {
				throw new ResourceException("短信验证码错误！");
			}
		}

		Members existing = memberAccountService.findMemberByCompanyAndMobile(companyId, mobile);
		if (existing == null) {
			Map<String, Object> postData = new HashMap<>();
			postData.put("mobile", mobile);
			postData.put("region_mobile", mobile);
			postData.put("mobile_country_code", "86");
			postData.put("company_id", companyId);
			postData.put("wxa_appid", "");
			postData.put("authorizer_appid", "");
			postData.put("sex", 0);
			postData.put("username", stringVal(params.get("nickname")));
			postData.put("avatar", stringVal(params.get("avatar")));
			postData.put("email", "");
			postData.put("password", password);
			postData.put("api_from", "h5app");
			postData.put("auth_type", "social_oauth");
			postData.put("user_type", userType);
			postData.put("unionid", unionid);
			postData.put("open_id", unionid);
			postData.put("force_password", 0);
			Map<String, Object> created = memberAccountService.creatMemberForH5Post(postData, false);
			Map<String, Object> out = new HashMap<>();
			out.put("user_id", created.get("user_id"));
			out.put("company_id", companyId);
			out.put("is_new", 1);
			out.put("unionid", unionid);
			out.put("open_id", unionid);
			out.put("mobile", mobile);
			return out;
		}

		if (StringUtils.hasText(password)
				&& !memberAccountService.passwordMatches(password, existing.getPassword())) {
			throw new ResourceException("账号或密码错误");
		}

		long userId = existing.getUserId();
		memberAccountService.createMemberPlatformAssociation(companyId, userId, unionid, userType);
		Map<String, Object> out = new HashMap<>();
		out.put("user_id", userId);
		out.put("company_id", companyId);
		out.put("is_new", 0);
		out.put("unionid", unionid);
		out.put("open_id", unionid);
		out.put("mobile", mobile);
		return out;
	}

	private void validateParams(Map<String, Object> params) {
		String username = stringVal(params.get("username")).trim();
		if (!StringUtils.hasText(username)) {
			throw new ResourceException("手机号必填！");
		}
		if (!MOBILE_CN.matcher(username).matches()) {
			throw new ResourceException("手机号格式有误！");
		}
		String unionId = stringVal(params.get("union_id")).trim();
		if (!StringUtils.hasText(unionId)) {
			throw new ResourceException("参数有误！");
		}

		Object passwordRaw = params.get("password");
		String password = passwordRaw == null ? "" : stringVal(passwordRaw).trim();
		boolean hasPassword = StringUtils.hasText(password);
		if (!hasPassword) {
			String checkType = stringVal(params.get("check_type")).trim();
			String vcode = stringVal(params.get("vcode")).trim();
			if (!StringUtils.hasText(checkType) || !StringUtils.hasText(vcode)) {
				throw new ResourceException("短信验证码必填！");
			}
		} else {
			if (!PASSWORD_ALPHA_NUM.matcher(password).matches()) {
				throw new ResourceException("validation.alpha_num");
			}
			if (!PASSWORD_DIGITS_6_16.matcher(password).matches()) {
				throw new ResourceException("validation.digits_between");
			}
		}
	}

	private static long parsePositiveLong(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o);
	}
}
