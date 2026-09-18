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

import cn.shopex.ecshopx.orders.repository.OrderEpidemicRegisterListFilter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class EpidemicRegisterExportFileJobPayloadSupport {

	private EpidemicRegisterExportFileJobPayloadSupport() {}

	public static LinkedHashMap<String, Object> exportFilterToMap(
			OrderEpidemicRegisterListFilter f, String datapassBlockHeader) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", f.getCompanyId());
		if (f.getDistributorIdEq() != null) {
			m.put("distributor_id", f.getDistributorIdEq());
		}
		if (f.getDistributorIdIn() != null && !f.getDistributorIdIn().isEmpty()) {
			m.put("distributor_ids", new ArrayList<>(f.getDistributorIdIn()));
		}
		if (f.getOrderTimeGte() != null) {
			m.put("order_time|gte", f.getOrderTimeGte());
		}
		if (f.getOrderTimeLte() != null) {
			m.put("order_time|lte", f.getOrderTimeLte());
		}
		if (datapassBlockHeader != null) {
			m.put("datapass_block", datapassBlockHeader);
		}
		return m;
	}

	public static OrderEpidemicRegisterListFilter listFilterFromExportPayload(Map<String, Object> payload) {
		Object rawFilter = payload.get("filter");
		Map<?, ?> fm;
		if (rawFilter instanceof Map<?, ?> rm) {
			fm = rm;
		} else {
			fm = Map.of();
		}
		OrderEpidemicRegisterListFilter filter = new OrderEpidemicRegisterListFilter();
		long companyId = longFrom(payload.get("company_id"));
		if (companyId == 0L) {
			companyId = longFrom(fm.get("company_id"));
		}
		filter.setCompanyId(companyId);
		if (fm.containsKey("distributor_id")) {
			Long eq = longObjectToNullableLong(fm.get("distributor_id"));
			filter.setDistributorIdEq(eq);
		}
		if (fm.containsKey("distributor_ids")) {
			filter.setDistributorIdIn(parseLongList(fm.get("distributor_ids")));
		}
		filter.setOrderTimeGte(intFrom(fm.get("order_time|gte")));
		filter.setOrderTimeLte(intFrom(fm.get("order_time|lte")));
		return filter;
	}

	public static long operatorIdFromPayload(Map<String, Object> payload) {
		return longFrom(payload.get("operator_id"));
	}

	public static boolean datapassMaskingFromPayload(Map<String, Object> payload) {
		Object rawFilter = payload.get("filter");
		if (!(rawFilter instanceof Map<?, ?> fm)) {
			return false;
		}
		Object v = fm.get("datapass_block");
		if (v == null) {
			return false;
		}
		return truthyDatapassToken(String.valueOf(v));
	}

	private static List<Long> parseLongList(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof List<?> list) {
			List<Long> out = new ArrayList<>(list.size());
			for (Object o : list) {
				out.add(longFrom(o));
			}
			return out;
		}
		return null;
	}

	private static Long longObjectToNullableLong(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Integer intFrom(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return null;
		}
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

	private static boolean truthyDatapassToken(String v) {
		if (v == null) {
			return false;
		}
		String t = v.trim();
		if (t.isEmpty()) {
			return false;
		}
		if ("0".equals(t)) {
			return false;
		}
		return !"false".equalsIgnoreCase(t);
	}
}
