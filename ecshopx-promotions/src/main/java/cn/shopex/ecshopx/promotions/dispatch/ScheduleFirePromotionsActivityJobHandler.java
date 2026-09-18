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

package cn.shopex.ecshopx.promotions.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.promotions.schedule.ScheduleFirePromotionActivityConsumer;
import cn.shopex.ecshopx.promotions.schedule.ScheduleFirePromotionActivityMessage;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ScheduleFirePromotionsActivityJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(ScheduleFirePromotionsActivityJobHandler.class);

	private final ScheduleFirePromotionActivityConsumer scheduleFirePromotionActivityConsumer;

	public ScheduleFirePromotionsActivityJobHandler(
			ScheduleFirePromotionActivityConsumer scheduleFirePromotionActivityConsumer) {
		this.scheduleFirePromotionActivityConsumer = scheduleFirePromotionActivityConsumer;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		Objects.requireNonNull(payload, "payload");
		try {
			String activityType =
					payload.get("activity_type") == null ? "" : String.valueOf(payload.get("activity_type"));
			@SuppressWarnings("unchecked")
			Map<String, Object> activityInfoRaw = (Map<String, Object>) payload.get("activity_info");
			Map<String, Object> activityInfo = activityInfoRaw != null ? activityInfoRaw : Map.of();
			long triggerTime = requireLong(payload.get("trigger_time"), "trigger_time");
			int pageSize = requireInt(payload.get("page_size"), "page_size");
			int page = requireInt(payload.get("page"), "page");
			ScheduleFirePromotionActivityMessage message =
					new ScheduleFirePromotionActivityMessage(activityType, activityInfo, triggerTime, pageSize, page);
			scheduleFirePromotionActivityConsumer.handle(message);
		} catch (BadRequestException e) {
			throw e;
		} catch (RuntimeException e) {
			log.error("ScheduleFirePromotionsActivity job failed: {}", e.toString());
		}
	}

	private static long requireLong(Object raw, String field) {
		if (raw == null) {
			throw new BadRequestException("missing job payload field: " + field);
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("invalid job payload field: " + field);
		}
	}

	private static int requireInt(Object raw, String field) {
		if (raw == null) {
			throw new BadRequestException("missing job payload field: " + field);
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("invalid job payload field: " + field);
		}
	}
}
