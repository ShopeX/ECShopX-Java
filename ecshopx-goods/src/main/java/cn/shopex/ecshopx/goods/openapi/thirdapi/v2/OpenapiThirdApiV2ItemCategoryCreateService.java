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
import cn.shopex.ecshopx.goods.service.ItemsCategoryClassificationService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2ItemCategoryCreateService {

	private final ItemsCategoryClassificationService itemsCategoryClassificationService;

	public OpenapiThirdApiV2ItemCategoryCreateService(
			ItemsCategoryClassificationService itemsCategoryClassificationService) {
		this.itemsCategoryClassificationService = itemsCategoryClassificationService;
	}

	public Map<String, Object> executeOpenapiCreateItemCategory(
			long companyId,
			String categoryNameRaw,
			String sortRaw,
			String imageUrlRaw,
			String parentIdRaw) {
		try {
			validateCategoryName(categoryNameRaw);
			validateParentIdRequired(parentIdRaw);
			Map<String, Object> params = buildCreateClassificationParams(
					categoryNameRaw, sortRaw, imageUrlRaw, parentIdRaw);
			itemsCategoryClassificationService.createClassification(companyId, 0L, params, "zh-CN");
		} catch (OpenapiItemsV2FailException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new OpenapiItemsV2FailException(
					OpenapiErrorCode.GOODS_CATEGORY_ERROR, ex.getMessage());
		}
		return Map.of("status", Boolean.TRUE);
	}

	private void validateCategoryName(String categoryNameRaw) {
		if (categoryNameRaw == null || categoryNameRaw.isEmpty()) {
			throw new OpenapiItemsV2FailException(
					OpenapiErrorCode.SERVICE_MISSING_PARAMS, "分类名称必填");
		}
	}

	private void validateParentIdRequired(String parentIdRaw) {
		if (parentIdRaw == null || parentIdRaw.isEmpty()) {
			throw new OpenapiItemsV2FailException(
					OpenapiErrorCode.SERVICE_MISSING_PARAMS, "上级分类ID必填");
		}
	}

	private Map<String, Object> buildCreateClassificationParams(
			String categoryNameRaw, String sortRaw, String imageUrlRaw, String parentIdRaw) {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("category_name", categoryNameRaw);
		if (sortRaw != null) {
			params.put("sort", sortRaw);
		}
		if (imageUrlRaw != null) {
			params.put("image_url", imageUrlRaw);
		}
		long parentInt = phpIntval(parentIdRaw);
		if (parentInt != 0L) {
			params.put("parent_id", parentInt);
		}
		return params;
	}

	private static long phpIntval(String raw) {
		if (raw == null || raw.isEmpty()) {
			return 0L;
		}
		String s = raw.trim();
		if (s.isEmpty()) {
			return 0L;
		}
		int i = 0;
		while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '-')) {
			i++;
		}
		if (i == 0 || (i == 1 && s.charAt(0) == '-')) {
			return 0L;
		}
		try {
			return Long.parseLong(s.substring(0, i));
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
