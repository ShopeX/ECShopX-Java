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

package cn.shopex.ecshopx.hfpay.service.export;

import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HfpayWithdrawRecordExportFileJobHandler {

	private final HfpayWithdrawRecordCsvExportService csvExportService;

	public HfpayWithdrawRecordExportFileJobHandler(HfpayWithdrawRecordCsvExportService csvExportService) {
		this.csvExportService = csvExportService;
	}

	public void run(long companyId, long operatorId, LinkedHashMap<String, Object> filter) {
		String startDateTime = stringFromFilter(filter, "start_date");
		String endDateTime = stringFromFilter(filter, "end_date");
		Long distributorId = extractDistributorId(filter.get("distributor_id"));
		String orderId = extractOrderId(filter.get("order_id"));

		boolean cashStatusInProgress = false;
		Integer cashStatusExact = null;
		Object csRaw = filter.get("cash_status");
		if (csRaw != null) {
			String cs = String.valueOf(csRaw).trim();
			if (StringUtils.hasText(cs)) {
				try {
					int parsed = Integer.parseInt(cs);
					if (parsed == 1) {
						cashStatusInProgress = true;
					} else {
						cashStatusExact = parsed;
					}
				} catch (NumberFormatException ignored) {
					// omit cash_status filter
				}
			}
		}

		HfpayWithdrawRecordExportContext ctx =
				new HfpayWithdrawRecordExportContext(
						companyId,
						operatorId,
						startDateTime,
						endDateTime,
						distributorId,
						orderId,
						cashStatusInProgress,
						cashStatusExact);
		csvExportService.runExport(ctx);
	}

	private static String stringFromFilter(LinkedHashMap<String, Object> filter, String key) {
		Object raw = filter.get(key);
		return raw == null ? "" : String.valueOf(raw).trim();
	}

	private static Long extractDistributorId(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : null;
		}
		try {
			long v = Long.parseLong(String.valueOf(raw).trim());
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String extractOrderId(Object raw) {
		if (raw == null) {
			return null;
		}
		String orderS = String.valueOf(raw).trim();
		try {
			long oid = Long.parseLong(orderS);
			if (oid > 0L) {
				return orderS;
			}
		} catch (NumberFormatException ignored) {
			// omit
		}
		return null;
	}
}
