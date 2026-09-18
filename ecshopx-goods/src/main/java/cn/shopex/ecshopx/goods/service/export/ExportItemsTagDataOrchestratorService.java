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

package cn.shopex.ecshopx.goods.service.export;

import cn.shopex.ecshopx.common.dispatch.ExportItemsTagDataExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.goods.service.export.dto.ExportItemsCodeFilterBuildResult;
import cn.shopex.ecshopx.goods.service.export.dto.ExportItemsTagOrchestratorResult;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ExportItemsTagDataOrchestratorService {

	private final ExportItemsTagFilterBuildService tagFilterBuildService;
	private final ExportItemsTagDataExportFileJobDispatchPublisher exportItemsTagDataExportFileJobDispatchPublisher;

	public ExportItemsTagDataOrchestratorService(ExportItemsTagFilterBuildService tagFilterBuildService,
			ExportItemsTagDataExportFileJobDispatchPublisher exportItemsTagDataExportFileJobDispatchPublisher) {
		this.tagFilterBuildService = tagFilterBuildService;
		this.exportItemsTagDataExportFileJobDispatchPublisher = exportItemsTagDataExportFileJobDispatchPublisher;
	}

	public ExportItemsTagOrchestratorResult submitOrEmptyList(long companyId, long operatorId, String operatorType,
			Long merchantId, Map<String, Object> inputData) {
		ExportItemsCodeFilterBuildResult built = tagFilterBuildService.build(companyId, operatorId, operatorType, merchantId,
				inputData);
		if (built instanceof ExportItemsCodeFilterBuildResult.EmptyItemBnList) {
			return new ExportItemsTagOrchestratorResult.EmptyList();
		}
		ExportItemsCodeFilterBuildResult.ParamsReady pr = (ExportItemsCodeFilterBuildResult.ParamsReady) built;
		LinkedHashMap<String, Object> params = new LinkedHashMap<>(pr.params());
		if ("merchant".equalsIgnoreCase(operatorType != null ? operatorType : "") && merchantId != null) {
			params.put("merchant_id", merchantId);
		}
		String opType = operatorType != null ? operatorType : "";
		params.put("operator_type", opType);
		exportItemsTagDataExportFileJobDispatchPublisher.publish(companyId, operatorId, opType, merchantId, params);
		return new ExportItemsTagOrchestratorResult.Enqueued();
	}
}
