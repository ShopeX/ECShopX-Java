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

import cn.shopex.ecshopx.superadmin.domain.WxappTemplate;
import cn.shopex.ecshopx.superadmin.mapper.WxappTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxaGetTemplateWeappListService {

	private static final List<String> DOMAIN_KEYS = List.of(
			"requestdomain",
			"wsrequestdomain",
			"uploaddomain",
			"downloaddomain",
			"webviewdomain");

	private final WxappTemplateMapper wxappTemplateMapper;
	private final WxaAuthorizerListService wxaAuthorizerListService;

	@Value("${ecshopx.wechat.wxa-template-list-fallback-keys:}")
	private String wxaTemplateListFallbackKeys;

	public WxaGetTemplateWeappListService(
			WxappTemplateMapper wxappTemplateMapper,
			WxaAuthorizerListService wxaAuthorizerListService) {
		this.wxappTemplateMapper = wxappTemplateMapper;
		this.wxaAuthorizerListService = wxaAuthorizerListService;
	}

	public List<Map<String, Object>> getTemplateWeappList(long companyId) {
		LinkedHashMap<String, LinkedHashMap<String, Object>> templateByKeyName = new LinkedHashMap<>();
		String raw = wxaTemplateListFallbackKeys == null ? "" : wxaTemplateListFallbackKeys.trim();
		if (raw.isEmpty()) {
			raw = "yykweishop";
		}
		for (String part : raw.split(",")) {
			if (part == null) {
				continue;
			}
			String key = part.trim();
			if (key.isEmpty()) {
				continue;
			}
			WxappTemplate row = loadOneByKeyName(key);
			LinkedHashMap<String, Object> meta = new LinkedHashMap<>();
			meta.put("key_name", key);
			if (row != null) {
				meta.put("template_id", row.getTemplateId() == null ? 0 : row.getTemplateId());
				meta.put("name", pickDisplayName(row));
			} else {
				meta.put("template_id", 0);
				meta.put("name", "");
			}
			templateByKeyName.put(key, meta);
		}

		List<Map<String, Object>> authorizerList = wxaAuthorizerListService.getWxaList(companyId);
		Map<String, Map<String, Object>> indexAuthorizer = new LinkedHashMap<>();
		for (Map<String, Object> value : authorizerList) {
			String keyTemplateName = "";
			Object wt = value.get("weappTemplate");
			if (wt instanceof Map<?, ?> wtm) {
				Object kn = wtm.get("key_name");
				keyTemplateName = kn == null ? "" : String.valueOf(kn).trim();
			}
			indexAuthorizer.put(keyTemplateName, value);
		}

		List<Map<String, Object>> result = new ArrayList<>();
		for (Map.Entry<String, LinkedHashMap<String, Object>> entry : templateByKeyName.entrySet()) {
			String templateName = entry.getKey();
			Map<String, Object> value = entry.getValue();
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("template_id", value.get("template_id"));
			row.put("domain", emptyDomainStructure());
			row.put("name", value.get("name"));
			row.put("key_name", value.get("key_name"));
			Map<String, Object> auth = indexAuthorizer.get(templateName);
			row.put("authorizer", auth != null ? auth : new LinkedHashMap<String, Object>());
			result.add(row);
		}
		return result;
	}

	private WxappTemplate loadOneByKeyName(String key) {
		LambdaQueryWrapper<WxappTemplate> w = new LambdaQueryWrapper<>();
		w.eq(WxappTemplate::getKeyName, key).last("LIMIT 1");
		return wxappTemplateMapper.selectOne(w);
	}

	private static String pickDisplayName(WxappTemplate row) {
		String tag = row.getTag();
		if (StringUtils.hasText(tag)) {
			return tag.trim();
		}
		String name = row.getName();
		return name == null ? "" : name;
	}

	private static LinkedHashMap<String, Object> emptyDomainStructure() {
		LinkedHashMap<String, Object> d = new LinkedHashMap<>();
		for (String col : DOMAIN_KEYS) {
			d.put(col, new ArrayList<>());
		}
		return d;
	}
}
