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

package cn.shopex.ecshopx.goods.openapi;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.openapi.OpenapiProductStockUpdatePort;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.items.ItemStoreService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OpenapiProductStockUpdatePortImpl implements OpenapiProductStockUpdatePort {

	private static final Logger log = LoggerFactory.getLogger(OpenapiProductStockUpdatePortImpl.class);
	private static final TypeReference<List<Map<String, Object>>> SKU_LIST_TYPE = new TypeReference<>() {};

	private final ItemsRepository itemsRepository;
	private final ItemStoreService itemStoreService;
	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final ObjectMapper objectMapper;

	public OpenapiProductStockUpdatePortImpl(
			ItemsRepository itemsRepository,
			ItemStoreService itemStoreService,
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			ObjectMapper objectMapper) {
		this.itemsRepository = itemsRepository;
		this.itemStoreService = itemStoreService;
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.objectMapper = objectMapper;
	}

	@Override
	public Map<String, Object> updateItemStore(long companyId, String skuListRaw) {
		List<Map<String, Object>> listQuantity = parseSkuList(skuListRaw);

		List<String> itemBns = new ArrayList<>();
		for (Map<String, Object> entry : listQuantity) {
			Object skuId = entry.get("sku_id");
			if (skuId != null) {
				itemBns.add(String.valueOf(skuId));
			}
		}
		Map<String, Items> itemBnMap = itemsRepository.mapItemsByItemBnIn(itemBns);

		List<String> notFindSku = new ArrayList<>();
		List<String> notUpdateSku = new ArrayList<>();
		List<Map<String, Object>> updateError = new ArrayList<>();
		List<String> activityBns = List.of();

		for (Map<String, Object> value : listQuantity) {
			Object skuIdObj = value.get("sku_id");
			String skuId = skuIdObj == null ? null : String.valueOf(skuIdObj).trim();
			if (!StringUtils.hasText(skuId) || !value.containsKey("stock")) {
				continue;
			}
			if (activityBns.contains(skuId)) {
				notUpdateSku.add(skuId);
				continue;
			}
			Items item = itemBnMap.get(skuId);
			if (item == null) {
				notFindSku.add(skuId);
				continue;
			}
			if (!isPhpEmptyShopCode(value.get("shop_code"))) {
				handleShopBranch(companyId, value, skuId, updateError);
			} else {
				handleHeadquartersBranch(item, value, skuId, updateError);
			}
		}

		if (!notFindSku.isEmpty()) {
			log.debug("openapi-更新库存商品不存在：{}", notFindSku);
		}
		if (!notUpdateSku.isEmpty()) {
			log.debug("openapi-活动商品暂不更新库存：{}", notUpdateSku);
		}
		if (!updateError.isEmpty()) {
			log.debug("openapi-库存更新失败商品：{}", updateError);
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("not_find_sku", notFindSku);
		data.put("not_update_sku", notUpdateSku);
		data.put("update_error", updateError);
		return data;
	}

	private void handleHeadquartersBranch(Items item, Map<String, Object> value, String skuId,
			List<Map<String, Object>> updateError) {
		long itemId = item.getItemId();
		int store = parseStockWeak(value.get("stock"));
		try {
			itemStoreService.saveItemStore(itemId, store, 0L);
			itemsRepository.updateSingleItemStoreIfExists(itemId, store);
		} catch (Exception e) {
			Map<String, Object> err = new LinkedHashMap<>();
			err.put("sku_id", skuId);
			err.put("error", "更新失败");
			updateError.add(err);
		}
	}

	private void handleShopBranch(long companyId, Map<String, Object> value, String skuId,
			List<Map<String, Object>> updateError) {
		String shopCode = String.valueOf(value.get("shop_code")).trim();
		Map<String, Object> distInfo = distributorRepositoryGetInfoSimpleService
				.getInfoSimpleByShopCode(companyId, shopCode);
		long distributorId = extractLong(distInfo, "distributor_id");
		if (distributorId <= 0L) {
			Map<String, Object> err = new LinkedHashMap<>();
			err.put("sku_id", skuId);
			err.put("shop_code", String.valueOf(value.get("shop_code")));
			err.put("error", "门店不存在");
			updateError.add(err);
			return;
		}
		Map<String, Object> shopUpdateData = new LinkedHashMap<>();
		shopUpdateData.put("distributor_id", distributorId);
		shopUpdateData.put("item_bn", skuId);
		shopUpdateData.put("store", value.get("stock"));
		try {
			boolean ok = handleShopRowLikePhp(companyId, shopUpdateData);
			if (!ok) {
				Map<String, Object> err = new LinkedHashMap<>();
				err.put("sku_id", skuId);
				err.put("shop_code", String.valueOf(value.get("shop_code")));
				err.put("error", "更新失败");
				updateError.add(err);
			}
		} catch (Exception e) {
			Map<String, Object> err = new LinkedHashMap<>();
			err.put("sku_id", skuId);
			err.put("shop_code", String.valueOf(value.get("shop_code")));
			err.put("error", e.getMessage());
			updateError.add(err);
		}
	}

	private boolean handleShopRowLikePhp(long companyId, Map<String, Object> row) {
		Object did = row.get("did");
		if (did == null || String.valueOf(did).trim().isEmpty()) {
			throw new BadRequestException("请填写店铺ID");
		}
		return false;
	}

	private List<Map<String, Object>> parseSkuList(String skuListRaw) {
		try {
			Object parsed = objectMapper.readValue(skuListRaw, Object.class);
			if (parsed == null) {
				throw new ResourceException("数据有误");
			}
			if (!(parsed instanceof List<?>)) {
				throw new ResourceException("数据有误");
			}
			return objectMapper.convertValue(parsed, SKU_LIST_TYPE);
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("数据有误");
		}
	}

	private static boolean isPhpEmptyShopCode(Object shopCode) {
		if (shopCode == null) {
			return true;
		}
		if (shopCode instanceof Number n && n.intValue() == 0) {
			return true;
		}
		String s = String.valueOf(shopCode).trim();
		return s.isEmpty() || "0".equals(s);
	}

	private static int parseStockWeak(Object stock) {
		if (stock == null) {
			return 0;
		}
		if (stock instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(stock).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
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
}
