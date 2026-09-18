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

import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiItemsV2FailException;
import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import cn.shopex.ecshopx.goods.service.items.ItemStoreService;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2ItemStoreSyncService {

	private static final String MSG_ITEM_CODE_ERROR = "商品货号参数错误";
	private static final String MSG_STORE_ERROR = "同步库存数参数错误";
	private static final String MSG_DISTRIBUTOR_CODE_ERROR = "店铺ID参数错误";
	private static final String MSG_DISTRIBUTOR_NOT_FOUND = "店铺找不到";
	private static final String MSG_GOODS_NOT_FOUND = "商品找不到";
	private static final String MSG_TOTAL_STORE_FORBIDDEN = "商品总部发货无法更新库存和价格";
	private static final String MSG_UNKNOWN_ERROR = "未知错误";

	private final ItemsRepository itemsRepository;
	private final ItemStoreService itemStoreService;
	private final DistributorItemsRepository distributorItemsRepository;
	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver;
	private final OpenapiThirdApiV2ItemStoreSyncService self;

	public OpenapiThirdApiV2ItemStoreSyncService(
			ItemsRepository itemsRepository,
			ItemStoreService itemStoreService,
			DistributorItemsRepository distributorItemsRepository,
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver,
			@Lazy OpenapiThirdApiV2ItemStoreSyncService self) {
		this.itemsRepository = itemsRepository;
		this.itemStoreService = itemStoreService;
		this.distributorItemsRepository = distributorItemsRepository;
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.itemsCategoryDistributorIdResolver = itemsCategoryDistributorIdResolver;
		this.self = self;
	}

	public void executeSyncStore(long companyId, Map<String, Object> mergedParams) {
		try {
			ValidatedSyncParams validated = validateAndExtract(mergedParams);
			String productModel = itemsCategoryDistributorIdResolver.resolveProductModel(companyId);
			if (StringUtils.hasText(validated.distributorCodeRaw()) && "standard".equals(productModel)) {
				self.syncDistributorStoreCover(
						companyId,
						validated.distributorCodeRaw().trim(),
						validated.itemBn(),
						validated.store());
			} else {
				syncHeadquartersStoreCover(companyId, validated.itemBn(), validated.store());
			}
		} catch (OpenapiItemsV2FailException ex) {
			throw ex;
		} catch (Exception ex) {
			throw toSyncFail(ex);
		}
	}

	private static ValidatedSyncParams validateAndExtract(Map<String, Object> mergedParams) {
		if (!mergedParams.containsKey("item_code")) {
			throw missingParams(MSG_ITEM_CODE_ERROR);
		}
		Object itemCodeRaw = mergedParams.get("item_code");
		if (itemCodeRaw == null || String.valueOf(itemCodeRaw).trim().isEmpty()) {
			throw missingParams(MSG_ITEM_CODE_ERROR);
		}

		if (!mergedParams.containsKey("store")) {
			throw missingParams(MSG_STORE_ERROR);
		}
		Integer parsedStore = parseNonNegativeIntStrict(mergedParams.get("store"));
		if (parsedStore == null) {
			throw missingParams(MSG_STORE_ERROR);
		}

		if (mergedParams.containsKey("distributor_code")) {
			Object distributorCodeRaw = mergedParams.get("distributor_code");
			if (distributorCodeRaw != null && !isScalarValue(distributorCodeRaw)) {
				throw missingParams(MSG_DISTRIBUTOR_CODE_ERROR);
			}
		}

		String itemBn = String.valueOf(mergedParams.get("item_code")).trim();
		String distributorCodeRaw = mergedParams.containsKey("distributor_code")
				? String.valueOf(mergedParams.get("distributor_code"))
				: null;
		return new ValidatedSyncParams(itemBn, parsedStore, distributorCodeRaw);
	}

	private void syncHeadquartersStoreCover(long companyId, String itemBn, int store) {
		Items item = itemsRepository.findByItemBnAndCompany(itemBn, companyId);
		if (item == null || item.getItemId() == null) {
			throw new OpenapiItemsV2FailException(OpenapiErrorCode.GOODS_NOT_FOUND, MSG_GOODS_NOT_FOUND);
		}
		long itemId = item.getItemId();
		Integer currentStore = item.getStore();
		if (currentStore != null && currentStore == store) {
			return;
		}
		itemsRepository.updateSingleItemStoreIfExists(itemId, store);
		itemStoreService.saveItemStore(itemId, store, 0L);
	}

	@Transactional(rollbackFor = Exception.class)
	public void syncDistributorStoreCover(long companyId, String shopCode, String itemBn, int store) {
		Map<String, Object> distInfo =
				distributorRepositoryGetInfoSimpleService.getInfoSimpleByShopCode(companyId, shopCode);
		long distributorId = extractLong(distInfo, "distributor_id");
		if (distributorId <= 0L) {
			throw new OpenapiItemsV2FailException(OpenapiErrorCode.DISTRIBUTOR_NOT_FOUND, MSG_DISTRIBUTOR_NOT_FOUND);
		}

		Items itemInfo = itemsRepository.findByItemBnAndCompany(itemBn, companyId);
		if (itemInfo == null || itemInfo.getItemId() == null || itemInfo.getItemId() <= 0L) {
			throw new OpenapiItemsV2FailException(OpenapiErrorCode.GOODS_NOT_FOUND, MSG_GOODS_NOT_FOUND);
		}
		long itemId = itemInfo.getItemId();
		boolean isDefault = Boolean.TRUE.equals(itemInfo.getIsDefault());
		long goodsId = itemInfo.getGoodsId() != null ? itemInfo.getGoodsId() : 0L;
		long defaultItemId = itemInfo.getDefaultItemId() != null ? itemInfo.getDefaultItemId() : 0L;

		Optional<DistributorItems> existingOpt =
				distributorItemsRepository.findByDistributorIdAndCompanyIdAndItemId(distributorId, companyId, itemId);
		boolean isTotalStoreEffective;
		if (existingOpt.isPresent()) {
			isTotalStoreEffective = Boolean.TRUE.equals(existingOpt.get().getIsTotalStore());
		} else if (defaultItemId > 0L) {
			Optional<DistributorItems> spuRow = distributorItemsRepository
					.findByDistributorIdAndCompanyIdAndItemId(distributorId, companyId, defaultItemId);
			isTotalStoreEffective = spuRow.isPresent() && Boolean.TRUE.equals(spuRow.get().getIsTotalStore());
		} else {
			isTotalStoreEffective = false;
		}

		if (isTotalStoreEffective) {
			throw new OpenapiItemsV2FailException(OpenapiErrorCode.DISTRIBUTOR_ITEM_ERROR, MSG_TOTAL_STORE_FORBIDDEN);
		}

		if (existingOpt.isPresent()) {
			distributorItemsRepository.updateColumnsByDistributorCompanyItem(
					distributorId, companyId, itemId, null, null, (long) store, null);
			itemStoreService.saveItemStore(itemId, store, distributorId);
			return;
		}

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
		created.setIsCanSale(false);
		created.setIsTotalStore(isTotalStoreEffective);
		created.setStore((long) store);
		created.setPrice(intToLong(itemInfo.getPrice()));
		distributorItemsRepository.insert(created);
		itemStoreService.saveItemStore(itemId, store, distributorId);
	}

	private static Integer parseNonNegativeIntStrict(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number number) {
			if (raw instanceof Double || raw instanceof Float || raw instanceof BigDecimal) {
				double d = number.doubleValue();
				if (d != Math.floor(d)) {
					return null;
				}
			}
			int value = number.intValue();
			return value >= 0 ? value : null;
		}
		String text = String.valueOf(raw).trim();
		if (!text.matches("^[0-9]+$")) {
			return null;
		}
		try {
			long parsed = Long.parseLong(text);
			if (parsed > Integer.MAX_VALUE) {
				return null;
			}
			return (int) parsed;
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private static boolean isScalarValue(Object value) {
		return value instanceof String || value instanceof Number;
	}

	private static long extractLong(Map<String, Object> map, String key) {
		if (map == null || map.isEmpty()) {
			return 0L;
		}
		Object raw = map.get(key);
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long intToLong(Integer v) {
		return v == null ? 0L : v.longValue();
	}

	private static OpenapiItemsV2FailException missingParams(String message) {
		return new OpenapiItemsV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}

	private static OpenapiItemsV2FailException toSyncFail(Exception ex) {
		String msg = ex.getMessage();
		if (msg == null || msg.isBlank()) {
			msg = MSG_UNKNOWN_ERROR;
		}
		return new OpenapiItemsV2FailException("E5000", msg);
	}

	private record ValidatedSyncParams(String itemBn, int store, String distributorCodeRaw) {}
}
