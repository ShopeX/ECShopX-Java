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

package cn.shopex.ecshopx.distribution.support;

import cn.shopex.ecshopx.distribution.domain.BasicConfig;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BasicConfigColumnNamesDataMapper {

	private BasicConfigColumnNamesDataMapper() {}

	public static Map<String, Object> toColumnNamesData(BasicConfig row) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", row.getCompanyId());
		m.put("is_buy", Boolean.TRUE.equals(row.getIsBuy()));
		m.put("limit_rebate", normalizeLimitRebate(row.getLimitRebate()));
		m.put("limit_time", row.getLimitTime());
		m.put("return_name", row.getReturnName());
		m.put("return_address", row.getReturnAddress());
		m.put("return_phone", row.getReturnPhone());
		m.put("is_income_tax", row.getIsIncomeTax());
		m.put("income_tax_params", row.getIncomeTaxParams());
		m.put("created", row.getCreated());
		m.put("updated", row.getUpdated());
		return m;
	}

	private static Object normalizeLimitRebate(Object raw) {
		if (raw == null) {
			return 100;
		}
		String s;
		if (raw instanceof Number) {
			Number n = (Number) raw;
			if (n instanceof Long || n instanceof Integer || n instanceof Short || n instanceof Byte) {
				s = Long.toString(n.longValue());
			} else {
				BigDecimal bd = new BigDecimal(n.toString());
				s = bd.stripTrailingZeros().toPlainString();
			}
		} else {
			s = raw.toString().trim();
		}
		s = s.trim();
		if (s.isEmpty() || "0".equals(s)) {
			return 100;
		}
		try {
			if (new BigDecimal(s).compareTo(BigDecimal.ZERO) == 0) {
				return 100;
			}
		} catch (NumberFormatException ignored) {
			// keep trimmed string
		}
		return s;
	}
}
