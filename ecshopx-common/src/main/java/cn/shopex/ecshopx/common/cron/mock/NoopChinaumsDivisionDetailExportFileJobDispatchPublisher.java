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

import cn.shopex.ecshopx.common.dispatch.ChinaumsDivisionDetailExportFileJobDispatchPublisher;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoopChinaumsDivisionDetailExportFileJobDispatchPublisher
		implements ChinaumsDivisionDetailExportFileJobDispatchPublisher {

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public void enqueueChinaumsDivisionDetailExport(
			long companyId, long operatorId, LinkedHashMap<String, Object> filterMap) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][chinaums-division-detail-export-file-job] called#{}, companyId={}, operatorId={}, filterKeys={}",
				n,
				companyId,
				operatorId,
				filterMap == null ? null : filterMap.keySet());
	}
}
