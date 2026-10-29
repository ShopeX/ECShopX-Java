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

package cn.shopex.ecshopx.goods.service.operatorcart;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.operatorcart.OperatorCartFastBuyFacade;
import cn.shopex.ecshopx.common.operatorcart.OperatorCartSkuLoadFacade;
import cn.shopex.ecshopx.common.operatorcart.dto.OperatorCartSkuRowDto;
import cn.shopex.ecshopx.companys.service.operatorcart.OperatorCartCompanyProductModelReader;
import cn.shopex.ecshopx.companys.service.operatorcart.OperatorFastBuyStockValidator;
import cn.shopex.ecshopx.companys.service.operatorcart.OperatorShopFastBuyRedisService;
import cn.shopex.ecshopx.distribution.service.DistributorPlatformStoreBuyGateService;
import cn.shopex.ecshopx.distribution.service.DistributorPlatformStoreBuySupport;
import cn.shopex.ecshopx.goods.service.cart.salesperson.SalespersonDistributorCartFormatAndTotalService;
import cn.shopex.ecshopx.goods.service.items.OnsalePlatformStoreSupport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OperatorCartFastBuyFacadeImpl implements OperatorCartFastBuyFacade {

	private final OperatorShopFastBuyRedisService operatorShopFastBuyRedisService;
	private final OperatorCartSkuLoadFacade operatorCartSkuLoadFacade;
	private final OperatorCartCompanyProductModelReader productModelReader;
	private final DistributorPlatformStoreBuyGateService distributorPlatformStoreBuyGateService;
	private final OperatorCartHandleValidCartService operatorCartHandleValidCartService;
	private final SalespersonDistributorCartFormatAndTotalService salespersonDistributorCartFormatAndTotalService;

	public OperatorCartFastBuyFacadeImpl(
			OperatorShopFastBuyRedisService operatorShopFastBuyRedisService,
			OperatorCartSkuLoadFacade operatorCartSkuLoadFacade,
			OperatorCartCompanyProductModelReader productModelReader,
			DistributorPlatformStoreBuyGateService distributorPlatformStoreBuyGateService,
			OperatorCartHandleValidCartService operatorCartHandleValidCartService,
			SalespersonDistributorCartFormatAndTotalService salespersonDistributorCartFormatAndTotalService) {
		this.operatorShopFastBuyRedisService = operatorShopFastBuyRedisService;
		this.operatorCartSkuLoadFacade = operatorCartSkuLoadFacade;
		this.productModelReader = productModelReader;
		this.distributorPlatformStoreBuyGateService = distributorPlatformStoreBuyGateService;
		this.operatorCartHandleValidCartService = operatorCartHandleValidCartService;
		this.salespersonDistributorCartFormatAndTotalService = salespersonDistributorCartFormatAndTotalService;
	}

	@Override
	public Map<String, Object> addFastBuyCartData(
			long companyId, long operatorId, long distributorId, long itemId, long num, boolean accumulate) {
		validateWriteParams(companyId, operatorId, distributorId, itemId, num);
		String productModel = productModelReader.getProductModel(companyId);
		OperatorCartSkuRowDto sku =
				operatorCartSkuLoadFacade.resolveSkuForOperatorCartAdd(companyId, distributorId, itemId, productModel);
		Map<String, Object> itemInfo = skuToItemInfo(sku);
		distributorPlatformStoreBuyGateService.assertShopadminFastBuyAllowed(companyId, distributorId, itemInfo);
		int platformStock = OnsalePlatformStoreSupport.resolvePlatformStoreForSkuRow(itemInfo);
		int shopStock = resolveShopStockForFastBuy(distributorId, productModel, itemInfo);
		OperatorFastBuyStockValidator.validate(shopStock, platformStock, (int) Math.min(num, Integer.MAX_VALUE));

		Map<String, Object> existing = operatorShopFastBuyRedisService.get(companyId, operatorId, distributorId);
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("company_id", companyId);
		row.put("operator_id", operatorId);
		row.put("distributor_id", distributorId);
		row.put("item_id", itemId);
		row.put("is_checked", true);
		row.put("special_type", sku.getSpecialType() != null ? sku.getSpecialType() : "normal");
		long existingItemId = longVal(existing.get("item_id"), 0L);
		if (accumulate && existingItemId == itemId) {
			row.put("num", longVal(existing.get("num"), 0L) + num);
		} else {
			row.put("num", num);
		}
		operatorShopFastBuyRedisService.set(companyId, operatorId, distributorId, row);
		row.put("cart_id", 0);
		return row;
	}

	@Override
	public Map<String, Object> getFastBuyCartDataList(
			long companyId, long operatorId, long distributorId, long targetUserId, boolean isSubmit) {
		Map<String, Object> bucket = operatorShopFastBuyRedisService.get(companyId, operatorId, distributorId);
		long itemId = longVal(bucket.get("item_id"), 0L);
		long num = longVal(bucket.get("num"), 0L);
		if (itemId <= 0L) {
			if (isSubmit) {
				throw new ResourceException("购物车为空");
			}
			return emptyBody();
		}
		String productModel = productModelReader.getProductModel(companyId);
		OperatorCartSkuRowDto sku =
				operatorCartSkuLoadFacade.resolveSkuForOperatorCartAdd(companyId, distributorId, itemId, productModel);
		Map<String, Object> itemInfo = skuToItemInfo(sku);
		int platformStock = OnsalePlatformStoreSupport.resolvePlatformStoreForSkuRow(itemInfo);
		int shopStock = resolveShopStockForFastBuy(distributorId, productModel, itemInfo);
		if (isSubmit) {
			OperatorFastBuyStockValidator.validate(shopStock, platformStock, (int) Math.min(num, Integer.MAX_VALUE));
		}
		itemInfo.put("store", Math.max(intVal(itemInfo.get("store")), platformStock));

		List<Map<String, Object>> cartRowMaps = new ArrayList<>();
		Map<String, Object> cartRow = new LinkedHashMap<>();
		cartRow.put("cart_id", 0L);
		cartRow.put("company_id", companyId);
		cartRow.put("operator_id", operatorId);
		cartRow.put("distributor_id", distributorId);
		cartRow.put("item_id", itemId);
		cartRow.put("num", num);
		cartRow.put("is_checked", true);
		cartRow.put("special_type", sku.getSpecialType() != null ? sku.getSpecialType() : "normal");
		cartRow.put("shop_type", "shop_offline");
		cartRow.put("shop_id", distributorId);
		cartRow.put("activity_type", "normal");
		cartRow.put("items_id", List.of());
		cartRowMaps.add(cartRow);

		Map<Long, Map<String, Object>> itemByItemId = new LinkedHashMap<>();
		itemByItemId.put(itemId, itemInfo);

		Map<String, Object> handled =
				operatorCartHandleValidCartService.handle(companyId, targetUserId, cartRowMaps, itemByItemId);
		handled.put("is_check_store", Boolean.FALSE);
		return salespersonDistributorCartFormatAndTotalService.apply(
				companyId, targetUserId, handled, isSubmit, distributorId, "distributor", "zh-CN");
	}

	@Override
	public void revalidateFastBuyPlatformStock(long companyId, long operatorId, long distributorId) {
		Map<String, Object> bucket = operatorShopFastBuyRedisService.get(companyId, operatorId, distributorId);
		long itemId = longVal(bucket.get("item_id"), 0L);
		long num = longVal(bucket.get("num"), 0L);
		if (itemId <= 0L) {
			throw new ResourceException("立即购买已失效，请重新加购");
		}
		String productModel = productModelReader.getProductModel(companyId);
		OperatorCartSkuRowDto sku =
				operatorCartSkuLoadFacade.resolveSkuForOperatorCartAdd(companyId, distributorId, itemId, productModel);
		Map<String, Object> itemInfo = skuToItemInfo(sku);
		distributorPlatformStoreBuyGateService.assertShopadminFastBuyAllowed(companyId, distributorId, itemInfo);
		int platformStock = OnsalePlatformStoreSupport.resolvePlatformStoreForSkuRow(itemInfo);
		int shopStock = resolveShopStockForFastBuy(distributorId, productModel, itemInfo);
		OperatorFastBuyStockValidator.validate(shopStock, platformStock, (int) Math.min(num, Integer.MAX_VALUE));
	}

	@Override
	public void clearFastBuyCart(long companyId, long operatorId, long distributorId) {
		operatorShopFastBuyRedisService.clear(companyId, operatorId, distributorId);
	}

	static int resolveShopStockForFastBuy(long distributorId, String productModel, Map<String, Object> itemInfo) {
		if (distributorId <= 0L) {
			return 0;
		}
		if (productModel != null && "platform".equalsIgnoreCase(productModel)) {
			return 0;
		}
		if (DistributorPlatformStoreBuySupport.isSkuHeadquartersShipment(itemInfo)) {
			return 0;
		}
		return Math.max(0, intVal(itemInfo.get("store")));
	}

	private static Map<String, Object> skuToItemInfo(OperatorCartSkuRowDto sku) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("item_id", sku.getItemId());
		m.put("store", sku.getStore());
		m.put("logistics_store", sku.getLogisticsStore());
		if (sku.getIsTotalStore() != null) {
			m.put("is_total_store", sku.getIsTotalStore());
		}
		if (sku.getDistributorStore() != null) {
			m.put("distributor_store", sku.getDistributorStore());
		}
		m.put("item_name", sku.getItemName() != null ? sku.getItemName() : "");
		m.put("special_type", sku.getSpecialType() != null ? sku.getSpecialType() : "normal");
		m.put("price", (long) sku.getUnitPriceFen());
		m.put("sale_price", (long) sku.getUnitPriceFen());
		m.put("approve_status", sku.getItemApproveStatus() != null ? sku.getItemApproveStatus() : "onsale");
		m.put("item_type", sku.getItemType() != null ? sku.getItemType() : "normal");
		return m;
	}

	private static void validateWriteParams(long companyId, long operatorId, long distributorId, long itemId, long num) {
		if (operatorId <= 0L) {
			throw new BadRequestException("管理员信息有误");
		}
		if (companyId <= 0L) {
			throw new BadRequestException("企业信息有误");
		}
		if (distributorId <= 0L) {
			throw new BadRequestException("店铺信息有误");
		}
		if (itemId <= 0L) {
			throw new BadRequestException("购物车商品有误");
		}
		if (num <= 0L) {
			throw new ResourceException("购买数量有误");
		}
	}

	private static Map<String, Object> emptyBody() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("invalid_cart", List.of());
		m.put("valid_cart", List.of());
		return m;
	}

	private static long longVal(Object o, long dft) {
		if (o == null) {
			return dft;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return dft;
		}
	}

	private static int intVal(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
