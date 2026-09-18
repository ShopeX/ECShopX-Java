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

package cn.shopex.ecshopx.members.openapi.thirdapi.v2;

import java.util.LinkedHashMap;
import java.util.Map;

final class OpenapiMemberV2EnumNormalize {

	private OpenapiMemberV2EnumNormalize() {}

	private static final Map<Integer, String> SEX_MAP = Map.of(0, "未知", 1, "男", 2, "女");
	private static final Map<Integer, String> EDU_BACKGROUND_MAP =
			Map.of(0, "硕士及以上", 1, "本科", 2, "大专", 3, "高中/中专及以下", 4, "其他");
	private static final Map<Integer, String> INCOME_MAP =
			Map.of(0, "5万以下", 1, "5万 ~ 15万", 2, "15万 ~ 30万", 3, "30万以上", 4, "其他");
	private static final Map<Integer, String> INDUSTRY_MAP = buildIndustryMap();

	private static Map<Integer, String> buildIndustryMap() {
		Map<Integer, String> map = new LinkedHashMap<>();
		map.put(0, "金融/银行/投资");
		map.put(1, "计算机/互联网");
		map.put(2, "媒体/出版/影视/文化");
		map.put(3, "政府/公共事业");
		map.put(4, "房地产/建材/工程");
		map.put(5, "咨询/法律");
		map.put(6, "加工制造");
		map.put(7, "教育培训");
		map.put(8, "医疗保健");
		map.put(9, "运输/物流/交通");
		map.put(10, "零售/贸易");
		map.put(11, "旅游/度假");
		map.put(12, "其他");
		return Map.copyOf(map);
	}

	static void normalizeEnumField(Map<String, Object> row, String key, Map<Integer, String> map) {
		if (!row.containsKey(key)) {
			return;
		}
		Object raw = row.get(key);
		if (raw == null) {
			return;
		}
		Integer intKey = toIntKey(raw);
		if (intKey != null && map.containsKey(intKey)) {
			row.put(key, intKey);
			return;
		}
		Integer fromValue = findKeyByValue(map, raw);
		row.put(key, fromValue);
	}

	static void normalizeListRow(Map<String, Object> row) {
		normalizeEnumField(row, "sex", SEX_MAP);
		normalizeEnumField(row, "edu_background", EDU_BACKGROUND_MAP);
		normalizeEnumField(row, "income", INCOME_MAP);
		normalizeEnumField(row, "industry", INDUSTRY_MAP);
	}

	private static Integer toIntKey(Object raw) {
		if (raw instanceof Number n) {
			return n.intValue();
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Integer findKeyByValue(Map<Integer, String> map, Object raw) {
		String target = String.valueOf(raw);
		for (Map.Entry<Integer, String> entry : map.entrySet()) {
			if (entry.getValue().equals(target)) {
				return entry.getKey();
			}
		}
		return null;
	}
}
