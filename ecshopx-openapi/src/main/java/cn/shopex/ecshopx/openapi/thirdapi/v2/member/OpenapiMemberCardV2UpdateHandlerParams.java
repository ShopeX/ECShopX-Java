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

package cn.shopex.ecshopx.openapi.thirdapi.v2.member;

import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiMemberQueryParams;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OpenapiMemberCardV2UpdateHandlerParams {

	private static final List<String> COLUMNS =
			List.of("brand_name", "logo_url", "title", "color", "background_pic_url");

	private OpenapiMemberCardV2UpdateHandlerParams() {}

	public static Map<String, Object> collectPresentFields(
			Map<String, Object> body,
			String brandNameParam,
			String logoUrlParam,
			String titleParam,
			String colorParam,
			String backgroundPicUrlParam) {
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		String[] queryParams = {
			brandNameParam, logoUrlParam, titleParam, colorParam, backgroundPicUrlParam
		};
		for (int i = 0; i < COLUMNS.size(); i++) {
			String column = COLUMNS.get(i);
			String queryParam = queryParams[i];
			if (OpenapiMemberQueryParams.isParamPresent(queryParam, body, column)) {
				params.put(column, toOpenapiString(queryParam, body, column));
			}
		}
		return params;
	}

	private static String toOpenapiString(String queryParam, Map<String, Object> body, String key) {
		if (body != null && body.containsKey(key)) {
			Object raw = body.get(key);
			return raw == null ? "" : String.valueOf(raw);
		}
		return queryParam;
	}
}
