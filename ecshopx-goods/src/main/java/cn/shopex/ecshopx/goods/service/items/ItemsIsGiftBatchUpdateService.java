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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.promotions.service.ItemCreatePromotionGuardService;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ItemsIsGiftBatchUpdateService {

	private final ItemsRepository itemsRepository;
	private final ItemCreatePromotionGuardService itemCreatePromotionGuardService;

	public ItemsIsGiftBatchUpdateService(
			ItemsRepository itemsRepository, ItemCreatePromotionGuardService itemCreatePromotionGuardService) {
		this.itemsRepository = itemsRepository;
		this.itemCreatePromotionGuardService = itemCreatePromotionGuardService;
	}

	public void batchUpdateItemGift(long companyId, String operatorType, long goodsId, String statusRaw) {
		String op = operatorType == null ? "" : operatorType.trim();
		if ("supplier".equalsIgnoreCase(op)) {
			throw new ResourceException("供应商不支持该操作");
		}
		if ("false".equals(statusRaw)) {
			List<Items> listPositivePrice = itemsRepository.listByCompanyIdAndGoodsId(companyId, goodsId, true);
			List<Items> listAll = itemsRepository.listByCompanyIdAndGoodsId(companyId, goodsId, false);
			if (listPositivePrice.size() != listAll.size()) {
				throw new ResourceException("存在价格设置为0元的商品无法设置为非赠品，请检查后再次提交");
			}
			itemsRepository.updateIsGiftByCompanyAndGoodsId(companyId, goodsId, false);
			return;
		}
		if ("true".equals(statusRaw)) {
			List<Items> listAll = itemsRepository.listByCompanyIdAndGoodsId(companyId, goodsId, false);
			if (listAll.isEmpty()) {
				return;
			}
			List<Long> itemIds = listAll.stream()
					.map(Items::getItemId)
					.filter(Objects::nonNull)
					.distinct()
					.toList();
			itemCreatePromotionGuardService.checkNotFinishedActivityValid(companyId, true, itemIds, List.of(goodsId));
			itemsRepository.updateIsGiftByCompanyAndGoodsId(companyId, goodsId, true);
		}
	}
}
