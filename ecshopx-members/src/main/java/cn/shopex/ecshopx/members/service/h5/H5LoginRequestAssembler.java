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

package cn.shopex.ecshopx.members.service.h5;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class H5LoginRequestAssembler {

	public Map<String, Object> merge(HttpServletRequest request, Map<String, Object> jsonBody) {
		Map<String, Object> result = new LinkedHashMap<>();
		Map<String, String[]> pm = request.getParameterMap();
		for (Map.Entry<String, String[]> e : pm.entrySet()) {
			String[] values = e.getValue();
			if (values != null && values.length > 0) {
				result.put(e.getKey(), values[0]);
			}
		}
		if (jsonBody != null) {
			result.putAll(jsonBody);
		}
		String origin = request.getHeader("Origin");
		if (origin != null) {
			result.put("origin", origin);
		}
		return result;
	}
}
