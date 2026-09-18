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

import cn.shopex.ecshopx.distribution.domain.Distributor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorListRowFormatService {

	/**
	 * Head-office placeholder row: same display fields as a normal list row but no {@code selfDeliveryRule}.
	 * Package-private: only {@link DistributorSelfMetaService} uses this; public API is {@link #formatStoreRow}.
	 */
	Map<String, Object> formatStoreInfoOnly(Distributor distributor, ObjectMapper objectMapper) {
		return buildFormattedRowWithoutSelfDeliveryRule(distributor, objectMapper);
	}

	public Map<String, Object> formatStoreRow(
			Distributor distributor, Map<String, Object> selfDeliverySetting, ObjectMapper objectMapper) {
		Map<String, Object> row = buildFormattedRowWithoutSelfDeliveryRule(distributor, objectMapper);
		row.put("selfDeliveryRule", selfDeliverySetting);
		return row;
	}

	private static Map<String, Object> buildFormattedRowWithoutSelfDeliveryRule(
			Distributor distributor, ObjectMapper objectMapper) {
		Map<String, Object> row = new LinkedHashMap<>(DistributorRowMaps.toApiRow(distributor, objectMapper));
		String province = stringOf(row.get("province"));
		String city = stringOf(row.get("city"));
		String area = stringOf(row.get("area"));
		String address = stringOf(row.get("address"));
		if (province != null
				&& (province.equals("上海市")
						|| province.equals("北京市")
						|| province.equals("重庆市")
						|| province.equals("天津市"))) {
			row.put("store_address", nullToEmpty(province) + nullToEmpty(area) + nullToEmpty(address));
		} else {
			row.put(
					"store_address",
					nullToEmpty(province) + nullToEmpty(city) + nullToEmpty(area) + nullToEmpty(address));
		}
		row.put("store_name", row.get("name"));
		row.put("phone", row.get("mobile"));
		Object rateVal = row.get("rate");
		Integer rateInt = null;
		if (rateVal instanceof Number n) {
			rateInt = n.intValue();
		} else if (rateVal != null && StringUtils.hasText(rateVal.toString())) {
			try {
				rateInt = Integer.parseInt(rateVal.toString().trim());
			} catch (NumberFormatException ignored) {
				rateInt = null;
			}
		}
		if (rateInt != null && rateInt != 0) {
			row.put(
					"rate",
					BigDecimal.valueOf(rateInt.longValue())
							.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
							.stripTrailingZeros()
							.toPlainString());
		} else {
			row.put("rate", "");
		}
		numericBool(row, "is_ziti");
		numericBool(row, "is_delivery");
		numericBool(row, "is_open_salesman");
		numericBool(row, "is_default");
		numericBool(row, "is_self_delivery");
		numericBool(row, "auto_sync_goods");
		numericBool(row, "is_audit_goods");
		numericBool(row, "is_distributor");
		numericBool(row, "review_status");
		Object isDadaVal = row.get("is_dada");
		if (isDadaVal != null) {
			row.put("is_dada", zeroOneIntFromDb(isDadaVal));
		}
		numericBool(row, "dada_shop_create");
		numericBool(row, "is_require_subdistrict");
		numericBool(row, "is_require_building");
		if (!row.containsKey("distance_show")) {
			row.put("distance_show", "");
		}
		if (!row.containsKey("distance_unit")) {
			row.put("distance_unit", "");
		}
		return row;
	}

	/** Entity maps {@code is_dada} as {@link Boolean}; legacy rows may still deserialize as {@link Number}. */
	private static int zeroOneIntFromDb(Object v) {
		if (v instanceof Boolean b) {
			return b ? 1 : 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		String s = v.toString().trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s) ? 1 : 0;
	}

	private static void numericBool(Map<String, Object> row, String key) {
		Object v = row.get(key);
		if (v == null) {
			return;
		}
		if (v instanceof Boolean) {
			return;
		}
		if (v instanceof Number n) {
			row.put(key, n.intValue() == 1);
			return;
		}
		String s = v.toString();
		row.put(key, "1".equals(s) || "true".equalsIgnoreCase(s));
	}

	private static String stringOf(Object o) {
		return o == null ? null : o.toString();
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}
}
