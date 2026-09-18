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

package cn.shopex.ecshopx.kaquan.openapi.thirdapi.v2;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiDiscountCardV2UserDiscountListPresentationService {

	public void apply(List<Map<String, Object>> list) {
		if (list == null || list.isEmpty()) {
			return;
		}
		for (int i = 0; i < list.size(); i++) {
			Map<String, Object> item = list.get(i);
			list.set(i, toSlimRow(item));
		}
	}

	private static Map<String, Object> toSlimRow(Map<String, Object> item) {
		LinkedHashMap<String, Object> slim = new LinkedHashMap<>();
		slim.put("card_id", parseIntFlexible(item.get("card_id")));
		slim.put("code", stringify(item.get("code"), "0"));
		slim.put("plat_account", stringify(item.get("user_id"), "0"));
		slim.put("card_type", stringify(item.get("card_type"), "0"));
		slim.put("begin_date", stringify(item.get("begin_date"), ""));
		slim.put("end_date", stringify(item.get("end_date"), "0"));
		slim.put("status", OpenapiDiscountCardV2OpenApiStatusSupport.mapFromDbRow(item));
		return slim;
	}

	private static String stringify(Object value, String defaultVal) {
		if (value == null) {
			return defaultVal;
		}
		if (value instanceof Number number) {
			if (value instanceof Double || value instanceof Float) {
				double d = number.doubleValue();
				if (d == Math.rint(d)) {
					return String.valueOf((long) d);
				}
			}
			return String.valueOf(number.longValue());
		}
		return String.valueOf(value);
	}

	private static int parseIntFlexible(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number number) {
			return number.intValue();
		}
		try {
			String text = String.valueOf(raw).trim();
			if (text.isEmpty()) {
				return 0;
			}
			if (text.contains(".")) {
				return (int) Double.parseDouble(text);
			}
			return Integer.parseInt(text);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
