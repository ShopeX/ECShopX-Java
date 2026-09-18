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

import cn.shopex.ecshopx.promotions.dto.ItemActivityCheckRow;
import cn.shopex.ecshopx.promotions.dto.LimitItemTagCheckRow;
import cn.shopex.ecshopx.promotions.dto.LimitPromotionNameRow;
import cn.shopex.ecshopx.promotions.dto.MarketingActivityItemTagCheckRow;
import cn.shopex.ecshopx.promotions.dto.MarketingActivityLabelRow;
import cn.shopex.ecshopx.promotions.mapper.LimitItemPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.LimitPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityItemsMapper;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ItemsTagActivityCheckService {

	private final LimitItemPromotionsMapper limitItemPromotionsMapper;
	private final MarketingActivityItemsMapper marketingActivityItemsMapper;
	private final LimitPromotionsMapper limitPromotionsMapper;
	private final MarketingActivityMapper marketingActivityMapper;

	public ItemsTagActivityCheckService(LimitItemPromotionsMapper limitItemPromotionsMapper,
			MarketingActivityItemsMapper marketingActivityItemsMapper, LimitPromotionsMapper limitPromotionsMapper,
			MarketingActivityMapper marketingActivityMapper) {
		this.limitItemPromotionsMapper = limitItemPromotionsMapper;
		this.marketingActivityItemsMapper = marketingActivityItemsMapper;
		this.limitPromotionsMapper = limitPromotionsMapper;
		this.marketingActivityMapper = marketingActivityMapper;
	}

	public boolean checkActivity(List<ItemActivityCheckRow> itemRows, List<Long> tagIds, long companyId,
			AtomicReference<String> errMsgOut) {
		if (tagIds == null || tagIds.isEmpty()) {
			return true;
		}
		if (itemRows == null || itemRows.isEmpty()) {
			return true;
		}

		List<Long> requestItemIds = itemRows.get(0).getAllRequestItemIds();
		if (requestItemIds == null) {
			requestItemIds = List.of();
		}

		List<Long> brandIds = itemRows.stream()
				.map(ItemActivityCheckRow::getBrandId)
				.filter(Objects::nonNull)
				.distinct()
				.collect(Collectors.toList());
		List<Long> categoryIds = itemRows.stream()
				.map(ItemActivityCheckRow::getMainCatId)
				.filter(Objects::nonNull)
				.distinct()
				.collect(Collectors.toList());

		int now = (int) (System.currentTimeMillis() / 1000L);

		List<LimitItemTagCheckRow> relItemArr = limitItemPromotionsMapper.selectActiveRowsForTagActivityCheck(companyId,
				now, tagIds, requestItemIds, brandIds, categoryIds);

		for (int k = 0; k < relItemArr.size(); k++) {
			for (int kk = k + 1; kk < relItemArr.size(); kk++) {
				LimitItemTagCheckRow v = relItemArr.get(k);
				LimitItemTagCheckRow vv = relItemArr.get(kk);
				int vst = nz(v.getStartTime());
				int vet = nz(v.getEndTime());
				int vvst = nz(vv.getStartTime());
				int vvet = nz(vv.getEndTime());
				if (vvst < vet && vvet > vst) {
					List<Long> limitIds = new ArrayList<>(2);
					if (v.getLimitId() != null) {
						limitIds.add(v.getLimitId());
					}
					if (vv.getLimitId() != null) {
						limitIds.add(vv.getLimitId());
					}
					if (!limitIds.isEmpty()) {
						List<LimitPromotionNameRow> names = limitPromotionsMapper.selectLimitNamesByLimitIds(limitIds);
						if (names != null && !names.isEmpty()) {
							String joined = names.stream()
									.map(LimitPromotionNameRow::getLimitName)
									.filter(Objects::nonNull)
									.collect(Collectors.joining(", "));
							if (!joined.isEmpty()) {
								errMsgOut.set("商品标签导致限购活动 " + joined + " 冲突");
							}
						}
					}
					return false;
				}
			}
		}

		List<MarketingActivityItemTagCheckRow> relActivityItems = marketingActivityItemsMapper
				.selectActiveRowsForTagActivityCheck(companyId, now, tagIds, requestItemIds, brandIds, categoryIds);

		Map<String, List<MarketingActivityItemTagCheckRow>> relItems = new LinkedHashMap<>();
		for (MarketingActivityItemTagCheckRow row : relActivityItems) {
			String mt = row.getMarketingType();
			String key;
			if ("full_discount".equals(mt) || "full_minus".equals(mt)) {
				key = "满减满折";
			} else {
				key = mt != null ? mt : "";
			}
			relItems.computeIfAbsent(key, x -> new ArrayList<>()).add(row);
		}

		for (List<MarketingActivityItemTagCheckRow> group : relItems.values()) {
			for (int k = 0; k < group.size(); k++) {
				for (int kk = k + 1; kk < group.size(); kk++) {
					MarketingActivityItemTagCheckRow v = group.get(k);
					MarketingActivityItemTagCheckRow vv = group.get(kk);
					int vst = nz(v.getStartTime());
					int vet = nz(v.getEndTime());
					int vvst = nz(vv.getStartTime());
					int vvet = nz(vv.getEndTime());
					if (vvst < vet && vvet > vst) {
						List<Long> marketingIds = new ArrayList<>(2);
						if (v.getMarketingId() != null) {
							marketingIds.add(v.getMarketingId());
						}
						if (vv.getMarketingId() != null) {
							marketingIds.add(vv.getMarketingId());
						}
						if (!marketingIds.isEmpty()) {
							List<MarketingActivityLabelRow> activityInfo =
									marketingActivityMapper.selectMarketingLabelsByMarketingIds(marketingIds);
							if (activityInfo != null && !activityInfo.isEmpty()) {
								List<String> tips = new ArrayList<>();
								for (MarketingActivityLabelRow activity : activityInfo) {
									String tag = activity.getPromotionTag() != null ? activity.getPromotionTag() : "";
									String name = activity.getMarketingName() != null ? activity.getMarketingName() : "";
									tips.add("【" + tag + "】" + name);
								}
								errMsgOut.set("商品标签导致活动 " + String.join(", ", tips) + " 冲突");
							}
						}
						return false;
					}
				}
			}
		}

		return true;
	}

	private static int nz(Integer t) {
		return t != null ? t : 0;
	}
}
