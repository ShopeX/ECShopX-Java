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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.selfservice.domain.RegistrationRecord;
import cn.shopex.ecshopx.selfservice.dto.admin.v1.RegistrationRecordRegistrationVerifyCommand;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationRecordMapper;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RegistrationRecordRegistrationVerifyService {

	private final RegistrationRecordMapper registrationRecordMapper;
	private final RegistrationRecordRowMapAssembler registrationRecordRowMapAssembler;
	private final MessageSource messageSource;
	private final StringRedisTemplate companysRedisTemplate;

	public RegistrationRecordRegistrationVerifyService(
			RegistrationRecordMapper registrationRecordMapper,
			RegistrationRecordRowMapAssembler registrationRecordRowMapAssembler,
			MessageSource messageSource,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.registrationRecordMapper = registrationRecordMapper;
		this.registrationRecordRowMapAssembler = registrationRecordRowMapAssembler;
		this.messageSource = messageSource;
		this.companysRedisTemplate = companysRedisTemplate;
	}

	public Map<String, Object> registrationVerify(
			RegistrationRecordRegistrationVerifyCommand cmd, String verifyOperatorMobile, Locale locale) {
		String key = "registrationVerify:" + Long.toString(cmd.recordId());
		Boolean ok = companysRedisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(3));
		if (!Boolean.TRUE.equals(ok)) {
			throw new ResourceException(
					messageSource.getMessage("selfservice.registration_record.operation_too_frequent", null, locale));
		}

		RegistrationRecord entity = registrationRecordMapper.selectById(cmd.recordId());
		if (entity == null) {
			throw new ResourceException(messageSource.getMessage("selfservice.registration_record.not_exist", null, locale));
		}
		if (!"passed".equals(entity.getStatus())) {
			throw new ResourceException(
					messageSource.getMessage("selfservice.registration_record.cannot_verify", null, locale));
		}
		if (!Objects.equals(entity.getVerifyCode(), cmd.verifyCode())) {
			throw new ResourceException(
					messageSource.getMessage("selfservice.registration_record.verify_code_error", null, locale));
		}

		long epochSecond = java.time.Instant.now().getEpochSecond();
		entity.setStatus("verified");
		entity.setVerifyTime(epochSecond);
		entity.setVerifyOperator(verifyOperatorMobile == null ? "" : verifyOperatorMobile);
		entity.setUpdated((int) epochSecond);

		int rows = registrationRecordMapper.updateById(entity);
		if (rows == 0) {
			throw new ResourceException(
					messageSource.getMessage("selfservice.registration_record.no_update_data_found", null, locale));
		}

		return registrationRecordRowMapAssembler.toApiRow(entity);
	}
}
