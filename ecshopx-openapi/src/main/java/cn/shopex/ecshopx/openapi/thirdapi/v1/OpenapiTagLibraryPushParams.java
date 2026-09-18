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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class OpenapiTagLibraryPushParams {

	private OpenapiTagLibraryPushParams() {}

	public static boolean isInvalidTagLibrary(Object raw) {
		if (raw == null) {
			return true;
		}
		if (!(raw instanceof List<?> list)) {
			return true;
		}
		if (list.isEmpty()) {
			return true;
		}
		for (Object element : list) {
			if (!(element instanceof Map<?, ?>)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public static List<Map<String, Object>> resolveTagLibrary(Map<String, Object> body) {
		Object raw = body != null ? body.get("tag_library") : null;
		List<?> list = (List<?>) raw;
		List<Map<String, Object>> result = new ArrayList<>(list.size());
		for (Object element : list) {
			result.add((Map<String, Object>) element);
		}
		return result;
	}
}
