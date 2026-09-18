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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class WxaGetTemplateListService {

	private static final List<String> LIST_DOMAIN_KEYS = List.of(
			"requestdomain",
			"wsrequestdomain",
			"uploaddomain",
			"downloaddomain",
			"webviewdomain");

	private final WxaTemplateDomainInfoService wxaTemplateDomainInfoService;
	private final SuperadminWxappTemplateMetadataService superadminWxappTemplateMetadataService;
	private final Environment environment;

	public WxaGetTemplateListService(
			WxaTemplateDomainInfoService wxaTemplateDomainInfoService,
			SuperadminWxappTemplateMetadataService superadminWxappTemplateMetadataService,
			Environment environment) {
		this.wxaTemplateDomainInfoService = wxaTemplateDomainInfoService;
		this.superadminWxappTemplateMetadataService = superadminWxappTemplateMetadataService;
		this.environment = environment;
	}

	public List<Map<String, Object>> getTemplateList(String authorizerAppid) {
		boolean fallbackOnly = Boolean.parseBoolean(
				environment.getProperty("ecshopx.wechat.template-list-use-fallback-only", "false"));
		Map<String, LinkedHashMap<String, Object>> templateByKey;
		boolean equivalentConfigWxaBranch;
		if (fallbackOnly) {
			templateByKey = loadTemplateMapFromConfiguredFallbackKeys();
			equivalentConfigWxaBranch = true;
		} else {
			templateByKey = wxaTemplateDomainInfoService.listWxappTemplatesMergedAsDataList();
			if (templateByKey == null || templateByKey.isEmpty()) {
				templateByKey = loadTemplateMapFromConfiguredFallbackKeys();
				equivalentConfigWxaBranch = true;
			} else {
				equivalentConfigWxaBranch = false;
			}
		}
		List<Map<String, Object>> list = new ArrayList<>();
		for (Map.Entry<String, LinkedHashMap<String, Object>> e : templateByKey.entrySet()) {
			String templateName = e.getKey();
			LinkedHashMap<String, Object> row = e.getValue();
			LinkedHashMap<String, Object> info = new LinkedHashMap<>(row);
			info.put("template_name", templateName);
			info.put("is_open", Boolean.TRUE);
			if (equivalentConfigWxaBranch) {
				list.add(trimToTemplateListResponseItem(info));
			} else {
				list.add(info);
			}
		}
		return list;
	}

	private LinkedHashMap<String, LinkedHashMap<String, Object>> loadTemplateMapFromConfiguredFallbackKeys() {
		String raw = environment.getProperty("ecshopx.wechat.wxa-template-list-fallback-keys", "");
		LinkedHashMap<String, LinkedHashMap<String, Object>> filled = new LinkedHashMap<>();
		if (raw != null && !raw.isEmpty()) {
			for (String segment : raw.split(",")) {
				String k = segment == null ? "" : segment.trim();
				if (k.isEmpty()) {
					continue;
				}
				superadminWxappTemplateMetadataService
						.loadFallbackRowFromPropertiesOnly(k)
						.ifPresent(row -> filled.put(k, new LinkedHashMap<>(row)));
			}
		}
		return filled;
	}

	private LinkedHashMap<String, Object> trimToTemplateListResponseItem(LinkedHashMap<String, Object> info) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("key_name", info.get("key_name"));
		out.put("name", info.get("name"));
		out.put("tag", info.get("tag"));
		out.put("template_id", info.get("template_id"));
		out.put("version", info.containsKey("version") ? info.get("version") : null);
		out.put("desc", info.get("desc"));
		out.put("domain", copyDomainForListResponse(info.get("domain")));
		out.put("template_name", info.get("template_name"));
		out.put("is_open", info.get("is_open"));
		return out;
	}

	private Map<String, Object> copyDomainForListResponse(Object rawDomain) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		Map<?, ?> m = rawDomain instanceof Map ? (Map<?, ?>) rawDomain : Map.of();
		for (String k : LIST_DOMAIN_KEYS) {
			Object v = m.get(k);
			if (v instanceof List<?> list) {
				out.put(k, new ArrayList<>(list));
			} else {
				out.put(k, new ArrayList<>(wxaTemplateDomainInfoService.coerceDomainListProperty(v)));
			}
		}
		return out;
	}
}
