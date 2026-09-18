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

package cn.shopex.ecshopx.chinaumspay.dispatch;

import cn.shopex.ecshopx.chinaumspay.service.divisionlist.DivisionListExportContext;
import cn.shopex.ecshopx.chinaumspay.service.divisionlist.DivisionListExportFilter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class ChinaumsDivisionListExportFileJobPayloadSupport {

	private ChinaumsDivisionListExportFileJobPayloadSupport() {}

	public static LinkedHashMap<String, Object> filterToMap(DivisionListExportFilter filter) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", filter.getCompanyId());
		if (StringUtils.hasText(filter.getBackStatus())) {
			m.put("back_status", filter.getBackStatus());
		}
		if (StringUtils.hasText(filter.getCreateTimeBegin()) && StringUtils.hasText(filter.getCreateTimeEnd())) {
			m.put("create_time|gte", filter.getCreateTimeBegin());
			m.put("create_time|lte", filter.getCreateTimeEnd());
		}
		if (filter.getDistributorId() != null) {
			m.put("distributor_id", filter.getDistributorId());
		}
		return m;
	}

	public static DivisionListExportContext contextFromPayload(Map<String, Object> payload) {
		long companyId = longFrom(payload.get("company_id"));
		long operatorId = longFrom(payload.get("operator_id"));
		LinkedHashMap<String, Object> fm = new LinkedHashMap<>();
		Object rawFilter = payload.get("filter");
		if (rawFilter instanceof Map<?, ?> map) {
			for (Map.Entry<?, ?> e : map.entrySet()) {
				fm.put(String.valueOf(e.getKey()), e.getValue());
			}
		}

		String backStatus = "";
		Object rawBack = fm.get("back_status");
		if (rawBack != null) {
			String s = String.valueOf(rawBack).trim();
			if (StringUtils.hasText(s)) {
				backStatus = s;
			}
		}

		String createTimeBegin = "";
		String createTimeEnd = "";
		Object rawGte = fm.get("create_time|gte");
		Object rawLte = fm.get("create_time|lte");
		if (rawGte != null && rawLte != null) {
			String g = String.valueOf(rawGte).trim();
			String l = String.valueOf(rawLte).trim();
			if (StringUtils.hasText(g) && StringUtils.hasText(l)) {
				createTimeBegin = g;
				createTimeEnd = l;
			}
		}

		Long distributorId = null;
		if (fm.containsKey("distributor_id")) {
			Object rawDist = fm.get("distributor_id");
			if (rawDist != null && StringUtils.hasText(String.valueOf(rawDist).trim())) {
				distributorId = longFrom(rawDist);
			}
		}

		DivisionListExportFilter divisionFilter = new DivisionListExportFilter(
				companyId, backStatus, createTimeBegin, createTimeEnd, distributorId);
		return new DivisionListExportContext(companyId, operatorId, divisionFilter);
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
