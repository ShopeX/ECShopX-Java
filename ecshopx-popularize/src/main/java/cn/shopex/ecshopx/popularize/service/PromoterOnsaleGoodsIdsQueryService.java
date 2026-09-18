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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.popularize.domain.PromoterGoods;
import cn.shopex.ecshopx.popularize.mapper.PromoterGoodsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Resolves distinct goods identifiers linked to a promoter shop scope for list filtering.
 */
@Service
public class PromoterOnsaleGoodsIdsQueryService {

	private final PromoterGoodsMapper promoterGoodsMapper;

	public PromoterOnsaleGoodsIdsQueryService(PromoterGoodsMapper promoterGoodsMapper) {
		this.promoterGoodsMapper = promoterGoodsMapper;
	}

	/**
	 * @param companyId       merchant scope
	 * @param promoterShopId  promoter shop dimension id (stored as {@code user_id} on promoter-goods rows)
	 * @param onlyOnsale      caller intent for on-sale-only filtering; reserved — current query returns all matched rows
	 */
	public List<Long> listGoodsIdsByPromoterShop(long companyId, long promoterShopId, boolean onlyOnsale) {
		if (companyId <= 0L || promoterShopId <= 0L) {
			return List.of();
		}
		LambdaQueryWrapper<PromoterGoods> w = new LambdaQueryWrapper<>();
		w.eq(PromoterGoods::getCompanyId, companyId).eq(PromoterGoods::getUserId, promoterShopId).isNotNull(PromoterGoods::getGoodsId)
				.gt(PromoterGoods::getGoodsId, 0L).orderByAsc(PromoterGoods::getId);
		List<PromoterGoods> rows = promoterGoodsMapper.selectList(w);
		Set<Long> ordered = new LinkedHashSet<>();
		for (PromoterGoods r : rows) {
			if (r.getGoodsId() != null && r.getGoodsId() > 0L) {
				ordered.add(r.getGoodsId());
			}
		}
		return new ArrayList<>(ordered);
	}
}
