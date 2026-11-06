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

package cn.shopex.ecshopx.employeepurchase.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeeFrontCheckQueryMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.dto.EmployeeEmailVcodeEnterpriseRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ECX-10950：员工邮箱验证码在发信前限制冷却、当日次数和请求 IP。 */
@ExtendWith(MockitoExtension.class)
class EmployeeEmailVcodeSendServiceRateLimitTest {

	@Mock
	private EmployeeFrontCheckQueryMapper employeeFrontCheckQueryMapper;

	@Mock
	private EmployeePurchaseEmailVcodeRedisService emailVcodeRedisService;

	@Mock
	private EmployeeEmailVcodeMailGateway mailGateway;

	private EmployeeEmailVcodeSendService service;

	@BeforeEach
	void setUp() {
		service =
				new EmployeeEmailVcodeSendService(
						employeeFrontCheckQueryMapper, emailVcodeRedisService, mailGateway);
	}

	@Test
	void cooldown_rejectsBeforeStoringCode() {
		when(employeeFrontCheckQueryMapper.selectFirstEnterpriseForEmailVcode(11L, "@Example.com", null, 7L))
				.thenReturn(readyEnterprise());
		when(emailVcodeRedisService.hasSendCooldown(11L, "a@example.com")).thenReturn(true);

		assertThatThrownBy(() -> service.sendEmailVcode(11L, "A@Example.com", 0, 7L, "203.0.113.8"))
				.isInstanceOf(ResourceException.class)
				.hasMessage("发送过于频繁，请稍后再试");

		verify(emailVcodeRedisService, never()).storeVcodeWithTtl(anyString(), anyString(), anyLong());
	}

	@Test
	void sixthEmailSendOfTheDay_rejectsBeforeStoringCode() {
		when(employeeFrontCheckQueryMapper.selectFirstEnterpriseForEmailVcode(11L, "@Example.com", null, 7L))
				.thenReturn(readyEnterprise());
		when(emailVcodeRedisService.hasSendCooldown(11L, "a@example.com")).thenReturn(false);
		when(emailVcodeRedisService.nextDailyEmailSendCount(11L, "a@example.com")).thenReturn(6L);

		assertThatThrownBy(() -> service.sendEmailVcode(11L, "A@Example.com", 0, 7L, "203.0.113.8"))
				.isInstanceOf(ResourceException.class)
				.hasMessage("该邮箱今日发送次数已达上限");

		verify(emailVcodeRedisService, never()).storeVcodeWithTtl(anyString(), anyString(), anyLong());
		verify(emailVcodeRedisService, never()).rememberSendCooldown(11L, "a@example.com");
	}

	@Test
	void twoHundredFirstIpSendOfTheDay_rejectsBeforeStoringCode() {
		when(employeeFrontCheckQueryMapper.selectFirstEnterpriseForEmailVcode(11L, "@Example.com", null, 7L))
				.thenReturn(readyEnterprise());
		when(emailVcodeRedisService.hasSendCooldown(11L, "a@example.com")).thenReturn(false);
		when(emailVcodeRedisService.nextDailyEmailSendCount(11L, "a@example.com")).thenReturn(1L);
		when(emailVcodeRedisService.nextDailyIpSendCount(11L, "203.0.113.8")).thenReturn(201L);

		assertThatThrownBy(() -> service.sendEmailVcode(11L, "A@Example.com", 0, 7L, "203.0.113.8"))
				.isInstanceOf(ResourceException.class)
				.hasMessage("请求过于频繁，请稍后再试");

		verify(emailVcodeRedisService, never()).storeVcodeWithTtl(anyString(), anyString(), anyLong());
		verify(emailVcodeRedisService, never()).rememberSendCooldown(11L, "a@example.com");
		verify(mailGateway, never()).send(any(), anyString(), anyString());
	}

	@Test
	void allowedSend_recordsCooldownBeforeMail() {
		when(employeeFrontCheckQueryMapper.selectFirstEnterpriseForEmailVcode(11L, "@Example.com", null, 7L))
				.thenReturn(readyEnterprise());
		when(emailVcodeRedisService.hasSendCooldown(11L, "a@example.com")).thenReturn(false);
		when(emailVcodeRedisService.nextDailyEmailSendCount(11L, "a@example.com")).thenReturn(5L);
		when(emailVcodeRedisService.nextDailyIpSendCount(11L, "203.0.113.8")).thenReturn(200L);
		when(mailGateway.send(any(), eq("A@Example.com"), anyString())).thenReturn(true);

		assertThat(service.sendEmailVcode(11L, "A@Example.com", 0, 7L, "203.0.113.8")).isTrue();

		InOrder order = inOrder(emailVcodeRedisService, mailGateway);
		order.verify(emailVcodeRedisService).rememberSendCooldown(11L, "a@example.com");
		order.verify(emailVcodeRedisService).storeVcodeWithTtl(eq("A@Example.com"), anyString(), eq(1800L));
		order.verify(mailGateway).send(any(), eq("A@Example.com"), anyString());
	}

	@Test
	void blankIp_skipsIpCountAndStillRecordsCooldown() {
		when(employeeFrontCheckQueryMapper.selectFirstEnterpriseForEmailVcode(11L, "@Example.com", null, 7L))
				.thenReturn(readyEnterprise());
		when(emailVcodeRedisService.hasSendCooldown(11L, "a@example.com")).thenReturn(false);
		when(emailVcodeRedisService.nextDailyEmailSendCount(11L, "a@example.com")).thenReturn(1L);
		when(mailGateway.send(any(), eq("A@Example.com"), anyString())).thenReturn(false);

		assertThat(service.sendEmailVcode(11L, "A@Example.com", 0, 7L, "  ")).isFalse();

		verify(emailVcodeRedisService, never()).nextDailyIpSendCount(anyLong(), anyString());
		verify(emailVcodeRedisService).rememberSendCooldown(11L, "a@example.com");
	}

	private static EmployeeEmailVcodeEnterpriseRow readyEnterprise() {
		EmployeeEmailVcodeEnterpriseRow row = new EmployeeEmailVcodeEnterpriseRow();
		row.setAuthType("email");
		row.setRelayHost("smtp.example.com");
		row.setSmtpPort("465");
		row.setEmailUser("hr@example.com");
		row.setEmailPassword("secret");
		return row;
	}
}
