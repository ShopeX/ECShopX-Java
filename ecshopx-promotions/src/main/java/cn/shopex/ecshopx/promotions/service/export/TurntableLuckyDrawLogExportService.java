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

package cn.shopex.ecshopx.promotions.service.export;

import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.common.dispatch.LuckyDrawLogExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.promotions.service.TurntableConfigService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TurntableLuckyDrawLogExportService {

	private final TurntableConfigService turntableConfigService;
	private final OperatorsQueryService operatorsQueryService;
	private final LuckyDrawLogExportFileJobDispatchPublisher luckyDrawLogExportFileJobDispatchPublisher;

	public TurntableLuckyDrawLogExportService(
			TurntableConfigService turntableConfigService,
			OperatorsQueryService operatorsQueryService,
			LuckyDrawLogExportFileJobDispatchPublisher luckyDrawLogExportFileJobDispatchPublisher) {
		this.turntableConfigService = turntableConfigService;
		this.operatorsQueryService = operatorsQueryService;
		this.luckyDrawLogExportFileJobDispatchPublisher = luckyDrawLogExportFileJobDispatchPublisher;
	}

	public void exportLog(
			long companyId,
			long operatorId,
			long merchantId,
			String activityIdRaw,
			String datapassBlockHeader) {
		turntableConfigService.parseRequiredActivityId(activityIdRaw);
		long supplierId;
		if (operatorId <= 0L) {
			supplierId = 0L;
		} else {
			LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
			filter.put("company_id", companyId);
			filter.put("operator_id", operatorId);
			filter.put("operator_type", "supplier");
			Map<String, Object> supplier = operatorsQueryService.getInfo(filter);
			supplierId = (supplier != null && !supplier.isEmpty()) ? operatorId : 0L;
		}
		luckyDrawLogExportFileJobDispatchPublisher.enqueue(
				companyId, operatorId, merchantId, supplierId, activityIdRaw, datapassBlockHeader);
	}
}
