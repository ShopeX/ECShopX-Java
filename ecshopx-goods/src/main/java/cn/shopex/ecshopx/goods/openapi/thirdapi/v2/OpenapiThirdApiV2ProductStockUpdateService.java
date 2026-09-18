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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiItemsV2FailException;
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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2ProductStockUpdateService {

	private static final Logger log = LoggerFactory.getLogger(OpenapiThirdApiV2ProductStockUpdateService.class);
	private static final String MSG_FORMAT_ERROR = "商品信息格式错误";
	private static final String MSG_SKU_ID_REQUIRED = "商品信息的货号必填";
	private static final String MSG_STOCK_INVALID = "商品信息的库存为0-999999999的整数";
	private static final String MSG_GOODS_NOT_FOUND = "商品找不到";
	private static final TypeReference<List<Map<String, Object>>> SKU_LIST_TYPE = new TypeReference<>() {};

	private final ItemsRepository itemsRepository;
	private final ItemStoreService itemStoreService;
	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final ObjectMapper objectMapper;

	public OpenapiThirdApiV2ProductStockUpdateService(
			ItemsRepository itemsRepository,
			ItemStoreService itemStoreService,
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			ObjectMapper objectMapper) {
		this.itemsRepository = itemsRepository;
		this.itemStoreService = itemStoreService;
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> executeOpenapiProductStockUpdate(long companyId, String skuListRaw) {
		List<Map<String, Object>> listQuantity = parseSkuList(skuListRaw);
		validateSkuListElements(listQuantity);

		List<String> itemBns = new ArrayList<>();
		for (Map<String, Object> entry : listQuantity) {
			Object skuId = entry.get("sku_id");
			if (skuId != null) {
				itemBns.add(String.valueOf(skuId));
			}
		}
		Map<String, Items> itemBnMap = itemsRepository.mapItemsByItemBnIn(itemBns);

		if (!itemBns.isEmpty() && itemBnMap.isEmpty()) {
			throw new OpenapiItemsV2FailException(OpenapiErrorCode.GOODS_NOT_FOUND, MSG_GOODS_NOT_FOUND);
		}

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
			if (isV2ShopBranch(value)) {
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
		int store = parseStockStrict(value.get("stock"));
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
		if (!StringUtils.hasText(skuListRaw)) {
			throw new OpenapiItemsV2FailException(
					OpenapiErrorCode.SERVICE_PARAMS_FORMAT_ERROR, MSG_FORMAT_ERROR);
		}
		try {
			Object parsed = objectMapper.readValue(skuListRaw.trim(), Object.class);
			if (parsed == null) {
				throw new OpenapiItemsV2FailException(
						OpenapiErrorCode.SERVICE_PARAMS_FORMAT_ERROR, MSG_FORMAT_ERROR);
			}
			if (!(parsed instanceof List<?> rawList)) {
				throw new OpenapiItemsV2FailException(
						OpenapiErrorCode.SERVICE_PARAMS_FORMAT_ERROR, MSG_FORMAT_ERROR);
			}
			for (Object element : rawList) {
				if (!(element instanceof Map<?, ?>)) {
					throw new OpenapiItemsV2FailException(
							OpenapiErrorCode.SERVICE_PARAMS_FORMAT_ERROR, MSG_FORMAT_ERROR);
				}
			}
			return objectMapper.convertValue(parsed, SKU_LIST_TYPE);
		} catch (OpenapiItemsV2FailException e) {
			throw e;
		} catch (Exception e) {
			throw new OpenapiItemsV2FailException(
					OpenapiErrorCode.SERVICE_PARAMS_FORMAT_ERROR, MSG_FORMAT_ERROR);
		}
	}

	private void validateSkuListElements(List<Map<String, Object>> listQuantity) {
		for (Map<String, Object> entry : listQuantity) {
			if (!entry.containsKey("sku_id")) {
				throw new OpenapiItemsV2FailException(
						OpenapiErrorCode.SERVICE_MISSING_PARAMS, MSG_SKU_ID_REQUIRED);
			}
			Object skuId = entry.get("sku_id");
			if (skuId == null || !StringUtils.hasText(String.valueOf(skuId).trim())) {
				throw new OpenapiItemsV2FailException(
						OpenapiErrorCode.SERVICE_MISSING_PARAMS, MSG_SKU_ID_REQUIRED);
			}
			if (!entry.containsKey("stock")) {
				throw new OpenapiItemsV2FailException(
						OpenapiErrorCode.SERVICE_MISSING_PARAMS, MSG_STOCK_INVALID);
			}
			Integer stock = parseStockStrict(entry.get("stock"));
			if (stock == null || stock < 0 || stock > 999_999_999) {
				throw new OpenapiItemsV2FailException(
						OpenapiErrorCode.SERVICE_MISSING_PARAMS, MSG_STOCK_INVALID);
			}
		}
	}

	private static boolean isV2ShopBranch(Map<String, Object> value) {
		if (!value.containsKey("shop_code")) {
			return false;
		}
		return phpTruthy(value.get("shop_code"));
	}

	private static boolean phpTruthy(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = String.valueOf(raw).trim();
		return !s.isEmpty() && !"0".equals(s);
	}

	private static Integer parseStockStrict(Object stock) {
		if (stock == null) {
			return null;
		}
		if (stock instanceof Number n) {
			double d = n.doubleValue();
			if (d == Math.floor(d)) {
				return n.intValue();
			}
			return null;
		}
		String s = String.valueOf(stock).trim();
		if (!s.matches("-?\\d+")) {
			return null;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return null;
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
