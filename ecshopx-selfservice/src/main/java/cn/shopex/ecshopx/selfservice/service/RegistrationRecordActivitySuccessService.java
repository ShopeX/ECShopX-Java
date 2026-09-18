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
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivity;
import cn.shopex.ecshopx.selfservice.domain.RegistrationRecord;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationRecordMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RegistrationRecordActivitySuccessService {

	private static final Logger log = LoggerFactory.getLogger(RegistrationRecordActivitySuccessService.class);

	private final PointMemberAddPointService pointMemberAddPointService;
	private final RegistrationRecordEnterpriseWhitelistApplyService registrationRecordEnterpriseWhitelistApplyService;
	private final RegistrationRecordMapper registrationRecordMapper;
	private final MessageSource messageSource;

	public RegistrationRecordActivitySuccessService(
			PointMemberAddPointService pointMemberAddPointService,
			RegistrationRecordEnterpriseWhitelistApplyService registrationRecordEnterpriseWhitelistApplyService,
			RegistrationRecordMapper registrationRecordMapper,
			MessageSource messageSource) {
		this.pointMemberAddPointService = pointMemberAddPointService;
		this.registrationRecordEnterpriseWhitelistApplyService = registrationRecordEnterpriseWhitelistApplyService;
		this.registrationRecordMapper = registrationRecordMapper;
		this.messageSource = messageSource;
	}

	public void activitySuccess(RegistrationRecord recordSnapshot, RegistrationActivity activityInfo, Locale locale) {
		log.debug("registration activitySuccess recordId={} activityId={}", recordSnapshot.getRecordId(), activityInfo.getActivityId());
		try {
			if (activityInfo.getGiftPoints() != null
					&& activityInfo.getGiftPoints() > 0
					&& (recordSnapshot.getGetPoints() == null || recordSnapshot.getGetPoints() == 0L)) {
				int gift = activityInfo.getGiftPoints();
				long uid = recordSnapshot.getUserId() != null ? recordSnapshot.getUserId() : 0L;
				pointMemberAddPointService.addPointForSelfserviceRegistration(
						uid, recordSnapshot.getCompanyId(), gift, locale);
				LambdaUpdateWrapper<RegistrationRecord> uw = new LambdaUpdateWrapper<>();
				uw.eq(RegistrationRecord::getCompanyId, recordSnapshot.getCompanyId())
						.eq(RegistrationRecord::getRecordId, recordSnapshot.getRecordId())
						.set(RegistrationRecord::getGetPoints, (long) gift);
				int rows = registrationRecordMapper.update(null, uw);
				if (rows == 0) {
					throw new ResourceException(
							messageSource.getMessage("selfservice.registration_record.no_update_data_found", null, locale));
				}
			}
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(e.getMessage() != null ? e.getMessage() : "");
		}

		try {
			if (Integer.valueOf(1).equals(activityInfo.getIsWhiteList())
					&& StringUtils.hasText(activityInfo.getEnterpriseIds())
					&& (recordSnapshot.getIsWhiteList() == null || recordSnapshot.getIsWhiteList() == 0L)) {
				registrationRecordEnterpriseWhitelistApplyService.applyWhiteListForPassedRegistration(
						recordSnapshot, activityInfo);
			}
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(e.getMessage() != null ? e.getMessage() : "");
		}
	}
}
