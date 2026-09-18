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

package cn.shopex.ecshopx.selfservice.dispatch;

import cn.shopex.ecshopx.common.dispatch.ExportFileJobTypeHandler;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.selfservice.service.export.RegistrationRecordCsvExportService;
import cn.shopex.ecshopx.selfservice.service.export.RegistrationRecordExportJobContext;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RegistrationRecordExportFileJobTypeHandler implements ExportFileJobTypeHandler {

	private static final Logger log = LoggerFactory.getLogger(RegistrationRecordExportFileJobTypeHandler.class);

	private final RegistrationRecordCsvExportService csvExportService;

	public RegistrationRecordExportFileJobTypeHandler(RegistrationRecordCsvExportService csvExportService) {
		this.csvExportService = csvExportService;
	}

	@Override
	public String exportType() {
		return "selform_registration_record";
	}

	@Override
	public void handle(Map<String, Object> payload) {
		try {
			RegistrationRecordExportJobContext ctx = parseContext(payload);
			csvExportService.runExport(ctx);
		} catch (RuntimeException e) {
			log.debug("registration record export job failed: {}", e.toString());
		}
	}

	private static RegistrationRecordExportJobContext parseContext(Map<String, Object> payload) {
		long companyId = extractRequiredLong(payload, "company_id");
		long operatorId = extractRequiredLong(payload, "operator_id");
		long supplierId = extractRequiredLong(payload, "supplier_id");
		long activityId = extractRequiredLong(payload, "activity_id");
		String mobilePlain = extractOptionalTrimmedString(payload, "mobile");
		Integer startCreatedInclusive = extractOptionalInteger(payload, "start_time");
		Integer endCreatedInclusive = extractOptionalInteger(payload, "end_time");
		String datapassBlock = extractDatapassBlock(payload);
		String localeTag = extractRequiredString(payload, "locale_language_tag");
		Locale locale = Locale.forLanguageTag(localeTag);
		return new RegistrationRecordExportJobContext(
				companyId,
				operatorId,
				supplierId,
				activityId,
				mobilePlain,
				startCreatedInclusive,
				endCreatedInclusive,
				datapassBlock,
				locale);
	}

	private static long extractRequiredLong(Map<String, Object> payload, String key) {
		Object raw = payload.get(key);
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}

	private static String extractRequiredString(Map<String, Object> payload, String key) {
		Object raw = payload.get(key);
		if (raw == null) {
			throw new BadRequestException("missing payload field: " + key);
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException("empty payload field: " + key);
		}
		return s;
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

	private static String extractOptionalTrimmedString(Map<String, Object> payload, String key) {
		Object raw = payload.get(key);
		if (raw == null) {
			return null;
		}
		String s = String.valueOf(raw).trim();
		return StringUtils.hasText(s) ? s : null;
	}

	private static String extractDatapassBlock(Map<String, Object> payload) {
		Object raw = payload.get("datapass_block");
		if (raw == null) {
			return "";
		}
		return String.valueOf(raw);
	}
}
