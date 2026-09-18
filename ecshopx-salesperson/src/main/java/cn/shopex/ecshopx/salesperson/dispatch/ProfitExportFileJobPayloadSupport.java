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

package cn.shopex.ecshopx.salesperson.dispatch;

import cn.shopex.ecshopx.salesperson.service.export.ProfitExportJobContext;
import java.util.LinkedHashMap;
import java.util.Map;

final class ProfitExportFileJobPayloadSupport {

	private ProfitExportFileJobPayloadSupport() {}

	static ProfitExportJobContext toContext(Map<String, Object> payload, String handlerExportType) {
		Object rawType = payload.get("type");
		String typeStr = rawType == null ? null : String.valueOf(rawType);
		if (!handlerExportType.equals(typeStr)) {
			throw new IllegalStateException("export type mismatch: expected " + handlerExportType + ", got " + typeStr);
		}
		long companyId = extractRequiredLong(payload, "company_id");
		long operatorId = extractRequiredLong(payload, "operator_id");
		LinkedHashMap<String, Object> filter = extractFilterMap(payload.get("filter"));
		Object dateObj = filter.get("date");
		if (dateObj == null) {
			throw new IllegalStateException("filter missing date");
		}
		String dateYm = String.valueOf(dateObj);
		Object put = filter.get("profit_user_type");
		String profitUserTypeRaw = put == null ? null : String.valueOf(put);
		return new ProfitExportJobContext(handlerExportType, companyId, dateYm, profitUserTypeRaw, operatorId);
	}

	static LinkedHashMap<String, Object> extractFilterMap(Object raw) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		if (raw instanceof Map<?, ?> m) {
			for (Map.Entry<?, ?> e : m.entrySet()) {
				out.put(String.valueOf(e.getKey()), e.getValue());
			}
		}
		return out;
	}

	static long extractRequiredLong(Map<String, Object> payload, String key) {
		Object v = payload.get(key);
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
