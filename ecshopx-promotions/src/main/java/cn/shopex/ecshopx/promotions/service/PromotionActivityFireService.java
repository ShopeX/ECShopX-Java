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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.promotions.domain.PromotionActivity;
import cn.shopex.ecshopx.promotions.mapper.PromotionActivityMapper;
import cn.shopex.ecshopx.promotions.service.schedule.PromotionActivityScheduleFireActionService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Scans active promotion activities of a type and runs scheduled marketing actions per matching row. */
@Service
public class PromotionActivityFireService {

	private final PromotionActivityMapper promotionActivityMapper;
	private final PromotionActivityCreateService promotionActivityCreateService;
	private final PromotionActivityScheduleFireActionService promotionActivityScheduleFireActionService;

	public PromotionActivityFireService(
			PromotionActivityMapper promotionActivityMapper,
			PromotionActivityCreateService promotionActivityCreateService,
			PromotionActivityScheduleFireActionService promotionActivityScheduleFireActionService) {
		this.promotionActivityMapper = promotionActivityMapper;
		this.promotionActivityCreateService = promotionActivityCreateService;
		this.promotionActivityScheduleFireActionService = promotionActivityScheduleFireActionService;
	}

	public void fire(long companyId, Map<String, Object> memberInfo, String activityType) {
		long now = Instant.now().getEpochSecond();
		QueryWrapper<PromotionActivity> w = new QueryWrapper<>();
		w.eq("company_id", companyId)
				.eq("activity_type", activityType)
				.eq("activity_status", "valid")
				.lt("begin_time", now)
				.ge("end_time", now);
		List<PromotionActivity> rows = promotionActivityMapper.selectList(w);
		if (rows == null || rows.isEmpty()) {
			return;
		}
		for (PromotionActivity row : rows) {
			Map<String, Object> activityInfo = promotionActivityCreateService.toActivityListRow(row);
			promotionActivityScheduleFireActionService.actionPromotionActivity(activityType, activityInfo, memberInfo);
		}
	}
}
