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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.employeepurchase.mapper.EmployeeFrontCheckQueryMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.dto.EmployeeEmailVcodeEnterpriseRow;
import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeEmailVcodeSendServiceSecureRandomTest {

	@Mock private EmployeeFrontCheckQueryMapper employeeFrontCheckQueryMapper;
	@Mock private EmployeePurchaseEmailVcodeRedisService emailVcodeRedisService;
	@Mock private EmployeeEmailVcodeMailGateway mailGateway;

	private EmployeeEmailVcodeSendService service;

	@BeforeEach
	void setUp() {
		service =
				new EmployeeEmailVcodeSendService(
						employeeFrontCheckQueryMapper, emailVcodeRedisService, mailGateway);
	}

	@Test
	@DisplayName("生成器为 SecureRandom 且产出 6 位数字码")
	void generateSixDigitVcode_usesSecureRandomAndSixDigits() throws Exception {
		Field field = EmployeeEmailVcodeSendService.class.getDeclaredField("SECURE_RANDOM");
		field.setAccessible(true);
		Object random = field.get(null);
		assertThat(random).isInstanceOf(SecureRandom.class);
		assertThat(random).isNotInstanceOf(ThreadLocalRandom.class);

		for (int i = 0; i < 20; i++) {
			String code = EmployeeEmailVcodeSendService.generateSixDigitVcode();
			assertThat(code).matches("\\d{6}");
			int n = Integer.parseInt(code);
			assertThat(n).isBetween(100000, 999999);
		}
	}

	@Test
	@DisplayName("发送路径存入的验证码为 6 位数字")
	void sendEmailVcode_storesSixDigitCode() {
		when(employeeFrontCheckQueryMapper.selectFirstEnterpriseForEmailVcode(
						eq(11L), eq("@example.com"), eq(null), eq(7L)))
				.thenReturn(readyEnterprise());
		when(emailVcodeRedisService.hasSendCooldown(11L, "a@example.com")).thenReturn(false);
		when(emailVcodeRedisService.nextDailyEmailSendCount(11L, "a@example.com")).thenReturn(1L);
		when(emailVcodeRedisService.nextDailyIpSendCount(11L, "203.0.113.8")).thenReturn(1L);
		when(mailGateway.send(any(), anyString(), anyString())).thenReturn(true);

		service.sendEmailVcode(11L, "a@example.com", 0, 7L, "203.0.113.8");

		ArgumentCaptor<String> codeCap = ArgumentCaptor.forClass(String.class);
		verify(emailVcodeRedisService).storeVcodeWithTtl(eq("a@example.com"), codeCap.capture(), eq(1800L));
		assertThat(codeCap.getValue()).matches("\\d{6}");
		assertThat(Integer.parseInt(codeCap.getValue())).isBetween(100000, 999999);
	}

	private static EmployeeEmailVcodeEnterpriseRow readyEnterprise() {
		EmployeeEmailVcodeEnterpriseRow row = new EmployeeEmailVcodeEnterpriseRow();
		row.setAuthType("email");
		row.setRelayHost("smtp.example.com");
		row.setSmtpPort("465");
		row.setEmailUser("noreply@example.com");
		row.setEmailPassword("secret");
		return row;
	}
}
