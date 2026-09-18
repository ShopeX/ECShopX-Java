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

import cn.shopex.ecshopx.common.dispatch.ExportItemsCodeDataExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.service.export.dto.ExportItemsCodeFilterBuildResult;
import cn.shopex.ecshopx.goods.service.export.dto.ExportItemsCodeOrchestratorResult;
import cn.shopex.ecshopx.wechat.repository.WeappAuthorizerAppidRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ExportItemsCodeDataOrchestratorService {

	private static final String TEMPLATE_YYKWEISHOP = "yykweishop";

	private final ExportItemsCodeFilterBuildService exportItemsCodeFilterBuildService;
	private final WeappAuthorizerAppidRepository weappAuthorizerAppidRepository;
	private final ExportItemsCodeDataExportFileJobDispatchPublisher exportItemsCodeDataExportFileJobDispatchPublisher;

	public ExportItemsCodeDataOrchestratorService(ExportItemsCodeFilterBuildService exportItemsCodeFilterBuildService,
			WeappAuthorizerAppidRepository weappAuthorizerAppidRepository,
			ExportItemsCodeDataExportFileJobDispatchPublisher exportItemsCodeDataExportFileJobDispatchPublisher) {
		this.exportItemsCodeFilterBuildService = exportItemsCodeFilterBuildService;
		this.weappAuthorizerAppidRepository = weappAuthorizerAppidRepository;
		this.exportItemsCodeDataExportFileJobDispatchPublisher = exportItemsCodeDataExportFileJobDispatchPublisher;
	}

	public ExportItemsCodeOrchestratorResult submitOrEmptyList(long companyId, long operatorId, String operatorType,
			Long merchantId, Map<String, Object> inputData) {
		String exportType = Optional.ofNullable(inputData.get("export_type")).map(Object::toString).filter(StringUtils::hasText)
				.orElse("wxa");

		ExportItemsCodeFilterBuildResult built = exportItemsCodeFilterBuildService.build(companyId, operatorId, operatorType,
				merchantId, inputData);
		if (built instanceof ExportItemsCodeFilterBuildResult.EmptyItemBnList) {
			return new ExportItemsCodeOrchestratorResult.EmptyItemBnList();
		}
		if (!(built instanceof ExportItemsCodeFilterBuildResult.ParamsReady pr)) {
			throw new IllegalStateException("unexpected filter result");
		}

		LinkedHashMap<String, Object> params = new LinkedHashMap<>(pr.params());
		params.put("export_type", exportType);
		if ("wxa".equalsIgnoreCase(exportType)) {
			String appid = weappAuthorizerAppidRepository.findAuthorizerAppid(companyId, TEMPLATE_YYKWEISHOP)
					.orElseThrow(() -> new ResourceException("没有开通此小程序，不能下载."));
			params.put("wxaappid", appid);
		}

		if ("merchant".equalsIgnoreCase(operatorType != null ? operatorType : "") && merchantId != null) {
			params.put("merchant_id", merchantId);
		}

		params.put("is_default", Boolean.TRUE);
		params.put("is_default_true", Boolean.TRUE);

		String opType = operatorType != null ? operatorType : "";
		params.put("operator_type", opType);
		exportItemsCodeDataExportFileJobDispatchPublisher.publish(companyId, operatorId, opType, merchantId, params);
		return new ExportItemsCodeOrchestratorResult.Enqueued();
	}
}
