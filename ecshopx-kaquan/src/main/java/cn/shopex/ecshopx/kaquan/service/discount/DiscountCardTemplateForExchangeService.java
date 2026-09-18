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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DiscountCardTemplateForExchangeService {

	private final DiscountCardsMapper discountCardsMapper;
	private final RelItemsMapper relItemsMapper;
	private final DiscountCardsRowMapperService discountCardsRowMapperService;
	private final UserDiscountExchangeCardGoodsLookupService userDiscountExchangeCardGoodsLookupService;

	public DiscountCardTemplateForExchangeService(
			DiscountCardsMapper discountCardsMapper,
			RelItemsMapper relItemsMapper,
			DiscountCardsRowMapperService discountCardsRowMapperService,
			UserDiscountExchangeCardGoodsLookupService userDiscountExchangeCardGoodsLookupService) {
		this.discountCardsMapper = discountCardsMapper;
		this.relItemsMapper = relItemsMapper;
		this.discountCardsRowMapperService = discountCardsRowMapperService;
		this.userDiscountExchangeCardGoodsLookupService = userDiscountExchangeCardGoodsLookupService;
	}

	public Map<String, Object> loadForExchange(long companyId, long templateCardId, long selectedItemId) {
		if (templateCardId <= 0L) {
			return Map.of();
		}
		DiscountCards card = discountCardsMapper.selectOne(new LambdaQueryWrapper<DiscountCards>()
				.eq(DiscountCards::getCompanyId, companyId)
				.eq(DiscountCards::getCardId, templateCardId)
				.last("LIMIT 1"));
		if (card == null) {
			return Map.of();
		}
		Map<String, Object> detail = new LinkedHashMap<>(discountCardsRowMapperService.toSnakeCaseMap(card));
		int ub = card.getUseBound() == null ? 0 : card.getUseBound();
		List<Map<String, Object>> relItems = new ArrayList<>();
		switch (ub) {
			case 0 -> {
				// 全场：无关联商品行
			}
			case 1 -> {
				LambdaQueryWrapper<RelItems> q = new LambdaQueryWrapper<RelItems>()
						.eq(RelItems::getCompanyId, companyId)
						.eq(RelItems::getCardId, templateCardId)
						.eq(RelItems::getItemType, "normal");
				if (selectedItemId > 0L) {
					q.eq(RelItems::getItemId, selectedItemId);
				}
				List<RelItems> rows = relItemsMapper.selectList(q);
				for (RelItems r : rows) {
					relItems.add(relItemRow(r.getItemId(), r.getUseLimit()));
				}
			}
			case 2 -> {
				List<RelItems> catRows = relItemsMapper.selectList(new LambdaQueryWrapper<RelItems>()
						.eq(RelItems::getCompanyId, companyId)
						.eq(RelItems::getCardId, templateCardId)
						.eq(RelItems::getItemType, "category"));
				boolean categoryHit = false;
				int matchedCategoryLimit = 0;
				outerCat:
				for (RelItems cr : catRows) {
					if (cr.getItemId() == null) {
						continue;
					}
					List<Long> ids = userDiscountExchangeCardGoodsLookupService.getItemIdsByCategoryTree(companyId, cr.getItemId());
					for (Long id : ids) {
						if (id != null && id == selectedItemId) {
							matchedCategoryLimit = cr.getUseLimit() != null ? cr.getUseLimit() : 0;
							categoryHit = true;
							break outerCat;
						}
					}
				}
				if (categoryHit) {
					relItems.add(relItemRow(selectedItemId, matchedCategoryLimit));
				}
			}
			case 3 -> {
				List<Long> tagIds = parseCommaLongs(card.getTagIds());
				if (!tagIds.isEmpty()) {
					Set<Long> tagged = new LinkedHashSet<>(userDiscountExchangeCardGoodsLookupService.listItemIdsByTagIds(
							companyId, tagIds));
					if (tagged.contains(selectedItemId)) {
						int ul = card.getUseLimit() != null ? card.getUseLimit() : 0;
						relItems.add(relItemRow(selectedItemId, ul));
					}
				}
			}
			case 4 -> {
				List<Long> brandIds = parseCommaLongs(card.getBrandIds());
				List<Long> hit = userDiscountExchangeCardGoodsLookupService.listItemIdsByBrandAttributeIds(
						companyId, brandIds, selectedItemId);
				if (!hit.isEmpty() && hit.contains(selectedItemId)) {
					int ul = card.getUseLimit() != null ? card.getUseLimit() : 0;
					relItems.add(relItemRow(selectedItemId, ul));
				}
			}
			case 5 -> {
				List<RelItems> allRows = relItemsMapper.selectList(new LambdaQueryWrapper<RelItems>()
						.eq(RelItems::getCompanyId, companyId)
						.eq(RelItems::getCardId, templateCardId));
				Set<Long> excluded = new LinkedHashSet<>();
				for (RelItems r : allRows) {
					if (r.getItemId() != null) {
						excluded.add(r.getItemId());
					}
				}
				if (!excluded.contains(selectedItemId)) {
					int ul = card.getUseLimit() != null ? card.getUseLimit() : 0;
					relItems.add(relItemRow(selectedItemId, ul));
				}
			}
			default -> {
				// unknown use_bound: 与无适用商品一致
			}
		}
		if (ub > 0 && relItems.isEmpty()) {
			relItems.add(relItemRow(-1L, 0));
		}
		detail.put("rel_items", relItems);
		return detail;
	}

	private static Map<String, Object> relItemRow(long itemId, Integer useLimit) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("item_id", itemId);
		row.put("use_limit", useLimit != null ? useLimit : 0);
		return row;
	}

	private static List<Long> parseCommaLongs(String raw) {
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		String t = raw.trim();
		if (t.startsWith(",")) {
			t = t.substring(1);
		}
		if (t.endsWith(",")) {
			t = t.substring(0, t.length() - 1);
		}
		List<Long> out = new ArrayList<>();
		for (String p : t.split(",")) {
			if (!StringUtils.hasText(p)) {
				continue;
			}
			try {
				out.add(Long.parseLong(p.trim()));
			} catch (NumberFormatException ignored) {
				// skip
			}
		}
		return out;
	}
}
