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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardNormalRelItemsReplacementService;
import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountExchangeCardGoodsLookupService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ItemTagEditPromotionExecutionService {

	private static final int PAGE_SIZE = 100;

	private final DiscountCardsMapper discountCardsMapper;
	private final UserDiscountExchangeCardGoodsLookupService userDiscountExchangeCardGoodsLookupService;
	private final DiscountCardNormalRelItemsReplacementService discountCardNormalRelItemsReplacementService;

	public ItemTagEditPromotionExecutionService(
			DiscountCardsMapper discountCardsMapper,
			UserDiscountExchangeCardGoodsLookupService userDiscountExchangeCardGoodsLookupService,
			DiscountCardNormalRelItemsReplacementService discountCardNormalRelItemsReplacementService) {
		this.discountCardsMapper = discountCardsMapper;
		this.userDiscountExchangeCardGoodsLookupService = userDiscountExchangeCardGoodsLookupService;
		this.discountCardNormalRelItemsReplacementService = discountCardNormalRelItemsReplacementService;
	}

	public void handleItemTagEditSuccess(Map<String, Object> entities) {
		long companyId = readRequiredCompanyId(entities);
		if (companyId <= 0L) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		int offset = 0;
		while (true) {
			LambdaQueryWrapper<DiscountCards> w = new LambdaQueryWrapper<>();
			w.eq(DiscountCards::getCompanyId, companyId)
					.eq(DiscountCards::getUseBound, 3)
					.ge(DiscountCards::getEndDate, now)
					.orderByAsc(DiscountCards::getCardId)
					.last("LIMIT " + PAGE_SIZE + " OFFSET " + offset);
			List<DiscountCards> chunk = discountCardsMapper.selectList(w);
			if (chunk == null || chunk.isEmpty()) {
				break;
			}
			for (DiscountCards card : chunk) {
				if (card.getCardId() == null) {
					continue;
				}
				List<Long> tagIds = parseTagIds(card.getTagIds());
				List<Long> itemIds = userDiscountExchangeCardGoodsLookupService.listItemIdsByTagIds(companyId, tagIds);
				discountCardNormalRelItemsReplacementService.replace(companyId, card.getCardId(), itemIds);
			}
			if (chunk.size() < PAGE_SIZE) {
				break;
			}
			offset += PAGE_SIZE;
		}
	}

	private static long readRequiredCompanyId(Map<String, Object> entities) {
		Object raw = entities.get("company_id");
		if (raw == null) {
			throw new BadRequestException("company_id is required");
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("company_id is invalid");
		}
	}

	private static List<Long> parseTagIds(String raw) {
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (String seg : raw.split(",")) {
			String t = seg.trim();
			if (t.isEmpty()) {
				continue;
			}
			try {
				out.add(Long.parseLong(t));
			} catch (NumberFormatException ignored) {
			}
		}
		return out;
	}
}
