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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorUpdateParamNormalizer {

	public void normalize(Map<String, Object> merged, Map<String, Object> user, long pathDistributorId) {
		merged.put("distributor_id", pathDistributorId);
		String op = user.get("operator_type") != null ? user.get("operator_type").toString() : "";
		if ("merchant".equals(op)) {
			merged.put("merchant_id", toLong(user.get("merchant_id")));
			merged.put("distribution_type", "1");
		}
		if ("distributor".equals(op)) {
			merged.remove("is_audit_goods");
			merged.remove("auto_sync_goods");
		}
		if (merged.containsKey("is_dada")) {
			Object isDada = merged.get("is_dada");
			if (isDada != null && "false".equalsIgnoreCase(isDada.toString().trim())) {
				merged.put("is_dada", "");
			}
		}
		if (merged.containsKey("regionauth_id")) {
			Object ra = merged.get("regionauth_id");
			if (ra == null || !StringUtils.hasText(ra.toString())) {
				merged.put("regionauth_id", 0L);
			}
		}
		normalizeOfflineFlags(merged, pathDistributorId);
		applyRegionsToProvinceCityArea(merged);
		if (merged.containsKey("distributor_self") && "1".equals(str(merged.get("distributor_self")))) {
			merged.put("is_valid", "true");
		}
		if (merged.containsKey("open_divided")) {
			Object od = merged.get("open_divided");
			boolean on = od instanceof Boolean b ? b : "true".equalsIgnoreCase(str(od)) || "1".equals(str(od));
			merged.put("open_divided", on ? 1L : 0L);
		}
		if (merged.containsKey("review_result")) {
			Object rr = merged.get("review_result");
			boolean ok = "true".equalsIgnoreCase(str(rr));
			merged.put("is_valid", ok ? "true" : "false");
			merged.put("review_status", Boolean.TRUE);
			merged.remove("review_result");
		}
		if (merged.get("regions") instanceof List<?> r && r.size() == 2) {
			merged.put("area", "");
		}
		coerceTriBoolIfPresent(merged, "is_ziti");
		coerceTriBoolIfPresent(merged, "is_delivery");
		coerceTriBoolIfPresent(merged, "is_self_delivery");
		coerceTriBoolIfPresent(merged, "auto_sync_goods");
		coerceTriBoolIfPresent(merged, "is_audit_goods");
		coerceTriBoolIfPresent(merged, "is_require_subdistrict");
		coerceTriBoolIfPresent(merged, "is_require_building");
		coerceTriBoolIfPresent(merged, "is_open_salesman");
		Object cid = user.get("company_id");
		if (cid != null) {
			merged.put("company_id", toLong(cid));
		}
		normalizeLedgerRateIfOpen(merged);
		joinOfflineAftersalesDistributorIds(merged);
	}

	private void normalizeOfflineFlags(Map<String, Object> merged, long pathDistributorId) {
		if (merged.containsKey("offline_aftersales")) {
			boolean on = truthy(merged.get("offline_aftersales"));
			merged.put("offline_aftersales", on);
			merged.put("offline_aftersales_self", on);
		} else if (merged.containsKey("offline_aftersales_self")) {
			merged.put("offline_aftersales_self", truthy(merged.get("offline_aftersales_self")));
		}
		if (merged.containsKey("offline_aftersales_other")) {
			merged.put("offline_aftersales_other", truthy(merged.get("offline_aftersales_other")));
		}
		if (merged.containsKey("offline_aftersales_distributor_id")) {
			List<Long> ids = parseIdList(merged.get("offline_aftersales_distributor_id"));
			if (!ids.isEmpty()) {
				merged.put("offline_aftersales_self", ids.contains(pathDistributorId));
			}
		}
	}

	private void applyRegionsToProvinceCityArea(Map<String, Object> merged) {
		Object regionsObj = merged.get("regions");
		Object regionsIdObj = merged.get("regions_id");
		if (regionsObj == null || regionsIdObj == null) {
			return;
		}
		if (regionsObj instanceof List<?> r && r.size() >= 1) {
			merged.put("province", str(r.get(0)));
		}
		if (regionsObj instanceof List<?> r && r.size() >= 2) {
			merged.put("city", str(r.get(1)));
		}
		if (regionsObj instanceof List<?> r && r.size() >= 3) {
			merged.put("area", str(r.get(2)));
		}
	}

	private void normalizeLedgerRateIfOpen(Map<String, Object> merged) {
		if (!merged.containsKey("is_open")) {
			return;
		}
		if (!truthyIsOpen(merged.get("is_open"))) {
			return;
		}
		Object rateObj = merged.get("rate");
		BigDecimal r = BigDecimal.ZERO;
		if (rateObj != null && StringUtils.hasText(rateObj.toString())) {
			r = new BigDecimal(rateObj.toString().trim());
		}
		int scaled = r.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).intValue();
		if (scaled < 0 || scaled > 3000) {
			throw new ResourceException("平台服务费率超出允许范围");
		}
		merged.put("rate", scaled);
	}

	private void joinOfflineAftersalesDistributorIds(Map<String, Object> merged) {
		if (!merged.containsKey("offline_aftersales_distributor_id")) {
			return;
		}
		Object v = merged.get("offline_aftersales_distributor_id");
		if (v instanceof String s && !StringUtils.hasText(s)) {
			merged.put("offline_aftersales_distributor_id", "");
			return;
		}
		List<Long> ids = parseIdList(v);
		if (ids.isEmpty()) {
			merged.put("offline_aftersales_distributor_id", "");
			return;
		}
		String joined = ids.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
		merged.put("offline_aftersales_distributor_id", joined);
	}

	private static List<Long> parseIdList(Object v) {
		List<Long> out = new ArrayList<>();
		if (v instanceof List<?> list) {
			for (Object o : list) {
				if (o != null) {
					out.add(toLong(o));
				}
			}
			return out;
		}
		if (v instanceof String s && StringUtils.hasText(s)) {
			for (String part : s.split(",")) {
				String t = part.trim();
				if (StringUtils.hasText(t)) {
					out.add(Long.parseLong(t));
				}
			}
		}
		return out;
	}

	private static void coerceTriBoolIfPresent(Map<String, Object> m, String k) {
		if (!m.containsKey(k)) {
			return;
		}
		Object v = m.get(k);
		if (v instanceof Boolean) {
			return;
		}
		String s = v == null ? "" : v.toString().trim();
		boolean on = StringUtils.hasText(s) && !"false".equalsIgnoreCase(s) && !"0".equals(s);
		m.put(k, on);
	}

	private static boolean truthy(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		String s = v.toString().trim();
		return !s.isEmpty() && !"false".equalsIgnoreCase(s) && !"0".equals(s);
	}

	private static boolean truthyIsOpen(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		String s = v.toString().trim();
		return "true".equalsIgnoreCase(s) || "1".equals(s);
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}
}
