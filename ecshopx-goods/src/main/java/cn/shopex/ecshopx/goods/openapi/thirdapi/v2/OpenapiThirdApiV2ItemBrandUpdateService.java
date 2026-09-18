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
import cn.shopex.ecshopx.goods.service.ItemsAttributesUpdateService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2ItemBrandUpdateService {

	private final ItemsAttributesUpdateService itemsAttributesUpdateService;

	public OpenapiThirdApiV2ItemBrandUpdateService(
			ItemsAttributesUpdateService itemsAttributesUpdateService) {
		this.itemsAttributesUpdateService = itemsAttributesUpdateService;
	}

	public Map<String, Object> executeOpenapiUpdateItemBrand(
			long companyId,
			String brandIdRaw,
			String brandNameRaw,
			String imageUrlRaw) {
		try {
			validateBrandId(brandIdRaw);
			validateBrandName(brandNameRaw);
			long attributeId = parseBrandIdForUpdate(brandIdRaw);
			Map<String, Object> input = buildUpdateAttrInput(brandNameRaw, imageUrlRaw);
			itemsAttributesUpdateService.updateAttr(companyId, attributeId, input, "zh-CN");
		} catch (OpenapiItemsV2FailException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new OpenapiItemsV2FailException(
					OpenapiErrorCode.GOODS_BRAND_ERROR, ex.getMessage());
		}
		return Map.of("status", Boolean.TRUE);
	}

	private void validateBrandId(String brandIdRaw) {
		if (brandIdRaw == null || brandIdRaw.isEmpty()) {
			throw new OpenapiItemsV2FailException(
					OpenapiErrorCode.SERVICE_MISSING_PARAMS, "品牌ID必填");
		}
	}

	private void validateBrandName(String brandNameRaw) {
		if (brandNameRaw == null || brandNameRaw.isEmpty()) {
			throw new OpenapiItemsV2FailException(
					OpenapiErrorCode.SERVICE_MISSING_PARAMS, "品牌名称必填");
		}
	}

	private long parseBrandIdForUpdate(String brandIdRaw) {
		String trimmed = brandIdRaw.trim();
		if (trimmed.isEmpty()) {
			throw brandNotFound();
		}
		try {
			return Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw brandNotFound();
		}
	}

	private static OpenapiItemsV2FailException brandNotFound() {
		return new OpenapiItemsV2FailException(
				OpenapiErrorCode.GOODS_BRAND_ERROR, "更新的数据不存在");
	}

	private Map<String, Object> buildUpdateAttrInput(String brandNameRaw, String imageUrlRaw) {
		Map<String, Object> input = new LinkedHashMap<>();
		input.put("attribute_name", brandNameRaw);
		if (imageUrlRaw != null) {
			input.put("image_url", imageUrlRaw);
		}
		return input;
	}
}
