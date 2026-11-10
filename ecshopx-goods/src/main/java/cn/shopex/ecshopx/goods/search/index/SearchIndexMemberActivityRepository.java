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

package cn.shopex.ecshopx.goods.search.index;

import cn.shopex.ecshopx.promotions.domain.MemberPrice;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsActivity;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsRelGoods;
import cn.shopex.ecshopx.promotions.domain.SeckillRelGoods;
import cn.shopex.ecshopx.promotions.mapper.MemberPriceMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsRelGoodsMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillRelGoodsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Repository
public class SearchIndexMemberActivityRepository {

	private final MemberPriceMapper memberPriceMapper;
	private final SeckillRelGoodsMapper seckillRelGoodsMapper;
	private final PromotionGroupsRelGoodsMapper promotionGroupsRelGoodsMapper;
	private final PromotionGroupsActivityMapper promotionGroupsActivityMapper;
	private final ObjectMapper objectMapper;

	public SearchIndexMemberActivityRepository(
			MemberPriceMapper memberPriceMapper,
			SeckillRelGoodsMapper seckillRelGoodsMapper,
			PromotionGroupsRelGoodsMapper promotionGroupsRelGoodsMapper,
			PromotionGroupsActivityMapper promotionGroupsActivityMapper,
			ObjectMapper objectMapper) {
		this.memberPriceMapper = memberPriceMapper;
		this.seckillRelGoodsMapper = seckillRelGoodsMapper;
		this.promotionGroupsRelGoodsMapper = promotionGroupsRelGoodsMapper;
		this.promotionGroupsActivityMapper = promotionGroupsActivityMapper;
		this.objectMapper = objectMapper;
	}

	public SearchIndexMemberPriceMaterializer.Buckets loadMemberPrices(
			long companyId, long defaultItemId, Collection<Long> skuItemIds) {
		LinkedHashSet<Long> probeOrder = new LinkedHashSet<>();
		probeOrder.add(defaultItemId);
		if (skuItemIds != null) {
			probeOrder.addAll(skuItemIds);
		}
		for (long itemId : probeOrder) {
			if (itemId <= 0) {
				continue;
			}
			MemberPrice row =
					memberPriceMapper.selectOne(
							new LambdaQueryWrapper<MemberPrice>()
									.eq(MemberPrice::getCompanyId, companyId)
									.eq(MemberPrice::getItemId, itemId)
									.last("LIMIT 1"));
			if (row == null || !StringUtils.hasText(row.getMprice())) {
				continue;
			}
			SearchIndexMemberPriceMaterializer.Buckets buckets =
					SearchIndexMemberPriceMaterializer.fromStorageJson(objectMapper, row.getMprice());
			if (!buckets.grade().isEmpty() || !buckets.vipGrade().isEmpty()) {
				return buckets;
			}
		}
		return SearchIndexMemberPriceMaterializer.fromStorageJson(objectMapper, null);
	}

	public List<SearchIndexSource.Activity> loadActivities(long companyId, Collection<Long> skuItemIds) {
		if (CollectionUtils.isEmpty(skuItemIds)) {
			return List.of();
		}
		List<SearchIndexSource.Activity> out = new ArrayList<>();
		out.addAll(mapSeckillActivities(companyId, skuItemIds));
		out.addAll(mapGroupActivities(companyId, skuItemIds));
		return List.copyOf(out);
	}

	private List<SearchIndexSource.Activity> mapSeckillActivities(long companyId, Collection<Long> skuItemIds) {
		List<SeckillRelGoods> rows =
				seckillRelGoodsMapper.selectList(
						new LambdaQueryWrapper<SeckillRelGoods>()
								.eq(SeckillRelGoods::getCompanyId, companyId)
								.in(SeckillRelGoods::getItemId, skuItemIds)
								.eq(SeckillRelGoods::getDisabled, false));
		List<SearchIndexSource.Activity> mapped = new ArrayList<>(rows.size());
		for (SeckillRelGoods rel : rows) {
			if (rel.getSeckillId() == null || rel.getActivityPrice() == null) {
				continue;
			}
			String indexType = seckillTypeToIndexType(rel.getSeckillType());
			if (indexType == null) {
				continue;
			}
			long startAt = resolveSeckillStartEpoch(rel);
			long endAt = rel.getActivityEndTime() != null ? rel.getActivityEndTime().longValue() : 0L;
			long priceFen = rel.getActivityPrice().longValue();
			if (priceFen <= 0) {
				continue;
			}
			mapped.add(
					new SearchIndexSource.Activity(
							indexType, rel.getSeckillId(), priceFen, startAt, endAt, null));
		}
		return mapped;
	}

	private static String seckillTypeToIndexType(String seckillType) {
		if (!StringUtils.hasText(seckillType)) {
			return "seckill";
		}
		return switch (seckillType.trim()) {
			case "normal" -> "seckill";
			case "limited_time_sale" -> "limited_time_sale";
			default -> null;
		};
	}

	private static long resolveSeckillStartEpoch(SeckillRelGoods rel) {
		if (rel.getActivityStartTime() != null && rel.getActivityStartTime() > 0) {
			return rel.getActivityStartTime().longValue();
		}
		if (rel.getActivityReleaseTime() != null && rel.getActivityReleaseTime() > 0) {
			return rel.getActivityReleaseTime().longValue();
		}
		return 0L;
	}

	private List<SearchIndexSource.Activity> mapGroupActivities(long companyId, Collection<Long> skuItemIds) {
		List<PromotionGroupsRelGoods> relRows =
				promotionGroupsRelGoodsMapper.selectList(
						new LambdaQueryWrapper<PromotionGroupsRelGoods>()
								.eq(PromotionGroupsRelGoods::getCompanyId, companyId)
								.in(PromotionGroupsRelGoods::getItemId, skuItemIds));
		if (relRows.isEmpty()) {
			return List.of();
		}
		Set<Long> activityIds = new HashSet<>();
		for (PromotionGroupsRelGoods rel : relRows) {
			if (rel.getGroupsActivityId() != null && rel.getGroupsActivityId() > 0) {
				activityIds.add(rel.getGroupsActivityId());
			}
		}
		if (activityIds.isEmpty()) {
			return List.of();
		}
		List<PromotionGroupsActivity> activities =
				promotionGroupsActivityMapper.selectList(
						new LambdaQueryWrapper<PromotionGroupsActivity>()
								.eq(PromotionGroupsActivity::getCompanyId, companyId)
								.in(PromotionGroupsActivity::getGroupsActivityId, activityIds)
								.eq(PromotionGroupsActivity::getDisabled, false));
		Map<Long, PromotionGroupsActivity> activityById = new HashMap<>();
		for (PromotionGroupsActivity act : activities) {
			if (act.getGroupsActivityId() != null) {
				activityById.put(act.getGroupsActivityId(), act);
			}
		}
		List<SearchIndexSource.Activity> mapped = new ArrayList<>();
		for (PromotionGroupsRelGoods rel : relRows) {
			Long actId = rel.getGroupsActivityId();
			if (actId == null || actId <= 0) {
				continue;
			}
			PromotionGroupsActivity act = activityById.get(actId);
			if (act == null) {
				continue;
			}
			long priceFen = rel.getActivityPrice() != null ? rel.getActivityPrice() : 0L;
			if (priceFen <= 0) {
				continue;
			}
			long startAt = act.getBeginTime() != null ? act.getBeginTime() : 0L;
			long endAt = act.getEndTime() != null ? act.getEndTime() : 0L;
			mapped.add(new SearchIndexSource.Activity("group", actId, priceFen, startAt, endAt, null));
		}
		return mapped;
	}
}
