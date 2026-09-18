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

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivity;
import cn.shopex.ecshopx.selfservice.domain.RegistrationRecord;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityMapper;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationRecordMapper;
import cn.shopex.ecshopx.selfservice.support.RegistrationActivityFrontSubmitMessageKeys;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RegistrationRecordFrontSaveRecordService {

	private final RegistrationRecordMapper registrationRecordMapper;
	private final RegistrationActivityMapper registrationActivityMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final RegistrationRecordActivitySuccessService registrationRecordActivitySuccessService;
	private final MessageSource messageSource;
	private final StringRedisTemplate companysRedisTemplate;

	public RegistrationRecordFrontSaveRecordService(
			RegistrationRecordMapper registrationRecordMapper,
			RegistrationActivityMapper registrationActivityMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			RegistrationRecordActivitySuccessService registrationRecordActivitySuccessService,
			MessageSource messageSource,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.registrationRecordMapper = registrationRecordMapper;
		this.registrationActivityMapper = registrationActivityMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.registrationRecordActivitySuccessService = registrationRecordActivitySuccessService;
		this.messageSource = messageSource;
		this.companysRedisTemplate = companysRedisTemplate;
	}

	public RegistrationRecord saveRecord(
			long userId,
			long companyId,
			String wxappAppid,
			String openId,
			String authMobilePlain,
			String formMobilePlain,
			String trueName,
			long distributorId,
			long recordId,
			String contentJson,
			RegistrationActivity activityInfo,
			Locale locale) {
		boolean needCheck = Integer.valueOf(1).equals(activityInfo.getIsNeedCheck());
		String status = needCheck ? "pending" : "passed";
		Long formId = activityInfo.getTempId();
		int now = (int) (System.currentTimeMillis() / 1000L);

		if (recordId > 0L) {
			RegistrationRecord rec = registrationRecordMapper.selectById(recordId);
			if (rec == null) {
				throw new ResourceException(
						messageSource.getMessage("selfservice.registration_record.no_update_data_found", null, locale));
			}
			rec.setTrueName(trueName == null ? "" : trueName);
			rec.setContent(contentJson);
			rec.setFormId(formId);
			rec.setStatus(status);
			rec.setWxappAppid(wxappAppid);
			rec.setOpenId(openId);
			rec.setDistributorId(distributorId);
			rec.setMobile(sensitiveFieldEncryptor.encrypt(authMobilePlain == null ? "" : authMobilePlain));
			rec.setFormMobile(sensitiveFieldEncryptor.encrypt(formMobilePlain == null ? "" : formMobilePlain));
			rec.setUpdated(now);
			int u = registrationRecordMapper.updateById(rec);
			if (u == 0) {
				throw new ResourceException(
						messageSource.getMessage("selfservice.registration_record.no_update_data_found", null, locale));
			}
			return registrationRecordMapper.selectById(recordId);
		}

		long activityId = activityInfo.getActivityId() != null ? activityInfo.getActivityId() : 0L;
		long recordNo = genRecordNo(companyId, activityId, activityInfo.getGroupNo(), locale);
		String groupNoStored = activityInfo.getGroupNo() == null ? "" : activityInfo.getGroupNo();

		RegistrationRecord row = new RegistrationRecord();
		row.setActivityId(activityId);
		row.setUserId(userId);
		row.setCompanyId(companyId);
		row.setDistributorId(distributorId);
		row.setWxappAppid(wxappAppid);
		row.setOpenId(openId);
		row.setTrueName(trueName == null ? "" : trueName);
		row.setContent(contentJson);
		row.setFormId(formId);
		row.setStatus(status);
		row.setGroupNo(groupNoStored);
		row.setRecordNo(recordNo);
		row.setVerifyCode((long) ThreadLocalRandom.current().nextInt(111_111, 1_000_000));
		row.setIsWhiteList(0L);
		row.setGetPoints(0L);
		row.setVerifyTime(0L);
		row.setVerifyOperator("");
		row.setMobile(sensitiveFieldEncryptor.encrypt(authMobilePlain == null ? "" : authMobilePlain));
		row.setFormMobile(sensitiveFieldEncryptor.encrypt(formMobilePlain == null ? "" : formMobilePlain));
		row.setCreated(now);
		row.setUpdated(now);

		int ins = registrationRecordMapper.insert(row);
		if (ins == 0) {
			throw new ResourceException(
					messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.ACTIVITY_TOO_POPULAR, null, locale));
		}

		String lockSuffix = normalizeGroupNoForLock(activityInfo.getGroupNo());
		companysRedisTemplate.delete("genRecordNo:" + activityId + ":" + lockSuffix);

		RegistrationRecord persisted = registrationRecordMapper.selectById(row.getRecordId());
		if (!needCheck) {
			registrationRecordActivitySuccessService.activitySuccess(persisted, activityInfo, locale);
			persisted = registrationRecordMapper.selectById(row.getRecordId());
		}
		return persisted;
	}

	private long genRecordNo(long companyId, long activityId, String groupNoRaw, Locale locale) {
		String lockSuffix = normalizeGroupNoForLock(groupNoRaw);
		String redisKey = "genRecordNo:" + activityId + ":" + lockSuffix;
		Boolean ok = companysRedisTemplate.opsForValue().setIfAbsent(redisKey, "1", Duration.ofSeconds(10));
		if (!Boolean.TRUE.equals(ok)) {
			throw new ResourceException(
					messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.ACTIVITY_TOO_POPULAR, null, locale));
		}
		try {
			boolean useGroupBranch = hasEffectiveGroupNoForQuery(groupNoRaw);
			long count;
			if (useGroupBranch) {
				String gn = groupNoRaw.trim();
				List<RegistrationActivity> acts =
						registrationActivityMapper.selectList(
								new LambdaQueryWrapper<RegistrationActivity>()
										.eq(RegistrationActivity::getCompanyId, companyId)
										.eq(RegistrationActivity::getGroupNo, gn));
				if (acts.isEmpty()) {
					throw new ResourceException(
							messageSource.getMessage(
									RegistrationActivityFrontSubmitMessageKeys.ACTIVITY_GROUP_CODE_ERROR,
									new Object[] {gn},
									locale));
				}
				List<Long> ids = acts.stream().map(RegistrationActivity::getActivityId).collect(Collectors.toList());
				count =
						registrationRecordMapper.selectCount(
								new LambdaQueryWrapper<RegistrationRecord>().in(RegistrationRecord::getActivityId, ids));
			} else {
				count =
						registrationRecordMapper.selectCount(
								new LambdaQueryWrapper<RegistrationRecord>()
										.eq(RegistrationRecord::getActivityId, activityId));
			}
			return count + 1;
		} catch (ResourceException e) {
			companysRedisTemplate.delete(redisKey);
			throw e;
		} catch (RuntimeException e) {
			companysRedisTemplate.delete(redisKey);
			throw e;
		}
	}

	private static String normalizeGroupNoForLock(String groupNoRaw) {
		if (!StringUtils.hasText(groupNoRaw)) {
			return "0";
		}
		if ("0".equals(groupNoRaw.trim())) {
			return "0";
		}
		return groupNoRaw.trim();
	}

	private static boolean hasEffectiveGroupNoForQuery(String groupNoRaw) {
		if (!StringUtils.hasText(groupNoRaw)) {
			return false;
		}
		return !"0".equals(groupNoRaw.trim());
	}
}
