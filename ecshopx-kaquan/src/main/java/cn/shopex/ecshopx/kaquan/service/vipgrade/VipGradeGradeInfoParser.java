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

package cn.shopex.ecshopx.kaquan.service.vipgrade;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parses merged request {@code grade_info} into a list of mutable maps. */
public final class VipGradeGradeInfoParser {

	private VipGradeGradeInfoParser() {}

	public static List<Map<String, Object>> parseGradeInfoList(Object gradeInfoRaw, ObjectMapper objectMapper) {
		if (gradeInfoRaw == null) {
			throw new BadRequestException("grade_info 格式无效");
		}
		if (gradeInfoRaw instanceof String s) {
			try {
				JsonNode node = objectMapper.readTree(s);
				if (node == null || !node.isArray()) {
					throw new BadRequestException("grade_info 格式无效");
				}
				List<Map<String, Object>> list = new ArrayList<>();
				for (JsonNode el : node) {
					list.add(objectMapper.convertValue(el, new TypeReference<Map<String, Object>>() {}));
				}
				return list;
			} catch (BadRequestException e) {
				throw e;
			} catch (Exception e) {
				throw new BadRequestException("grade_info 格式无效");
			}
		}
		if (gradeInfoRaw instanceof List<?> rawList) {
			List<Map<String, Object>> list = new ArrayList<>();
			for (Object el : rawList) {
				if (el instanceof Map<?, ?> mm) {
					Map<String, Object> row = new LinkedHashMap<>();
					for (Map.Entry<?, ?> e : mm.entrySet()) {
						if (e.getKey() != null) {
							row.put(String.valueOf(e.getKey()), e.getValue());
						}
					}
					list.add(row);
				} else {
					try {
						list.add(objectMapper.convertValue(el, new TypeReference<Map<String, Object>>() {}));
					} catch (IllegalArgumentException e) {
						throw new BadRequestException("grade_info 格式无效");
					}
				}
			}
			return list;
		}
		throw new BadRequestException("grade_info 格式无效");
	}
}
