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

package cn.shopex.ecshopx.promotions.schedule;

import cn.shopex.ecshopx.promotions.service.schedule.MembershipSchedulePromotionActivitySupport;
import cn.shopex.ecshopx.promotions.service.schedule.PromotionActivityScheduleFireActionService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 与 PHP 队列 Job 对位：按消息分页拉取会员并执行逐单 {@code actionPromotionActivity}。
 */
@Service
public class ScheduleFirePromotionActivityConsumer {

	private final MembershipSchedulePromotionActivitySupport membershipSchedulePromotionActivitySupport;
	private final PromotionActivityScheduleFireActionService promotionActivityScheduleFireActionService;

	public ScheduleFirePromotionActivityConsumer(
			MembershipSchedulePromotionActivitySupport membershipSchedulePromotionActivitySupport,
			PromotionActivityScheduleFireActionService promotionActivityScheduleFireActionService) {
		this.membershipSchedulePromotionActivitySupport = membershipSchedulePromotionActivitySupport;
		this.promotionActivityScheduleFireActionService = promotionActivityScheduleFireActionService;
	}

	public void handle(ScheduleFirePromotionActivityMessage message) {
		List<Map<String, Object>> members =
				membershipSchedulePromotionActivitySupport.getMembers(
						message.activityType(),
						message.activityInfo(),
						message.triggerTime(),
						message.pageSize(),
						message.page());
		for (Map<String, Object> memberRow : members) {
			promotionActivityScheduleFireActionService.actionPromotionActivity(
					message.activityType(), message.activityInfo(), memberRow);
		}
	}
}
