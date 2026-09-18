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

package cn.shopex.ecshopx.goods.service.espier;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.items.ItemStoreService;
import cn.shopex.ecshopx.promotions.service.ItemCreatePromotionGuardService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UpdateDistributionItemImportRowService {

	private static final int STORE_MIN = 0;
	private static final int STORE_MAX = 999999;
	private static final int BOOL_TRUE = 1;
	private static final int BOOL_FALSE = 0;

	private final ItemsRepository itemsRepository;
	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final DistributorItemsRepository distributorItemsRepository;
	private final ItemStoreService itemStoreService;
	private final ItemCreatePromotionGuardService itemCreatePromotionGuardService;

	public UpdateDistributionItemImportRowService(
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
	public void applyRow(long companyId, Map<String, Object> row) {
		Map<String, String> rd = normalizeRow(row);
		long distributionId = parseLongOrZero(rd.get("distribution_id"));
		String shopCode = rd.get("shop_code");
		String spuBn = rd.get("spu_bn");
		String itemBn = rd.get("item_bn");
		String isOnsaleRaw = rd.get("is_onsale");
		String isTotalRaw = rd.get("is_total_store");
		String itemStoreRaw = rd.get("item_store");
		String itemPriceRaw = rd.get("item_price");

		if (distributionId <= 0L && !StringUtils.hasText(shopCode)) {
			throw new BadRequestException("店铺ID与店铺号必填一项");
		}
		if (!StringUtils.hasText(spuBn)) {
			throw new BadRequestException("未填写商品SPU");
		}
		if (!StringUtils.hasText(itemBn)) {
			throw new BadRequestException("未填写商品货号");
		}

		Boolean updateIsCanSale = null;
		if (StringUtils.hasText(isOnsaleRaw)) {
			Integer v = parseBoolChoice(isOnsaleRaw, "更新是否上架数据失败,请查看填写说明重新上传或联系客服处理");
			updateIsCanSale = v == BOOL_TRUE;
		}

		Boolean updateIsTotalStore = null;
		if (StringUtils.hasText(isTotalRaw)) {
			Integer v = parseBoolChoice(isTotalRaw, "更新是否总部发货数据失败,请查看填写说明重新上传或联系客服处理");
			updateIsTotalStore = v == BOOL_TRUE;
		}

		Long updateStore = null;
		if (StringUtils.hasText(itemStoreRaw)) {
			if (!isStrictIntString(itemStoreRaw)) {
				throw new ResourceException("库存数量格式错误");
			}
			int st = Integer.parseInt(itemStoreRaw.trim());
			if (st < STORE_MIN) {
				throw new ResourceException("库存数量不能小于" + STORE_MIN);
			}
			if (st > STORE_MAX) {
				throw new ResourceException("库存数量不能大于" + STORE_MAX);
			}
			updateStore = (long) st;
		}

		Long updatePriceFen = null;
		if (StringUtils.hasText(itemPriceRaw)) {
			BigDecimal yuan = new BigDecimal(itemPriceRaw.trim());
			if (yuan.compareTo(BigDecimal.ZERO) <= 0) {
				throw new ResourceException("更新价格数据失败,请查看填写说明重新上传或联系客服处理");
			}
			updatePriceFen = yuan.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
		}

		boolean anyUpdate = updateIsCanSale != null || updateIsTotalStore != null || updateStore != null || updatePriceFen != null;
		if (!anyUpdate) {
			return;
		}

		Items itemInfo = itemsRepository.findByItemBnAndCompany(itemBn, companyId);
		if (itemInfo == null || itemInfo.getItemId() == null || itemInfo.getItemId() <= 0L) {
			throw new ResourceException("未查询到对应商品");
		}
		long itemId = itemInfo.getItemId();
		boolean isDefault = Boolean.TRUE.equals(itemInfo.getIsDefault());
		long goodsId = itemInfo.getGoodsId() != null ? itemInfo.getGoodsId() : 0L;
		Items spuInfo = itemsRepository.findDefaultByGoodsBnAndCompany(spuBn, companyId);
		if (spuInfo == null) {
			throw new ResourceException("未查询到对应商品SPU");
		}
		if (goodsId <= 0L || !Long.valueOf(goodsId).equals(spuInfo.getGoodsId())) {
			throw new ResourceException("商品货号与商品SPU不属于同一商品");
		}
		long defaultItemId = spuInfo.getItemId() != null ? spuInfo.getItemId() : 0L;

		long distributorId = resolveDistributorId(companyId, distributionId, shopCode);
		if (distributorId <= 0L) {
			throw new ResourceException("未查询到对应店铺");
		}

		Optional<DistributorItems> existingOpt =
				distributorItemsRepository.findByDistributorIdAndCompanyIdAndItemId(distributorId, companyId, itemId);

		boolean issetData = existingOpt.isPresent();
		boolean isTotalStoreEffective;

		if (!issetData) {
			boolean isCanSaleEffective = updateIsCanSale != null ? updateIsCanSale : false;
			if (updateIsTotalStore == null) {
				Optional<DistributorItems> spuRow = defaultItemId > 0L
						? distributorItemsRepository.findByDistributorIdAndCompanyIdAndItemId(distributorId, companyId, defaultItemId)
						: Optional.empty();
				if (spuRow.isEmpty()) {
					isTotalStoreEffective = false;
				} else {
					isTotalStoreEffective = Boolean.TRUE.equals(spuRow.get().getIsTotalStore());
				}
			} else {
				isTotalStoreEffective = updateIsTotalStore;
			}
			if (isTotalStoreEffective && (updateStore != null || updatePriceFen != null)) {
				throw new ResourceException("商品总部发货无法更新库存和价格");
			}
			long storeVal = updateStore != null ? updateStore : intToLong(itemInfo.getStore());
			long priceVal = updatePriceFen != null ? updatePriceFen : intToLong(itemInfo.getPrice());
			if (updatePriceFen != null) {
				assertPriceVsPromotions(companyId, itemInfo, itemId, updatePriceFen);
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
			created.setIsCanSale(isCanSaleEffective);
			created.setIsTotalStore(isTotalStoreEffective);
			created.setStore(storeVal);
			created.setPrice(priceVal);
			distributorItemsRepository.insert(created);
			if (updateIsTotalStore != null && defaultItemId > 0L) {
				distributorItemsRepository.updateIsTotalStoreByDistributorAndGoodsGroup(distributorId, defaultItemId, isTotalStoreEffective);
			}
			return;
		}

		DistributorItems distributorItem = existingOpt.get();
		boolean isTotalStoreFromPatch = updateIsTotalStore != null ? updateIsTotalStore : Boolean.TRUE.equals(distributorItem.getIsTotalStore());
		isTotalStoreEffective = isTotalStoreFromPatch;
		if (isTotalStoreEffective && (updateStore != null || updatePriceFen != null)) {
			throw new ResourceException("商品总部发货无法更新库存和价格");
		}

		if (updatePriceFen != null) {
			assertPriceVsPromotions(companyId, itemInfo, itemId, updatePriceFen);
		}

		distributorItemsRepository.updateColumnsByDistributorCompanyItem(
				distributorId,
				companyId,
				itemId,
				updateIsCanSale,
				updateIsTotalStore,
				updateStore,
				updatePriceFen);

		if (updateStore != null && updateIsTotalStore != null && Boolean.FALSE.equals(updateIsTotalStore)) {
			int st = updateStore > Integer.MAX_VALUE ? Integer.MAX_VALUE : updateStore.intValue();
			itemStoreService.saveItemStore(itemId, st, distributorId);
		}

		if (updateIsTotalStore != null && defaultItemId > 0L) {
			distributorItemsRepository.updateIsTotalStoreByDistributorAndGoodsGroup(distributorId, defaultItemId, isTotalStoreEffective);
		}
	}

	private void assertPriceVsPromotions(long companyId, Items itemInfo, long itemId, long priceFen) {
		Map<Long, Long> priceMap = new HashMap<>();
		priceMap.put(itemId, priceFen);
		Long gid = itemInfo.getGoodsId();
		if (gid != null && gid > 0L) {
			priceMap.put(gid, priceFen);
		}
		List<Long> goodsIds = gid != null && gid > 0L ? List.of(gid) : List.of();
		itemCreatePromotionGuardService.checkItemPrice(companyId, goodsIds, priceMap);
	}

	private static long intToLong(Integer v) {
		return v == null ? 0L : v.longValue();
	}

	private static boolean isStrictIntString(String s) {
		String t = s.trim();
		if (t.isEmpty()) {
			return false;
		}
		for (int i = 0; i < t.length(); i++) {
			if (!Character.isDigit(t.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	private static Integer parseBoolChoice(String raw, String err) {
		String t = raw.trim();
		try {
			int n = new BigDecimal(t).intValue();
			if (n == BOOL_TRUE) {
				return BOOL_TRUE;
			}
			if (n == BOOL_FALSE) {
				return BOOL_FALSE;
			}
		} catch (NumberFormatException ignored) {
		}
		throw new ResourceException(err);
	}

	private long resolveDistributorId(long companyId, long distributionId, String shopCode) {
		if (distributionId > 0L) {
			Object info = distributorRepositoryGetInfoSimpleService.getInfoSimple(companyId, Long.toString(distributionId));
			long did = parseDistributorIdFromInfo(info);
			if (did > 0L) {
				return did;
			}
		}
		if (StringUtils.hasText(shopCode)) {
			Map<String, Object> m = distributorRepositoryGetInfoSimpleService.getInfoSimpleByShopCode(companyId, shopCode);
			return parseDistributorIdFromMap(m);
		}
		return 0L;
	}

	private static long parseDistributorIdFromInfo(Object info) {
		if (info instanceof Map<?, ?> m) {
			return parseDistributorIdFromMap(castStringObjectMap(m));
		}
		if (info instanceof List<?> list && list.size() == 1 && list.get(0) instanceof Map<?, ?> m) {
			return parseDistributorIdFromMap(castStringObjectMap(m));
		}
		return 0L;
	}

	private static long parseDistributorIdFromMap(Map<String, Object> m) {
		if (m == null || m.isEmpty()) {
			return 0L;
		}
		Object v = m.get("distributor_id");
		return v == null ? 0L : parseLongOrZero(String.valueOf(v));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> castStringObjectMap(Map<?, ?> m) {
		Map<String, Object> out = new HashMap<>();
		for (Map.Entry<?, ?> e : m.entrySet()) {
			out.put(String.valueOf(e.getKey()), e.getValue());
		}
		return out;
	}

	private static Map<String, String> normalizeRow(Map<String, Object> row) {
		Map<String, String> out = new HashMap<>();
		for (Map.Entry<String, Object> e : row.entrySet()) {
			Object v = e.getValue();
			if (v == null) {
				out.put(e.getKey(), "");
			} else if (v instanceof String s) {
				out.put(e.getKey(), s.trim());
			} else {
				out.put(e.getKey(), String.valueOf(v).trim());
			}
		}
		return out;
	}

	private static long parseLongOrZero(String s) {
		if (s == null || s.isBlank()) {
			return 0L;
		}
		try {
			return new BigDecimal(s.trim()).longValue();
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
