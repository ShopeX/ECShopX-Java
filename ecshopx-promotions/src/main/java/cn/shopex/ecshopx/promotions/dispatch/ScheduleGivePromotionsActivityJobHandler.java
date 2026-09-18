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
import cn.shopex.ecshopx.promotions.service.give.PromotionActivityGiveAsyncService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * When this job message is consumed from the dispatch bus, this handler runs the same scheduled promotion-give work as the legacy worker {@code handle} entry point.
 */
@Component
public class ScheduleGivePromotionsActivityJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(ScheduleGivePromotionsActivityJobHandler.class);

	private final PromotionActivityGiveAsyncService promotionActivityGiveAsyncService;

	public ScheduleGivePromotionsActivityJobHandler(
			PromotionActivityGiveAsyncService promotionActivityGiveAsyncService) {
		this.promotionActivityGiveAsyncService = promotionActivityGiveAsyncService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		Objects.requireNonNull(payload, "payload");
		try {
			long companyId = requireLong(payload.get("company_id"), "company_id");
			long distributorId = requireLong(payload.get("distributor_id"), "distributor_id");
			String sender = payload.get("sender") == null ? "" : String.valueOf(payload.get("sender"));
			List<Long> users = requireLongList(payload.get("users"), "users");
			List<Long> coupons = requireLongList(payload.get("coupons"), "coupons");
			String sourceFrom =
					payload.get("source_from") == null ? "" : String.valueOf(payload.get("source_from"));
			long triggerTimeEpochSec = requireLong(payload.get("trigger_time"), "trigger_time");
			promotionActivityGiveAsyncService.executeScheduleGive(
					companyId,
					distributorId,
					sender,
					new ArrayList<>(users),
					new ArrayList<>(coupons),
					sourceFrom,
					triggerTimeEpochSec);
		} catch (BadRequestException e) {
			throw e;
		} catch (RuntimeException e) {
			log.error("ScheduleGivePromotionsActivity job failed: {}", e.toString());
			throw e;
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

	private static List<Long> requireLongList(Object raw, String field) {
		if (raw == null) {
			throw new BadRequestException("missing job payload field: " + field);
		}
		if (!(raw instanceof List<?> list)) {
			throw new BadRequestException("invalid job payload field: " + field);
		}
		List<Long> out = new ArrayList<>(list.size());
		for (Object el : list) {
			if (el == null) {
				throw new BadRequestException("invalid job payload field: " + field);
			}
			if (el instanceof Number n) {
				out.add(n.longValue());
			} else {
				try {
					out.add(Long.parseLong(String.valueOf(el).trim()));
				} catch (NumberFormatException e) {
					throw new BadRequestException("invalid job payload field: " + field);
				}
			}
		}
		return out;
	}
}
