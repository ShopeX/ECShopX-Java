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

package cn.shopex.ecshopx.distribution.dispatch;

import cn.shopex.ecshopx.distribution.service.dto.DistributorWhiteListExportFilter;
import cn.shopex.ecshopx.distribution.service.export.DistributorWhiteListExportJobContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class DistributorWhiteListExportFileJobPayloadSupport {

	private DistributorWhiteListExportFileJobPayloadSupport() {}

	public static LinkedHashMap<String, Object> filterToMap(DistributorWhiteListExportFilter f) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", f.companyId());
		m.put("distributor_ids", f.distributorIds());
		m.put("mobile_filter_active", f.mobileFilterActive());
		m.put("mobile", f.mobile() == null ? "" : f.mobile());
		m.put("username_prefix", f.usernamePrefix());
		m.put("shop_not_found", f.shopNotFound());
		return m;
	}

	public static DistributorWhiteListExportFilter filterFromMap(Map<String, Object> m) {
		if (m == null || m.isEmpty()) {
			return new DistributorWhiteListExportFilter(0L, null, false, "", null, false);
		}
		long companyId = longFrom(m.get("company_id"));
		List<Long> distributorIds = distributorIdsFrom(m.get("distributor_ids"));
		boolean mobileFilterActive = booleanFrom(m.get("mobile_filter_active"), false);
		String mobile = strOrEmpty(m.get("mobile"));
		String usernamePrefix = strOrNullTrimmed(m.get("username_prefix"));
		boolean shopNotFound = booleanFrom(m.get("shop_not_found"), false);
		return new DistributorWhiteListExportFilter(
				companyId, distributorIds, mobileFilterActive, mobile, usernamePrefix, shopNotFound);
	}

	public static DistributorWhiteListExportJobContext toContext(Map<String, Object> payload) {
		long companyId = longFrom(payload.get("company_id"));
		long operatorId = longFrom(payload.get("operator_id"));
		long merchantId = longFrom(payload.get("merchant_id"));
		long supplierId = longFrom(payload.get("supplier_id"));
		String datapassBlockRaw = strOrNull(payload.get("datapass_block"));
		DistributorWhiteListExportFilter filter = filterFromMap(copyFilterMap(payload.get("filter")));
		return new DistributorWhiteListExportJobContext(
				companyId, operatorId, merchantId, supplierId, filter, datapassBlockRaw);
	}

	private static Map<String, Object> copyFilterMap(Object rawFilter) {
		if (!(rawFilter instanceof Map<?, ?> src)) {
			return new LinkedHashMap<>();
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : src.entrySet()) {
			if (e.getKey() != null) {
				out.put(String.valueOf(e.getKey()), e.getValue());
			}
		}
		return out;
	}

	private static String strOrNull(Object v) {
		if (v == null) {
			return null;
		}
		String s = v.toString();
		return s;
	}

	private static String strOrEmpty(Object v) {
		if (v == null) {
			return "";
		}
		return v.toString();
	}

	private static String strOrNullTrimmed(Object v) {
		if (v == null) {
			return null;
		}
		String s = v.toString().trim();
		return StringUtils.hasText(s) ? s : null;
	}

	private static boolean booleanFrom(Object v, boolean defaultValue) {
		if (v == null) {
			return defaultValue;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof String s) {
			if (!StringUtils.hasText(s)) {
				return defaultValue;
			}
			return Boolean.parseBoolean(s.trim());
		}
		return defaultValue;
	}

	private static List<Long> distributorIdsFrom(Object o) {
		if (o == null) {
			return null;
		}
		if (!(o instanceof List<?> list)) {
			return null;
		}
		if (list.isEmpty()) {
			return List.of();
		}
		List<Long> out = new ArrayList<>(list.size());
		for (Object item : list) {
			if (item == null) {
				continue;
			}
			if (item instanceof Number n) {
				out.add(n.longValue());
			} else {
				String s = item.toString().trim();
				if (StringUtils.hasText(s)) {
					out.add(Long.parseLong(s));
				}
			}
		}
		return out.isEmpty() ? List.of() : out;
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
