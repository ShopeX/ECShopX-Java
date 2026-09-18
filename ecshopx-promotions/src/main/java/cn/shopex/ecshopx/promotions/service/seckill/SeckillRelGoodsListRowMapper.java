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

package cn.shopex.ecshopx.promotions.service.seckill;

import cn.shopex.ecshopx.promotions.domain.SeckillRelGoods;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Builds list rows and per-iteration activity summary aligned with legacy list serialization. */
public final class SeckillRelGoodsListRowMapper {

	private static final DateTimeFormatter DATE_TIME =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private SeckillRelGoodsListRowMapper() {}

	public static Map<String, Object> toRow(
			SeckillRelGoods e, int nowEpochSec, Consumer<Map<String, Object>> activitySink) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("seckill_id", e.getSeckillId());
		result.put("item_id", e.getItemId());
		result.put("company_id", e.getCompanyId());
		result.put("seckill_type", e.getSeckillType());
		result.put("item_title", e.getItemTitle());
		result.put("activity_price", e.getActivityPrice() != null ? e.getActivityPrice() : 0);
		result.put("activity_store", e.getActivityStore() != null ? e.getActivityStore() : 0);
		result.put("activity_start_time", e.getActivityStartTime() != null ? e.getActivityStartTime() : 0);
		result.put("activity_end_time", e.getActivityEndTime() != null ? e.getActivityEndTime() : 0);
		result.put(
				"activity_release_time",
				e.getActivityReleaseTime() != null ? e.getActivityReleaseTime() : 0);
		result.put("sales_store", e.getSalesStore() != null ? e.getSalesStore() : 0);
		result.put("limit_num", e.getLimitNum() != null ? e.getLimitNum() : 0);
		result.put("item_type", e.getItemType());
		result.put("item_pic", e.getItemPic());
		result.put("sort", e.getSort() != null ? e.getSort() : 0);
		result.put("is_show", e.getIsShow());
		result.put("item_spec_desc", e.getItemSpecDesc());
		result.put("created", e.getCreated());
		result.put("updated", e.getUpdated());
		result.put("disabled", e.getDisabled());

		int end = e.getActivityEndTime() != null ? e.getActivityEndTime() : 0;
		int start = e.getActivityStartTime() != null ? e.getActivityStartTime() : 0;
		int release = e.getActivityReleaseTime() != null ? e.getActivityReleaseTime() : 0;

		Integer lastSeconds = null;
		if (nowEpochSec >= end) {
			result.put("status", "it_has_ended");
		} else if (nowEpochSec >= start && nowEpochSec < end) {
			result.put("status", "in_sale");
			lastSeconds = Math.max(0, end - nowEpochSec);
			result.put("last_seconds", lastSeconds);
		} else if (nowEpochSec >= release && nowEpochSec < start) {
			result.put("status", "in_the_notice");
			lastSeconds = Math.max(0, start - nowEpochSec);
			result.put("last_seconds", lastSeconds);
		} else if (nowEpochSec < release) {
			result.put("status", "waiting");
		}

		result.put("created_date", formatEpochDate(e.getCreated()));
		result.put("updated_date", formatEpochDate(e.getUpdated()));

		Map<String, Object> activityPayload = new LinkedHashMap<>();
		activityPayload.put("seckill_id", result.get("seckill_id"));
		activityPayload.put("activity_price", result.get("activity_price"));
		activityPayload.put("activity_store", result.get("activity_store"));
		activityPayload.put("activity_start_time", result.get("activity_start_time"));
		activityPayload.put("activity_end_time", result.get("activity_end_time"));
		activityPayload.put("activity_release_time", result.get("activity_release_time"));
		activityPayload.put("status", result.get("status"));
		activityPayload.put("last_seconds", lastSeconds != null ? lastSeconds : 0);
		activitySink.accept(activityPayload);

		return result;
	}

	private static String formatEpochDate(Integer epoch) {
		if (epoch == null) {
			return "";
		}
		return DATE_TIME.format(Instant.ofEpochSecond(epoch.longValue()));
	}
}
