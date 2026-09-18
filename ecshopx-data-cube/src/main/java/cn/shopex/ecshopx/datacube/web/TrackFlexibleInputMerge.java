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

package cn.shopex.ecshopx.datacube.web;

import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * Merges query string with JSON body for {@code request->all(...)}-style input on POST.
 */
public final class TrackFlexibleInputMerge {

	private TrackFlexibleInputMerge() {}

	public static Map<String, Object> merge(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		if (jsonLike) {
			request.getParameterMap().forEach((k, v) -> {
				if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
					out.put(k, v[0]);
				}
			});
			if (body != null) {
				out.putAll(body);
			}
		} else {
			if (body != null) {
				out.putAll(body);
			} else {
				out.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
			}
		}
		return out;
	}
}
