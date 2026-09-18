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

package cn.shopex.ecshopx.community.service.front;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CommunityFrontOrderListFilterBuilder {

	public Map<String, Object> build(long companyId, long userId, HttpServletRequest request, boolean chiefBranch) {
		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);

		String tsBegin = request.getParameter("time_start_begin");
		if (StringUtils.hasText(tsBegin)) {
			filter.put("create_time|gte", tsBegin.trim());
			filter.put("create_time|lte", request.getParameter("time_start_end"));
		}

		String mobile = request.getParameter("mobile");
		if (StringUtils.hasText(mobile)) {
			filter.put("mobile", mobile.trim());
		}

		String orderType = request.getParameter("order_type");
		filter.put("order_type", StringUtils.hasText(orderType) ? orderType.trim() : "normal");

		String orderClass = request.getParameter("order_class");
		filter.put("order_class", StringUtils.hasText(orderClass) ? orderClass.trim() : "community");

		Integer status = parseOptionalInt(request.getParameter("status"));
		if (status != null) {
			applyStatusSwitch(filter, status, request);
		}

		if (!chiefBranch) {
			filter.put("user_id", userId);
			String activityId = request.getParameter("activity_id");
			if (StringUtils.hasText(activityId)) {
				long act = longLoose(activityId.trim());
				if (act > 0L) {
					filter.put("act_id", List.of(act));
				}
			}
		}

		return filter;
	}

	private static void applyStatusSwitch(Map<String, Object> filter, int status, HttpServletRequest request) {
		switch (status) {
			case 1 -> {
				filter.put("order_status|in", List.of("PAYED", "WAIT_BUYER_CONFIRM"));
				filter.put("ziti_status", "NOTZITI");
			}
			case 3 -> {
				filter.put("order_status", "DONE");
				filter.put("delivery_status", "DONE");
				filter.put("ziti_status", "DONE");
			}
			case 4 -> {
				filter.put("order_status", "PAYED");
				filter.put("ziti_status", "PENDING");
			}
			case 5 -> {
				filter.put("order_status", "NOTPAY");
				filter.put("auto_cancel_time|gt", System.currentTimeMillis() / 1000L);
			}
			case 6 -> {
				filter.put("order_status", "PAYED");
				filter.put("ziti_status", "NOTZITI");
			}
			case 7 -> {
				filter.put("order_status", "DONE");
				String isRate = request.getParameter("is_rate");
				filter.put("is_rate", StringUtils.hasText(isRate) ? isRate.trim() : "0");
			}
			default -> {
				// no extra conditions
			}
		}
	}

	private static Integer parseOptionalInt(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long longLoose(String raw) {
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
