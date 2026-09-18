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

package cn.shopex.ecshopx.members.dispatch;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BatchActionMembersJobPayloadSupport {

	private BatchActionMembersJobPayloadSupport() {
	}

	public static Map<String, Object> toPayload(
			long companyId,
			Map<String, Object> operatorParams,
			String actionType,
			Map<String, Object> chunkFilter,
			int page,
			int pageSize) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("action_type", actionType);
		payload.put("params", operatorParams == null ? new LinkedHashMap<>() : new LinkedHashMap<>(operatorParams));
		payload.put("filter", chunkFilter == null ? new LinkedHashMap<>() : new LinkedHashMap<>(chunkFilter));
		payload.put("page", page);
		payload.put("page_size", pageSize);
		payload.put("is_queue", Boolean.TRUE);
		return payload;
	}
}
