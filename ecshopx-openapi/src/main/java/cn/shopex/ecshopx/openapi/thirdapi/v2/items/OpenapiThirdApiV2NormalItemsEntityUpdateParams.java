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

public final class OpenapiThirdApiV2NormalItemsEntityUpdateParams {

	private OpenapiThirdApiV2NormalItemsEntityUpdateParams() {}

	public static Map<String, Object> buildMergedParams(
			HttpServletRequest request, Map<String, Object> body) {
		Map<String, Object> merged =
				new LinkedHashMap<>(OpenapiThirdApiV2NormalItemsEntityAddParams.buildMergedParams(request, body));
		Map<String, Object> queryMap = FlexibleHttpServletParameterMap.toObjectMap(request);
		if (body != null && body.containsKey("old_item_bn")) {
			merged.put("old_item_bn", body.get("old_item_bn"));
		} else if (queryMap.containsKey("old_item_bn")) {
			merged.put("old_item_bn", queryMap.get("old_item_bn"));
		}
		return merged;
	}
}
