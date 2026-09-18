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

import cn.shopex.ecshopx.selfservice.domain.FormSetting;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class FormSettingApiRowAssembler {

	private final ObjectMapper objectMapper;

	public FormSettingApiRowAssembler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> toRow(FormSetting entity) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("id", entity.getId());
		out.put("company_id", entity.getCompanyId());
		out.put("distributor_id", entity.getDistributorId());
		out.put("field_title", entity.getFieldTitle());
		out.put("field_name", entity.getFieldName());
		out.put("pic_name", entity.getPicName());
		out.put("form_element", entity.getFormElement());
		out.put("status", entity.getStatus());
		out.put("sort", entity.getSort());
		out.put("is_required", entity.getIsRequired());
		out.put("image_url", entity.getImageUrl() == null ? "" : entity.getImageUrl());
		out.put("options", parseOptionsValue(entity.getOptions()));
		out.put("created", entity.getCreated());
		out.put("updated", entity.getUpdated());
		return out;
	}

	public Map<String, Object> toDataInfoRow(FormSetting entity) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("id", entity.getId());
		out.put("company_id", entity.getCompanyId());
		out.put("distributor_id", entity.getDistributorId());
		out.put("field_title", entity.getFieldTitle());
		out.put("field_name", entity.getFieldName());
		out.put("pic_name", entity.getPicName());
		out.put("form_element", entity.getFormElement());
		out.put("status", entity.getStatus());
		out.put("sort", entity.getSort());
		out.put("is_required", entity.getIsRequired());
		out.put("image_url", entity.getImageUrl() == null ? "" : entity.getImageUrl());
		out.put("options", parseOptionsValue(entity.getOptions()));
		return out;
	}

	private Object parseOptionsValue(String optStr) {
		if (!StringUtils.hasText(optStr)) {
			return null;
		}
		try {
			JsonNode node = objectMapper.readTree(optStr);
			if (node == null || node.isNull()) {
				return null;
			}
			if (node.isTextual()) {
				return node.asText();
			}
			Object converted = objectMapper.convertValue(node, Object.class);
			if (converted instanceof List<?> list && list.isEmpty()) {
				return null;
			}
			return converted;
		} catch (JsonProcessingException ex) {
			return null;
		}
	}
}
