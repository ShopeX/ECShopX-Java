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

import cn.shopex.ecshopx.promotions.domain.PackageItemPromotions;
import cn.shopex.ecshopx.promotions.domain.PackageMainItemPromotions;
import cn.shopex.ecshopx.promotions.domain.PackagePromotions;
import cn.shopex.ecshopx.promotions.mapper.PackageItemPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.PackageMainItemPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.PackagePromotionsMapper;
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
public class PackagePromotionItemsShelvesSyncContributor implements SalespersonItemsShelvesSyncContributor {

	private final PackagePromotionsMapper packagePromotionsMapper;
	private final PackageMainItemPromotionsMapper packageMainItemPromotionsMapper;
	private final PackageItemPromotionsMapper packageItemPromotionsMapper;
	private final SalespersonItemsShelvesMapper salespersonItemsShelvesMapper;

	public PackagePromotionItemsShelvesSyncContributor(
			PackagePromotionsMapper packagePromotionsMapper,
			PackageMainItemPromotionsMapper packageMainItemPromotionsMapper,
			PackageItemPromotionsMapper packageItemPromotionsMapper,
			SalespersonItemsShelvesMapper salespersonItemsShelvesMapper) {
		this.packagePromotionsMapper = packagePromotionsMapper;
		this.packageMainItemPromotionsMapper = packageMainItemPromotionsMapper;
		this.packageItemPromotionsMapper = packageItemPromotionsMapper;
		this.salespersonItemsShelvesMapper = salespersonItemsShelvesMapper;
	}

	@Override
	public boolean supports(String activityType) {
		return "package".equals(activityType);
	}

	@Override
	public void sync(long companyId, long packageId) {
		LambdaQueryWrapper<SalespersonItemsShelves> deleteWrap = new LambdaQueryWrapper<>();
		deleteWrap
				.eq(SalespersonItemsShelves::getCompanyId, companyId)
				.eq(SalespersonItemsShelves::getActivityId, packageId)
				.eq(SalespersonItemsShelves::getActivityType, "package");
		salespersonItemsShelvesMapper.delete(deleteWrap);

		PackagePromotions pkg = packagePromotionsMapper.selectById(packageId);
		if (pkg == null || !Objects.equals(pkg.getCompanyId(), companyId)) {
			return;
		}
		if (!"AGREE".equals(pkg.getPackageStatus())) {
			return;
		}
		long now = System.currentTimeMillis() / 1000L;
		if (pkg.getEndTime() != null && now > pkg.getEndTime().longValue()) {
			return;
		}

		Set<Long> itemIds = new LinkedHashSet<>();
		LambdaQueryWrapper<PackageMainItemPromotions> mainWrap = new LambdaQueryWrapper<>();
		mainWrap
				.eq(PackageMainItemPromotions::getPackageId, packageId)
				.eq(PackageMainItemPromotions::getCompanyId, companyId);
		List<PackageMainItemPromotions> mains = packageMainItemPromotionsMapper.selectList(mainWrap);
		for (PackageMainItemPromotions m : mains) {
			if (m.getMainItemId() != null && m.getMainItemId() > 0L) {
				itemIds.add(m.getMainItemId());
			}
		}

		LambdaQueryWrapper<PackageItemPromotions> itemWrap = new LambdaQueryWrapper<>();
		itemWrap.eq(PackageItemPromotions::getPackageId, packageId).eq(PackageItemPromotions::getCompanyId, companyId);
		List<PackageItemPromotions> items = packageItemPromotionsMapper.selectList(itemWrap);
		for (PackageItemPromotions it : items) {
			if (it.getItemId() != null && it.getItemId() > 0L) {
				itemIds.add(it.getItemId());
			}
		}

		long start = pkg.getStartTime() == null ? 0L : pkg.getStartTime().longValue();
		long end = pkg.getEndTime() == null ? 0L : pkg.getEndTime().longValue();
		for (Long itemId : itemIds) {
			SalespersonItemsShelves row = new SalespersonItemsShelves();
			row.setCompanyId(companyId);
			row.setActivityId(packageId);
			row.setActivityType("package");
			row.setDistributorId(0L);
			row.setItemId(itemId);
			row.setStartTime(start);
			row.setEndTime(end);
			salespersonItemsShelvesMapper.insert(row);
		}
	}
}
