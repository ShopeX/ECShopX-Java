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

package cn.shopex.ecshopx.salesperson.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.MarketingCenterTasksCompletePort;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SalespersonTaskJobHandler implements DispatchHandler {

	private final MarketingCenterTasksCompletePort marketingCenterTasksCompletePort;

	public SalespersonTaskJobHandler(MarketingCenterTasksCompletePort marketingCenterTasksCompletePort) {
		this.marketingCenterTasksCompletePort = marketingCenterTasksCompletePort;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long companyId = extractLong(payload, "company_id");
		long subtaskId = extractLong(payload, "subtask_id");
		String storeBn = extractString(payload, "store_bn");
		String employeeNumber = extractString(payload, "employee_number");
		String userId = extractString(payload, "user_id");
		if (companyId <= 0L
				|| subtaskId <= 0L
				|| !StringUtils.hasText(storeBn)
				|| !StringUtils.hasText(employeeNumber)
				|| !StringUtils.hasText(userId)) {
			return;
		}
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", companyId);
		params.put("subtask_id", subtaskId);
		params.put("store_bn", storeBn);
		params.put("employee_number", employeeNumber);
		params.put("user_id", userId);
		Object rawItemId = payload.get("item_id");
		if (rawItemId != null) {
			long itemId = toLong(rawItemId);
			if (itemId > 0L) {
				params.put("item_id", itemId);
			}
		}
		marketingCenterTasksCompletePort.completeTasks(companyId, params);
	}

	private static long extractLong(Map<String, Object> payload, String key) {
		Object raw = payload.get(key);
		return toLong(raw);
	}

	private static long toLong(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException ex) {
			return 0L;
		}
	}

	private static String extractString(Map<String, Object> payload, String key) {
		Object raw = payload.get(key);
		if (raw == null) {
			return "";
		}
		return String.valueOf(raw).trim();
	}
}
