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

import cn.shopex.ecshopx.common.openapi.OpenapiDistributorItemDownloadCommand;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorV2FailException;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import java.util.Map;

public final class OpenapiThirdApiV2DistributorItemDownloadParams {

	private static final String MSG_SHOP_CODE = "店铺号参数有误";
	private static final String MSG_ITEM_CODE = "商品货号参数错误";

	private OpenapiThirdApiV2DistributorItemDownloadParams() {}

	public static OpenapiDistributorItemDownloadCommand resolve(
			String shopCodeParam,
			String itemCodeParam,
			Map<String, Object> body) {
		String shopCode = OpenapiRequestParams.mergeString(shopCodeParam, body, "shop_code");
		if (shopCode == null) {
			throw new OpenapiDistributorV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, MSG_SHOP_CODE);
		}

		String itemCode = OpenapiRequestParams.mergeString(itemCodeParam, body, "item_code");
		if (itemCode == null) {
			throw new OpenapiDistributorV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, MSG_ITEM_CODE);
		}

		return new OpenapiDistributorItemDownloadCommand(shopCode, itemCode);
	}
}
