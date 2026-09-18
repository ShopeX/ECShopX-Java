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

package cn.shopex.ecshopx.wechat.service.wxa;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.wechat.service.wxa.model.MergedUploadWxaInput;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxaUploadWxaInputMergeService {

	public MergedUploadWxaInput merge(HttpServletRequest request, @Nullable Map<String, Object> bodyJson) {
		String wxaAppId = firstEffectiveWxaField(request, bodyJson, "wxaAppId");
		String wxaName = firstEffectiveWxaField(request, bodyJson, "wxa_name");
		String templateName = resolveTemplateName(request, bodyJson);
		Map<String, Object> templateOptions = resolveTemplateOptions(request, bodyJson);
		return new MergedUploadWxaInput(wxaAppId, wxaName, templateName, templateOptions);
	}

	public MergedUploadWxaInput mergeForSubmitReview(
			HttpServletRequest request, @Nullable Map<String, Object> bodyJson) {
		String wxaAppId = firstEffectiveWxaField(request, bodyJson, "wxaAppId");
		String wxaName = firstEffectiveWxaField(request, bodyJson, "wxa_name");
		String templateName = resolveTemplateNameForSubmitReview(request, bodyJson);
		Map<String, Object> templateOptions = resolveTemplateOptions(request, bodyJson);
		return new MergedUploadWxaInput(wxaAppId, wxaName, templateName, templateOptions);
	}

	private static String resolveTemplateNameForSubmitReview(
			HttpServletRequest request, @Nullable Map<String, Object> body) {
		if (request.getParameterMap().containsKey("templateName")) {
			String v = request.getParameter("templateName");
			return v == null ? null : v.trim();
		}
		if (body != null && body.containsKey("templateName")) {
			Object o = body.get("templateName");
			return o == null ? null : String.valueOf(o).trim();
		}
		return null;
	}

	private static String firstEffectiveWxaField(
			HttpServletRequest request, @Nullable Map<String, Object> body, String key) {
		String q = request.getParameter(key);
		if (StringUtils.hasText(q)) {
			return q.trim();
		}
		if (body != null && body.containsKey(key)) {
			Object o = body.get(key);
			if (o == null) {
				return "";
			}
			String s = String.valueOf(o).trim();
			return StringUtils.hasText(s) ? s : "";
		}
		return "";
	}

	private static String resolveTemplateName(HttpServletRequest request, @Nullable Map<String, Object> body) {
		if (request.getParameter("templateName") != null) {
			return request.getParameter("templateName").trim();
		}
		if (body != null && body.containsKey("templateName")) {
			Object o = body.get("templateName");
			return o == null ? "" : String.valueOf(o).trim();
		}
		return "yykweishop";
	}

	private static Map<String, Object> resolveTemplateOptions(
			HttpServletRequest request, @Nullable Map<String, Object> body) {
		String qp = request.getParameter("templateOptions");
		if (StringUtils.hasText(qp)) {
			throw new BadRequestException("模板参数格式错误");
		}
		if (body == null || !body.containsKey("templateOptions")) {
			return new LinkedHashMap<>();
		}
		Object raw = body.get("templateOptions");
		if (raw == null) {
			return new LinkedHashMap<>();
		}
		if (raw instanceof Map<?, ?> m) {
			Map<String, Object> out = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				out.put(String.valueOf(e.getKey()), e.getValue());
			}
			return out;
		}
		if (raw instanceof List<?> list) {
			Map<String, Object> out = new LinkedHashMap<>();
			for (Object item : list) {
				if (item instanceof Map<?, ?> m) {
					for (Map.Entry<?, ?> e : m.entrySet()) {
						out.put(String.valueOf(e.getKey()), e.getValue());
					}
				}
			}
			return out;
		}
		throw new BadRequestException("模板参数格式错误");
	}
}
