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
import cn.shopex.ecshopx.distribution.repository.DistributorWriteRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorCreateParamNormalizer {

	private final DistributorWriteRepository distributorWriteRepository;

	public DistributorCreateParamNormalizer(DistributorWriteRepository distributorWriteRepository) {
		this.distributorWriteRepository = distributorWriteRepository;
	}

	public void normalize(Map<String, Object> merged, Map<String, Object> user) {
		normalizeHourRangeString(merged);
		Object distType = merged.get("distribution_type");
		if (distType == null || !StringUtils.hasText(distType.toString())) {
			merged.put("merchant_id", 0L);
		}
		String op = user.get("operator_type") != null ? user.get("operator_type").toString() : "";
		if ("merchant".equals(op)) {
			merged.put("merchant_id", toLong(user.get("merchant_id")));
			merged.put("distribution_type", "1");
		}
		Object isDada = merged.get("is_dada");
		if (isDada != null && "false".equalsIgnoreCase(isDada.toString().trim())) {
			merged.put("is_dada", "");
		}
		if (merged.get("regionauth_id") == null || !StringUtils.hasText(merged.get("regionauth_id").toString())) {
			merged.put("regionauth_id", 0L);
		}
		normalizeOfflineFlags(merged);
		applyRegionsToProvinceCityArea(merged);
		if ("distributor".equals(op)) {
			merged.remove("is_audit_goods");
			merged.remove("auto_sync_goods");
		}
		if (!merged.containsKey("is_delivery")) {
			merged.put("is_delivery", Boolean.TRUE);
		}
		if (!merged.containsKey("is_self_delivery")) {
			merged.put("is_self_delivery", Boolean.FALSE);
		}
		coerceTriBool(merged, "is_ziti");
		coerceTriBool(merged, "is_delivery");
		coerceTriBool(merged, "is_self_delivery");
		if (merged.containsKey("auto_sync_goods")) {
			coerceTriBool(merged, "auto_sync_goods");
		}
		if (merged.containsKey("is_audit_goods")) {
			coerceTriBool(merged, "is_audit_goods");
		}
		if (merged.containsKey("is_require_subdistrict")) {
			coerceTriBool(merged, "is_require_subdistrict");
		}
		if (merged.containsKey("is_require_building")) {
			coerceTriBool(merged, "is_require_building");
		}
		Object cid = user.get("company_id");
		if (cid != null) {
			merged.put("company_id", toLong(cid));
		}
		normalizeLedgerRate(merged);
		normalizeOfflineAftersalesDistributorIds(merged);
	}

	private void normalizeHourRangeString(Map<String, Object> merged) {
		Object h = merged.get("hour");
		if (h == null) {
			return;
		}
		if (h instanceof String) {
			return;
		}
		if (h instanceof List<?> list && list.size() == 2) {
			String a = str(list.get(0));
			String b = str(list.get(1));
			merged.put("hour", a + " - " + b);
			return;
		}
		if (h instanceof Object[] arr && arr.length == 2) {
			String a = str(arr[0]);
			String b = str(arr[1]);
			merged.put("hour", a + " - " + b);
		}
	}

	private void normalizeOfflineFlags(Map<String, Object> merged) {
		if (merged.containsKey("offline_aftersales")) {
			boolean on = truthy(merged.get("offline_aftersales"));
			merged.put("offline_aftersales", on);
			merged.put("offline_aftersales_self", on);
		} else if (!merged.containsKey("offline_aftersales_self")) {
			merged.put("offline_aftersales_self", Boolean.FALSE);
		} else {
			merged.put("offline_aftersales_self", truthy(merged.get("offline_aftersales_self")));
		}
		if (merged.containsKey("offline_aftersales_other")) {
			merged.put("offline_aftersales_other", truthy(merged.get("offline_aftersales_other")));
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

	private void normalizeLedgerRate(Map<String, Object> merged) {
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

	private void normalizeOfflineAftersalesDistributorIds(Map<String, Object> merged) {
		Object v = merged.get("offline_aftersales_distributor_id");
		if (v == null) {
			return;
		}
		if (v instanceof String s && !StringUtils.hasText(s)) {
			merged.remove("offline_aftersales_distributor_id");
			return;
		}
		List<Long> ids = parseIdList(v);
		if (ids.isEmpty()) {
			merged.remove("offline_aftersales_distributor_id");
			return;
		}
		long companyId = toLong(merged.get("company_id"));
		long merchantId = toLong(merged.get("merchant_id"));
		long cnt = distributorWriteRepository.countDistributorsForMerchantInList(companyId, merchantId, ids);
		if (cnt != ids.size()) {
			throw new ResourceException("售后店铺须与当前商户一致");
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
		return o == null ? "" : o.toString();
	}

	private static void coerceTriBool(Map<String, Object> m, String k) {
		Object v = m.get(k);
		if (v == null) {
			return;
		}
		if (v instanceof Boolean) {
			return;
		}
		String s = v.toString().trim();
		boolean on = StringUtils.hasText(s) && !"false".equalsIgnoreCase(s) && !"0".equals(s);
		m.put(k, on);
	}
}
