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
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import cn.shopex.ecshopx.goods.service.items.ItemStoreService;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2ItemStoreUpdateService {

	private static final String MSG_ITEM_CODE_ERROR = "商品货号参数错误";
	private static final String MSG_INCREASE_DECREASE_REQUIRED = "增加库存或减去库存二选一必填";
	private static final String MSG_INCREASE_STORE_ERROR = "增加库存数参数错误";
	private static final String MSG_DECREASE_STORE_ERROR = "减去库存数参数错误";
	private static final String MSG_BOTH_INCREASE_DECREASE = "增加库存或减去库存不能同时存在！";
	private static final String MSG_DISTRIBUTOR_CODE_ERROR = "店铺ID参数错误";
	private static final String MSG_GOODS_NOT_FOUND = "商品找不到";
	private static final String MSG_UNKNOWN_ERROR = "未知错误";

	private final ItemsRepository itemsRepository;
	private final ItemStoreService itemStoreService;
	private final ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver;

	public OpenapiThirdApiV2ItemStoreUpdateService(
			ItemsRepository itemsRepository,
			ItemStoreService itemStoreService,
			ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver) {
		this.itemsRepository = itemsRepository;
		this.itemStoreService = itemStoreService;
		this.itemsCategoryDistributorIdResolver = itemsCategoryDistributorIdResolver;
	}

	public void executeUpdateStore(long companyId, Map<String, Object> mergedParams) {
		try {
			ValidatedUpdateParams validated = validateAndExtract(mergedParams);
			String productModel = itemsCategoryDistributorIdResolver.resolveProductModel(companyId);
			if (StringUtils.hasText(validated.distributorCodeRaw()) && "standard".equals(productModel)) {
				return;
			}
			updateHeadquartersStoreDelta(companyId, validated.itemBn(), validated.delta());
		} catch (OpenapiItemsV2FailException ex) {
			throw ex;
		} catch (Exception ex) {
			throw toUpdateFail(ex);
		}
	}

	private static ValidatedUpdateParams validateAndExtract(Map<String, Object> mergedParams) {
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

		boolean hasIncrease = mergedParams.containsKey("increase_store");
		boolean hasDecrease = mergedParams.containsKey("decrease_store");

		if (!hasIncrease && !hasDecrease) {
			throw missingParams(MSG_INCREASE_DECREASE_REQUIRED);
		}
		if (hasIncrease && hasDecrease) {
			throw missingParams(MSG_BOTH_INCREASE_DECREASE);
		}

		String itemBn = String.valueOf(mergedParams.get("item_code")).trim();
		String distributorCodeRaw = mergedParams.containsKey("distributor_code")
				? String.valueOf(mergedParams.get("distributor_code"))
				: null;

		if (hasIncrease) {
			Integer inc = parseNonNegativeIntStrict(mergedParams.get("increase_store"));
			if (inc == null) {
				throw missingParams(MSG_INCREASE_STORE_ERROR);
			}
			return new ValidatedUpdateParams(itemBn, inc, distributorCodeRaw);
		}

		Integer dec = parseNonNegativeIntStrict(mergedParams.get("decrease_store"));
		if (dec == null) {
			throw missingParams(MSG_DECREASE_STORE_ERROR);
		}
		return new ValidatedUpdateParams(itemBn, -dec, distributorCodeRaw);
	}

	private void updateHeadquartersStoreDelta(long companyId, String itemBn, int delta) {
		Items item = itemsRepository.findByItemBnAndCompany(itemBn, companyId);
		if (item == null || item.getItemId() == null) {
			throw new OpenapiItemsV2FailException(OpenapiErrorCode.GOODS_NOT_FOUND, MSG_GOODS_NOT_FOUND);
		}
		long itemId = item.getItemId();
		if (delta == 0) {
			return;
		}
		int affected = itemsRepository.adjustItemStoreByCompanyAndItemBn(companyId, itemBn, delta);
		if (affected == 0) {
			return;
		}
		Items updated = itemsRepository.findByItemBnAndCompany(itemBn, companyId);
		int newStore = updated != null && updated.getStore() != null ? updated.getStore() : 0;
		itemStoreService.saveItemStore(itemId, newStore, 0L);
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

	private static OpenapiItemsV2FailException missingParams(String message) {
		return new OpenapiItemsV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}

	private static OpenapiItemsV2FailException toUpdateFail(Exception ex) {
		String msg = ex.getMessage();
		if (msg == null || msg.isBlank()) {
			msg = MSG_UNKNOWN_ERROR;
		}
		return new OpenapiItemsV2FailException("E5000", msg);
	}

	private record ValidatedUpdateParams(String itemBn, int delta, String distributorCodeRaw) {}
}
