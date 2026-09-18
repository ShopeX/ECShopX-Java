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
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DistributorTagsRelInputNormalizer {

	private static final ObjectMapper JSON = new ObjectMapper();

	public record NormalizedRelTagInput(
			boolean distributorIdIsArray,
			boolean tagIdsIsArray,
			List<Long> distributorIds,
			List<Long> tagIdsAsList,
			Long scalarDistributorId,
			Long scalarTagId) {

		public boolean hasDistributorSelection() {
			if (distributorIdIsArray) {
				return distributorIds != null && !distributorIds.isEmpty();
			}
			return scalarDistributorId != null && scalarDistributorId != 0L;
		}

		public boolean hasTagSelection() {
			if (tagIdsIsArray) {
				return tagIdsAsList != null && !tagIdsAsList.isEmpty();
			}
			return scalarTagId != null && scalarTagId != 0L;
		}
	}

	public NormalizedRelTagInput normalize(Map<String, Object> merged) {
		Map<String, Object> m = merged != null ? merged : Collections.emptyMap();
		Object rawDist = m.get("distributor_id");
		Object rawTag = m.get("tag_ids");

		boolean distArray = isArraySemantic(rawDist);
		boolean tagArray = isArraySemantic(rawTag);

		List<Long> distributorIds;
		Long scalarDist;
		if (distArray) {
			distributorIds = parseIdList(rawDist, "distributor_id");
			scalarDist = null;
		} else {
			distributorIds = List.of();
			scalarDist = parseScalarLongOrNull(rawDist, "distributor_id");
		}

		List<Long> tagIdsList;
		Long scalarTag;
		if (tagArray) {
			tagIdsList = parseIdList(rawTag, "tag_ids");
			scalarTag = null;
		} else {
			tagIdsList = List.of();
			scalarTag = parseScalarLongOrNull(rawTag, "tag_ids");
		}

		return new NormalizedRelTagInput(distArray, tagArray, distributorIds, tagIdsList, scalarDist, scalarTag);
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

	private static Long parseScalarLongOrNull(Object raw, String fieldName) {
		if (isEmptySemantic(raw)) {
			return null;
		}
		if (raw instanceof List<?> || raw instanceof Object[] || raw instanceof Collection<?>) {
			return null;
		}
		if (raw instanceof String s && StringUtils.hasText(s.trim()) && s.trim().startsWith("[")) {
			return null;
		}
		return parseRequiredLong(raw, fieldName);
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

	private static boolean isEmptySemantic(Object o) {
		if (o == null) {
			return true;
		}
		if (Boolean.FALSE.equals(o)) {
			return true;
		}
		if (o instanceof Number n && n.longValue() == 0L) {
			return true;
		}
		if (o instanceof String s && !StringUtils.hasText(s.trim())) {
			return true;
		}
		if (o instanceof List<?> list && list.isEmpty()) {
			return true;
		}
		if (o instanceof Object[] arr && arr.length == 0) {
			return true;
		}
		if (o instanceof Collection<?> c && c.isEmpty()) {
			return true;
		}
		return false;
	}
}
