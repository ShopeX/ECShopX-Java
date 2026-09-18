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

import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.selfservice.dto.admin.v1.FormSettingCreateRequest;
import cn.shopex.ecshopx.selfservice.dto.admin.v1.FormSettingOptionItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class FormSettingCreateInputMerger {

	private final ObjectMapper objectMapper;

	public FormSettingCreateInputMerger(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public void mergeQueryIntoBody(HttpServletRequest request, FormSettingCreateRequest body) {
		String ct = request.getContentType();
		boolean isJson = ct != null && ct.toLowerCase().contains("application/json");
		if (isJson) {
			applyQueryScalarIfBodyBlank(request, "id", body.getId(), body::setId);
			applyQueryScalarIfBodyBlank(request, "field_title", body.getFieldTitle(), body::setFieldTitle);
			applyQueryScalarIfBodyBlank(request, "field_name", body.getFieldName(), body::setFieldName);
			applyQueryScalarIfBodyBlank(request, "form_element", body.getFormElement(), body::setFormElement);
			applyQueryScalarIfBodyBlank(request, "image_url", body.getImageUrl(), body::setImageUrl);
			applyQueryScalarIfBodyBlank(request, "pic_name", body.getPicName(), body::setPicName);
		}
		if (!isJson && body.getOptions() == null) {
			Map<String, Object> flat = FlexibleHttpServletParameterMap.toObjectMap(request);
			Object raw = flat.get("options");
			if (raw != null) {
				try {
					List<FormSettingOptionItem> parsed =
							objectMapper.convertValue(raw, new TypeReference<List<FormSettingOptionItem>>() {});
					body.setOptions(parsed);
				} catch (IllegalArgumentException ignored) {
					// keep null; service handles by form_element
				}
			}
		}
	}

	private void applyQueryScalarIfBodyBlank(
			HttpServletRequest request,
			String paramKey,
			String current,
			java.util.function.Consumer<String> setter) {
		String q = request.getParameter(paramKey);
		if (!StringUtils.hasText(q)) {
			return;
		}
		if (current == null || !StringUtils.hasText(current)) {
			setter.accept(q);
		}
	}
}
