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

import cn.shopex.ecshopx.common.dispatch.SavePromotionItemTagJobDispatchPublisher;
import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.promotions.domain.MarketingActivity;
import cn.shopex.ecshopx.promotions.domain.PromotionsItemsTag;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionsItemsTagMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CreateItemSuccessPromotionExecutionService {

	private static final Set<String> TAG_SYNC_MARKETING_TYPES = Set.of(
			"full_discount",
			"full_minus",
			"full_gift",
			"plus_price_buy",
			"single_gift",
			"self_select",
			"full_court_gift",
			"member_preference");

	private final MarketingActivityMapper marketingActivityMapper;
	private final MarketingActivityCatalogAccess marketingActivityCatalogAccess;
	private final PromotionsItemsTagMapper promotionsItemsTagMapper;
	private final SavePromotionItemTagJobDispatchPublisher savePromotionItemTagJobDispatchPublisher;

	public CreateItemSuccessPromotionExecutionService(
			MarketingActivityMapper marketingActivityMapper,
			MarketingActivityCatalogAccess marketingActivityCatalogAccess,
			PromotionsItemsTagMapper promotionsItemsTagMapper,
			SavePromotionItemTagJobDispatchPublisher savePromotionItemTagJobDispatchPublisher) {
		this.marketingActivityMapper = marketingActivityMapper;
		this.marketingActivityCatalogAccess = marketingActivityCatalogAccess;
		this.promotionsItemsTagMapper = promotionsItemsTagMapper;
		this.savePromotionItemTagJobDispatchPublisher = savePromotionItemTagJobDispatchPublisher;
	}

	public void handleItemCreateSuccess(Map<String, Object> entities, List<Long> itemIds) {
		long companyId = readLong(entities.get("company_id"));
		if (companyId <= 0L) {
			return;
		}
		List<Long> normalizedIds = itemIds.stream().filter(Objects::nonNull).filter(id -> id > 0L).distinct().toList();
		if (normalizedIds.isEmpty()) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaQueryWrapper<MarketingActivity> aw = new LambdaQueryWrapper<>();
		aw.eq(MarketingActivity::getCompanyId, companyId)
				.le(MarketingActivity::getStartTime, now)
				.ge(MarketingActivity::getEndTime, now)
				.in(MarketingActivity::getMarketingType, TAG_SYNC_MARKETING_TYPES);
		List<MarketingActivity> activities = marketingActivityMapper.selectList(aw);
		if (activities == null || activities.isEmpty()) {
			return;
		}
		List<Long> marketingIdOrder =
				activities.stream().map(MarketingActivity::getMarketingId).filter(Objects::nonNull).distinct().toList();
		if (marketingIdOrder.isEmpty()) {
			return;
		}
		Map<Long, MarketingActivity> activityById = new LinkedHashMap<>();
		for (MarketingActivity a : activities) {
			if (a.getMarketingId() != null) {
				activityById.putIfAbsent(a.getMarketingId(), a);
			}
		}
		Map<Long, LinkedHashSet<Long>> hitsByMarketing = new LinkedHashMap<>();
		for (long itemId : normalizedIds) {
			List<Long> mids =
					marketingActivityCatalogAccess.listMarketingIdsHitBySkuItem(companyId, itemId, marketingIdOrder, now);
			for (Long mid : mids) {
				if (mid == null || mid <= 0L) {
					continue;
				}
				hitsByMarketing.computeIfAbsent(mid, k -> new LinkedHashSet<>()).add(itemId);
			}
		}
		for (Map.Entry<Long, LinkedHashSet<Long>> en : hitsByMarketing.entrySet()) {
			long marketingId = en.getKey();
			MarketingActivity act = activityById.get(marketingId);
			if (act == null) {
				continue;
			}
			String marketingType = act.getMarketingType();
			if (!StringUtils.hasText(marketingType)) {
				continue;
			}
			LinkedHashSet<Long> merged = loadExistingTaggedItemIds(companyId, marketingId, marketingType);
			merged.addAll(en.getValue());
			if (merged.isEmpty()) {
				continue;
			}
			int startT = act.getStartTime() != null ? act.getStartTime() : 0;
			int endT = act.getEndTime() != null ? act.getEndTime() : 0;
			String itemType = StringUtils.hasText(act.getItemType()) ? act.getItemType() : "normal";
			savePromotionItemTagJobDispatchPublisher.publishSavePromotionItemTag(
					companyId,
					marketingId,
					marketingType,
					startT,
					endT,
					itemType,
					new ArrayList<>(merged),
					Map.of());
		}
	}

	private LinkedHashSet<Long> loadExistingTaggedItemIds(long companyId, long marketingId, String tagType) {
		LambdaQueryWrapper<PromotionsItemsTag> w = new LambdaQueryWrapper<>();
		w.eq(PromotionsItemsTag::getCompanyId, companyId)
				.eq(PromotionsItemsTag::getPromotionId, marketingId)
				.eq(PromotionsItemsTag::getTagType, tagType);
		List<PromotionsItemsTag> rows = promotionsItemsTagMapper.selectList(w);
		LinkedHashSet<Long> out = new LinkedHashSet<>();
		if (rows == null) {
			return out;
		}
		for (PromotionsItemsTag row : rows) {
			if (row.getItemId() != null && row.getItemId() > 0L) {
				out.add(row.getItemId());
			}
		}
		return out;
	}

	private static long readLong(Object v) {
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
