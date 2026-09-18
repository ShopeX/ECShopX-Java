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

package cn.shopex.ecshopx.orders.statement;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Locale;

/**
 * 结算周期 {@code [n,"day|week|month"]} 与存库 JSON 的互转；缺键/非法与 PHP 宽松语义对齐并短路。
 */
public record StatementPeriodValue(int n, String unit) {
	private static final TypeReference<List<Object>> TYP = new TypeReference<>() {};

	public static StatementPeriodValue tryParse(String periodJson, ObjectMapper objectMapper) {
		if (periodJson == null || periodJson.isBlank()) {
			return null;
		}
		try {
			List<Object> p = objectMapper.readValue(periodJson, TYP);
			if (p == null || p.size() < 2) {
				return null;
			}
			int n0 = 0;
			if (p.get(0) instanceof Number num) {
				n0 = num.intValue();
			} else {
				return null;
			}
			String u0 = p.get(1) == null ? null : p.get(1).toString().toLowerCase(Locale.ROOT);
			if (u0 == null || u0.isEmpty() || (!
					u0.equals("day")
					&& !u0.equals("week")
					&& !u0.equals("month"))) {
				return null;
			}
			if (n0 <= 0) {
				return null;
			}
			return new StatementPeriodValue(n0, u0);
		} catch (Exception e) {
			return null;
		}
	}
}
