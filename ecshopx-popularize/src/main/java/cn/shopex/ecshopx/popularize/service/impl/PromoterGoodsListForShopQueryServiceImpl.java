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

package cn.shopex.ecshopx.popularize.service.impl;

import cn.shopex.ecshopx.popularize.mapper.PromoterGoodsMapper;
import cn.shopex.ecshopx.popularize.service.PromoterGoodsListForShopQueryService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PromoterGoodsListForShopQueryServiceImpl implements PromoterGoodsListForShopQueryService {

	private final PromoterGoodsMapper promoterGoodsMapper;

	public PromoterGoodsListForShopQueryServiceImpl(PromoterGoodsMapper promoterGoodsMapper) {
		this.promoterGoodsMapper = promoterGoodsMapper;
	}

	@Override
	public long countJoinedItemsForShop(long companyId, long userId, boolean isAllGoods) {
		return promoterGoodsMapper.countJoinedItemsForShop(companyId, userId, isAllGoods);
	}

	@Override
	public List<Long> listGoodsIdsJoinedItemsForShop(long companyId, long userId, boolean isAllGoods) {
		return promoterGoodsMapper.listGoodsIdsJoinedItemsForShop(companyId, userId, isAllGoods);
	}
}
