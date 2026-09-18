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

package cn.shopex.ecshopx.employeepurchase.dispatch;

import cn.shopex.ecshopx.common.dispatch.ExportFileJobTypeHandler;
import cn.shopex.ecshopx.employeepurchase.service.dto.ActivityItemsExportQuery;
import cn.shopex.ecshopx.employeepurchase.service.export.EmployeePurchaseActivityItemsCsvExportService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmployeePurchaseActivityItemsExportFileJobTypeHandler implements ExportFileJobTypeHandler {

	private static final Logger log =
			LoggerFactory.getLogger(EmployeePurchaseActivityItemsExportFileJobTypeHandler.class);

	private final EmployeePurchaseActivityItemsCsvExportService csvExportService;

	public EmployeePurchaseActivityItemsExportFileJobTypeHandler(
			EmployeePurchaseActivityItemsCsvExportService csvExportService) {
		this.csvExportService = csvExportService;
	}

	@Override
	public String exportType() {
		return "employee_purchase_activity_items";
	}

	@Override
	public void handle(Map<String, Object> payload) {
		try {
			csvExportService.runExport(parseQuery(payload));
		} catch (RuntimeException e) {
			log.debug("employee purchase activity items export job failed: {}", e.toString());
		}
	}

	@SuppressWarnings("unchecked")
	static ActivityItemsExportQuery parseQuery(Map<String, Object> payload) {
		long companyId = extractRequiredLong(payload, "company_id");
		long activityId = extractRequiredLong(payload, "activity_id");
		long operatorId = extractRequiredLong(payload, "operator_id");
		long distributorId = extractOptionalLong(payload, "distributor_id", 0L);
		Integer distributorScope = extractOptionalInteger(payload, "distributor_scope");
		Long mainCatId = extractOptionalLongObject(payload, "main_cat_id");
		Long category = extractOptionalLongObject(payload, "category");
		String itemName = extractOptionalString(payload, "item_name");
		String itemBn = extractOptionalString(payload, "item_bn");
		Integer shelfStatus = extractOptionalInteger(payload, "shelf_status");
		List<Long> itemIds = null;
		Object rawItemIds = payload.get("item_id");
		if (rawItemIds instanceof List<?> list) {
			itemIds = new ArrayList<>();
			for (Object o : list) {
				long v = toLong(o);
				if (v > 0L) {
					itemIds.add(v);
				}
			}
		}
		return new ActivityItemsExportQuery(
				companyId,
				activityId,
				distributorId,
				operatorId,
				distributorScope,
				mainCatId,
				category,
				itemName,
				itemBn,
				shelfStatus,
				itemIds);
	}

	private static long extractRequiredLong(Map<String, Object> payload, String key) {
		Object raw = payload.get(key);
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}

	private static long extractOptionalLong(Map<String, Object> payload, String key, long defaultVal) {
		Object raw = payload.get(key);
		if (raw == null) {
			return defaultVal;
		}
		return toLong(raw);
	}

	private static Long extractOptionalLongObject(Map<String, Object> payload, String key) {
		Object raw = payload.get(key);
		if (raw == null) {
			return null;
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return null;
		}
		long v = toLong(raw);
		return v > 0L ? v : null;
	}

	private static Integer extractOptionalInteger(Map<String, Object> payload, String key) {
		Object raw = payload.get(key);
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return null;
		}
		return Integer.parseInt(s);
	}

	private static String extractOptionalString(Map<String, Object> payload, String key) {
		Object raw = payload.get(key);
		if (raw == null) {
			return null;
		}
		String s = String.valueOf(raw).trim();
		return s.isEmpty() ? null : s;
	}

	private static long toLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}
}
