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

package cn.shopex.ecshopx.selfservice.service.export;

import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RegistrationRecordExportOrchestratorService {

	private static final String MSG_PLEASE_SELECT_ACTIVITY =
			"selfservice.registration_record.export_please_select_activity";
	private static final String MSG_NO_DATA = "selfservice.registration_record.export_error_no_data";
	private static final String MSG_MAX = "selfservice.registration_record.export_error_max_15000_records";

	private final RegistrationRecordExportFilterSupport registrationRecordExportFilterSupport;
	private final RegistrationRecordExportDispatchPublisher registrationRecordExportDispatchPublisher;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final MessageSource messageSource;
	private final OperatorsQueryService operatorsQueryService;

	public RegistrationRecordExportOrchestratorService(
			RegistrationRecordExportFilterSupport registrationRecordExportFilterSupport,
			RegistrationRecordExportDispatchPublisher registrationRecordExportDispatchPublisher,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			MessageSource messageSource,
			OperatorsQueryService operatorsQueryService) {
		this.registrationRecordExportFilterSupport = registrationRecordExportFilterSupport;
		this.registrationRecordExportDispatchPublisher = registrationRecordExportDispatchPublisher;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.messageSource = messageSource;
		this.operatorsQueryService = operatorsQueryService;
	}

	public void exportRegistrationRecord(
			long companyId,
			long operatorId,
			long activityId,
			String mobile,
			String startTimeRaw,
			String endTimeRaw,
			String datapassBlock,
			Locale locale) {
		if (activityId <= 0L) {
			throw new ResourceException(messageSource.getMessage(MSG_PLEASE_SELECT_ACTIVITY, null, locale));
		}
		Integer startCreatedInclusive = null;
		Integer endCreatedInclusive = null;
		if (StringUtils.hasText(startTimeRaw) && StringUtils.hasText(endTimeRaw)) {
			startCreatedInclusive = parseEpochSecondsStrict(startTimeRaw.trim());
			endCreatedInclusive = parseEpochSecondsStrict(endTimeRaw.trim());
		}
		String mobilePlain = StringUtils.hasText(mobile) ? mobile.trim() : null;
		long count = registrationRecordExportFilterSupport.countForExport(
				companyId,
				activityId,
				mobilePlain,
				startCreatedInclusive,
				endCreatedInclusive,
				sensitiveFieldEncryptor);
		if (count <= 0L) {
			throw new ResourceException(messageSource.getMessage(MSG_NO_DATA, null, locale));
		}
		if (count > 15000L) {
			throw new ResourceException(messageSource.getMessage(MSG_MAX, null, locale));
		}
		long supplierId = resolveSupplierId(companyId, operatorId);
		RegistrationRecordExportJobContext ctx = new RegistrationRecordExportJobContext(
				companyId,
				operatorId,
				supplierId,
				activityId,
				mobilePlain,
				startCreatedInclusive,
				endCreatedInclusive,
				datapassBlock == null ? "" : datapassBlock,
				locale);
		registrationRecordExportDispatchPublisher.enqueue(ctx);
	}

	private long resolveSupplierId(long companyId, long operatorId) {
		long supplierId = 0L;
		if (operatorId > 0L) {
			Map<String, Object> filter = new LinkedHashMap<>();
			filter.put("company_id", companyId);
			filter.put("operator_id", operatorId);
			filter.put("operator_type", "supplier");
			Map<String, Object> info = operatorsQueryService.getInfo(filter);
			if (info != null && !info.isEmpty()) {
				supplierId = operatorId;
			}
		}
		return supplierId;
	}

	private int parseEpochSecondsStrict(String raw) {
		if (!raw.matches("^-?\\d+$")) {
			throw new BadRequestException("时间参数格式错误");
		}
		try {
			long v = Long.parseLong(raw);
			if (v > Integer.MAX_VALUE || v < Integer.MIN_VALUE) {
				throw new BadRequestException("时间参数格式错误");
			}
			return (int) v;
		} catch (NumberFormatException ex) {
			throw new BadRequestException("时间参数格式错误");
		}
	}
}
