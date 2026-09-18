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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiItemsV2FailException;
import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsPriceStoreStatusPatch;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import cn.shopex.ecshopx.promotions.service.ItemCreatePromotionGuardService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2ItemPriceSyncService {

	private static final String MSG_ITEM_CODE_ERROR = "商品货号参数错误";
	private static final String MSG_DISTRIBUTOR_CODE_ERROR = "店铺ID参数错误";
	private static final String MSG_PRICE_ERROR = "销售价参数错误";
	private static final String MSG_MARKET_PRICE_ERROR = "市场价参数错误";
	private static final String MSG_COST_PRICE_ERROR = "成本价参数错误";
	private static final String MSG_NO_PRICE_SPECIFIED = "应至少指定一个要更新的价格";
	private static final String MSG_DISTRIBUTOR_NOT_FOUND = "店铺找不到";
	private static final String MSG_GOODS_NOT_FOUND = "商品找不到";
	private static final String MSG_TOTAL_STORE_FORBIDDEN = "商品总部发货无法更新库存和价格";
	private static final String MSG_UNKNOWN_ERROR = "未知错误";

	private final ItemsRepository itemsRepository;
	private final DistributorItemsRepository distributorItemsRepository;
	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver;
	private final ItemCreatePromotionGuardService itemCreatePromotionGuardService;
	private final OpenapiThirdApiV2ItemPriceSyncService self;

	public OpenapiThirdApiV2ItemPriceSyncService(
			ItemsRepository itemsRepository,
			DistributorItemsRepository distributorItemsRepository,
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver,
			ItemCreatePromotionGuardService itemCreatePromotionGuardService,
			@Lazy OpenapiThirdApiV2ItemPriceSyncService self) {
		this.itemsRepository = itemsRepository;
		this.distributorItemsRepository = distributorItemsRepository;
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.itemsCategoryDistributorIdResolver = itemsCategoryDistributorIdResolver;
		this.itemCreatePromotionGuardService = itemCreatePromotionGuardService;
		this.self = self;
	}

	public void executeSyncPrice(long companyId, Map<String, Object> mergedParams) {
		try {
			ValidatedSyncParams validated = validateAndExtract(mergedParams);
			String productModel = itemsCategoryDistributorIdResolver.resolveProductModel(companyId);
			if (StringUtils.hasText(validated.distributorCodeRaw()) && "standard".equals(productModel)) {
				self.syncDistributorPriceCover(
						companyId,
						validated.distributorCodeRaw().trim(),
						validated.itemBn(),
						mergedParams);
			} else {
				syncHeadquartersPricePartial(companyId, validated.itemBn(), mergedParams);
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

		if (mergedParams.containsKey("distributor_code")) {
			Object distributorCodeRaw = mergedParams.get("distributor_code");
			if (distributorCodeRaw != null && !isScalarValue(distributorCodeRaw)) {
				throw missingParams(MSG_DISTRIBUTOR_CODE_ERROR);
			}
		}

		if (mergedParams.containsKey("price")) {
			Integer priceFen = tryYuanToFenTruncated(mergedParams.get("price"));
			if (priceFen == null || priceFen <= 0) {
				throw missingParams(MSG_PRICE_ERROR);
			}
		}
		if (mergedParams.containsKey("market_price")) {
			Integer marketPriceFen = tryYuanToFenTruncated(mergedParams.get("market_price"));
			if (marketPriceFen == null || marketPriceFen <= 0) {
				throw missingParams(MSG_MARKET_PRICE_ERROR);
			}
		}
		if (mergedParams.containsKey("cost_price")) {
			Integer costPriceFen = tryYuanToFenTruncated(mergedParams.get("cost_price"));
			if (costPriceFen == null || costPriceFen <= 0) {
				throw missingParams(MSG_COST_PRICE_ERROR);
			}
		}

		String itemBn = String.valueOf(mergedParams.get("item_code")).trim();
		String distributorCodeRaw = mergedParams.containsKey("distributor_code")
				? String.valueOf(mergedParams.get("distributor_code"))
				: null;
		return new ValidatedSyncParams(itemBn, distributorCodeRaw);
	}

	private void syncHeadquartersPricePartial(long companyId, String itemBn, Map<String, Object> mergedParams) {
		Items item = itemsRepository.findByItemBnAndCompany(itemBn, companyId);
		if (item == null || item.getItemId() == null) {
			throw new OpenapiItemsV2FailException(OpenapiErrorCode.GOODS_NOT_FOUND, MSG_GOODS_NOT_FOUND);
		}

		Integer priceFen = mergedParams.containsKey("price")
				? yuanToFenTruncated(mergedParams.get("price"))
				: null;
		Integer marketPriceFen = mergedParams.containsKey("market_price")
				? yuanToFenTruncated(mergedParams.get("market_price"))
				: null;
		Integer costPriceFen = mergedParams.containsKey("cost_price")
				? yuanToFenTruncated(mergedParams.get("cost_price"))
				: null;

		if (priceFen == null && marketPriceFen == null && costPriceFen == null) {
			throw missingParams(MSG_NO_PRICE_SPECIFIED);
		}

		ItemsPriceStoreStatusPatch patch = new ItemsPriceStoreStatusPatch(
				priceFen, costPriceFen, marketPriceFen,
				null, null, null, null, null, null);
		itemsRepository.updateByItemsPriceStoreFilter(companyId, item.getItemId(), null, null, patch);
	}

	@Transactional(rollbackFor = Exception.class)
	public void syncDistributorPriceCover(
			long companyId, String shopCode, String itemBn, Map<String, Object> mergedParams) {
		Long priceFen = null;
		if (mergedParams.containsKey("price") && isNumericScalar(mergedParams.get("price"))) {
			priceFen = (long) yuanToFenTruncated(mergedParams.get("price"));
		}
		if (priceFen == null) {
			return;
		}

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

		assertPriceVsPromotions(companyId, itemInfo, itemId, priceFen);

		if (existingOpt.isPresent()) {
			distributorItemsRepository.updateColumnsByDistributorCompanyItem(
					distributorId, companyId, itemId, null, null, null, priceFen);
			return;
		}

		long storeVal = intToLong(itemInfo.getStore());

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
		created.setStore(storeVal);
		created.setPrice(priceFen);
		distributorItemsRepository.insert(created);
	}

	private void assertPriceVsPromotions(long companyId, Items itemInfo, long itemId, long priceFen) {
		Map<Long, Long> priceMap = new HashMap<>();
		priceMap.put(itemId, priceFen);
		Long gid = itemInfo.getGoodsId();
		List<Long> goodsIds = gid != null && gid > 0L ? List.of(gid) : List.of();
		if (gid != null && gid > 0L) {
			priceMap.put(gid, priceFen);
		}
		try {
			itemCreatePromotionGuardService.checkItemPrice(companyId, goodsIds, priceMap);
		} catch (ResourceException ex) {
			throw new OpenapiItemsV2FailException(OpenapiErrorCode.SUCCESS, ex.getMessage());
		}
	}

	private static Integer tryYuanToFenTruncated(Object raw) {
		try {
			return yuanToFenTruncated(raw);
		} catch (Exception ex) {
			return null;
		}
	}

	private static int yuanToFenTruncated(Object raw) {
		BigDecimal bd = new BigDecimal(String.valueOf(raw).trim());
		return bd.multiply(BigDecimal.valueOf(100))
				.setScale(0, RoundingMode.DOWN)
				.intValueExact();
	}

	private static boolean isNumericScalar(Object value) {
		if (value == null) {
			return false;
		}
		if (value instanceof Number) {
			return true;
		}
		if (value instanceof String s) {
			try {
				new BigDecimal(s.trim());
				return true;
			} catch (NumberFormatException ignored) {
				return false;
			}
		}
		return false;
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

	private record ValidatedSyncParams(String itemBn, String distributorCodeRaw) {}
}
