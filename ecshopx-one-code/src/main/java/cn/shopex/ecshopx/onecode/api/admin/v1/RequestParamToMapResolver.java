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

package cn.shopex.ecshopx.onecode.api.admin.v1;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the merged query map used by one-code list GET handlers (batchs, things).
 */
public final class RequestParamToMapResolver {

	private RequestParamToMapResolver() {
	}

	public static Map<String, Object> toMergedMap(HttpServletRequest request) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("page", request.getParameter("page"));
		merged.put("pageSize", request.getParameter("pageSize"));
		if (request.getParameterMap().containsKey("thing_id")) {
			String[] arr = request.getParameterMap().get("thing_id");
			merged.put("thing_id", arr != null && arr.length > 0 ? arr[0] : "");
		}
		if (request.getParameterMap().containsKey("thing_name")) {
			String[] arr = request.getParameterMap().get("thing_name");
			merged.put("thing_name", arr != null && arr.length > 0 ? arr[0] : "");
		}
		return merged;
	}
}
