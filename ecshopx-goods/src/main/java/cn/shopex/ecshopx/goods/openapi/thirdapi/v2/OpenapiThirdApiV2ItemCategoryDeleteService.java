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
import cn.shopex.ecshopx.goods.service.ItemsCategoryDeleteService;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2ItemCategoryDeleteService {

	private final ItemsCategoryDeleteService itemsCategoryDeleteService;

	public OpenapiThirdApiV2ItemCategoryDeleteService(
			ItemsCategoryDeleteService itemsCategoryDeleteService) {
		this.itemsCategoryDeleteService = itemsCategoryDeleteService;
	}

	public Map<String, Object> executeOpenapiDeleteItemCategory(long companyId, String categoryIdRaw) {
		if (categoryIdRaw == null || categoryIdRaw.isEmpty()) {
			throw missingParams("分类ID必填");
		}
		Long categoryId = parseCategoryIdForDelete(categoryIdRaw);
		if (categoryId == null) {
			return Map.of("status", Boolean.TRUE);
		}
		try {
			itemsCategoryDeleteService.deleteItemsCategory(companyId, categoryId);
		} catch (OpenapiItemsV2FailException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new OpenapiItemsV2FailException(
					OpenapiErrorCode.GOODS_CATEGORY_DELETE_ERROR, ex.getMessage());
		}
		return Map.of("status", Boolean.TRUE);
	}

	private static Long parseCategoryIdForDelete(String categoryIdRaw) {
		String trimmed = categoryIdRaw.trim();
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
