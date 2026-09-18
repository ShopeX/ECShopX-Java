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

package cn.shopex.ecshopx.kaquan.service.discount;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DiscountCardConsumeExCardRequestMergeService {

	public Map<String, Object> merge(HttpServletRequest request, Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		Map<String, String[]> pm = request.getParameterMap();
		for (Map.Entry<String, String[]> e : pm.entrySet()) {
			String key = e.getKey();
			String[] vals = e.getValue();
			if (vals == null || vals.length == 0) {
				continue;
			}
			merged.put(key, vals[0]);
		}
		if (body != null && !body.isEmpty()) {
			for (Map.Entry<String, Object> ent : body.entrySet()) {
				if (ent.getKey() != null && ent.getValue() != null) {
					merged.put(ent.getKey(), coerceToString(ent.getValue()));
				}
			}
		}
		return merged;
	}

	private static String coerceToString(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof String s) {
			return s;
		}
		return v.toString();
	}
}
