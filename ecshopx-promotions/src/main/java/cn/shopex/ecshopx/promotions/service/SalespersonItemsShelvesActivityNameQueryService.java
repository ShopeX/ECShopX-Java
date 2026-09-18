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

import cn.shopex.ecshopx.promotions.domain.MarketingActivity;
import cn.shopex.ecshopx.promotions.domain.PackagePromotions;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsActivity;
import cn.shopex.ecshopx.promotions.domain.SeckillActivity;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.PackagePromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillActivityMapper;
import cn.shopex.ecshopx.salesperson.domain.SalespersonItemsShelves;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonItemsShelvesMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Resolves display names for promotion activities referenced on item detail (shelves-style tagging).
 * Uses {@code salesperson_items_shelves} to align branch and scope with shelf-backed promotions, then loads names from promotions mappers.
 */
@Service
public class SalespersonItemsShelvesActivityNameQueryService {

	private final SalespersonItemsShelvesMapper salespersonItemsShelvesMapper;
	private final MarketingActivityMapper marketingActivityMapper;
	private final SeckillActivityMapper seckillActivityMapper;
	private final PromotionGroupsActivityMapper promotionGroupsActivityMapper;
	private final PackagePromotionsMapper packagePromotionsMapper;

	public SalespersonItemsShelvesActivityNameQueryService(
			SalespersonItemsShelvesMapper salespersonItemsShelvesMapper,
			MarketingActivityMapper marketingActivityMapper,
			SeckillActivityMapper seckillActivityMapper,
			PromotionGroupsActivityMapper promotionGroupsActivityMapper,
			PackagePromotionsMapper packagePromotionsMapper) {
		this.salespersonItemsShelvesMapper = salespersonItemsShelvesMapper;
		this.marketingActivityMapper = marketingActivityMapper;
		this.seckillActivityMapper = seckillActivityMapper;
		this.promotionGroupsActivityMapper = promotionGroupsActivityMapper;
		this.packagePromotionsMapper = packagePromotionsMapper;
	}

	@Nullable
	public String resolveActivityName(long companyId, long itemId, long promotionId, String tagType) {
		if (promotionId < 1L) {
			return null;
		}
		String branchKey = resolveBranchKey(companyId, itemId, promotionId, tagType);
		if (!StringUtils.hasText(branchKey)) {
			return null;
		}
		String tt = branchKey.trim();
		return switch (tt) {
			case "full_discount", "full_minus", "full_gift" -> marketingName(companyId, promotionId);
			case "normal", "limited_time_sale" -> seckillName(companyId, promotionId);
			case "single_group" -> groupName(companyId, promotionId);
			case "package" -> packageName(companyId, promotionId);
			default -> null;
		};
	}

	private String resolveBranchKey(long companyId, long itemId, long promotionId, String tagType) {
		if (itemId >= 1L) {
			LambdaQueryWrapper<SalespersonItemsShelves> w = new LambdaQueryWrapper<>();
			w.eq(SalespersonItemsShelves::getCompanyId, companyId)
					.eq(SalespersonItemsShelves::getItemId, itemId)
					.eq(SalespersonItemsShelves::getActivityId, promotionId);
			SalespersonItemsShelves shelf = salespersonItemsShelvesMapper.selectOne(w);
			if (shelf != null) {
				String fromShelf = branchKeyFromShelfActivityType(shelf.getActivityType());
				if (StringUtils.hasText(fromShelf)) {
					return fromShelf;
				}
			}
		}
		return tagType != null ? tagType.trim() : "";
	}

	@Nullable
	private static String branchKeyFromShelfActivityType(@Nullable String activityType) {
		if (!StringUtils.hasText(activityType)) {
			return null;
		}
		String at = activityType.trim();
		return switch (at) {
			case "full_discount", "full_minus", "full_gift" -> at;
			case "seckill" -> "normal";
			case "limited_time_sale" -> "limited_time_sale";
			case "group" -> "single_group";
			case "package" -> "package";
			default -> null;
		};
	}

	@Nullable
	private String marketingName(long companyId, long marketingId) {
		if (marketingId < 1L) {
			return null;
		}
		LambdaQueryWrapper<MarketingActivity> w = new LambdaQueryWrapper<>();
		w.eq(MarketingActivity::getCompanyId, companyId).eq(MarketingActivity::getMarketingId, marketingId);
		MarketingActivity row = marketingActivityMapper.selectOne(w);
		return row != null && StringUtils.hasText(row.getMarketingName()) ? row.getMarketingName() : null;
	}

	@Nullable
	private String seckillName(long companyId, long seckillId) {
		if (seckillId < 1L) {
			return null;
		}
		LambdaQueryWrapper<SeckillActivity> w = new LambdaQueryWrapper<>();
		w.eq(SeckillActivity::getCompanyId, companyId).eq(SeckillActivity::getSeckillId, seckillId);
		SeckillActivity row = seckillActivityMapper.selectOne(w);
		return row != null && StringUtils.hasText(row.getActivityName()) ? row.getActivityName() : null;
	}

	@Nullable
	private String groupName(long companyId, long groupsActivityId) {
		if (groupsActivityId < 1L) {
			return null;
		}
		LambdaQueryWrapper<PromotionGroupsActivity> w = new LambdaQueryWrapper<>();
		w.eq(PromotionGroupsActivity::getCompanyId, companyId)
				.eq(PromotionGroupsActivity::getGroupsActivityId, groupsActivityId);
		PromotionGroupsActivity row = promotionGroupsActivityMapper.selectOne(w);
		return row != null && StringUtils.hasText(row.getActName()) ? row.getActName() : null;
	}

	@Nullable
	private String packageName(long companyId, long packageId) {
		if (packageId < 1L) {
			return null;
		}
		LambdaQueryWrapper<PackagePromotions> w = new LambdaQueryWrapper<>();
		w.eq(PackagePromotions::getCompanyId, companyId).eq(PackagePromotions::getPackageId, packageId);
		PackagePromotions row = packagePromotionsMapper.selectOne(w);
		return row != null && StringUtils.hasText(row.getPackageName()) ? row.getPackageName() : null;
	}
}
