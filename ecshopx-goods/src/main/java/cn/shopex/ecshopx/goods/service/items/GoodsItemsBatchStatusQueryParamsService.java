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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.goods.service.export.ExportItemsTagFilterBuildService;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class GoodsItemsBatchStatusQueryParamsService {

	private final ExportItemsTagFilterBuildService exportItemsTagFilterBuildService;

	public GoodsItemsBatchStatusQueryParamsService(ExportItemsTagFilterBuildService exportItemsTagFilterBuildService) {
		this.exportItemsTagFilterBuildService = exportItemsTagFilterBuildService;
	}

	public Optional<Map<String, Object>> tryBuildParams(long companyId, long operatorId, String operatorType, Long merchantId,
			Map<String, Object> queryMapOrNull) {
		Map<String, Object> input = queryMapOrNull == null ? Collections.emptyMap() : queryMapOrNull;
		return exportItemsTagFilterBuildService.buildParamsOptionalForBatchApproveStatus(companyId, operatorId, operatorType, merchantId, input);
	}
}
