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

package cn.shopex.ecshopx.promotions.service.salesperson;

import cn.shopex.ecshopx.promotions.domain.MarketingActivity;
import cn.shopex.ecshopx.promotions.domain.MarketingActivityItems;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityItemsMapper;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityMapper;
import cn.shopex.ecshopx.salesperson.domain.SalespersonItemsShelves;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonItemsShelvesMapper;
import cn.shopex.ecshopx.salesperson.service.SalespersonItemsShelvesSyncContributor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MarketingActivityItemsShelvesSyncContributor implements SalespersonItemsShelvesSyncContributor {

	private static final Set<String> SUPPORTED_MARKETING_TYPES = Set.of(
			"full_discount",
			"full_minus",
			"full_gift",
			"plus_price_buy",
			"single_gift",
			"self_select",
			"full_court_gift",
			"member_preference");

	private final MarketingActivityMapper marketingActivityMapper;
	private final MarketingActivityItemsMapper marketingActivityItemsMapper;
	private final SalespersonItemsShelvesMapper salespersonItemsShelvesMapper;

	public MarketingActivityItemsShelvesSyncContributor(
			MarketingActivityMapper marketingActivityMapper,
			MarketingActivityItemsMapper marketingActivityItemsMapper,
			SalespersonItemsShelvesMapper salespersonItemsShelvesMapper) {
		this.marketingActivityMapper = marketingActivityMapper;
		this.marketingActivityItemsMapper = marketingActivityItemsMapper;
		this.salespersonItemsShelvesMapper = salespersonItemsShelvesMapper;
	}

	@Override
	public boolean supports(String activityType) {
		return StringUtils.hasText(activityType) && SUPPORTED_MARKETING_TYPES.contains(activityType);
	}

	@Override
	public void sync(long companyId, long activityId) {
		MarketingActivity activity = marketingActivityMapper.selectById(activityId);
		if (activity == null || !Objects.equals(activity.getCompanyId(), companyId)) {
			return;
		}
		String activityType = activity.getMarketingType();
		if (!StringUtils.hasText(activityType) || !supports(activityType)) {
			return;
		}

		LambdaQueryWrapper<SalespersonItemsShelves> deleteWrap = new LambdaQueryWrapper<>();
		deleteWrap
				.eq(SalespersonItemsShelves::getCompanyId, companyId)
				.eq(SalespersonItemsShelves::getActivityId, activityId)
				.eq(SalespersonItemsShelves::getActivityType, activityType);
		salespersonItemsShelvesMapper.delete(deleteWrap);

		LambdaQueryWrapper<MarketingActivityItems> itemWrap = new LambdaQueryWrapper<>();
		itemWrap
				.eq(MarketingActivityItems::getMarketingId, activityId)
				.eq(MarketingActivityItems::getCompanyId, companyId);
		List<MarketingActivityItems> itemRows = marketingActivityItemsMapper.selectList(itemWrap);

		Set<Long> itemIds = new LinkedHashSet<>();
		for (MarketingActivityItems row : itemRows) {
			if (row.getItemId() != null && row.getItemId() > 0L) {
				itemIds.add(row.getItemId());
			}
		}

		int st = activity.getStartTime() != null ? activity.getStartTime() : 0;
		int et = activity.getEndTime() != null ? activity.getEndTime() : 0;
		long start = st;
		long end = et;
		for (Long itemId : itemIds) {
			SalespersonItemsShelves shelf = new SalespersonItemsShelves();
			shelf.setCompanyId(companyId);
			shelf.setActivityId(activityId);
			shelf.setActivityType(activityType);
			shelf.setDistributorId(0L);
			shelf.setItemId(itemId);
			shelf.setStartTime(start);
			shelf.setEndTime(end);
			salespersonItemsShelvesMapper.insert(shelf);
		}
	}
}
