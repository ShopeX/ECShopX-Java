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

package cn.shopex.ecshopx.openapi.thirdapi.v1;

import java.util.LinkedHashMap;
import java.util.Map;

public final class OpenapiImageListParams {

	private OpenapiImageListParams() {}

	public record BuildResult(
			Map<String, Object> queryMap,
			boolean disabledFilterActive,
			Object disabledFilterValue,
			boolean imageNameFilterActive,
			String imageNameFilterValue,
			boolean imageCatIdFilterActive,
			Object imageCatIdFilterValue) {}

	public static BuildResult build(
			String storageParam,
			String imageNameParam,
			String disabledParam,
			String imageCatIdParam,
			String distributorIdParam,
			String pageParam,
			String pageSizeParam,
			Map<String, Object> body) {
		Map<String, Object> queryMap = new LinkedHashMap<>();

		queryMap.put("storage", OpenapiRequestParams.originalString(storageParam, body, "storage"));

		boolean imageNameFilterActive = false;
		String imageNameFilterValue = null;
		if (!OpenapiMemberQueryParams.isPhpEmpty(imageNameParam, body, "image_name")) {
			imageNameFilterActive = true;
			imageNameFilterValue = OpenapiRequestParams.originalString(imageNameParam, body, "image_name");
		}

		String disabledMerged = OpenapiRequestParams.mergeString(disabledParam, body, "disabled");
		boolean disabledFilterActive = OpenapiMemberQueryParams.isPhpTruthy(disabledMerged);
		Object disabledFilterValue =
				disabledFilterActive ? OpenapiRequestParams.originalString(disabledParam, body, "disabled") : null;

		boolean imageCatIdFilterActive = false;
		Object imageCatIdFilterValue = null;
		if (!OpenapiMemberQueryParams.isPhpEmpty(imageCatIdParam, body, "image_cat_id")) {
			imageCatIdFilterActive = true;
			imageCatIdFilterValue = OpenapiRequestParams.originalString(imageCatIdParam, body, "image_cat_id");
		}

		String distributorId = OpenapiRequestParams.mergeString(distributorIdParam, body, "distributor_id");
		queryMap.put("distributor_id", distributorId != null ? distributorId : "0");

		putIfPresent(queryMap, pageParam, body, "page");
		putIfPresent(queryMap, pageSizeParam, body, "pageSize");

		return new BuildResult(
				queryMap,
				disabledFilterActive,
				disabledFilterValue,
				imageNameFilterActive,
				imageNameFilterValue,
				imageCatIdFilterActive,
				imageCatIdFilterValue);
	}

	private static void putIfPresent(
			Map<String, Object> map, String queryParam, Map<String, Object> body, String key) {
		if (body != null && body.containsKey(key)) {
			map.put(key, body.get(key));
		} else if (queryParam != null) {
			map.put(key, queryParam);
		}
	}
}
