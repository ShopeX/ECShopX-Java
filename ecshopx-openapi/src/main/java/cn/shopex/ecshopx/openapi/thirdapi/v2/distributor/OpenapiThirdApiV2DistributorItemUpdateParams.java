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

package cn.shopex.ecshopx.openapi.thirdapi.v2.distributor;

import cn.shopex.ecshopx.common.openapi.OpenapiDistributorItemUpdateCommand;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorV2FailException;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

public final class OpenapiThirdApiV2DistributorItemUpdateParams {

	private static final String MSG_SHOP_CODE = "店铺号参数有误";
	private static final String MSG_ITEM_CODE = "商品货号参数错误";
	private static final String MSG_IS_CAN_SALE = "店铺商品是否上架参数错误";
	private static final String MSG_IS_TOTAL_STORE = "店铺商品是否总部发货参数错误";

	private OpenapiThirdApiV2DistributorItemUpdateParams() {}

	public static OpenapiDistributorItemUpdateCommand resolve(
			String shopCodeParam,
			String itemCodeParam,
			String isCanSaleParam,
			String isTotalStoreParam,
			String storeParam,
			String priceParam,
			Map<String, Object> body) {
		String shopCode = OpenapiRequestParams.mergeString(shopCodeParam, body, "shop_code");
		if (shopCode == null) {
			throw new OpenapiDistributorV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, MSG_SHOP_CODE);
		}

		String itemCode = OpenapiRequestParams.mergeString(itemCodeParam, body, "item_code");
		if (itemCode == null) {
			throw new OpenapiDistributorV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, MSG_ITEM_CODE);
		}

		Optional<Boolean> isCanSaleOpt =
				parseZeroOneOptional(isCanSaleParam, body, "is_can_sale", MSG_IS_CAN_SALE);
		Optional<Boolean> isTotalStoreOpt =
				parseZeroOneOptional(isTotalStoreParam, body, "is_total_store", MSG_IS_TOTAL_STORE);
		Optional<Long> storeOpt = parseNumericStoreOptional(storeParam, body, "store");
		Optional<Long> priceFenOpt = parseNumericPriceOptional(priceParam, body, "price");

		return new OpenapiDistributorItemUpdateCommand(
				shopCode, itemCode, isCanSaleOpt, isTotalStoreOpt, storeOpt, priceFenOpt);
	}

	private static Optional<Boolean> parseZeroOneOptional(
			String param, Map<String, Object> body, String key, String errorMsg) {
		Optional<String> present = OpenapiRequestParams.presentOptionalString(param, body, key);
		if (present.isEmpty()) {
			return Optional.empty();
		}
		String raw = present.get();
		if (raw == null) {
			throw new OpenapiDistributorV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, errorMsg);
		}
		String t = raw.trim();
		if ("0".equals(t)) {
			return Optional.of(false);
		}
		if ("1".equals(t)) {
			return Optional.of(true);
		}
		throw new OpenapiDistributorV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, errorMsg);
	}

	private static Optional<Long> parseNumericStoreOptional(
			String param, Map<String, Object> body, String key) {
		Optional<String> present = OpenapiRequestParams.presentOptionalString(param, body, key);
		if (present.isEmpty()) {
			return Optional.empty();
		}
		String raw = present.get();
		if (!isNumericEquivalent(raw)) {
			return Optional.empty();
		}
		BigDecimal bd = new BigDecimal(raw.trim());
		return Optional.of((long) bd.intValue());
	}

	private static Optional<Long> parseNumericPriceOptional(
			String param, Map<String, Object> body, String key) {
		Optional<String> present = OpenapiRequestParams.presentOptionalString(param, body, key);
		if (present.isEmpty()) {
			return Optional.empty();
		}
		String raw = present.get();
		if (!isNumericEquivalent(raw)) {
			return Optional.empty();
		}
		BigDecimal yuan = new BigDecimal(raw.trim());
		long fen = yuan.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
		return Optional.of(fen);
	}

	private static boolean isNumericEquivalent(String raw) {
		if (raw == null || raw.isBlank()) {
			return false;
		}
		try {
			new BigDecimal(raw.trim());
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
