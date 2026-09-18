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

import cn.shopex.ecshopx.promotions.domain.LimitCategoryPromotions;
import cn.shopex.ecshopx.promotions.domain.MarketingActivityCategory;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MarketingActivityInfoAssemblySupport {

	private static final ZoneId RESPONSE_ZONE = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter START_END_FORMAT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(RESPONSE_ZONE);

	public void applyStartEndTimeStringOverrides(Map<String, Object> result, int startEpoch, int endEpoch) {
		result.put("start_time", START_END_FORMAT.format(Instant.ofEpochSecond(startEpoch)));
		result.put("end_time", START_END_FORMAT.format(Instant.ofEpochSecond(endEpoch)));
	}

	public List<Long> filterTruthyShopIds(Object shopIdsFromPayload) {
		if (!(shopIdsFromPayload instanceof List<?> raw)) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (Object el : raw) {
			if (el == null) {
				continue;
			}
			if (el instanceof Boolean b && !b) {
				continue;
			}
			if (el instanceof Number n) {
				if (n.longValue() != 0L) {
					out.add(n.longValue());
				}
				continue;
			}
			if (el instanceof String s) {
				String t = s.trim();
				if (t.isEmpty()) {
					continue;
				}
				try {
					long v = Long.parseLong(t);
					if (v != 0L) {
						out.add(v);
					}
				} catch (NumberFormatException ignored) {
					// drop invalid token
				}
			}
		}
		return out;
	}

	public void putCategorySplitsFromRows(List<MarketingActivityCategory> rows, Map<String, Object> result) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		List<Long> relCategoryIds = new ArrayList<>();
		List<Long> itemCategory = new ArrayList<>();
		for (MarketingActivityCategory r : rows) {
			accumulateCategorySplits(r.getCategoryId(), r.getCategoryLevel(), relCategoryIds, itemCategory);
		}
		result.put("rel_category_ids", relCategoryIds.isEmpty() ? List.of() : relCategoryIds);
		result.put("item_category", itemCategory.isEmpty() ? List.of() : itemCategory);
	}

	public void putLimitCategorySplitsFromRows(List<LimitCategoryPromotions> rows, Map<String, Object> result) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		List<Long> relCategoryIds = new ArrayList<>();
		List<Long> itemCategory = new ArrayList<>();
		for (LimitCategoryPromotions r : rows) {
			accumulateCategorySplits(r.getCategoryId(), r.getCategoryLevel(), relCategoryIds, itemCategory);
		}
		result.put("rel_category_ids", relCategoryIds.isEmpty() ? List.of() : relCategoryIds);
		result.put("item_category", itemCategory.isEmpty() ? List.of() : itemCategory);
	}

	private static void accumulateCategorySplits(
			Long categoryId, Integer categoryLevel, List<Long> relCategoryIds, List<Long> itemCategory) {
		if (categoryId == null) {
			return;
		}
		if (categoryLevel == null || categoryLevel == 0) {
			relCategoryIds.add(categoryId);
		} else if (categoryLevel > 0) {
			itemCategory.add(categoryId);
		}
	}
}
