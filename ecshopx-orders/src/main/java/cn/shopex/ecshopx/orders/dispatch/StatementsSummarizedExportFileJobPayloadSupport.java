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

package cn.shopex.ecshopx.orders.dispatch;

import cn.shopex.ecshopx.orders.service.statement.export.StatementsSummarizedExportJobContext;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StatementsSummarizedExportFileJobPayloadSupport {

	private StatementsSummarizedExportFileJobPayloadSupport() {}

	public static StatementsSummarizedExportJobContext contextFromPayload(Map<String, Object> payload) {
		long companyId = longFrom(payload.get("company_id"));
		long operatorId = longFrom(payload.get("operator_id"));
		Object rawFilter = payload.get("filter");
		LinkedHashMap<String, Object> exportFilter = new LinkedHashMap<>();
		if (rawFilter instanceof Map<?, ?> m) {
			for (Map.Entry<?, ?> e : m.entrySet()) {
				exportFilter.put(String.valueOf(e.getKey()), e.getValue());
			}
		}
		return new StatementsSummarizedExportJobContext(companyId, operatorId, exportFilter);
	}

	private static long longFrom(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
