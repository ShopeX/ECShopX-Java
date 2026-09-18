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

package cn.shopex.ecshopx.config;

import cn.shopex.ecshopx.common.dispatch.PromotionsDispatchJobNames;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import cn.shopex.ecshopx.promotions.schedule.ScheduleFirePromotionActivityEnqueuePort;
import cn.shopex.ecshopx.promotions.schedule.ScheduleFirePromotionActivityMessage;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ScheduleFirePromotionActivityDispatchPublisherImpl implements ScheduleFirePromotionActivityEnqueuePort {

	private final DispatchFacade dispatchFacade;

	public ScheduleFirePromotionActivityDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public long enqueue(ScheduleFirePromotionActivityMessage message) {
		Map<String, Object> payload = new LinkedHashMap<>(8);
		payload.put("activity_type", message.activityType());
		payload.put("activity_info", message.activityInfo());
		payload.put("trigger_time", message.triggerTime());
		payload.put("page_size", message.pageSize());
		payload.put("page", message.page());
		dispatchFacade.dispatchJob(
				PromotionsDispatchJobNames.SCHEDULE_FIRE_PROMOTIONS_ACTIVITY,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"default",
						null,
						RetryPolicy.platformDefault()));
		return 1L;
	}
}
