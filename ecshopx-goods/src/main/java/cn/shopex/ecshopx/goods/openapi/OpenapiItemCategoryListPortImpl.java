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

import cn.shopex.ecshopx.common.openapi.OpenapiItemCategoryListPort;
import cn.shopex.ecshopx.goods.openapi.thirdapi.v2.OpenapiThirdApiV2ItemCategoryListService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenapiItemCategoryListPortImpl implements OpenapiItemCategoryListPort {

	private final OpenapiThirdApiV2ItemCategoryListService itemCategoryListService;

	public OpenapiItemCategoryListPortImpl(
			OpenapiThirdApiV2ItemCategoryListService itemCategoryListService) {
		this.itemCategoryListService = itemCategoryListService;
	}

	@Override
	public List<Map<String, Object>> getItemCategoryList(long companyId) {
		return itemCategoryListService.executeOpenapiGetItemCategoryList(companyId);
	}

	@Override
	public List<Map<String, Object>> getItemMainCategoryList(long companyId) {
		return itemCategoryListService.executeOpenapiGetItemMainCategoryList(companyId);
	}
}
