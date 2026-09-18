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

package cn.shopex.ecshopx.goods.service.excard;

import cn.shopex.ecshopx.common.inventory.ItemInventoryLineContext;
import cn.shopex.ecshopx.common.order.excard.ExcardInventoryPort;
import cn.shopex.ecshopx.common.order.excard.ExcardOrderItemSnapshot;
import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.service.distributor.DistributorItemSkuInfoForStoreService;
import cn.shopex.ecshopx.goods.service.items.ItemInventoryOrchestratorService;
import org.springframework.stereotype.Service;

@Service
public class GoodsExcardInventoryPort implements ExcardInventoryPort {

	private final ItemInventoryOrchestratorService itemInventoryOrchestratorService;
	private final DistributorItemSkuInfoForStoreService distributorItemSkuInfoForStoreService;
	private final ItemsMapper itemsMapper;

	public GoodsExcardInventoryPort(
			ItemInventoryOrchestratorService itemInventoryOrchestratorService,
			DistributorItemSkuInfoForStoreService distributorItemSkuInfoForStoreService,
			ItemsMapper itemsMapper) {
		this.itemInventoryOrchestratorService = itemInventoryOrchestratorService;
		this.distributorItemSkuInfoForStoreService = distributorItemSkuInfoForStoreService;
		this.itemsMapper = itemsMapper;
	}

	@Override
	public boolean minusItemStore(long companyId, long itemId, int num, long distributorId, boolean isTotalStore) {
		return minusItemStore(
				ItemInventoryLineContext.of(companyId, itemId, 0L, distributorId, isTotalStore, "", 0L), num);
	}

	@Override
	public boolean minusItemStore(ItemInventoryLineContext ctx, int num) {
		return itemInventoryOrchestratorService.minusItemStore(ctx, num);
	}

	@Override
	public boolean resolveIsTotalStore(long companyId, long itemId, long distributorId) {
		return distributorItemSkuInfoForStoreService.isTotalStore(companyId, itemId, distributorId);
	}

	@Override
	public ExcardOrderItemSnapshot loadOrderItemSnapshot(long companyId, long itemId, long distributorId) {
		Items it = itemsMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Items>()
				.eq(Items::getCompanyId, companyId)
				.eq(Items::getItemId, itemId)
				.last("LIMIT 1"));
		if (it == null) {
			return new ExcardOrderItemSnapshot(
					itemId,
					0L,
					"",
					"",
					"",
					"",
					"",
					0,
					0,
					0);
		}
		int price = it.getPrice() != null ? it.getPrice() : 0;
		int cost = it.getCostPrice() != null ? it.getCostPrice() : 0;
		int market = price;
		if (distributorId > 0L) {
			DistributorItems di = distributorItemSkuInfoForStoreService.findRow(companyId, itemId, distributorId);
			if (di != null && di.getPrice() != null && di.getPrice() > 0) {
				price = di.getPrice().intValue();
			}
		}
		String pic = firstPic(it.getPics());
		long goodsId = it.getGoodsId() != null ? it.getGoodsId() : 0L;
		String goodsBn = it.getGoodsBn() != null ? it.getGoodsBn() : "";
		String itemBn = it.getItemBn() != null ? it.getItemBn() : "";
		String name = it.getItemName() != null ? it.getItemName() : "";
		String unit = it.getItemUnit() != null ? it.getItemUnit() : "";
		return new ExcardOrderItemSnapshot(itemId, goodsId, itemBn, goodsBn, name, unit, pic, price, market, cost);
	}

	private static String firstPic(String pics) {
		if (pics == null || pics.isEmpty()) {
			return "";
		}
		int comma = pics.indexOf(',');
		return comma > 0 ? pics.substring(0, comma).trim() : pics.trim();
	}
}
