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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Equivalent to legacy assoc_unique(..., goods_id, activity_price, ASC) without trailing sort-by-sort. */
public final class SeckillAssocUniqueByGoodsId {

	private SeckillAssocUniqueByGoodsId() {}

	public static List<Map<String, Object>> apply(List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return rows == null ? List.of() : new ArrayList<>(rows);
		}
		List<Map<String, Object>> copy = new ArrayList<>(rows);
		copy.sort(Comparator.comparingInt(SeckillAssocUniqueByGoodsId::activityPriceKey));

		List<Map<String, Object>> out = new ArrayList<>();
		Set<Long> seenGoods = new HashSet<>();
		for (Map<String, Object> row : copy) {
			Long gid = goodsIdOf(row);
			if (gid == null) {
				continue;
			}
			if (seenGoods.contains(gid)) {
				continue;
			}
			seenGoods.add(gid);
			out.add(row);
		}
		return out;
	}

	private static int activityPriceKey(Map<String, Object> row) {
		Object v = row.get("activity_price");
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static Long goodsIdOf(Map<String, Object> row) {
		Object v = row.get("goods_id");
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
