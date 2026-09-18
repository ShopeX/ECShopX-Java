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
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseEmployeeCsvExportService;
import cn.shopex.ecshopx.employeepurchase.service.dto.EmployeeAdminExportQuery;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class EmployeePurchaseEmployeesExportFileJobTypeHandler implements ExportFileJobTypeHandler {

	private static final Logger log =
			LoggerFactory.getLogger(EmployeePurchaseEmployeesExportFileJobTypeHandler.class);

	private final EmployeePurchaseEmployeeCsvExportService csvExportService;

	public EmployeePurchaseEmployeesExportFileJobTypeHandler(EmployeePurchaseEmployeeCsvExportService csvExportService) {
		this.csvExportService = csvExportService;
	}

	@Override
	public String exportType() {
		return "employee_purchase_employees";
	}

	@Override
	public void handle(Map<String, Object> payload) {
		try {
			EmployeeAdminExportQuery query = parseAdminExportQuery(payload);
			long operatorId = extractOperatorId(payload);
			boolean datapassBlock = extractDatapassBlock(payload);
			csvExportService.runExport(query, operatorId, datapassBlock);
		} catch (RuntimeException e) {
			log.debug("employee purchase employees export job failed: {}", e.toString());
		}
	}

	private static EmployeeAdminExportQuery parseAdminExportQuery(Map<String, Object> payload) {
		long companyId = extractRequiredLong(payload, "company_id");
		Integer distributorId = extractOptionalInteger(payload, "distributor_id");
		String mobile = extractOptionalTrimmedString(payload, "mobile");
		String account = extractOptionalTrimmedString(payload, "account");
		String email = extractOptionalTrimmedString(payload, "email");
		String memberMobile = extractOptionalTrimmedString(payload, "member_mobile");
		Long enterpriseId = extractOptionalLong(payload, "enterprise_id");
		return new EmployeeAdminExportQuery(
				companyId, distributorId, mobile, account, email, memberMobile, enterpriseId);
	}

	private static long extractRequiredLong(Map<String, Object> payload, String key) {
		Object raw = payload.get(key);
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}

	private static long extractOperatorId(Map<String, Object> payload) {
		return extractRequiredLong(payload, "operator_id");
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
		if (!StringUtils.hasText(s)) {
			return null;
		}
		return Integer.parseInt(s);
	}

	private static Long extractOptionalLong(Map<String, Object> payload, String key) {
		Object raw = payload.get(key);
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		return Long.parseLong(s);
	}

	private static String extractOptionalTrimmedString(Map<String, Object> payload, String key) {
		Object raw = payload.get(key);
		if (raw == null) {
			return null;
		}
		String s = String.valueOf(raw).trim();
		return StringUtils.hasText(s) ? s : null;
	}

	private static boolean extractDatapassBlock(Map<String, Object> payload) {
		Object raw = payload.get("datapass_block");
		if (raw == null) {
			return false;
		}
		if (raw instanceof Number n) {
			return n.intValue() != 0;
		}
		return Integer.parseInt(String.valueOf(raw).trim()) != 0;
	}
}
