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

package cn.shopex.ecshopx.form.service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Forces JWT company scope onto user-transcript list filters: strips client
 * {@code company_id} / {@code company_id|*} keys then sets {@code company_id}.
 */
public final class UserTranscriptListCompanyScope {

	private UserTranscriptListCompanyScope() {}

	public static Map<String, Object> apply(Map<String, Object> filters, long companyId) {
		Map<String, Object> scoped = new LinkedHashMap<>();
		if (filters != null) {
			for (Map.Entry<String, Object> e : filters.entrySet()) {
				String key = e.getKey();
				if (key == null) {
					continue;
				}
				if ("company_id".equals(key) || key.startsWith("company_id|")) {
					continue;
				}
				scoped.put(key, e.getValue());
			}
		}
		scoped.put("company_id", companyId);
		return scoped;
	}
}
