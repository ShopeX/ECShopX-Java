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

package cn.shopex.ecshopx.selfservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.selfservice.domain.RegistrationRecord;
import cn.shopex.ecshopx.selfservice.dto.admin.v1.RegistrationRecordRegistrationVerifyCommand;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationRecordMapper;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.MessageSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/** ECX-10931：报名记录核销必须限定当前租户。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistrationRecordVerifyCompanyScopeTest {

	private RegistrationRecordMapper registrationRecordMapper;
	private RegistrationRecordRowMapAssembler rowMapAssembler;
	private MessageSource messageSource;
	private StringRedisTemplate companysRedisTemplate;
	private RegistrationRecordRegistrationVerifyService service;

	@BeforeEach
	void setUp() {
		registrationRecordMapper = mock(RegistrationRecordMapper.class);
		rowMapAssembler = mock(RegistrationRecordRowMapAssembler.class);
		messageSource = mock(MessageSource.class);
		companysRedisTemplate = mock(StringRedisTemplate.class);
		service =
				new RegistrationRecordRegistrationVerifyService(
						registrationRecordMapper, rowMapAssembler, messageSource, companysRedisTemplate);
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> valueOps = mock(ValueOperations.class);
		when(companysRedisTemplate.opsForValue()).thenReturn(valueOps);
		when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(Boolean.TRUE);
		when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
				.thenAnswer(inv -> inv.getArgument(0));
	}

	private static RegistrationRecord record(long recordId, long companyId, String status, Long verifyCode) {
		RegistrationRecord r = new RegistrationRecord();
		r.setRecordId(recordId);
		r.setCompanyId(companyId);
		r.setStatus(status);
		r.setVerifyCode(verifyCode);
		return r;
	}

	@Test
	void foreignCompanyRecord_isRejectedWithoutUpdate() {
		when(registrationRecordMapper.selectById(77L))
				.thenReturn(record(77L, 99L, "passed", 123456L));

		assertThatThrownBy(
						() ->
								service.registrationVerify(
										new RegistrationRecordRegistrationVerifyCommand(77L, 123456L),
										11L,
										"13900001001",
										Locale.CHINA))
				.isInstanceOf(ResourceException.class)
				.hasMessage("无权访问该报名记录");

		verify(registrationRecordMapper, never()).updateById(any(RegistrationRecord.class));
	}

	@Test
	void ownCompanyRecord_verifiesNormally() {
		RegistrationRecord own = record(77L, 11L, "passed", 123456L);
		when(registrationRecordMapper.selectById(77L)).thenReturn(own);
		when(registrationRecordMapper.updateById(any(RegistrationRecord.class))).thenReturn(1);
		when(rowMapAssembler.toApiRow(any(RegistrationRecord.class))).thenReturn(Map.of("record_id", 77L));

		Map<String, Object> out =
				service.registrationVerify(
						new RegistrationRecordRegistrationVerifyCommand(77L, 123456L),
						11L,
						"13900001001",
						Locale.CHINA);

		assertThat(out.get("record_id")).isEqualTo(77L);
		assertThat(own.getStatus()).isEqualTo("verified");
		assertThat(own.getVerifyOperator()).isEqualTo("13900001001");
		verify(registrationRecordMapper).updateById(any(RegistrationRecord.class));
	}

	@Test
	void missingRecord_keepsExistingMessage() {
		when(registrationRecordMapper.selectById(404L)).thenReturn(null);

		assertThatThrownBy(
						() ->
								service.registrationVerify(
										new RegistrationRecordRegistrationVerifyCommand(404L, 123456L),
										11L,
										"13900001001",
										Locale.CHINA))
				.isInstanceOf(ResourceException.class)
				.hasMessage("selfservice.registration_record.not_exist");
	}

	@Test
	void nonPassedStatus_keepsExistingMessage() {
		when(registrationRecordMapper.selectById(77L))
				.thenReturn(record(77L, 11L, "pending", 123456L));

		assertThatThrownBy(
						() ->
								service.registrationVerify(
										new RegistrationRecordRegistrationVerifyCommand(77L, 123456L),
										11L,
										"13900001001",
										Locale.CHINA))
				.isInstanceOf(ResourceException.class)
				.hasMessage("selfservice.registration_record.cannot_verify");
	}

	@Test
	void wrongVerifyCode_keepsExistingMessage() {
		when(registrationRecordMapper.selectById(77L))
				.thenReturn(record(77L, 11L, "passed", 123456L));

		assertThatThrownBy(
						() ->
								service.registrationVerify(
										new RegistrationRecordRegistrationVerifyCommand(77L, 999999L),
										11L,
										"13900001001",
										Locale.CHINA))
				.isInstanceOf(ResourceException.class)
				.hasMessage("selfservice.registration_record.verify_code_error");
	}
}
