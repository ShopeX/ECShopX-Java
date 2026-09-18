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

package cn.shopex.ecshopx.goods.service.wxapp;

import cn.shopex.ecshopx.promotions.service.SkuValidMarketingActivityService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxappGoodsItemsDetailActivityMergeService {

	public Map<String, Object> applyActivityBranchToDetail(Map<String, Object> result, Map<String, Object> promotionActivityData, long currentItemId,
			long companyId, long userId, long distributorId, SkuValidMarketingActivityService marketingActivity) {
		String activityType = String.valueOf(promotionActivityData.get("activity_type"));
		result.put("activity_type", activityType);
		@SuppressWarnings("unchecked")
		Map<String, Map<String, Object>> listMap =
				promotionActivityData.get("list") instanceof Map<?, ?> m ? (Map<String, Map<String, Object>>) m : Map.of();
		Map<String, Object> activityRow = listGet(listMap, currentItemId);
		result = replaceItemInfo(result, activityRow, activityType);
		result.put("item_total_store", result.get("store"));
		Object specRaw = result.get("spec_items");
		if (specRaw instanceof List<?> specList && !specList.isEmpty()) {
			long totalStore = 0L;
			List<Map<String, Object>> specMaps = new ArrayList<>();
			for (Object o : specList) {
				if (o instanceof Map<?, ?> rm) {
					specMaps.add((Map<String, Object>) rm);
				}
			}
			Iterator<Map<String, Object>> it = specMaps.iterator();
			while (it.hasNext()) {
				Map<String, Object> row = it.next();
				row.put("activity_type", activityType);
				Object iidObj = row.get("item_id");
				long sid = iidObj instanceof Number n ? n.longValue() : 0L;
				Map<String, Object> ai = listGet(listMap, sid);
				if (ai == null) {
					it.remove();
					continue;
				}
				replaceItemInfo(row, ai, activityType);
				totalStore += toLongOrZero(row.get("store"));
			}
			specMaps.sort((a, b) -> toBigDecimalOrZero(a.get("act_price")).compareTo(toBigDecimalOrZero(b.get("act_price"))));
			result.put("spec_items", specMaps);
			if (!specMaps.isEmpty()) {
				Map<String, Object> first = specMaps.get(0);
				for (Map.Entry<String, Object> e : first.entrySet()) {
					result.put(e.getKey(), e.getValue());
				}
				result.put("item_total_store", totalStore);
			}
		}
		if ("group".equals(activityType)) {
			Object gl = promotionActivityData.get("groups_list");
			result.put("groups_list", gl != null ? gl : List.of());
		}
		if ("limited_time_sale".equals(activityType)) {
			long goodsId = toLong(result.get("goods_id"));
			List<Map<String, Object>> pa = goodsId > 0L
					? marketingActivity.getValidMarketingActivityByGoodsId(companyId, goodsId, userId, distributorId)
					: marketingActivity.getValidMarketingActivityByItemId(companyId, toLong(result.get("item_id")), userId, distributorId);
			result.put("promotion_activity", pa);
		}
		result.put("activity_info", promotionActivityData.get("info"));
		result.put("promoter_price", 0L);
		return result;
	}

	private Map<String, Object> replaceItemInfo(Map<String, Object> itemInfo, Map<String, Object> activityItemInfo, String activityType) {
		if (activityItemInfo == null || activityItemInfo.isEmpty()) {
			return itemInfo;
		}
		itemInfo.put("limit_num", activityItemInfo.get("limit_num"));
		Object ss = activityItemInfo.get("sales_store");
		itemInfo.put("sales_store", ss != null ? ss : 0);
		itemInfo.put("act_price", activityItemInfo.get("activity_price"));
		Object st = activityItemInfo.get("store");
		if (st != null) {
			itemInfo.put("store", st);
		}
		if ("limited_time_sale".equals(activityType) || "seckill".equals(activityType)) {
			itemInfo.put("seckill_id", activityItemInfo.get("seckill_id"));
		}
		return itemInfo;
	}

	private static Map<String, Object> listGet(Map<String, Map<String, Object>> listMap, long itemId) {
		if (listMap == null) {
			return null;
		}
		Map<String, Object> r = listMap.get(String.valueOf(itemId));
		if (r != null) {
			return r;
		}
		return listMap.get(itemId);
	}

	private static long toLongOrZero(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long toLong(Object o) {
		return toLongOrZero(o);
	}

	private static BigDecimal toBigDecimalOrZero(Object o) {
		if (o == null) {
			return BigDecimal.ZERO;
		}
		if (o instanceof BigDecimal bd) {
			return bd;
		}
		if (o instanceof Number n) {
			return BigDecimal.valueOf(n.doubleValue());
		}
		try {
			return new BigDecimal(o.toString().trim());
		} catch (Exception e) {
			return BigDecimal.ZERO;
		}
	}
}
