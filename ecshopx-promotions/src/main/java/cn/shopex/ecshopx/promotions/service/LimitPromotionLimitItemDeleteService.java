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

import cn.shopex.ecshopx.promotions.domain.LimitItemPromotions;
import cn.shopex.ecshopx.promotions.mapper.LimitItemPromotionsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

@Service("limitPromotionLimitItemDeleteService")
public class LimitPromotionLimitItemDeleteService {

	private final LimitItemPromotionsMapper limitItemPromotionsMapper;

	public LimitPromotionLimitItemDeleteService(LimitItemPromotionsMapper limitItemPromotionsMapper) {
		this.limitItemPromotionsMapper = limitItemPromotionsMapper;
	}

	public void deleteLimitItem(long companyId, long distributorId, long itemId) {
		limitItemPromotionsMapper.delete(
				new LambdaQueryWrapper<LimitItemPromotions>()
						.eq(LimitItemPromotions::getCompanyId, companyId)
						.eq(LimitItemPromotions::getDistributorId, distributorId)
						.eq(LimitItemPromotions::getItemId, itemId));
	}
}
