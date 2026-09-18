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

package cn.shopex.ecshopx.members.service.admin;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.members.service.sms.MemberChinaMobileValidator;
import cn.shopex.ecshopx.members.service.sms.MemberSmsVerificationCodeDeliveryService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminMemberSmsCodeSendService {

	private final MemberAccountService memberAccountService;
	private final AdminMemberRegisterSettingService adminMemberRegisterSettingService;
	private final MemberSmsVerificationCodeDeliveryService memberSmsVerificationCodeDeliveryService;

	public AdminMemberSmsCodeSendService(
			MemberAccountService memberAccountService,
			AdminMemberRegisterSettingService adminMemberRegisterSettingService,
			MemberSmsVerificationCodeDeliveryService memberSmsVerificationCodeDeliveryService) {
		this.memberAccountService = memberAccountService;
		this.adminMemberRegisterSettingService = adminMemberRegisterSettingService;
		this.memberSmsVerificationCodeDeliveryService = memberSmsVerificationCodeDeliveryService;
	}

	public void getSmsCode(long companyId, String mobileRaw, String typeRaw, String tokenRaw, String yzmRaw) {
		String mobile = mobileRaw == null ? "" : mobileRaw.trim();

		String type = (typeRaw == null || typeRaw.trim().isEmpty()) ? "sign" : typeRaw.trim();

		MemberChinaMobileValidator.requireValidPlainMobile(mobile);

		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("mobile", mobile);
		Map<String, Object> row = memberAccountService.getMemberRowForAdminFilter(companyId, filter);
		if (row != null && !row.isEmpty() && "sign".equals(type)) {
			throw new ResourceException("该手机号已注册");
		}

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

		memberSmsVerificationCodeDeliveryService.deliver(companyId, mobile, type);
	}
}
