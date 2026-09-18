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
import cn.shopex.ecshopx.employeepurchase.service.dto.ActivityAdminExportQuery;
import cn.shopex.ecshopx.employeepurchase.service.export.EmployeePurchaseActivityQrcodeCsvExportService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmployeePurchaseActivityQrcodeExportFileJobTypeHandler implements ExportFileJobTypeHandler {

	private static final Logger log =
			LoggerFactory.getLogger(EmployeePurchaseActivityQrcodeExportFileJobTypeHandler.class);

	private final EmployeePurchaseActivityQrcodeCsvExportService csvExportService;

	public EmployeePurchaseActivityQrcodeExportFileJobTypeHandler(
			EmployeePurchaseActivityQrcodeCsvExportService csvExportService) {
		this.csvExportService = csvExportService;
	}

	@Override
	public String exportType() {
		return "employee_purchase_activity_qrcode";
	}

	@Override
	public void handle(Map<String, Object> payload) {
		try {
			csvExportService.runExport(parseQuery(payload));
		} catch (RuntimeException e) {
			log.debug("employee purchase activity qrcode export job failed: {}", e.toString());
		}
	}

	private static ActivityAdminExportQuery parseQuery(Map<String, Object> payload) {
		long companyId = extractRequiredLong(payload, "company_id");
		long activityId = extractRequiredLong(payload, "activity_id");
		long operatorId = extractRequiredLong(payload, "operator_id");
		Integer distributorId = extractOptionalInteger(payload, "distributor_id");
		return new ActivityAdminExportQuery(companyId, activityId, distributorId, operatorId);
	}

	private static long extractRequiredLong(Map<String, Object> payload, String key) {
		Object raw = payload.get(key);
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
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
}
