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

package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.domain.RelItems;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.mapper.RelItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DiscountCardInfoByIdForConsumeService {

	private final DiscountCardsMapper discountCardsMapper;
	private final RelItemsMapper relItemsMapper;
	private final DiscountCardsRowMapperService discountCardsRowMapperService;

	public DiscountCardInfoByIdForConsumeService(
			DiscountCardsMapper discountCardsMapper,
			RelItemsMapper relItemsMapper,
			DiscountCardsRowMapperService discountCardsRowMapperService) {
		this.discountCardsMapper = discountCardsMapper;
		this.relItemsMapper = relItemsMapper;
		this.discountCardsRowMapperService = discountCardsRowMapperService;
	}

	public Map<String, Object> load(long companyId, long cardId) {
		DiscountCards card = discountCardsMapper.selectOne(new LambdaQueryWrapper<DiscountCards>()
				.eq(DiscountCards::getCompanyId, companyId)
				.eq(DiscountCards::getCardId, cardId)
				.last("LIMIT 1"));
		if (card == null) {
			return Map.of();
		}
		Map<String, Object> m = new LinkedHashMap<>(discountCardsRowMapperService.toSnakeCaseMap(card));
		List<Map<String, Object>> relItems = new ArrayList<>();
		Integer ub = card.getUseBound();
		if (ub != null && ub == 1) {
			List<RelItems> rows = relItemsMapper.selectList(new LambdaQueryWrapper<RelItems>()
					.eq(RelItems::getCompanyId, companyId)
					.eq(RelItems::getCardId, cardId)
					.eq(RelItems::getItemType, "normal"));
			for (RelItems r : rows) {
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("item_id", r.getItemId());
				row.put("use_limit", r.getUseLimit());
				relItems.add(row);
			}
		}
		m.put("rel_items", relItems);
		return m;
	}
}
