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
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivityRelShop;
import cn.shopex.ecshopx.selfservice.domain.RegistrationRecord;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityMapper;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityRelShopMapper;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationRecordMapper;
import cn.shopex.ecshopx.selfservice.support.RegistrationActivityFrontSubmitMessageKeys;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RegistrationActivityFrontValidityService {

	private final RegistrationActivityMapper registrationActivityMapper;
	private final RegistrationActivityRelShopMapper registrationActivityRelShopMapper;
	private final RegistrationRecordMapper registrationRecordMapper;
	private final RegistrationActivityFrontUserGradeResolveService registrationActivityFrontUserGradeResolveService;
	private final MessageSource messageSource;

	public RegistrationActivityFrontValidityService(
			RegistrationActivityMapper registrationActivityMapper,
			RegistrationActivityRelShopMapper registrationActivityRelShopMapper,
			RegistrationRecordMapper registrationRecordMapper,
			RegistrationActivityFrontUserGradeResolveService registrationActivityFrontUserGradeResolveService,
			MessageSource messageSource) {
		this.registrationActivityMapper = registrationActivityMapper;
		this.registrationActivityRelShopMapper = registrationActivityRelShopMapper;
		this.registrationRecordMapper = registrationRecordMapper;
		this.registrationActivityFrontUserGradeResolveService = registrationActivityFrontUserGradeResolveService;
		this.messageSource = messageSource;
	}

	public Map<String, Object> toActivitySubmitInfoMap(RegistrationActivity activity) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("activity_id", activity.getActivityId());
		m.put("distributor_id", activity.getDistributorId());
		m.put("temp_id", activity.getTempId());
		m.put("activity_name", activity.getActivityName());
		m.put("area", activity.getArea());
		m.put("place", activity.getPlace());
		m.put("address", activity.getAddress());
		m.put("intro", activity.getIntro());
		m.put("show_fields", activity.getShowFields());
		m.put("pics", activity.getPics());
		m.put("gift_points", activity.getGiftPoints());
		m.put("is_allow_duplicate", activity.getIsAllowDuplicate());
		m.put("is_allow_cancel", activity.getIsAllowCancel());
		m.put("is_offline_verify", activity.getIsOfflineVerify());
		m.put("is_need_check", activity.getIsNeedCheck());
		m.put("is_white_list", activity.getIsWhiteList());
		m.put("enterprise_ids", activity.getEnterpriseIds());
		m.put("group_no", activity.getGroupNo());
		m.put("member_level", activity.getMemberLevel());
		m.put("distributor_ids", activity.getDistributorIds());
		m.put("join_tips", activity.getJoinTips());
		m.put("submit_form_tips", activity.getSubmitFormTips());
		m.put("content", activity.getContent());
		m.put("start_time", activity.getStartTime());
		m.put("end_time", activity.getEndTime());
		m.put("join_limit", activity.getJoinLimit());
		m.put("is_sms_notice", activity.getIsSmsNotice());
		m.put("is_wxapp_notice", activity.getIsWxappNotice());
		m.put("created", activity.getCreated());
		m.put("updated", activity.getUpdated());
		m.put("company_id", activity.getCompanyId());
		return m;
	}

	public RegistrationActivity checkActivityValid(
			long userId,
			long companyId,
			long activityId,
			long recordId,
			long distributorId,
			Locale locale) {
		RegistrationActivity activity = registrationActivityMapper.selectById(activityId);
		if (activity == null) {
			throw new ResourceException(
					messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.ACTIVITY_NOT_EXIST_ERR, null, locale));
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		if ((activity.getStartTime() != null && activity.getStartTime() > now)
				|| (activity.getEndTime() != null && activity.getEndTime() < now)) {
			throw new ResourceException(
					messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.ACTIVITY_NOT_STARTED_OR_ENDED, null, locale));
		}
		if (StringUtils.hasText(activity.getMemberLevel())) {
			List<String> allowed =
					Arrays.stream(activity.getMemberLevel().split(","))
							.map(String::trim)
							.filter(StringUtils::hasText)
							.collect(Collectors.toList());
			String token = registrationActivityFrontUserGradeResolveService.resolveUserGradeToken(userId, companyId);
			if (!StringUtils.hasText(token) || allowed.stream().noneMatch(t -> t.equals(token))) {
				throw new ResourceException(
						messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.ONLY_SPECIFIC_MEMBERS, null, locale));
			}
		}
		if (distributorId > 0L) {
			long count =
					registrationActivityRelShopMapper.selectCount(
							new LambdaQueryWrapper<RegistrationActivityRelShop>()
									.eq(RegistrationActivityRelShop::getActivityId, activityId)
									.and(
											w ->
													w.eq(RegistrationActivityRelShop::getDistributorId, 0L)
															.or()
															.eq(RegistrationActivityRelShop::getDistributorId, distributorId)));
			if (count == 0L) {
				throw new ResourceException(
						messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.ONLY_SPECIFIC_STORES, null, locale));
			}
		}
		if (recordId == 0L
				&& activity.getJoinLimit() != null
				&& activity.getJoinLimit() > 0) {
			long joined =
					registrationRecordMapper.selectCount(
							new LambdaQueryWrapper<RegistrationRecord>()
									.eq(RegistrationRecord::getActivityId, activityId));
			if (joined >= activity.getJoinLimit()) {
				throw new ResourceException(
						messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.ACTIVITY_QUOTA_FULL, null, locale));
			}
		}
		if (recordId > 0L) {
			RegistrationRecord rec = registrationRecordMapper.selectById(recordId);
			if (rec == null || !Objects.equals(rec.getUserId(), userId)) {
				throw new ResourceException(
						messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.CAN_ONLY_MODIFY_OWN, null, locale));
			}
			if (!"pending".equals(rec.getStatus()) && !"rejected".equals(rec.getStatus())) {
				throw new ResourceException(
						messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.STATUS_CANNOT_MODIFY, null, locale));
			}
		} else if (!Integer.valueOf(1).equals(activity.getIsAllowDuplicate())) {
			long dup =
					registrationRecordMapper.selectCount(
							new LambdaQueryWrapper<RegistrationRecord>()
									.eq(RegistrationRecord::getCompanyId, companyId)
									.eq(RegistrationRecord::getUserId, userId)
									.eq(RegistrationRecord::getActivityId, activityId));
			if (dup > 0L) {
				throw new ResourceException(
						messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.CANNOT_REGISTER_DUPLICATE, null, locale));
			}
		}
		return activity;
	}
}
