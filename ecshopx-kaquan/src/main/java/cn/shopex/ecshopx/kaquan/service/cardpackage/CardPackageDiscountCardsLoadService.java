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

package cn.shopex.ecshopx.kaquan.service.cardpackage;

import cn.shopex.ecshopx.kaquan.domain.CardPackage;
import cn.shopex.ecshopx.kaquan.domain.CardPackageItems;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.mapper.CardPackageItemsMapper;
import cn.shopex.ecshopx.kaquan.mapper.CardPackageMapper;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * 按卡券包 ID 列表加载关联的优惠券实体及包内配置的每张卡发放张数（同卡多行 {@code give_num} 相加）。
 */
@Service
public class CardPackageDiscountCardsLoadService {

	private final CardPackageMapper cardPackageMapper;
	private final CardPackageItemsMapper cardPackageItemsMapper;
	private final DiscountCardsMapper discountCardsMapper;

	public CardPackageDiscountCardsLoadService(CardPackageMapper cardPackageMapper,
			CardPackageItemsMapper cardPackageItemsMapper, DiscountCardsMapper discountCardsMapper) {
		this.cardPackageMapper = cardPackageMapper;
		this.cardPackageItemsMapper = cardPackageItemsMapper;
		this.discountCardsMapper = discountCardsMapper;
	}

	public CardPackageDiscountCardsLoadResult load(long companyId, List<Long> packageIds) {
		if (packageIds == null || packageIds.isEmpty()) {
			return new CardPackageDiscountCardsLoadResult(List.of(), Map.of());
		}
		List<CardPackage> existing = cardPackageMapper.selectList(new LambdaQueryWrapper<CardPackage>()
				.in(CardPackage::getPackageId, packageIds)
				.eq(CardPackage::getRowStatus, 1));
		if (existing.isEmpty()) {
			return new CardPackageDiscountCardsLoadResult(List.of(), Map.of());
		}
		List<Long> existingIds = existing.stream().map(CardPackage::getPackageId).filter(Objects::nonNull).toList();
		List<CardPackageItems> items = cardPackageItemsMapper.selectList(new LambdaQueryWrapper<CardPackageItems>()
				.eq(CardPackageItems::getCompanyId, companyId)
				.in(CardPackageItems::getPackageId, existingIds));

		Map<Long, Long> giveNumByCardId = new HashMap<>();
		for (CardPackageItems item : items) {
			Long cid = item.getCardId();
			if (cid == null) {
				continue;
			}
			long add = item.getGiveNum() != null ? item.getGiveNum() : 0L;
			giveNumByCardId.merge(cid, add, Long::sum);
		}

		if (giveNumByCardId.isEmpty()) {
			return new CardPackageDiscountCardsLoadResult(List.of(), Map.of());
		}
		List<Long> cardIds = new ArrayList<>(giveNumByCardId.keySet());
		List<DiscountCards> cards = discountCardsMapper.selectList(new LambdaQueryWrapper<DiscountCards>()
				.eq(DiscountCards::getCompanyId, companyId)
				.in(DiscountCards::getCardId, cardIds));
		cards.sort(Comparator.comparing(DiscountCards::getCardId, Comparator.nullsLast(Long::compareTo)));
		return new CardPackageDiscountCardsLoadResult(cards, giveNumByCardId);
	}
}
