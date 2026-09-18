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

package cn.shopex.ecshopx.selfservice.service;

import cn.shopex.ecshopx.selfservice.domain.FormTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class FormTemplateApiRowAssembler {

	private final ObjectMapper objectMapper;

	public FormTemplateApiRowAssembler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * Multilang read path may {@code put} JSON columns as raw strings. Detail responses typically expose decoded
	 * structures; list-style payloads may keep plain strings. When the value is valid JSON, parse to List/Map/scalars;
	 * otherwise leave opaque (return empty optional so caller keeps the string).
	 */
	public Optional<Object> tryDecodeMultilangJsonField(String value) {
		if (!StringUtils.hasText(value)) {
			return Optional.empty();
		}
		JsonNode n;
		try {
			n = objectMapper.readTree(value);
		} catch (JsonProcessingException ex) {
			return Optional.empty();
		}
		if (n == null || n.isNull()) {
			return Optional.of(Collections.emptyList());
		}
		return Optional.of(jsonNodeToResponseValue(n));
	}

	public Map<String, Object> toRow(FormTemplate entity) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("id", entity.getId());
		out.put("company_id", entity.getCompanyId());
		out.put("distributor_id", entity.getDistributorId());
		out.put("header_bg_pic", entity.getHeaderBgPic());
		out.put("header_height", entity.getHeaderHeight());
		out.put("tem_name", entity.getTemName());
		out.put("content", decodeColumnToResponseValue(entity.getContent()));
		out.put("status", entity.getStatus());
		out.put("form_style", entity.getFormStyle());
		out.put("header_link_title", entity.getHeaderLinkTitle());
		out.put("header_title", entity.getHeaderTitle());
		out.put("bottom_title", entity.getBottomTitle());
		out.put("key_index", decodeColumnToResponseValue(entity.getKeyIndex()));
		out.put("tem_type", entity.getTemType());
		return out;
	}

	private Object decodeColumnToResponseValue(String columnString) {
		if (!StringUtils.hasText(columnString)) {
			return Collections.emptyList();
		}
		JsonNode n;
		try {
			n = objectMapper.readTree(columnString);
		} catch (JsonProcessingException ex) {
			return Collections.emptyList();
		}
		if (n == null || n.isNull()) {
			return Collections.emptyList();
		}
		return jsonNodeToResponseValue(n);
	}

	private Object jsonNodeToResponseValue(JsonNode n) {
		if (n.isArray()) {
			return objectMapper.convertValue(n, new TypeReference<List<Object>>() {});
		}
		if (n.isObject()) {
			return objectMapper.convertValue(n, new TypeReference<Map<String, Object>>() {});
		}
		if (n.isBoolean()) {
			return n.booleanValue();
		}
		if (n.isInt()) {
			return n.intValue();
		}
		if (n.isLong()) {
			return n.longValue();
		}
		if (n.isFloatingPointNumber()) {
			return n.doubleValue();
		}
		if (n.isTextual()) {
			return n.asText();
		}
		if (n.isNumber()) {
			return n.numberValue();
		}
		return n.toString();
	}
}
