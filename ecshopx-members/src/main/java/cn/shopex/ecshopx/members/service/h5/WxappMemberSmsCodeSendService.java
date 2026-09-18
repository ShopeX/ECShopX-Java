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

package cn.shopex.ecshopx.members.service.h5;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.members.service.admin.AdminMemberRegisterSettingService;
import cn.shopex.ecshopx.members.service.sms.MemberChinaMobileValidator;
import cn.shopex.ecshopx.members.service.sms.MemberSmsVerificationCodeDeliveryService;
import cn.shopex.ecshopx.members.service.support.MemberRowOtherParams;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappMemberSmsCodeSendService {

	private static final Set<String> CODE_TYPES =
			Set.of("sign", "forgot_password", "login", "update", "merchant_login");

	private final MemberAccountService memberAccountService;
	private final AdminMemberRegisterSettingService adminMemberRegisterSettingService;
	private final MembersAssociationsMapper membersAssociationsMapper;
	private final MemberSmsVerificationCodeDeliveryService memberSmsVerificationCodeDeliveryService;

	public WxappMemberSmsCodeSendService(
			MemberAccountService memberAccountService,
			AdminMemberRegisterSettingService adminMemberRegisterSettingService,
			MembersAssociationsMapper membersAssociationsMapper,
			MemberSmsVerificationCodeDeliveryService memberSmsVerificationCodeDeliveryService) {
		this.memberAccountService = memberAccountService;
		this.adminMemberRegisterSettingService = adminMemberRegisterSettingService;
		this.membersAssociationsMapper = membersAssociationsMapper;
		this.memberSmsVerificationCodeDeliveryService = memberSmsVerificationCodeDeliveryService;
	}

	public void getSmsCode(
			long companyId,
			long authUserId,
			Map<String, Object> claims,
			String mobileRaw,
			String typeRaw,
			String tokenRaw,
			String yzmRaw) {
		String type = (typeRaw == null || typeRaw.isBlank()) ? "sign" : typeRaw.trim();
		if (!CODE_TYPES.contains(type)) {
			throw new ResourceException("手机验证码类型错误");
		}

		Map<String, Object> memberRow;
		if (authUserId > 0L && "forgot_password".equals(type)) {
			LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
			filter.put("user_id", Long.valueOf(authUserId));
			memberRow = memberAccountService.getMemberRowForAdminFilter(companyId, filter);
		} else {
			String phoneForLookup = mobileRaw == null ? "" : mobileRaw.trim();
			LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
			filter.put("mobile", phoneForLookup);
			memberRow = memberAccountService.getMemberRowForAdminFilter(companyId, filter);
		}

		if (memberRow != null && !memberRow.isEmpty() && "sign".equals(type)) {
			if (!MemberRowOtherParams.isUploadMember(memberRow)) {
				throw new ResourceException("该手机号已注册");
			}
		}

		if ((memberRow == null || memberRow.isEmpty()) && "forgot_password".equals(type)) {
			throw new ResourceException("该手机号未注册");
		}

		String phoneToSend;
		if (authUserId > 0L && "forgot_password".equals(type)) {
			Object rawMobile = memberRow.get("mobile");
			phoneToSend = (rawMobile == null) ? "" : rawMobile.toString().trim();
		} else {
			phoneToSend = mobileRaw == null ? "" : mobileRaw.trim();
		}

		if ("update".equals(type)) {
			if (!StringUtils.hasText(phoneToSend) || !phoneToSend.matches("^1\\d{10}$")) {
				throw new ResourceException("登录有误,请重新登录", 403);
			}
		} else {
			MemberChinaMobileValidator.requireValidPlainMobile(phoneToSend);
		}

		if (!"update".equals(type)) {
			String token = tokenRaw == null ? "" : tokenRaw.trim();
			String yzm = yzmRaw == null ? "" : yzmRaw.trim();
			if (!StringUtils.hasText(token)) {
				throw new BadRequestException("请输入图片验证码token");
			}
			if (!StringUtils.hasText(yzm)) {
				throw new BadRequestException("请输入图片验证码");
			}
			if (!adminMemberRegisterSettingService.verifyAndConsumeMemberImageVcode(token, companyId, yzm, type)) {
				throw new ResourceException("图片验证码错误");
			}
		}

		if ("sign".equals(type)) {
			String unionid = Objects.toString(claims.getOrDefault("unionid", ""), "").trim();
			MembersAssociations assoc =
					membersAssociationsMapper.selectOne(
							new LambdaQueryWrapper<MembersAssociations>()
									.eq(MembersAssociations::getUserId, authUserId)
									.eq(MembersAssociations::getUnionid, unionid)
									.eq(MembersAssociations::getCompanyId, companyId)
									.eq(MembersAssociations::getUserType, "baidu")
									.last("LIMIT 1"));
			if (assoc != null && !MemberRowOtherParams.isUploadMember(memberRow)) {
				throw new ResourceException("该账号已绑定手机号");
			}
		}

		memberSmsVerificationCodeDeliveryService.deliver(companyId, phoneToSend, type);
	}
}
