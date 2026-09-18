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

package cn.shopex.ecshopx.selfservice.integration;

import cn.shopex.ecshopx.common.port.selfservice.SendRegistrationWxRemindQueueMessage;
import cn.shopex.ecshopx.common.promotions.port.WxopenTemplateSendDispatchPublisher;
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivity;
import cn.shopex.ecshopx.selfservice.domain.RegistrationRecord;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityMapper;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SendRegistrationWxRemindJobHandler {

	private static final Logger log = LoggerFactory.getLogger(SendRegistrationWxRemindJobHandler.class);

	private static final String SCENE_REGISTRATION_ACTIVITY_NOTICE = "registrationActivityNotice";

	private static final DateTimeFormatter ACTIVITY_TIME =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final RegistrationActivityMapper registrationActivityMapper;
	private final RegistrationRecordMapper registrationRecordMapper;
	private final WxopenTemplateSendDispatchPublisher wxopenTemplateSendDispatchPublisher;

	public SendRegistrationWxRemindJobHandler(
			RegistrationActivityMapper registrationActivityMapper,
			RegistrationRecordMapper registrationRecordMapper,
			WxopenTemplateSendDispatchPublisher wxopenTemplateSendDispatchPublisher) {
		this.registrationActivityMapper = registrationActivityMapper;
		this.registrationRecordMapper = registrationRecordMapper;
		this.wxopenTemplateSendDispatchPublisher = wxopenTemplateSendDispatchPublisher;
	}

	public void handle(SendRegistrationWxRemindQueueMessage message) {
		try {
			long activityId = message.getActivityId();
			RegistrationActivity activity = registrationActivityMapper.selectById(activityId);
			if (activity == null) {
				return;
			}
			long nowSec = Instant.now().getEpochSecond();
			Integer endTime = activity.getEndTime();
			if (endTime != null && endTime.longValue() < nowSec) {
				return;
			}
			if (!Boolean.TRUE.equals(activity.getIsWxappNotice())) {
				return;
			}

			Map<String, Object> wxaData = new LinkedHashMap<>(8);
			wxaData.put("activity_name", emptyIfNull(activity.getActivityName()));
			wxaData.put("activity_start_time", formatEpoch(activity.getStartTime()));
			wxaData.put("activity_end_time", formatEpoch(activity.getEndTime()));
			wxaData.put("activity_address", emptyIfNull(activity.getAddress()));

			Map<String, Object> basePayload = new LinkedHashMap<>(8);
			basePayload.put("data", wxaData);
			basePayload.put("scenes_name", SCENE_REGISTRATION_ACTIVITY_NOTICE);
			basePayload.put("company_id", activity.getCompanyId());

			List<RegistrationRecord> records =
					registrationRecordMapper.selectList(new LambdaQueryWrapper<RegistrationRecord>()
							.eq(RegistrationRecord::getActivityId, activityId)
							.eq(RegistrationRecord::getStatus, "passed"));
			if (records == null || records.isEmpty()) {
				return;
			}
			for (RegistrationRecord v : records) {
				if (!StringUtils.hasText(v.getWxappAppid()) || !StringUtils.hasText(v.getOpenId())) {
					continue;
				}
				Map<String, Object> row = new LinkedHashMap<>(basePayload);
				row.put("appid", v.getWxappAppid().trim());
				row.put("openid", v.getOpenId().trim());
				row.put("page_query_str", "record_id=" + v.getRecordId());
				wxopenTemplateSendDispatchPublisher.publish(row, true);
			}
		} catch (RuntimeException e) {
			log.error(
					"SendRegistrationWxRemindJob_error => activity_id:{} {}",
					message.getActivityId(),
					e.getMessage(),
					e);
		}
	}

	private static String emptyIfNull(String s) {
		return s == null ? "" : s;
	}

	private static String formatEpoch(Integer epochSec) {
		if (epochSec == null) {
			return "";
		}
		return ACTIVITY_TIME.format(Instant.ofEpochSecond(epochSec.longValue()));
	}
}
