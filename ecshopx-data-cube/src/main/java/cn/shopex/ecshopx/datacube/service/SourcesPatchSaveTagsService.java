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

package cn.shopex.ecshopx.datacube.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.datacube.domain.Sources;
import cn.shopex.ecshopx.datacube.mapper.SourcesMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SourcesPatchSaveTagsService {

	private final SourcesMapper sourcesMapper;
	private final ObjectMapper objectMapper;

	public SourcesPatchSaveTagsService(SourcesMapper sourcesMapper, ObjectMapper objectMapper) {
		this.sourcesMapper = sourcesMapper;
		this.objectMapper = objectMapper;
	}

	public void patchSaveTags(long companyId, Object tagsIdRaw, Object sourceIdsRaw) {
		List<Object> newTags = normalizeNewTags(tagsIdRaw);
		List<Long> sourceIds = normalizeSourceIds(sourceIdsRaw);
		if (sourceIds.isEmpty()) {
			return;
		}
		List<Sources> rows = sourcesMapper.selectList(
				Wrappers.lambdaQuery(Sources.class)
						.eq(Sources::getCompanyId, companyId)
						.in(Sources::getSourceId, sourceIds));
		for (Sources row : rows) {
			String existing = row.getTagsId();
			List<Object> merged;
			if (StringUtils.hasText(existing)) {
				List<Object> oldList;
				try {
					oldList = objectMapper.readValue(existing, new TypeReference<List<Object>>() {});
				} catch (JsonProcessingException e) {
					oldList = Collections.emptyList();
				}
				if (oldList == null) {
					oldList = Collections.emptyList();
				}
				merged = dedupePreserveOrder(oldList, newTags);
			} else {
				merged = newTags;
			}
			String json;
			try {
				json = objectMapper.writeValueAsString(merged);
			} catch (JsonProcessingException e) {
				throw new ResourceException("tags_id 格式无效");
			}
			sourcesMapper.update(
					null,
					Wrappers.lambdaUpdate(Sources.class)
							.eq(Sources::getSourceId, row.getSourceId())
							.eq(Sources::getCompanyId, companyId)
							.set(Sources::getTagsId, json));
		}
	}

	private List<Object> dedupePreserveOrder(List<Object> oldList, List<Object> newTags) {
		LinkedHashSet<Object> set = new LinkedHashSet<>();
		set.addAll(oldList);
		set.addAll(newTags);
		return new ArrayList<>(set);
	}

	private List<Object> normalizeNewTags(Object tagsIdRaw) {
		if (tagsIdRaw == null) {
			return Collections.emptyList();
		}
		if (tagsIdRaw instanceof Collection<?> c && !(tagsIdRaw instanceof Map<?, ?>)) {
			return new ArrayList<>(c);
		}
		if (tagsIdRaw instanceof Map<?, ?> m) {
			return new ArrayList<>(m.values());
		}
		if (tagsIdRaw.getClass().isArray()) {
			return expandArrayToObjectList(tagsIdRaw);
		}
		if (tagsIdRaw instanceof String s) {
			return newTagsFromJsonString(s);
		}
		if (tagsIdRaw instanceof Number || tagsIdRaw instanceof Boolean) {
			return Collections.singletonList(tagsIdRaw);
		}
		return Collections.emptyList();
	}

	private List<Object> newTagsFromJsonString(String s) {
		JsonNode root;
		try {
			root = objectMapper.readTree(s);
		} catch (JsonProcessingException | IllegalArgumentException e) {
			return Collections.emptyList();
		}
		if (root == null || root.isNull() || root.isMissingNode()) {
			return Collections.emptyList();
		}
		if (root instanceof ArrayNode an) {
			List<Object> out = new ArrayList<>();
			for (JsonNode el : an) {
				if (el.isNumber() || el.isTextual() || el.isBoolean()) {
					Object v = jsonScalarToJava(el);
					if (v != null) {
						out.add(v);
					}
				}
			}
			return out;
		}
		if (root.isNumber() || root.isTextual() || root.isBoolean()) {
			Object v = jsonScalarToJava(root);
			return v != null ? Collections.singletonList(v) : Collections.emptyList();
		}
		if (root instanceof ObjectNode) {
			return Collections.emptyList();
		}
		return Collections.emptyList();
	}

	private static Object jsonScalarToJava(JsonNode n) {
		if (n.isNumber()) {
			return n.numberValue();
		}
		if (n.isTextual()) {
			return n.asText();
		}
		if (n.isBoolean()) {
			return n.booleanValue();
		}
		return null;
	}

	private List<Long> normalizeSourceIds(Object sourceIdsRaw) {
		List<Object> rawList = normalizeSourceIdsRawList(sourceIdsRaw);
		List<Long> out = new ArrayList<>();
		for (Object o : rawList) {
			if (o instanceof Number n) {
				out.add(n.longValue());
			} else if (o instanceof String str) {
				try {
					out.add(Long.parseLong(str.trim()));
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return out;
	}

	private List<Object> normalizeSourceIdsRawList(Object sourceIdsRaw) {
		if (sourceIdsRaw == null) {
			return Collections.emptyList();
		}
		if (sourceIdsRaw instanceof Collection<?> c && !(sourceIdsRaw instanceof Map<?, ?>)) {
			return new ArrayList<>(c);
		}
		if (sourceIdsRaw instanceof Map<?, ?> m) {
			return new ArrayList<>(m.values());
		}
		if (sourceIdsRaw.getClass().isArray()) {
			return expandArrayToObjectList(sourceIdsRaw);
		}
		if (sourceIdsRaw instanceof String s) {
			return sourceIdsFromJsonString(s);
		}
		if (sourceIdsRaw instanceof Number n) {
			return Collections.singletonList(n);
		}
		return Collections.emptyList();
	}

	private List<Object> sourceIdsFromJsonString(String s) {
		JsonNode root;
		try {
			root = objectMapper.readTree(s);
		} catch (JsonProcessingException | IllegalArgumentException e) {
			return Collections.emptyList();
		}
		if (root == null || root.isNull() || root.isMissingNode()) {
			return Collections.emptyList();
		}
		if (root instanceof ArrayNode an) {
			List<Object> out = new ArrayList<>();
			for (JsonNode el : an) {
				if (el.isNumber()) {
					out.add(el.numberValue());
				} else if (el.isTextual()) {
					out.add(el.asText());
				}
			}
			return out;
		}
		if (root.isNumber()) {
			return Collections.singletonList(root.numberValue());
		}
		if (root.isTextual()) {
			String t = root.asText();
			try {
				return Collections.singletonList(Long.parseLong(t.trim()));
			} catch (NumberFormatException e) {
				return Collections.emptyList();
			}
		}
		if (root instanceof ObjectNode || root.isBoolean()) {
			return Collections.emptyList();
		}
		return Collections.emptyList();
	}

	private static List<Object> expandArrayToObjectList(Object arr) {
		if (arr == null || !arr.getClass().isArray()) {
			return Collections.emptyList();
		}
		if (arr instanceof Object[] oa) {
			return new ArrayList<>(Arrays.asList(oa));
		}
		List<Object> out = new ArrayList<>();
		if (arr instanceof int[] a) {
			for (int v : a) {
				out.add(v);
			}
			return out;
		}
		if (arr instanceof long[] a) {
			for (long v : a) {
				out.add(v);
			}
			return out;
		}
		if (arr instanceof double[] a) {
			for (double v : a) {
				out.add(v);
			}
			return out;
		}
		if (arr instanceof float[] a) {
			for (float v : a) {
				out.add(v);
			}
			return out;
		}
		if (arr instanceof boolean[] a) {
			for (boolean v : a) {
				out.add(v);
			}
			return out;
		}
		if (arr instanceof byte[] a) {
			for (byte v : a) {
				out.add(v);
			}
			return out;
		}
		if (arr instanceof short[] a) {
			for (short v : a) {
				out.add(v);
			}
			return out;
		}
		if (arr instanceof char[] a) {
			for (char v : a) {
				out.add(String.valueOf(v));
			}
			return out;
		}
		return Collections.emptyList();
	}
}
