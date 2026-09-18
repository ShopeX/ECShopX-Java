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

import cn.shopex.ecshopx.common.dispatch.SendWxRemindJobDispatchPublisher;
import cn.shopex.ecshopx.promotions.domain.WxaNoticeTemplate;
import cn.shopex.ecshopx.promotions.mapper.WxaNoticeTemplateMapper;
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivity;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityMapper;
import cn.shopex.ecshopx.selfservice.service.multilang.RegistrationActivityOutsideMultiLangReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RegistrationActivityService {

	private static final String SCENE = "registrationActivityNotice";
	private static final String TEMPLATE_NAME = "yykweishop";
	private static final String REQUEST_LANG = "zh-CN";

	private final WxaNoticeTemplateMapper wxaNoticeTemplateMapper;
	private final RegistrationActivityMapper registrationActivityMapper;
	private final RegistrationActivityOutsideMultiLangReadService registrationActivityOutsideMultiLangReadService;
	private final SendWxRemindJobDispatchPublisher sendWxRemindJobDispatchPublisher;
	private final ObjectMapper objectMapper;

	/**
	 * 按整点 + 配置提前量扫描报名活动，将符合时间窗的活动投递到慢队列。
	 *
	 * @return 本 tick 实际入队次数（与命中活动一一对应）
	 */
	public int scheduleSendWxRemindMsg() {
		List<WxaNoticeTemplate> templates = wxaNoticeTemplateMapper.selectList(
				new LambdaQueryWrapper<WxaNoticeTemplate>()
						.eq(WxaNoticeTemplate::getScenesName, SCENE)
						.eq(WxaNoticeTemplate::getTemplateName, TEMPLATE_NAME)
						.eq(WxaNoticeTemplate::getIsOpen, true));
		if (templates == null || templates.isEmpty()) {
			return 0;
		}
		int dispatched = 0;
		long nowSec = Instant.now().getEpochSecond();
		for (WxaNoticeTemplate v : templates) {
			if (!StringUtils.hasText(v.getTemplateId()) || !StringUtils.hasText(v.getSendTimeDesc())) {
				continue;
			}
			JsonNode root;
			try {
				root = objectMapper.readTree(v.getSendTimeDesc());
			} catch (Exception e) {
				continue;
			}
			if (root == null || root.isNull() || root.isArray() || !root.isObject() || root.size() == 0) {
				continue;
			}
			int remindHour = parseRemindHour(root.get("value"));
			if (remindHour == 0) {
				continue;
			}
			remindHour++;
			long remindStart = nowSec + (long) remindHour * 3600L;
			long remindEnd = remindStart + 3600L;
			Long companyId = v.getCompanyId();
			if (companyId == null) {
				continue;
			}
			int startLo = toSafeIntEpoch(remindStart);
			int endLo = toSafeIntEpoch(remindEnd);
			LambdaQueryWrapper<RegistrationActivity> actW = new LambdaQueryWrapper<RegistrationActivity>()
					.eq(RegistrationActivity::getCompanyId, companyId)
					.ge(RegistrationActivity::getStartTime, startLo)
					.lt(RegistrationActivity::getStartTime, endLo)
					.eq(RegistrationActivity::getIsWxappNotice, true);
			List<RegistrationActivity> activities = registrationActivityMapper.selectList(actW);
			if (activities == null || activities.isEmpty()) {
				continue;
			}
			List<Map<String, Object>> rows = new ArrayList<>();
			for (RegistrationActivity a : activities) {
				Map<String, Object> row = new HashMap<>();
				row.put("activity_id", a.getActivityId());
				row.put("activity_name", a.getActivityName());
				rows.add(row);
			}
			registrationActivityOutsideMultiLangReadService.applyActivityNameOverrides(companyId, rows, REQUEST_LANG);
			for (RegistrationActivity a : activities) {
				sendWxRemindJobDispatchPublisher.publish(a.getActivityId());
				dispatched++;
			}
		}
		return dispatched;
	}

	private static int toSafeIntEpoch(long sec) {
		if (sec > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		if (sec < Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}
		return (int) sec;
	}

	private static int parseRemindHour(JsonNode valueNode) {
		if (valueNode == null || valueNode.isNull()) {
			return 0;
		}
		if (valueNode.isNumber()) {
			return valueNode.intValue();
		}
		if (valueNode.isTextual()) {
			String s = valueNode.asText();
			if (!StringUtils.hasText(s)) {
				return 0;
			}
			try {
				return Integer.parseInt(s.trim());
			} catch (NumberFormatException e) {
				return 0;
			}
		}
		return 0;
	}
}
