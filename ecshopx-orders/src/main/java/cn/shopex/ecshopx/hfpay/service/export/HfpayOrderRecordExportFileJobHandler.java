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

import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class HfpayOrderRecordExportFileJobHandler {

	private final HfpayOrderRecordCsvExportService csvExportService;
	private final OperatorsQueryService operatorsQueryService;

	public HfpayOrderRecordExportFileJobHandler(
			HfpayOrderRecordCsvExportService csvExportService,
			OperatorsQueryService operatorsQueryService) {
		this.csvExportService = csvExportService;
		this.operatorsQueryService = operatorsQueryService;
	}

	public void run(long companyId, long operatorId, LinkedHashMap<String, Object> filter) {
		long supplierId = 0L;
		if (operatorId > 0L) {
			Map<String, Object> opFilter = new HashMap<>();
			opFilter.put("company_id", companyId);
			opFilter.put("operator_id", operatorId);
			opFilter.put("operator_type", "supplier");
			Map<String, Object> row = operatorsQueryService.getInfo(opFilter);
			supplierId = (row != null && !row.isEmpty()) ? operatorId : 0L;
		}

		long startEpoch = longFromFilter(filter, "start_date");
		long endEpoch = longFromFilter(filter, "end_date");
		Long distributorId = extractDistributorId(filter.get("distributor_id"));
		String orderId = stringFromFilterNullable(filter, "order_id");
		String appPayType = stringFromFilterNullable(filter, "app_pay_type");
		Integer profitsharingStatus = extractProfitsharing(filter.get("profitsharing_status"));
		String orderStatus = stringFromFilterNullable(filter, "order_status");

		HfpayOrderRecordExportContext ctx =
				new HfpayOrderRecordExportContext(
						companyId,
						operatorId,
						supplierId,
						startEpoch,
						endEpoch,
						distributorId,
						orderId,
						appPayType,
						profitsharingStatus,
						orderStatus);
		csvExportService.runExport(ctx);
	}

	private static long longFromFilter(LinkedHashMap<String, Object> filter, String key) {
		Object raw = filter.get(key);
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}

	private static String stringFromFilterNullable(LinkedHashMap<String, Object> filter, String key) {
		Object raw = filter.get(key);
		if (raw == null) {
			return null;
		}
		String s = String.valueOf(raw).trim();
		return s.isEmpty() ? null : s;
	}

	private static Long extractDistributorId(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v != 0L ? v : null;
		}
		try {
			long v = Long.parseLong(String.valueOf(raw).trim());
			return v != 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Integer extractProfitsharing(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			int v = n.intValue();
			return v != 0 ? v : null;
		}
		try {
			int v = Integer.parseInt(String.valueOf(raw).trim());
			return v != 0 ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
