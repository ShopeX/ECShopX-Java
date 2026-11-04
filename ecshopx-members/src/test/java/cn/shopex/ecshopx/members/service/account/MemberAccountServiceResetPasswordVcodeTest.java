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

package cn.shopex.ecshopx.members.service.account;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.MembersCreateMemberSuccessDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.members.admin.AdminMemberCreateDistributorUserSideEffectPort;
import cn.shopex.ecshopx.common.members.admin.AdminMemberCreatePromoterSideEffectPort;
import cn.shopex.ecshopx.companys.service.protocol.ShopProtocolSetService;
import cn.shopex.ecshopx.members.dispatch.BindSalsepersonJobDispatchPublisher;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.mapper.MembersDeleteRecordMapper;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.mapper.ShopRelMemberMapper;
import cn.shopex.ecshopx.members.mapper.WechatUsersMapper;
import cn.shopex.ecshopx.members.service.h5.bind.ShoppingGuideForH5BindLookup;
import cn.shopex.ecshopx.members.service.reg.MemberRegSettingService;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmSettingReadPort;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatRelMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class MemberAccountServiceResetPasswordVcodeTest {

	@Mock MembersMapper membersMapper;
	@Mock MembersInfoMapper membersInfoMapper;
	@Mock WechatUsersMapper wechatUsersMapper;
	@Mock MembersAssociationsMapper membersAssociationsMapper;
	@Mock MembersCreateMemberSuccessDispatchPublisher membersCreateMemberSuccessDispatchPublisher;
	@Mock BindSalsepersonJobDispatchPublisher bindSalsepersonJobDispatchPublisher;
	@Mock StringRedisTemplate sharedStringRedisTemplate;
	@Mock ShopProtocolSetService shopProtocolSetService;
	@Mock MembersDeleteRecordMapper membersDeleteRecordMapper;
	@Mock AdminMemberCreatePromoterSideEffectPort adminMemberCreatePromoterSideEffectPort;
	@Mock AdminMemberCreateDistributorUserSideEffectPort adminMemberCreateDistributorUserSideEffectPort;
	@Mock ShoppingGuideForH5BindLookup shoppingGuideForH5BindLookup;
	@Mock WorkWechatRelMapper workWechatRelMapper;
	@Mock ShopRelMemberMapper shopRelMemberMapper;
	@Mock MemberRegSettingService memberRegSettingService;
	@Mock DmCrmSettingReadPort dmCrmSettingReadPort;

	MemberAccountService service;

	@BeforeEach
	void setUp() {
		service =
				new MemberAccountService(
						membersMapper,
						membersInfoMapper,
						wechatUsersMapper,
						membersAssociationsMapper,
						membersCreateMemberSuccessDispatchPublisher,
						bindSalsepersonJobDispatchPublisher,
						sharedStringRedisTemplate,
						shopProtocolSetService,
						membersDeleteRecordMapper,
						adminMemberCreatePromoterSideEffectPort,
						adminMemberCreateDistributorUserSideEffectPort,
						shoppingGuideForH5BindLookup,
						workWechatRelMapper,
						shopRelMemberMapper,
						memberRegSettingService,
						dmCrmSettingReadPort);
	}

	@Test
	void resetMemberPassword_withoutVcode_requiresSmsCheck_andDoesNotUpdate() {
		Members existing = new Members();
		existing.setUserId(42L);
		existing.setCompanyId(1L);
		existing.setMobile("enc-mobile");
		when(membersMapper.selectOne(any())).thenReturn(existing);
		when(memberRegSettingService.checkSmsVcode(eq("13800138000"), eq(1L), eq(""), eq("forgot_password")))
				.thenThrow(new ResourceException("验证码必填"));

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("mobile", "13800138000");
		merged.put("password", "abc123");

		assertThatThrownBy(() -> service.resetMemberPassword(1L, 0L, merged))
				.isInstanceOf(ResourceException.class)
				.hasMessage("验证码必填");

		verify(memberRegSettingService)
				.checkSmsVcode(eq("13800138000"), eq(1L), eq(""), eq("forgot_password"));
		verify(membersMapper, never()).updateById(any(Members.class));
	}

	@Test
	void resetMemberPassword_authenticatedWithoutVcode_stillRequiresSmsCheck() {
		Members existing = new Members();
		existing.setUserId(42L);
		existing.setCompanyId(1L);
		existing.setMobile("13800138000");
		when(membersMapper.selectOne(any())).thenReturn(existing);
		when(memberRegSettingService.checkSmsVcode(eq("13800138000"), eq(1L), eq(""), eq("forgot_password")))
				.thenThrow(new ResourceException("验证码必填"));

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("mobile", "13800138000");
		merged.put("password", "abc123");

		assertThatThrownBy(() -> service.resetMemberPassword(1L, 42L, merged))
				.isInstanceOf(ResourceException.class)
				.hasMessage("验证码必填");

		verify(memberRegSettingService)
				.checkSmsVcode(eq("13800138000"), eq(1L), eq(""), eq("forgot_password"));
		verify(membersMapper, never()).updateById(any(Members.class));
	}

	@Test
	void resetMemberPassword_wrongVcode_rejectsWithoutUpdate() {
		Members existing = new Members();
		existing.setUserId(42L);
		existing.setCompanyId(1L);
		existing.setMobile("enc-mobile");
		when(membersMapper.selectOne(any())).thenReturn(existing);
		when(memberRegSettingService.checkSmsVcode(
						eq("13800138000"), eq(1L), eq("000000"), eq("forgot_password")))
				.thenReturn(false);

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("mobile", "13800138000");
		merged.put("password", "abc123");
		merged.put("vcode", "000000");

		assertThatThrownBy(() -> service.resetMemberPassword(1L, 0L, merged))
				.isInstanceOf(ResourceException.class)
				.hasMessage("短信验证码错误");

		verify(membersMapper, never()).updateById(any(Members.class));
	}
}
