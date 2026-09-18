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

import cn.shopex.ecshopx.promotions.domain.PromotionGroupsRelGoods;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsRelGoodsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PromotionGroupsRelGoodsReadService {

	private final PromotionGroupsRelGoodsMapper promotionGroupsRelGoodsMapper;

	public PromotionGroupsRelGoodsReadService(PromotionGroupsRelGoodsMapper promotionGroupsRelGoodsMapper) {
		this.promotionGroupsRelGoodsMapper = promotionGroupsRelGoodsMapper;
	}

	public List<PromotionGroupsRelGoods> listByActivityId(long companyId, long actId) {
		return promotionGroupsRelGoodsMapper.selectList(
				new LambdaQueryWrapper<PromotionGroupsRelGoods>()
						.eq(PromotionGroupsRelGoods::getCompanyId, companyId)
						.eq(PromotionGroupsRelGoods::getGroupsActivityId, actId)
						.orderByAsc(PromotionGroupsRelGoods::getItemId));
	}

	public Optional<PromotionGroupsRelGoods> findByActivityAndItem(long companyId, long actId, long itemId) {
		PromotionGroupsRelGoods row =
				promotionGroupsRelGoodsMapper.selectOne(
						new LambdaQueryWrapper<PromotionGroupsRelGoods>()
								.eq(PromotionGroupsRelGoods::getCompanyId, companyId)
								.eq(PromotionGroupsRelGoods::getGroupsActivityId, actId)
								.eq(PromotionGroupsRelGoods::getItemId, itemId)
								.last("LIMIT 1"));
		return Optional.ofNullable(row);
	}

	public boolean hasRelRows(long companyId, long actId) {
		Long count =
				promotionGroupsRelGoodsMapper.selectCount(
						new LambdaQueryWrapper<PromotionGroupsRelGoods>()
								.eq(PromotionGroupsRelGoods::getCompanyId, companyId)
								.eq(PromotionGroupsRelGoods::getGroupsActivityId, actId));
		return count != null && count > 0L;
	}

	public boolean hasRelRowsByActId(long actId) {
		Long count =
				promotionGroupsRelGoodsMapper.selectCount(
						new LambdaQueryWrapper<PromotionGroupsRelGoods>()
								.eq(PromotionGroupsRelGoods::getGroupsActivityId, actId));
		return count != null && count > 0L;
	}
}
