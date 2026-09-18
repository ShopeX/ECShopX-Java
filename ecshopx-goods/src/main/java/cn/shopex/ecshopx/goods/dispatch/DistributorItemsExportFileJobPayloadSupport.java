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

package cn.shopex.ecshopx.goods.dispatch;

import cn.shopex.ecshopx.goods.service.distributor.dto.DistributorItemsExportContext;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class DistributorItemsExportFileJobPayloadSupport {

	private DistributorItemsExportFileJobPayloadSupport() {}

	public static DistributorItemsExportContext toContext(Map<String, Object> payload) {
		long companyId = longFrom(payload.get("company_id"));
		long operatorId = longFrom(payload.get("operator_id"));
		String acceptLanguage = strOrEmpty(payload.get("accept_language"));
		int pageSize = intFrom(payload.get("page_size"), 500);
		Map<String, Object> filter = copyFilter(payload.get("filter"));
		long distributorId = longFrom(filter.get("distributor_id"));
		return new DistributorItemsExportContext(
				companyId, distributorId, operatorId, acceptLanguage, filter, pageSize);
	}

	private static Map<String, Object> copyFilter(Object rawFilter) {
		if (!(rawFilter instanceof Map<?, ?> m)) {
			return new LinkedHashMap<>();
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : m.entrySet()) {
			if (e.getKey() != null) {
				out.put(String.valueOf(e.getKey()), e.getValue());
			}
		}
		return out;
	}

	private static String strOrEmpty(Object v) {
		if (v == null) {
			return "";
		}
		String s = v.toString();
		return s;
	}

	private static int intFrom(Object v, int defaultValue) {
		if (v == null) {
			return defaultValue;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s)) {
			return defaultValue;
		}
		return Integer.parseInt(s);
	}

	private static long longFrom(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
