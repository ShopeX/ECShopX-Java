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
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivity;
import cn.shopex.ecshopx.selfservice.domain.RegistrationRecord;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityMapper;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationRecordMapper;
import cn.shopex.ecshopx.selfservice.support.RegistrationActivityFrontCancelRecordMessageKeys;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
public class RegistrationActivityFrontCancelRecordService {

	private final RegistrationRecordMapper registrationRecordMapper;
	private final RegistrationActivityMapper registrationActivityMapper;
	private final RegistrationRecordRowMapAssembler registrationRecordRowMapAssembler;
	private final MessageSource messageSource;

	public RegistrationActivityFrontCancelRecordService(
			RegistrationRecordMapper registrationRecordMapper,
			RegistrationActivityMapper registrationActivityMapper,
			RegistrationRecordRowMapAssembler registrationRecordRowMapAssembler,
			MessageSource messageSource) {
		this.registrationRecordMapper = registrationRecordMapper;
		this.registrationActivityMapper = registrationActivityMapper;
		this.registrationRecordRowMapAssembler = registrationRecordRowMapAssembler;
		this.messageSource = messageSource;
	}

	public Map<String, Object> cancelRecord(Locale locale, long userId, long recordId) {
		RegistrationRecord record =
				registrationRecordMapper.selectOne(
						new LambdaQueryWrapper<RegistrationRecord>()
								.eq(RegistrationRecord::getRecordId, recordId)
								.eq(RegistrationRecord::getUserId, userId));
		if (record == null) {
			throw new ResourceException(
					messageSource.getMessage("selfservice.registration_record.not_exist", null, locale));
		}

		String st = record.getStatus() == null ? "" : record.getStatus();
		if (!("pending".equals(st) || "passed".equals(st))) {
			throw new ResourceException(
					messageSource.getMessage(
							RegistrationActivityFrontCancelRecordMessageKeys.REGISTRATION_STATUS_CANNOT_CANCEL,
							null,
							locale));
		}

		Long aid = record.getActivityId();
		if (aid == null || aid == 0L) {
			throw new ResourceException(
					messageSource.getMessage(
							RegistrationActivityFrontCancelRecordMessageKeys.ACTIVITY_NOT_ALLOW_CANCEL, null, locale));
		}
		RegistrationActivity act = registrationActivityMapper.selectById(aid);
		if (act == null) {
			throw new ResourceException(
					messageSource.getMessage(
							RegistrationActivityFrontCancelRecordMessageKeys.ACTIVITY_NOT_ALLOW_CANCEL, null, locale));
		}
		if (act.getIsAllowCancel() == null || act.getIsAllowCancel() == 0) {
			throw new ResourceException(
					messageSource.getMessage(
							RegistrationActivityFrontCancelRecordMessageKeys.ACTIVITY_NOT_ALLOW_CANCEL, null, locale));
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		int rows =
				registrationRecordMapper.update(
						null,
						new LambdaUpdateWrapper<RegistrationRecord>()
								.eq(RegistrationRecord::getRecordId, recordId)
								.eq(RegistrationRecord::getUserId, userId)
								.set(RegistrationRecord::getStatus, "canceled")
								.set(RegistrationRecord::getUpdated, now));
		if (rows == 0) {
			throw new ResourceException(
					messageSource.getMessage("selfservice.registration_record.no_update_data_found", null, locale));
		}

		RegistrationRecord fresh = registrationRecordMapper.selectById(recordId);
		if (fresh == null) {
			throw new ResourceException(
					messageSource.getMessage("selfservice.registration_record.no_update_data_found", null, locale));
		}
		return registrationRecordRowMapAssembler.toApiRow(fresh);
	}
}
