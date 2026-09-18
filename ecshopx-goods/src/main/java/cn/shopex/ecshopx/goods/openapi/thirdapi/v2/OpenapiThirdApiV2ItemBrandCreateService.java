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
import cn.shopex.ecshopx.goods.service.ItemsAttributesCreateService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2ItemBrandCreateService {

	private final ItemsAttributesCreateService itemsAttributesCreateService;

	public OpenapiThirdApiV2ItemBrandCreateService(ItemsAttributesCreateService itemsAttributesCreateService) {
		this.itemsAttributesCreateService = itemsAttributesCreateService;
	}

	public Map<String, Object> executeOpenapiCreateItemBrand(
			long companyId,
			String brandNameRaw,
			String imageUrlRaw) {
		try {
			validateBrandName(brandNameRaw);
			Map<String, Object> input = buildCreateAttrInput(brandNameRaw, imageUrlRaw);
			itemsAttributesCreateService.createAttr(companyId, input, "zh-CN");
		} catch (OpenapiItemsV2FailException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new OpenapiItemsV2FailException(
					OpenapiErrorCode.GOODS_BRAND_ERROR, ex.getMessage());
		}
		return Map.of("status", Boolean.TRUE);
	}

	private void validateBrandName(String brandNameRaw) {
		if (brandNameRaw == null || brandNameRaw.isEmpty()) {
			throw new OpenapiItemsV2FailException(
					OpenapiErrorCode.SERVICE_MISSING_PARAMS, "品牌名称必填");
		}
	}

	private Map<String, Object> buildCreateAttrInput(String brandNameRaw, String imageUrlRaw) {
		Map<String, Object> input = new LinkedHashMap<>();
		input.put("attribute_type", "brand");
		input.put("attribute_name", brandNameRaw);
		if (imageUrlRaw != null) {
			input.put("image_url", imageUrlRaw);
		}
		return input;
	}
}
