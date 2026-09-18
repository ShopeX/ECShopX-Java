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
import cn.shopex.ecshopx.companys.service.operator.sms.CompanySceneSmsSendPort;
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivity;
import cn.shopex.ecshopx.selfservice.domain.RegistrationRecord;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityMapper;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationRecordMapper;
import cn.shopex.ecshopx.wechat.service.WxaTemplateMsgRegistrationNotifyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RegistrationRecordReviewNotifyService {

	private static final Logger log = LoggerFactory.getLogger(RegistrationRecordReviewNotifyService.class);

	private final RegistrationRecordMapper registrationRecordMapper;
	private final RegistrationActivityMapper registrationActivityMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final CompanySceneSmsSendPort companySceneSmsSendPort;
	private final WxaTemplateMsgRegistrationNotifyService wxaTemplateMsgRegistrationNotifyService;

	public RegistrationRecordReviewNotifyService(
			RegistrationRecordMapper registrationRecordMapper,
			RegistrationActivityMapper registrationActivityMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			CompanySceneSmsSendPort companySceneSmsSendPort,
			WxaTemplateMsgRegistrationNotifyService wxaTemplateMsgRegistrationNotifyService) {
		this.registrationRecordMapper = registrationRecordMapper;
		this.registrationActivityMapper = registrationActivityMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.companySceneSmsSendPort = companySceneSmsSendPort;
		this.wxaTemplateMsgRegistrationNotifyService = wxaTemplateMsgRegistrationNotifyService;
	}

	public void sendMassage(long companyId, long recordId) {
		try {
			doSend(companyId, recordId);
		} catch (Exception e) {
			log.error("registration review notify failed, companyId={}, recordId={}", companyId, recordId, e);
		}
	}

	private void doSend(long companyId, long recordId) {
		RegistrationRecord record =
				registrationRecordMapper.selectOne(
						new LambdaQueryWrapper<RegistrationRecord>()
								.eq(RegistrationRecord::getCompanyId, companyId)
								.eq(RegistrationRecord::getRecordId, recordId)
								.last("LIMIT 1"));
		if (record == null) {
			return;
		}
		RegistrationActivity activity =
				registrationActivityMapper.selectOne(
						new LambdaQueryWrapper<RegistrationActivity>()
								.eq(RegistrationActivity::getCompanyId, companyId)
								.eq(RegistrationActivity::getActivityId, record.getActivityId())
								.last("LIMIT 1"));
		if (activity == null) {
			return;
		}

		Map<String, String> content = new LinkedHashMap<>();
		content.put("activity_name", activity.getActivityName() != null ? activity.getActivityName() : "");
		Integer st = activity.getStartTime();
		if (st != null && st > 0) {
			ZoneId z = ZoneId.systemDefault();
			DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分").withZone(z);
			content.put("activity_start_time", fmt.format(Instant.ofEpochSecond(st.longValue())));
		} else {
			content.put("activity_start_time", "");
		}
		content.put("activity_place", activity.getPlace() != null ? activity.getPlace() : "");
		content.put("activity_address", activity.getAddress() != null ? activity.getAddress() : "");
		content.put("activity_refuse_reason", record.getReason() != null ? record.getReason() : "");
		boolean passed = "passed".equals(record.getStatus());
		content.put("tmpl_name", passed ? "registration_success_notice" : "registration_fail_notice");

		String formMobilePlain =
				Objects.toString(sensitiveFieldEncryptor.decrypt(record.getFormMobile() == null ? "" : record.getFormMobile()), "");

		if (isNoticeEnabled(activity.getIsSmsNotice())) {
			Map<String, String> varMap = new LinkedHashMap<>(content);
			companySceneSmsSendPort.sendSceneTemplatedSms(
					companyId, formMobilePlain, content.get("tmpl_name"), varMap);
		}

		if (isNoticeEnabled(activity.getIsWxappNotice())) {
			Map<String, String> keywordData = new LinkedHashMap<>();
			keywordData.put("activity_name", content.get("activity_name"));
			keywordData.put("review_result", passed ? "审核通过" : "审核未通过");
			long uid = record.getUserId() != null ? record.getUserId() : 0L;
			wxaTemplateMsgRegistrationNotifyService.sendRegistrationResult(companyId, uid, keywordData);
		}
	}

	private static boolean isNoticeEnabled(Object flag) {
		if (Boolean.TRUE.equals(flag)) {
			return true;
		}
		return "true".equalsIgnoreCase(String.valueOf(flag));
	}
}
