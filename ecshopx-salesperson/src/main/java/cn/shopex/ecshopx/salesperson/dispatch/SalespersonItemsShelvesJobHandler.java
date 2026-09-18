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
import cn.shopex.ecshopx.salesperson.service.SalespersonItemsShelvesJobService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SalespersonItemsShelvesJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(SalespersonItemsShelvesJobHandler.class);

	private final SalespersonItemsShelvesJobService salespersonItemsShelvesJobService;

	public SalespersonItemsShelvesJobHandler(SalespersonItemsShelvesJobService salespersonItemsShelvesJobService) {
		this.salespersonItemsShelvesJobService = salespersonItemsShelvesJobService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long companyId = extractLong(payload, "companyId");
		long activityId = extractLong(payload, "activityId");
		String activityType = extractString(payload, "activityType");
		log.info(
				"SalespersonItemsShelvesJob companyId={} activityId={} activityType={}",
				companyId,
				activityId,
				activityType);
		if (companyId <= 0L || activityId <= 0L || !StringUtils.hasText(activityType)) {
			return;
		}
		salespersonItemsShelvesJobService.execute(companyId, activityId, activityType);
	}

	private static long extractLong(Map<String, Object> payload, String key) {
		Object raw = payload.get(key);
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
