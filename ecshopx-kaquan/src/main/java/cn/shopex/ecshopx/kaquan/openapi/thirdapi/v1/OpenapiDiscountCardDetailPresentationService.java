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

package cn.shopex.ecshopx.kaquan.openapi.thirdapi.v1;

import cn.shopex.ecshopx.common.util.ValuePresence;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class OpenapiDiscountCardDetailPresentationService {

	@SuppressWarnings("unchecked")
	public void apply(Map<String, Object> result) {
		Object tlObj = result.remove("time_limit");
		if (tlObj instanceof List<?> timeLimit && !timeLimit.isEmpty()) {
			String begin = "";
			String end = "";
			List<String> typeSequence = new ArrayList<>();
			Map<String, Object> timeLimitDate = new LinkedHashMap<>();
			for (Object el : timeLimit) {
				if (!(el instanceof Map<?, ?> vm)) {
					continue;
				}
				Map<String, Object> value = (Map<String, Object>) vm;
				Object typeObj = value.get("type");
				if (typeObj != null) {
					typeSequence.add(String.valueOf(typeObj));
				}
				Object bh = value.get("begin_hour");
				Object bm = value.get("begin_minute");
				Object eh = value.get("end_hour");
				Object em = value.get("end_minute");
				if (bh != null && bm != null && eh != null && em != null) {
					String bStr = bh + ":" + bm;
					String eStr = eh + ":" + em;
					if (begin.isEmpty() && end.isEmpty()) {
						begin = bStr;
						end = eStr;
						Map<String, Object> slot = new LinkedHashMap<>();
						slot.put("begin_time", begin);
						slot.put("end_time", end);
						timeLimitDate.put("1", slot);
					} else if (!begin.equals(bStr)) {
						Map<String, Object> slot2 = new LinkedHashMap<>();
						slot2.put("begin_time", bStr);
						slot2.put("end_time", eStr);
						timeLimitDate.put("2", slot2);
					}
				}
			}
			if (!typeSequence.isEmpty()) {
				Set<String> seen = new LinkedHashSet<>();
				List<String> uniqueTypes = new ArrayList<>();
				for (String t : typeSequence) {
					if (seen.add(t)) {
						uniqueTypes.add(t);
					}
				}
				result.put("time_limit_type", uniqueTypes);
			}
			if (!timeLimitDate.isEmpty()) {
				result.put("time_limit_date", timeLimitDate);
			}
		}
		result.put("begin_time", intValueFlexible(result.get("begin_date")));
		Object fixedTerm = result.get("fixed_term");
		result.put("days", fixedTerm != null ? intValueFlexible(fixedTerm) : 30);
		Object endDate = result.get("end_date");
		result.put("end_time", endDate != null ? intValueFlexible(endDate) : 0);
		double discValue = doubleValueFlexible(result.get("discount"));
		if (discValue > 0) {
			result.put("discount", (100.0 - discValue) / 10.0);
		}
		if (ValuePresence.hasEffectiveValue(result.get("least_cost"))) {
			result.put("least_cost", divideBy100AsDouble(result.get("least_cost")));
		}
		if (ValuePresence.hasEffectiveValue(result.get("reduce_cost"))) {
			result.put("reduce_cost", divideBy100AsDouble(result.get("reduce_cost")));
		}
	}

	private static int intValueFlexible(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		if (o == null) {
			return 0;
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static double doubleValueFlexible(Object o) {
		if (o instanceof Number n) {
			return n.doubleValue();
		}
		if (o == null) {
			return 0.0;
		}
		try {
			return Double.parseDouble(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}

	private static double divideBy100AsDouble(Object o) {
		if (o instanceof Number n) {
			return n.doubleValue() / 100.0;
		}
		if (o == null) {
			return 0.0;
		}
		try {
			return Double.parseDouble(o.toString().trim()) / 100.0;
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}
}
