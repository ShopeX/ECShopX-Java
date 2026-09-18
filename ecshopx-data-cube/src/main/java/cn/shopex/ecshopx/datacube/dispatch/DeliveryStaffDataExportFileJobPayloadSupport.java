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

package cn.shopex.ecshopx.datacube.dispatch;

import cn.shopex.ecshopx.companys.service.deliverystaff.AdminDeliveryStaffDataExportFilter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public final class DeliveryStaffDataExportFileJobPayloadSupport {

	private static final Pattern LEGACY_DISTRIBUTOR_ID_FRAGMENT =
			Pattern.compile("\"distributor_id\"\\s*:\\s*\"?(\\d+)\"?");

	private DeliveryStaffDataExportFileJobPayloadSupport() {}

	public static LinkedHashMap<String, Object> filterToMap(AdminDeliveryStaffDataExportFilter f) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", f.getCompanyId());
		if (StringUtils.hasText(f.getOperatorType())) {
			m.put("operator_type", f.getOperatorType());
		}
		if (StringUtils.hasText(f.getUsername())) {
			m.put("username", f.getUsername());
		}
		if (StringUtils.hasText(f.getMobilePlain())) {
			m.put("mobile", f.getMobilePlain());
		}
		if (f.getMerchantIdForOperators() != null) {
			m.put("merchant_id", f.getMerchantIdForOperators());
		}
		if (f.getDistributorId() != null) {
			m.put("distributor_id", f.getDistributorId());
		}
		if (f.getMatchDistributorIds() != null && !f.getMatchDistributorIds().isEmpty()) {
			m.put("distributor_ids", new ArrayList<>(f.getMatchDistributorIds()));
		}
		m.put("start_date", f.getStartEpoch());
		m.put("end_date", f.getEndEpoch());
		return m;
	}

	public static AdminDeliveryStaffDataExportFilter filterFromPayload(Map<String, Object> payload) {
		AdminDeliveryStaffDataExportFilter f = new AdminDeliveryStaffDataExportFilter();
		f.setCompanyId(longFrom(payload.get("company_id")));
		f.setOperatorId(longFrom(payload.get("operator_id")));
		Object rawFilter = payload.get("filter");
		if (!(rawFilter instanceof Map<?, ?> map)) {
			return f;
		}
		Object opType = map.get("operator_type");
		if (opType != null && StringUtils.hasText(String.valueOf(opType))) {
			f.setOperatorType(String.valueOf(opType));
		}
		Object username = map.get("username");
		if (username != null && StringUtils.hasText(String.valueOf(username))) {
			f.setUsername(String.valueOf(username));
		}
		Object mobile = map.get("mobile");
		if (mobile != null && StringUtils.hasText(String.valueOf(mobile))) {
			f.setMobilePlain(String.valueOf(mobile));
		}
		if (map.containsKey("merchant_id")) {
			f.setMerchantIdForOperators(longFrom(map.get("merchant_id")));
		}
		if (map.containsKey("distributor_id")) {
			f.setDistributorId(longFrom(map.get("distributor_id")));
		}
		Object rawDistIds = map.get("distributor_ids");
		if (rawDistIds instanceof List<?> list) {
			List<Long> ids = parseDistributorIdsFromPayloadList(list);
			if (!ids.isEmpty()) {
				f.setMatchDistributorIds(ids);
			}
		}
		if (map.containsKey("start_date")) {
			f.setStartEpoch(longFrom(map.get("start_date")));
		}
		if (map.containsKey("end_date")) {
			f.setEndEpoch(longFrom(map.get("end_date")));
		}
		return f;
	}

	private static List<Long> parseDistributorIdsFromPayloadList(List<?> list) {
		List<Long> ids = new ArrayList<>();
		for (Object o : list) {
			Long parsed = parseDistributorIdFromPayloadEntry(o);
			if (parsed != null) {
				ids.add(parsed);
			}
		}
		return ids;
	}

	private static Long parseDistributorIdFromPayloadEntry(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			long v = n.longValue();
			return v > 0 ? v : null;
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0 ? v : null;
		} catch (NumberFormatException ignored) {
			Matcher m = LEGACY_DISTRIBUTOR_ID_FRAGMENT.matcher(s);
			if (m.find()) {
				try {
					long v = Long.parseLong(m.group(1));
					return v > 0 ? v : null;
				} catch (NumberFormatException ex) {
					return null;
				}
			}
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
