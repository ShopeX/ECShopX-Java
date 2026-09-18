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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DistributorTagsDelInputNormalizer {

	private static final ObjectMapper JSON = new ObjectMapper();

	public record NormalizedDelTagInput(List<Long> distributorIds, List<Long> tagIds) {}

	public NormalizedDelTagInput normalize(Map<String, Object> merged) {
		Map<String, Object> m = merged != null ? merged : Collections.emptyMap();
		Object rawDist = m.get("distributor_ids");
		Object rawTag = m.get("tag_ids");

		if (!isArraySemantic(rawDist)) {
			throw new BadRequestException("店铺参数有误！");
		}
		if (!isArraySemantic(rawTag)) {
			throw new BadRequestException("店铺标签参数有误！");
		}

		List<Long> distList = parseIdList(rawDist, "distributor_ids");
		List<Long> tagList = parseIdList(rawTag, "tag_ids");

		if (distList.size() > 50) {
			throw new BadRequestException("店铺最多只能选择50个！");
		}
		if (tagList.size() > 50) {
			throw new BadRequestException("店铺标签最多只能选择50个！");
		}

		List<Long> distDedup = new ArrayList<>(new LinkedHashSet<>(distList));
		List<Long> tagDedup = new ArrayList<>(new LinkedHashSet<>(tagList));
		return new NormalizedDelTagInput(List.copyOf(distDedup), List.copyOf(tagDedup));
	}

	private static boolean isArraySemantic(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof List<?>) {
			return true;
		}
		if (raw instanceof Object[]) {
			return true;
		}
		if (raw instanceof Collection<?> c && !(raw instanceof List<?>)) {
			return true;
		}
		if (raw instanceof String s && StringUtils.hasText(s)) {
			String t = s.trim();
			if (t.startsWith("[")) {
				try {
					JsonNode node = JSON.readTree(t);
					return node != null && node.isArray();
				} catch (Exception e) {
					return false;
				}
			}
		}
		return false;
	}

	private static List<Long> parseIdList(Object raw, String fieldName) {
		List<Object> elements = new ArrayList<>();
		if (raw instanceof List<?> list) {
			for (Object o : list) {
				elements.add(o);
			}
		} else if (raw instanceof Object[] arr) {
			for (Object o : arr) {
				elements.add(o);
			}
		} else if (raw instanceof Collection<?> col) {
			for (Object o : col) {
				elements.add(o);
			}
		} else if (raw instanceof String s && StringUtils.hasText(s.trim()) && s.trim().startsWith("[")) {
			try {
				JsonNode node = JSON.readTree(s.trim());
				if (node != null && node.isArray()) {
					for (JsonNode n : node) {
						elements.add(jsonNodeToRaw(n));
					}
				}
			} catch (Exception e) {
				throw new BadRequestException(fieldName + " 格式错误");
			}
		}
		List<Long> out = new ArrayList<>(elements.size());
		for (Object o : elements) {
			out.add(parseRequiredLong(o, fieldName));
		}
		return out;
	}

	private static Object jsonNodeToRaw(JsonNode n) {
		if (n == null || n.isNull()) {
			return null;
		}
		if (n.isNumber()) {
			return n.numberValue();
		}
		if (n.isTextual()) {
			return n.asText();
		}
		if (n.isBoolean()) {
			return n.booleanValue();
		}
		return n.toString();
	}

	private static long parseRequiredLong(Object o, String fieldName) {
		if (o == null) {
			throw new BadRequestException(fieldName + " 含非法元素");
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException(fieldName + " 含非法元素");
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException(fieldName + " 含非法元素");
		}
	}
}
