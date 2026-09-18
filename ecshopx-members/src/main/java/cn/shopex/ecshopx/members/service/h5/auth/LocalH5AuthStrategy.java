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

package cn.shopex.ecshopx.members.service.h5.auth;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.config.H5LocalProperties;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.members.service.email.MemberEmailVerificationService;
import cn.shopex.ecshopx.members.service.h5.H5GenericUser;
import cn.shopex.ecshopx.members.service.reg.MemberRegSettingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class LocalH5AuthStrategy implements H5AuthStrategy {

	private final H5LocalProperties h5LocalProperties;

	private final MemberAccountService memberAccountService;

	private final MemberRegSettingService memberRegSettingService;

	private final MemberEmailVerificationService memberEmailVerificationService;

	private final MembersMapper membersMapper;

	public LocalH5AuthStrategy(
			H5LocalProperties h5LocalProperties,
			MemberAccountService memberAccountService,
			MemberRegSettingService memberRegSettingService,
			MemberEmailVerificationService memberEmailVerificationService,
			MembersMapper membersMapper) {
		this.h5LocalProperties = h5LocalProperties;
		this.memberAccountService = memberAccountService;
		this.memberRegSettingService = memberRegSettingService;
		this.memberEmailVerificationService = memberEmailVerificationService;
		this.membersMapper = membersMapper;
	}

	@Override
	public H5AuthType type() {
		return H5AuthType.LOCAL;
	}

	@Override
	public Optional<H5GenericUser> resolve(Map<String, Object> credentials) {
		long companyId = resolveCompanyId(credentials);
		if (companyId <= 0) {
			throw new ResourceException("缺少企业信息");
		}
		String mobile = stringVal(credentials.get("username"));
		if (!StringUtils.hasText(mobile)) {
			throw new ResourceException("用户名或密码错误");
		}
		String password = stringVal(credentials.get("password"));
		String checkType = stringVal(credentials.get("check_type"));
		if (!StringUtils.hasText(checkType)) {
			checkType = "password";
		}
		String vcode = stringVal(credentials.get("vcode"));
		boolean autoRegister = boolVal(credentials.get("auto_register"));
		boolean silent = boolVal(credentials.get("silent"));

		boolean isEmail = MemberEmailVerificationService.isValidEmail(mobile);
		Members userEntity = isEmail
				? findMemberByCompanyAndLoginEmail(companyId, mobile)
				: memberAccountService.findMemberByCompanyAndMobile(companyId, mobile);

		switch (checkType) {
			case "mobile":
				if (isEmail) {
					throw new ResourceException("验证类型有误！");
				}
				if (!memberRegSettingService.checkSmsVcode(mobile, companyId, vcode, "login")) {
					throw new ResourceException("短信验证码错误");
				}
				break;
			case "email_otp":
				if (!isEmail) {
					throw new ResourceException("验证类型有误！");
				}
				if (userEntity == null) {
					throw new ResourceException("该邮箱未注册");
				}
				if (userEntity.getEmailVerifiedAt() == null) {
					throw new ResourceException("邮箱未验证，请先完成验证");
				}
				if (!memberEmailVerificationService.consumeCode(
						companyId, mobile, MemberEmailVerificationService.PURPOSE_LOGIN, vcode)) {
					throw new ResourceException("邮箱验证码错误");
				}
				break;
			case "password":
				if (userEntity != null && isEmail && userEntity.getEmailVerifiedAt() == null) {
					throw new ResourceException("邮箱未验证，请先完成验证");
				}
				if (userEntity != null && !memberAccountService.passwordMatches(password, userEntity.getPassword())) {
					throw new ResourceException("用户名或密码错误");
				}
				break;
			default:
				throw new ResourceException("验证类型有误！");
		}

		Map<String, Object> userInfo;
		if (userEntity == null) {
			if (autoRegister) {
				userInfo = memberAccountService.createMemberLocalAutoRegister(companyId, mobile, password);
			} else {
				if (!silent) {
					throw new ResourceException("手机号码未注册，请注册后登陆");
				}
				userInfo = new HashMap<>();
				userInfo.put("user_id", null);
				userInfo.put("company_id", companyId);
				userInfo.put("is_new", 1);
			}
		} else {
			userInfo = new HashMap<>();
			userInfo.put("user_id", userEntity.getUserId());
			userInfo.put("company_id", companyId);
			userInfo.put("is_new", 0);
		}

		if (userInfo.get("user_id") != null) {
			userInfo.put("mobile", mobile);
		}

		Map<String, Object> tokenData = memberAccountService.getTokenData(userInfo);
		tokenData.put("operator_type", "user");
		return Optional.of(new H5GenericUser(tokenData));
	}

	private Members findMemberByCompanyAndLoginEmail(long companyId, String username) {
		String normalized = memberEmailVerificationService.normalizeEmail(username);
		if (normalized.isEmpty()) {
			return null;
		}
		return membersMapper.selectOne(new LambdaQueryWrapper<Members>()
				.eq(Members::getCompanyId, companyId)
				.eq(Members::getLoginEmail, normalized)
				.last("LIMIT 1"));
	}

	private long resolveCompanyId(Map<String, Object> credentials) {
		if (!h5LocalProperties.isSystemIsSaas()) {
			return parseLongSafe(h5LocalProperties.getSystemCompanysId());
		}
		if (StringUtils.hasText(h5LocalProperties.getSystemMainCompanysId())) {
			return parseLongSafe(h5LocalProperties.getSystemMainCompanysId());
		}
		Object cid = credentials.get("company_id");
		if (cid == null) {
			return 0L;
		}
		return toLong(cid);
	}

	private static boolean boolVal(Object o) {
		if (o instanceof Boolean b) {
			return b;
		}
		if (o == null) {
			return false;
		}
		return "1".equals(String.valueOf(o)) || "true".equalsIgnoreCase(String.valueOf(o));
	}

	private static long parseLongSafe(String s) {
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(o));
	}
}
