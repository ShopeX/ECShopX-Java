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

public final class OpenapiTagRelationPushParams {

	private OpenapiTagRelationPushParams() {}

	public static String mergeAction(String actionParam, Map<String, Object> body) {
		return OpenapiRequestParams.mergeString(actionParam, body, "action");
	}

	public static String mergeUserId(String userIdParam, Map<String, Object> body) {
		return OpenapiRequestParams.mergeString(userIdParam, body, "user_id");
	}

	public static String mergeMobile(String mobileParam, Map<String, Object> body) {
		return OpenapiRequestParams.mergeString(mobileParam, body, "mobile");
	}

	public static boolean isRelationsEmptyOrNotArray(Object raw) {
		if (raw == null) {
			return true;
		}
		if (!(raw instanceof List<?> list)) {
			return true;
		}
		return list.isEmpty();
	}

	public static boolean isClearMode(String action, Object relationsRaw) {
		return isRelationsEmptyOrNotArray(relationsRaw) && action != null && "clear".equals(action);
	}

	public static boolean isClearModeUserIdMissing(String userIdParam, Map<String, Object> body) {
		return OpenapiMemberQueryParams.isPhpEmpty(userIdParam, body, "user_id");
	}

	@SuppressWarnings("unchecked")
	public static List<Map<String, Object>> resolveRelations(Map<String, Object> body) {
		Object raw = body != null ? body.get("relations") : null;
		List<?> list = (List<?>) raw;
		List<Map<String, Object>> result = new ArrayList<>(list.size());
		for (Object element : list) {
			if (element instanceof Map<?, ?> map) {
				result.add((Map<String, Object>) map);
			}
		}
		return result;
	}
}
