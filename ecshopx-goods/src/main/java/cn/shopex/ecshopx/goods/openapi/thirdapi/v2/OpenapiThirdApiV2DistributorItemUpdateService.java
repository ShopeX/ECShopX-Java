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

package cn.shopex.ecshopx.goods.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.openapi.OpenapiDistributorItemUpdateCommand;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorV2FailException;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.items.ItemStoreService;
import cn.shopex.ecshopx.promotions.service.ItemCreatePromotionGuardService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpenapiThirdApiV2DistributorItemUpdateService {

	private final ItemsRepository itemsRepository;
	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final DistributorItemsRepository distributorItemsRepository;
	private final ItemStoreService itemStoreService;
	private final ItemCreatePromotionGuardService itemCreatePromotionGuardService;

	public OpenapiThirdApiV2DistributorItemUpdateService(
			ItemsRepository itemsRepository,
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			DistributorItemsRepository distributorItemsRepository,
			ItemStoreService itemStoreService,
			ItemCreatePromotionGuardService itemCreatePromotionGuardService) {
		this.itemsRepository = itemsRepository;
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.distributorItemsRepository = distributorItemsRepository;
		this.itemStoreService = itemStoreService;
		this.itemCreatePromotionGuardService = itemCreatePromotionGuardService;
	}

	@Transactional(rollbackFor = Exception.class)
	public void executeUpdate(long companyId, OpenapiDistributorItemUpdateCommand command) {
		if (!command.hasAnyPatch()) {
			return;
		}

		Map<String, Object> distInfo =
				distributorRepositoryGetInfoSimpleService.getInfoSimpleByShopCode(companyId, command.shopCode());
		long distributorId = parseLong(distInfo.get("distributor_id"));
		if (distributorId <= 0L) {
			throw new OpenapiDistributorV2FailException(OpenapiErrorCode.DISTRIBUTOR_NOT_FOUND, "店铺找不到");
		}

		Items itemInfo = itemsRepository.findByItemBnAndCompany(command.itemCode(), companyId);
		if (itemInfo == null || itemInfo.getItemId() == null || itemInfo.getItemId() <= 0L) {
			throw new OpenapiDistributorV2FailException(OpenapiErrorCode.GOODS_NOT_FOUND, "商品找不到");
		}
		long itemId = itemInfo.getItemId();
		long defaultItemId = nullToZero(itemInfo.getDefaultItemId());
		long goodsId = nullToZero(itemInfo.getGoodsId());
		boolean isDefault = Boolean.TRUE.equals(itemInfo.getIsDefault());

		if (command.isTotalStore().isPresent()
				&& defaultItemId > 0L
				&& itemId != defaultItemId) {
			throw new OpenapiDistributorV2FailException(
					OpenapiErrorCode.DISTRIBUTOR_ITEM_ERROR,
					"是否总部发货, 当前只支持更新spu级商品");
		}

		Optional<DistributorItems> existingOpt =
				distributorItemsRepository.findByDistributorIdAndCompanyIdAndItemId(
						distributorId, companyId, itemId);
		boolean issetRow = existingOpt.isPresent();

		Boolean patchIsTotalStore = command.isTotalStore().orElse(null);
		boolean isTotalStoreEffective;
		if (!issetRow) {
			if (patchIsTotalStore == null) {
				Optional<DistributorItems> spuRow = defaultItemId > 0L
						? distributorItemsRepository.findByDistributorIdAndCompanyIdAndItemId(
								distributorId, companyId, defaultItemId)
						: Optional.empty();
				isTotalStoreEffective = spuRow.isPresent()
						&& Boolean.TRUE.equals(spuRow.get().getIsTotalStore());
			} else {
				isTotalStoreEffective = patchIsTotalStore;
			}
		} else {
			isTotalStoreEffective = patchIsTotalStore != null
					? patchIsTotalStore
					: Boolean.TRUE.equals(existingOpt.get().getIsTotalStore());
		}

		boolean isCanSaleEffective = command.isCanSale().orElse(issetRow
				? Boolean.TRUE.equals(existingOpt.get().getIsCanSale())
				: false);
		if (!issetRow && command.isCanSale().isEmpty()) {
			isCanSaleEffective = false;
		}

		Long patchStore = command.store().orElse(null);
		Long patchPriceFen = command.priceFen().orElse(null);
		if (!issetRow) {
			if (patchStore == null) {
				patchStore = intToLong(itemInfo.getStore());
			}
			if (patchPriceFen == null) {
				patchPriceFen = intToLong(itemInfo.getPrice());
			}
		}

		if (isTotalStoreEffective
				&& (command.store().isPresent() || command.priceFen().isPresent())) {
			throw new OpenapiDistributorV2FailException(
					OpenapiErrorCode.DISTRIBUTOR_ITEM_ERROR,
					"商品总部发货无法更新库存和价格");
		}

		if (patchPriceFen != null) {
			assertPriceVsPromotions(companyId, itemInfo, itemId, defaultItemId, patchPriceFen);
		}

		if (issetRow) {
			distributorItemsRepository.updateColumnsByDistributorCompanyItem(
					distributorId,
					companyId,
					itemId,
					command.isCanSale().orElse(null),
					command.isTotalStore().orElse(null),
					command.store().orElse(null),
					command.priceFen().orElse(null));

			if (command.store().isPresent()) {
				int st = command.store().get().intValue();
				itemStoreService.saveItemStore(itemId, st, distributorId);
			}
		} else {
			DistributorItems created = new DistributorItems();
			created.setDistributorId(distributorId);
			created.setCompanyId(companyId);
			created.setShopId(0L);
			created.setItemId(itemId);
			created.setGoodsId(goodsId);
			created.setDefaultItemId(defaultItemId);
			created.setIsShow(isDefault);
			created.setIsSelfDelivery(false);
			created.setIsExpressDelivery(false);
			created.setIsCanSale(isCanSaleEffective);
			created.setIsTotalStore(isTotalStoreEffective);
			created.setStore(patchStore);
			created.setPrice(patchPriceFen);
			created.setGoodsCanSale(true);
			distributorItemsRepository.insert(created);

			if (patchStore != null) {
				itemStoreService.saveItemStore(itemId, patchStore.intValue(), distributorId);
			}
		}

		if (command.isTotalStore().isPresent() && defaultItemId > 0L) {
			distributorItemsRepository.updateIsTotalStoreByDistributorAndGoodsGroup(
					distributorId, defaultItemId, isTotalStoreEffective);
		}

		if (command.isCanSale().isPresent() && defaultItemId > 0L) {
			distributorItemsRepository.syncGoodsCanSaleByCompanyDistributorAndDefaultItemId(
					companyId, distributorId, defaultItemId);
		}
	}

	private void assertPriceVsPromotions(
			long companyId, Items itemInfo, long itemId, long defaultItemId, long priceFen) {
		Map<Long, Long> priceMap = new HashMap<>();
		priceMap.put(itemId, priceFen);
		List<Long> goodsIds = defaultItemId > 0L ? List.of(defaultItemId) : List.of();
		itemCreatePromotionGuardService.checkItemPrice(companyId, goodsIds, priceMap);
	}

	private static long intToLong(Integer v) {
		return v == null ? 0L : v.longValue();
	}

	private static long nullToZero(Long v) {
		return v == null ? 0L : v;
	}

	private static long parseLong(Object v) {
		if (v == null) {
			return 0L;
		}
		try {
			return new BigDecimal(String.valueOf(v).trim()).longValue();
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
