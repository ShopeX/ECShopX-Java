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

package cn.shopex.ecshopx.adapay.dispatch;

import cn.shopex.ecshopx.adapay.service.export.AdapayTradeDataExportContext;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AdapayTradeDataExportFileJobPayloadSupport {

	private AdapayTradeDataExportFileJobPayloadSupport() {}

	public static AdapayTradeDataExportContext contextFromPayload(Map<String, Object> payload) {
		long companyId = longFrom(payload.get("company_id"));
		long jwtOperatorId = longFrom(payload.get("operator_id"));
		Object rawFilter = payload.get("filter");
		Map<String, Object> preparedFilter = new LinkedHashMap<>();
		if (rawFilter instanceof Map<?, ?> m) {
			for (Map.Entry<?, ?> e : m.entrySet()) {
				preparedFilter.put(String.valueOf(e.getKey()), e.getValue());
			}
		}
		Object rawOp = preparedFilter.get("operator_type");
		String rawOperatorType = rawOp == null ? "" : rawOp.toString().trim();
		String outputOperatorType = "staff".equalsIgnoreCase(rawOperatorType) ? "admin" : rawOperatorType;
		return new AdapayTradeDataExportContext(companyId, jwtOperatorId, outputOperatorType, preparedFilter);
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
