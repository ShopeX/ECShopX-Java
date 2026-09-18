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

package cn.shopex.ecshopx.companys.service.setting;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Picks the inner WeChat shop display settings map for a language bucket key from the Redis
 * payload, or the full map when the payload is already flat (no per-language wrapper).
 */
public final class WxShopsSettingLangSliceHelper {

	private WxShopsSettingLangSliceHelper() {}

	public static Map<String, Object> innerMapForLang(Map<String, Object> loaded, String lang) {
		if (loaded == null || loaded.isEmpty()) {
			return Collections.emptyMap();
		}
		if (lang == null || lang.isBlank()) {
			return Collections.emptyMap();
		}
		Object v = loaded.get(lang);
		if (v instanceof Map<?, ?> m) {
			Map<String, Object> out = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				if (e.getKey() != null) {
					out.put(String.valueOf(e.getKey()), e.getValue());
				}
			}
			return out;
		}
		for (String key : new String[] {"intro", "logo", "brand_name"}) {
			if (!loaded.containsKey(key)) {
				continue;
			}
			Object val = loaded.get(key);
			if (!(val instanceof Map<?, ?>)) {
				return new LinkedHashMap<>(loaded);
			}
		}
		return Collections.emptyMap();
	}
}
