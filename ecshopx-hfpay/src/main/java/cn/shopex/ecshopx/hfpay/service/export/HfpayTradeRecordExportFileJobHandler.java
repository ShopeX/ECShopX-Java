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
public class HfpayTradeRecordExportFileJobHandler {

	private final HfpayTradeRecordCsvExportService csvExportService;
	private final OperatorsQueryService operatorsQueryService;

	public HfpayTradeRecordExportFileJobHandler(
			HfpayTradeRecordCsvExportService csvExportService,
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

		String startDateTime = stringFromFilter(filter, "start_date");
		String endDateTime = stringFromFilter(filter, "end_date");
		Integer distributorId = extractDistributorId(filter.get("distributor_id"));

		HfpayTradeRecordExportContext ctx =
				new HfpayTradeRecordExportContext(
						companyId, operatorId, supplierId, startDateTime, endDateTime, distributorId);
		csvExportService.runExport(ctx);
	}

	private static String stringFromFilter(LinkedHashMap<String, Object> filter, String key) {
		Object raw = filter.get(key);
		return raw == null ? "" : String.valueOf(raw).trim();
	}

	private static Integer extractDistributorId(Object raw) {
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
