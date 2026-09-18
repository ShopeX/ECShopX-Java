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

package cn.shopex.ecshopx.openapi.thirdapi.v2.items;

import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OpenapiThirdApiV2NormalItemsEntityAddParams {

	private static final String[] WHITELIST = {
		"item_main_cat_id",
		"item_name",
		"brief",
		"sort",
		"item_unit",
		"item_category",
		"brand_id",
		"templates_id",
		"is_gift",
		"pics",
		"intro",
		"nospec",
		"is_show_specimg",
		"approve_status",
		"item_bn",
		"weight",
		"volume",
		"price",
		"market_price",
		"cost_price",
		"barcode",
		"store",
		"item_params",
		"spec_images",
		"spec_items"
	};

	private OpenapiThirdApiV2NormalItemsEntityAddParams() {}

	public static Map<String, Object> buildMergedParams(
			HttpServletRequest request, Map<String, Object> body) {
		Map<String, Object> queryMap = FlexibleHttpServletParameterMap.toObjectMap(request);
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		for (String key : WHITELIST) {
			if (body != null && body.containsKey(key)) {
				merged.put(key, body.get(key));
			} else if (queryMap.containsKey(key)) {
				merged.put(key, queryMap.get(key));
			}
		}
		return merged;
	}
}
