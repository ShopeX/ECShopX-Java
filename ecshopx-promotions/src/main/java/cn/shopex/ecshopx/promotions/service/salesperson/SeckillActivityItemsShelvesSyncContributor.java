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

import cn.shopex.ecshopx.promotions.domain.SeckillActivity;
import cn.shopex.ecshopx.promotions.domain.SeckillRelGoods;
import cn.shopex.ecshopx.promotions.mapper.SeckillActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillRelGoodsMapper;
import cn.shopex.ecshopx.promotions.util.SeckillShopIdCsvParser;
import cn.shopex.ecshopx.salesperson.domain.SalespersonItemsShelves;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonItemsShelvesMapper;
import cn.shopex.ecshopx.salesperson.service.SalespersonItemsShelvesSyncContributor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SeckillActivityItemsShelvesSyncContributor implements SalespersonItemsShelvesSyncContributor {

	private final SeckillActivityMapper seckillActivityMapper;
	private final SeckillRelGoodsMapper seckillRelGoodsMapper;
	private final SalespersonItemsShelvesMapper salespersonItemsShelvesMapper;

	public SeckillActivityItemsShelvesSyncContributor(
			SeckillActivityMapper seckillActivityMapper,
			SeckillRelGoodsMapper seckillRelGoodsMapper,
			SalespersonItemsShelvesMapper salespersonItemsShelvesMapper) {
		this.seckillActivityMapper = seckillActivityMapper;
		this.seckillRelGoodsMapper = seckillRelGoodsMapper;
		this.salespersonItemsShelvesMapper = salespersonItemsShelvesMapper;
	}

	@Override
	public boolean supports(String activityType) {
		return "seckill".equals(activityType) || "limited_time_sale".equals(activityType);
	}

	@Override
	public void sync(long companyId, long activityId) {
		SeckillActivity activity = seckillActivityMapper.selectById(activityId);
		if (activity == null || !Objects.equals(activity.getCompanyId(), companyId)) {
			return;
		}

		String shelfActivityType =
				"normal".equals(activity.getSeckillType()) ? "seckill" : "limited_time_sale";

		long nowEpoch = System.currentTimeMillis() / 1000L;
		Integer endIntEarly = activity.getActivityEndTime();
		if (endIntEarly != null && nowEpoch > endIntEarly.longValue()) {
			return;
		}

		LambdaQueryWrapper<SalespersonItemsShelves> deleteWrap = new LambdaQueryWrapper<>();
		deleteWrap
				.eq(SalespersonItemsShelves::getCompanyId, companyId)
				.eq(SalespersonItemsShelves::getActivityId, activityId)
				.eq(SalespersonItemsShelves::getActivityType, shelfActivityType);
		salespersonItemsShelvesMapper.delete(deleteWrap);

		LambdaQueryWrapper<SeckillRelGoods> relWrap = new LambdaQueryWrapper<>();
		relWrap
				.eq(SeckillRelGoods::getCompanyId, companyId)
				.eq(SeckillRelGoods::getSeckillId, activityId)
				.eq(SeckillRelGoods::getDisabled, false)
				.gt(SeckillRelGoods::getItemId, 0L)
				.and(
						q ->
								q.isNull(SeckillRelGoods::getIsShow)
										.or()
										.eq(SeckillRelGoods::getIsShow, true));
		List<SeckillRelGoods> relRows = seckillRelGoodsMapper.selectList(relWrap);

		Set<Long> itemIds = new LinkedHashSet<>();
		for (SeckillRelGoods row : relRows) {
			if (row.getItemId() != null && row.getItemId() > 0L) {
				itemIds.add(row.getItemId());
			}
		}
		if (itemIds.isEmpty()) {
			return;
		}

		List<Long> distributorIds = resolveDistributorIdsForShelves(activity);

		int st = activity.getActivityStartTime() != null ? activity.getActivityStartTime() : 0;
		int et = activity.getActivityEndTime() != null ? activity.getActivityEndTime() : 0;
		long start = st;
		long end = et;

		for (Long itemId : itemIds) {
			for (Long distributorId : distributorIds) {
				SalespersonItemsShelves shelf = new SalespersonItemsShelves();
				shelf.setCompanyId(companyId);
				shelf.setActivityId(activityId);
				shelf.setActivityType(shelfActivityType);
				shelf.setDistributorId(distributorId);
				shelf.setItemId(itemId);
				shelf.setStartTime(start);
				shelf.setEndTime(end);
				salespersonItemsShelvesMapper.insert(shelf);
			}
		}
	}

	/** {@code use_bound} null/0 → all shops ({@code 0L} only); else CSV shops via {@link SeckillShopIdCsvParser}. */
	private static List<Long> resolveDistributorIdsForShelves(SeckillActivity activity) {
		Integer useBound = activity.getUseBound();
		if (useBound == null || useBound == 0) {
			return List.of(0L);
		}
		List<Long> parsed =
				SeckillShopIdCsvParser.parsePositiveShopIdsFromCsv(
						activity.getDistributorId() == null ? "" : activity.getDistributorId());
		if (parsed.isEmpty()) {
			return List.of(0L);
		}
		return parsed;
	}
}
