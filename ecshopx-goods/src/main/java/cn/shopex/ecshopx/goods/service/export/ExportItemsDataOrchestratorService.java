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

import cn.shopex.ecshopx.common.dispatch.ExportItemsDataExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.goods.service.export.dto.ExportItemsCodeFilterBuildResult;
import cn.shopex.ecshopx.goods.service.export.dto.ExportItemsDataOrchestratorResult;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ExportItemsDataOrchestratorService {

	private final ExportItemsCodeFilterBuildService exportItemsCodeFilterBuildService;
	private final ExportItemsDataExportFileJobDispatchPublisher exportItemsDataExportFileJobDispatchPublisher;

	public ExportItemsDataOrchestratorService(ExportItemsCodeFilterBuildService exportItemsCodeFilterBuildService,
			ExportItemsDataExportFileJobDispatchPublisher exportItemsDataExportFileJobDispatchPublisher) {
		this.exportItemsCodeFilterBuildService = exportItemsCodeFilterBuildService;
		this.exportItemsDataExportFileJobDispatchPublisher = exportItemsDataExportFileJobDispatchPublisher;
	}

	public ExportItemsDataOrchestratorResult submitOrEmptyList(long companyId, long operatorId, String operatorType,
			Long merchantId, Map<String, Object> inputData) {
		ExportItemsCodeFilterBuildResult built = exportItemsCodeFilterBuildService.build(companyId, operatorId, operatorType,
				merchantId, inputData);
		if (built instanceof ExportItemsCodeFilterBuildResult.EmptyItemBnList) {
			return new ExportItemsDataOrchestratorResult.EmptyItemBnList();
		}
		ExportItemsCodeFilterBuildResult.ParamsReady pr = (ExportItemsCodeFilterBuildResult.ParamsReady) built;

		String itemSource = resolveItemSource(inputData);
		if (!StringUtils.hasText(itemSource)) {
			Object fromParams = pr.params().get("item_source");
			if (fromParams != null && StringUtils.hasText(fromParams.toString())) {
				itemSource = fromParams.toString().trim();
			} else {
				itemSource = "item";
			}
		}

		String exportType = Optional.ofNullable(inputData.get("export_type")).map(Object::toString).filter(StringUtils::hasText)
				.map(String::trim).orElse("items");
		if ("supplier".equalsIgnoreCase(operatorType != null ? operatorType : "")
				|| "supplier".equalsIgnoreCase(itemSource)) {
			exportType = "supplier_goods";
		}

		LinkedHashMap<String, Object> params = new LinkedHashMap<>(pr.params());
		params.remove("wxaappid");

		if ("merchant".equalsIgnoreCase(operatorType != null ? operatorType : "") && merchantId != null) {
			params.put("merchant_id", merchantId);
		}

		ItemsDataExportContext ctx = new ItemsDataExportContext(companyId, operatorId, operatorType, merchantId, exportType,
				itemSource, params);
		exportItemsDataExportFileJobDispatchPublisher.publish(
				ctx.companyId(), ctx.operatorId(), ctx.exportType(), ctx.operatorType(), ctx.merchantId(),
				ctx.itemSource(), ctx.filterParams());
		return new ExportItemsDataOrchestratorResult.Enqueued();
	}

	private static String resolveItemSource(Map<String, Object> inputData) {
		Object holder = inputData.get("item_holder");
		if (holder != null && StringUtils.hasText(holder.toString())) {
			String h = holder.toString().trim();
			if ("supplier".equalsIgnoreCase(h)) {
				return "supplier";
			}
			if ("self".equalsIgnoreCase(h) || "platform".equalsIgnoreCase(h)) {
				return "platform";
			}
		}
		return Optional.ofNullable(inputData.get("item_source")).map(Object::toString).filter(StringUtils::hasText)
				.map(String::trim).orElse("");
	}
}
