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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivity;
import cn.shopex.ecshopx.selfservice.domain.RegistrationRecord;
import cn.shopex.ecshopx.selfservice.dto.admin.v1.RegistrationRecordRegistrationReviewCommand;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityMapper;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationRecordMapper;
import cn.shopex.ecshopx.selfservice.service.multilang.RegistrationRecordOutsideMultiLangWriteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RegistrationRecordRegistrationReviewService {

	private final RegistrationRecordMapper registrationRecordMapper;
	private final RegistrationActivityMapper registrationActivityMapper;
	private final RegistrationRecordOutsideMultiLangWriteService registrationRecordOutsideMultiLangWriteService;
	private final RegistrationRecordRowMapAssembler registrationRecordRowMapAssembler;
	private final RegistrationRecordActivitySuccessService registrationRecordActivitySuccessService;
	private final RegistrationRecordReviewNotifyService registrationRecordReviewNotifyService;
	private final MessageSource messageSource;
	private final StringRedisTemplate companysRedisTemplate;

	public RegistrationRecordRegistrationReviewService(
			RegistrationRecordMapper registrationRecordMapper,
			RegistrationActivityMapper registrationActivityMapper,
			RegistrationRecordOutsideMultiLangWriteService registrationRecordOutsideMultiLangWriteService,
			RegistrationRecordRowMapAssembler registrationRecordRowMapAssembler,
			RegistrationRecordActivitySuccessService registrationRecordActivitySuccessService,
			RegistrationRecordReviewNotifyService registrationRecordReviewNotifyService,
			MessageSource messageSource,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.registrationRecordMapper = registrationRecordMapper;
		this.registrationActivityMapper = registrationActivityMapper;
		this.registrationRecordOutsideMultiLangWriteService = registrationRecordOutsideMultiLangWriteService;
		this.registrationRecordRowMapAssembler = registrationRecordRowMapAssembler;
		this.registrationRecordActivitySuccessService = registrationRecordActivitySuccessService;
		this.registrationRecordReviewNotifyService = registrationRecordReviewNotifyService;
		this.messageSource = messageSource;
		this.companysRedisTemplate = companysRedisTemplate;
	}

	public Map<String, Object> registrationReview(
			long companyId, RegistrationRecordRegistrationReviewCommand cmd, String requestLangTag, Locale locale) {
		if (!cmd.approvalPassed() && !StringUtils.hasText(cmd.reason())) {
			throw new BadRequestException(
					messageSource.getMessage("selfservice.registration_record.rejection_reason_required", null, locale));
		}

		String redisKey = "registrationReview:" + cmd.recordId();
		Boolean ok = companysRedisTemplate.opsForValue().setIfAbsent(redisKey, "1", Duration.ofSeconds(3));
		if (!Boolean.TRUE.equals(ok)) {
			throw new ResourceException(
					messageSource.getMessage("selfservice.registration_record.operation_too_frequent", null, locale));
		}

		RegistrationRecord row =
				registrationRecordMapper.selectOne(
						new LambdaQueryWrapper<RegistrationRecord>()
								.eq(RegistrationRecord::getCompanyId, companyId)
								.eq(RegistrationRecord::getRecordId, cmd.recordId())
								.last("LIMIT 1"));
		if (row == null) {
			throw new ResourceException(messageSource.getMessage("selfservice.registration_record.not_exist", null, locale));
		}
		if (!"pending".equals(row.getStatus())) {
			throw new ResourceException(
					messageSource.getMessage("selfservice.registration_record.current_status_no_need_review", null, locale));
		}

		String newStatus = cmd.approvalPassed() ? "passed" : "rejected";

		RegistrationRecord snapshot = new RegistrationRecord();
		snapshot.setRecordId(row.getRecordId());
		snapshot.setCompanyId(row.getCompanyId());
		snapshot.setActivityId(row.getActivityId());
		snapshot.setUserId(row.getUserId());
		snapshot.setGetPoints(row.getGetPoints());
		snapshot.setIsWhiteList(row.getIsWhiteList());
		snapshot.setMobile(row.getMobile());
		snapshot.setFormMobile(row.getFormMobile());
		snapshot.setTrueName(row.getTrueName());

		int now = (int) Instant.now().getEpochSecond();
		row.setStatus(newStatus);
		row.setReason(cmd.reason());
		row.setUpdated(now);
		int u = registrationRecordMapper.updateById(row);
		if (u == 0) {
			throw new ResourceException(
					messageSource.getMessage("selfservice.registration_record.no_update_data_found", null, locale));
		}

		Map<String, Object> lang = new LinkedHashMap<>();
		lang.put("reason", cmd.reason());
		registrationRecordOutsideMultiLangWriteService.updateLangFields(cmd.recordId(), companyId, lang, requestLangTag);

		if ("passed".equals(newStatus)) {
			RegistrationActivity act =
					registrationActivityMapper.selectOne(
							new LambdaQueryWrapper<RegistrationActivity>()
									.eq(RegistrationActivity::getCompanyId, companyId)
									.eq(RegistrationActivity::getActivityId, snapshot.getActivityId())
									.last("LIMIT 1"));
			if (act == null) {
				throw new ResourceException(
						messageSource.getMessage("selfservice.registration_activity.no_update_data_found", null, locale));
			}
			registrationRecordActivitySuccessService.activitySuccess(snapshot, act, locale);
		}

		registrationRecordReviewNotifyService.sendMassage(companyId, cmd.recordId());

		RegistrationRecord fresh =
				registrationRecordMapper.selectOne(
						new LambdaQueryWrapper<RegistrationRecord>()
								.eq(RegistrationRecord::getCompanyId, companyId)
								.eq(RegistrationRecord::getRecordId, cmd.recordId())
								.last("LIMIT 1"));
		if (fresh == null) {
			throw new ResourceException(messageSource.getMessage("selfservice.registration_record.not_exist", null, locale));
		}
		return registrationRecordRowMapAssembler.toApiRow(fresh);
	}
}
