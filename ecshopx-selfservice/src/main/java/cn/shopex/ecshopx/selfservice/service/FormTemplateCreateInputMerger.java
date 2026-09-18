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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.selfservice.dto.admin.v1.FormTemplateCreateRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class FormTemplateCreateInputMerger {

	private final ObjectMapper objectMapper;

	public FormTemplateCreateInputMerger(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public void mergeQueryIntoBody(HttpServletRequest request, FormTemplateCreateRequest body) {
		String ct = request.getContentType();
		boolean isJson = ct != null && ct.toLowerCase().contains("application/json");
		if (isJson) {
			applyQueryScalarIfBodyBlank(request, "id", body.getId(), body::setId);
			applyQueryScalarIfBodyBlank(request, "tem_name", body.getTemName(), body::setTemName);
			applyQueryScalarIfBodyBlank(request, "tem_type", body.getTemType(), body::setTemType);
			applyQueryScalarIfBodyBlank(request, "form_style", body.getFormStyle(), body::setFormStyle);
			applyQueryScalarIfBodyBlank(request, "header_link_title", body.getHeaderLinkTitle(), body::setHeaderLinkTitle);
			applyQueryScalarIfBodyBlank(request, "header_title", body.getHeaderTitle(), body::setHeaderTitle);
			applyQueryScalarIfBodyBlank(request, "bottom_title", body.getBottomTitle(), body::setBottomTitle);
			applyQueryScalarIfBodyBlank(request, "header_bg_pic", body.getHeaderBgPic(), body::setHeaderBgPic);
			applyQueryScalarIfBodyBlank(request, "header_height", body.getHeaderHeight(), body::setHeaderHeight);
			applyQueryScalarIfBodyBlank(request, "status", body.getStatus(), body::setStatus);
			if (body.getContent() == null) {
				String qp = request.getParameter("content");
				if (StringUtils.hasText(qp)) {
					try {
						body.setContent(objectMapper.readTree(qp));
					} catch (JsonProcessingException ex) {
						throw new BadRequestException("content JSON 无效");
					}
				}
			}
			if (body.getKeyIndex() == null) {
				String qp = request.getParameter("key_index");
				if (StringUtils.hasText(qp)) {
					try {
						body.setKeyIndex(objectMapper.readTree(qp));
					} catch (JsonProcessingException ex) {
						throw new BadRequestException("key_index JSON 无效");
					}
				}
			}
		}
		if (!isJson) {
			Map<String, Object> flat = FlexibleHttpServletParameterMap.toObjectMap(request);
			if (!StringUtils.hasText(body.getId())) {
				Object rawId = flat.get("id");
				if (rawId != null
						&& !(rawId instanceof Map<?, ?>)
						&& !(rawId instanceof Iterable<?>)) {
					String sid = String.valueOf(rawId).trim();
					if (StringUtils.hasText(sid)) {
						body.setId(sid);
					}
				}
			}
			if (body.getContent() == null) {
				Object raw = flat.get("content");
				if (raw != null) {
					try {
						body.setContent(objectMapper.convertValue(raw, JsonNode.class));
					} catch (IllegalArgumentException ignored) {
						// keep null; service validates required
					}
				}
			}
			if (body.getKeyIndex() == null) {
				Object raw = flat.get("key_index");
				if (raw != null) {
					try {
						body.setKeyIndex(objectMapper.convertValue(raw, JsonNode.class));
					} catch (IllegalArgumentException ignored) {
						// keep null
					}
				}
			}
		}
	}

	private void applyQueryScalarIfBodyBlank(
			HttpServletRequest request, String paramKey, String current, Consumer<String> setter) {
		String q = request.getParameter(paramKey);
		if (!StringUtils.hasText(q)) {
			return;
		}
		if (current == null || !StringUtils.hasText(current)) {
			setter.accept(q);
		}
	}
}
