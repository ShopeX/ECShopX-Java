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

package cn.shopex.ecshopx.aftersales.dispatch;

import cn.shopex.ecshopx.aftersales.service.export.AftersalesRecordListExportFileJobHandler;
import cn.shopex.ecshopx.aftersales.service.export.AftersalesRecordListExportJobContext;
import cn.shopex.ecshopx.common.dispatch.ExportFileJobTypeHandler;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AftersaleRecordCountExportFileJobTypeHandler implements ExportFileJobTypeHandler {

	private final AftersalesRecordListExportFileJobHandler aftersalesRecordListExportFileJobHandler;

	public AftersaleRecordCountExportFileJobTypeHandler(
			AftersalesRecordListExportFileJobHandler aftersalesRecordListExportFileJobHandler) {
		this.aftersalesRecordListExportFileJobHandler = aftersalesRecordListExportFileJobHandler;
	}

	@Override
	public String exportType() {
		return AftersalesRecordListExportFileJobHandler.EXPORT_FILE_JOB_TYPE_AFTERSALE_RECORD_COUNT;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long companyId = extractRequiredLong(payload, "company_id");
		long operatorId = extractRequiredLong(payload, "operator_id");
		LinkedHashMap<String, Object> filter = extractFilterMap(payload.get("filter"));
		AftersalesRecordListExportJobContext ctx =
				new AftersalesRecordListExportJobContext(companyId, operatorId, filter);
		aftersalesRecordListExportFileJobHandler.run(ctx);
	}

	private static LinkedHashMap<String, Object> extractFilterMap(Object raw) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		if (raw instanceof Map<?, ?> m) {
			for (Map.Entry<?, ?> e : m.entrySet()) {
				out.put(String.valueOf(e.getKey()), e.getValue());
			}
		}
		return out;
	}

	private static long extractRequiredLong(Map<String, Object> payload, String key) {
		Object raw = payload.get(key);
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}
}
