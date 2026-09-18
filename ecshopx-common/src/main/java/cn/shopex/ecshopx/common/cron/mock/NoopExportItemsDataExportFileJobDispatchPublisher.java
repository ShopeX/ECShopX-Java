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

package cn.shopex.ecshopx.common.cron.mock;

import cn.shopex.ecshopx.common.dispatch.ExportItemsDataExportFileJobDispatchPublisher;
import java.util.LinkedHashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoopExportItemsDataExportFileJobDispatchPublisher implements ExportItemsDataExportFileJobDispatchPublisher {

	@Override
	public void publish(
			long companyId,
			long operatorId,
			String exportType,
			String operatorType,
			Long merchantId,
			String itemSource,
			LinkedHashMap<String, Object> filterParams) {
		log.info(
				"[cron-mock][export-items-data-export-file-job] publish companyId={} operatorId={} exportType={}",
				companyId,
				operatorId,
				exportType);
	}
}
