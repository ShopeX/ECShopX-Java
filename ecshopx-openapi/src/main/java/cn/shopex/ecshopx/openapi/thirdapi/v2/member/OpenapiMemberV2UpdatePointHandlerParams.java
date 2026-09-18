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

import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OpenapiMemberV2UpdatePointHandlerParams {

	private OpenapiMemberV2UpdatePointHandlerParams() {}

	public static Map<String, Object> mergeAll(
			Map<String, Object> body,
			String mobileParam,
			String increasePointParam,
			String decreasePointParam,
			String recordParam,
			String externalIdParam) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		putMerge(out, body, "mobile", mobileParam);
		putMerge(out, body, "increase_point", increasePointParam);
		putMerge(out, body, "decrease_point", decreasePointParam);
		putMerge(out, body, "record", recordParam);
		putMerge(out, body, "external_id", externalIdParam);
		return out;
	}

	private static void putMerge(
			LinkedHashMap<String, Object> out, Map<String, Object> body, String key, String queryParam) {
		if (body != null && body.containsKey(key)) {
			out.put(key, body.get(key));
		} else {
			String merged = OpenapiRequestParams.mergeString(queryParam, body, key);
			if (merged != null) {
				out.put(key, merged);
			}
		}
	}
}
