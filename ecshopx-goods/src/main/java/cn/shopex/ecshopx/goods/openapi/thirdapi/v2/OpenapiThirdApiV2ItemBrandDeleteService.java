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
import cn.shopex.ecshopx.goods.service.ItemsAttributesDeleteService;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2ItemBrandDeleteService {

	private final ItemsAttributesDeleteService itemsAttributesDeleteService;

	public OpenapiThirdApiV2ItemBrandDeleteService(
			ItemsAttributesDeleteService itemsAttributesDeleteService) {
		this.itemsAttributesDeleteService = itemsAttributesDeleteService;
	}

	public Map<String, Object> executeOpenapiDeleteItemBrand(long companyId, String brandIdRaw) {
		if (brandIdRaw == null || brandIdRaw.isEmpty()) {
			throw missingParams("品牌ID必填");
		}
		Long attributeId = parseBrandIdForDelete(brandIdRaw);
		if (attributeId == null) {
			return Map.of("status", Boolean.TRUE);
		}
		try {
			itemsAttributesDeleteService.deleteAttr(companyId, attributeId);
		} catch (OpenapiItemsV2FailException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new OpenapiItemsV2FailException(
					OpenapiErrorCode.GOODS_BRAND_DELETE_ERROR, ex.getMessage());
		}
		return Map.of("status", Boolean.TRUE);
	}

	private static Long parseBrandIdForDelete(String brandIdRaw) {
		String trimmed = brandIdRaw.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static OpenapiItemsV2FailException missingParams(String message) {
		return new OpenapiItemsV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}
}
