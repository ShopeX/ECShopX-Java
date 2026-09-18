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

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.employeepurchase.domain.ActivityItems;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityItemsMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Aligns PHP {@code ActivityItemsService::getItemsListActityPrice}: overlay employee-purchase
 * {@code activity_price} / {@code activity_store} onto item list rows by {@code item_id}.
 */
@Service
public class EmployeePurchaseActivityItemsPriceOverlayService {

	private final ActivityItemsMapper activityItemsMapper;

	public EmployeePurchaseActivityItemsPriceOverlayService(ActivityItemsMapper activityItemsMapper) {
		this.activityItemsMapper = activityItemsMapper;
	}

	public void overlayActivityPrice(List<Map<String, Object>> rows, long companyId, long activityId) {
		if (rows == null || rows.isEmpty() || companyId <= 0L || activityId <= 0L) {
			return;
		}
		List<Long> itemIds = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			if (row == null) {
				continue;
			}
			long itemId = longOrZero(row.get("item_id"));
			if (itemId > 0L) {
				itemIds.add(itemId);
			}
		}
		if (itemIds.isEmpty()) {
			return;
		}
		List<ActivityItems> list =
				activityItemsMapper.selectList(
						Wrappers.<ActivityItems>lambdaQuery()
								.eq(ActivityItems::getCompanyId, companyId)
								.eq(ActivityItems::getActivityId, activityId)
								.in(ActivityItems::getItemId, itemIds)
								.eq(ActivityItems::getShelfStatus, 1)
								.select(
										ActivityItems::getItemId,
										ActivityItems::getActivityPrice,
										ActivityItems::getActivityStore));
		if (list == null || list.isEmpty()) {
			return;
		}
		LinkedHashMap<Long, ActivityItems> byItemId = new LinkedHashMap<>();
		for (ActivityItems row : list) {
			if (row == null || row.getItemId() == null || row.getItemId() <= 0L) {
				continue;
			}
			byItemId.putIfAbsent(row.getItemId(), row);
		}
		for (Map<String, Object> row : rows) {
			if (row == null) {
				continue;
			}
			long itemId = longOrZero(row.get("item_id"));
			ActivityItems act = byItemId.get(itemId);
			if (act == null) {
				continue;
			}
			row.put("activity_price", intOrZero(act.getActivityPrice()));
			row.put("activity_store", intOrZero(act.getActivityStore()));
		}
	}

	private static int intOrZero(Integer v) {
		return v == null ? 0 : v.intValue();
	}

	private static long longOrZero(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v == null) {
			return 0L;
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
