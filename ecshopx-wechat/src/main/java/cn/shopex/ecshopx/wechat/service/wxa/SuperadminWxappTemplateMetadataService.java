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
import cn.shopex.ecshopx.superadmin.domain.WxappTemplate;
import cn.shopex.ecshopx.superadmin.mapper.WxappTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SuperadminWxappTemplateMetadataService {

	private static final TypeReference<Map<String, Object>> DOMAIN_TYPE = new TypeReference<>() {};

	private static final List<String> DOMAIN_KEYS = List.of(
			"requestdomain",
			"wsrequestdomain",
			"uploaddomain",
			"downloaddomain",
			"webviewdomain");

	private final WxappTemplateMapper wxappTemplateMapper;
	private final ObjectMapper objectMapper;
	private final Environment environment;
	private final WxaTemplateDomainInfoService wxaTemplateDomainInfoService;

	public SuperadminWxappTemplateMetadataService(
			WxappTemplateMapper wxappTemplateMapper,
			ObjectMapper objectMapper,
			Environment environment,
			WxaTemplateDomainInfoService wxaTemplateDomainInfoService) {
		this.wxappTemplateMapper = wxappTemplateMapper;
		this.objectMapper = objectMapper;
		this.environment = environment;
		this.wxaTemplateDomainInfoService = wxaTemplateDomainInfoService;
	}

	public Map<String, Object> resolveTemplateRow(String templateKey) {
		String normalized = templateKey == null ? "" : templateKey.trim();
		Map<String, Object> row = resolveTemplateRowOrNull(normalized);
		if (row != null) {
			return row;
		}
		throw new BadRequestException("选择的小程序模板不存在");
	}

	private Map<String, Object> resolveTemplateRowOrNull(String normalized) {
		Map<String, Object> fromDb = loadFromDb(normalized);
		if (fromDb != null) {
			return fromDb;
		}
		return loadFromProperties(normalized);
	}

	public void assertValidWxappTemplateForPageParams(String templateKey) {
		String t = templateKey == null ? "" : templateKey.trim();
		if ("pc".equals(t) || "h5".equals(t) || "app".equals(t)) {
			return;
		}
		if (resolveTemplateRowOrNull(t) == null) {
			throw new BadRequestException("小程序模板不存在");
		}
	}

	private Map<String, Object> loadFromDb(String normalized) {
		if (!StringUtils.hasText(normalized)) {
			return null;
		}
		LambdaQueryWrapper<WxappTemplate> w = new LambdaQueryWrapper<>();
		w.eq(WxappTemplate::getKeyName, normalized).last("LIMIT 1");
		WxappTemplate row = wxappTemplateMapper.selectOne(w);
		if (row == null) {
			return null;
		}
		return wxaTemplateDomainInfoService.toTemplateRowMap(row);
	}

	public Optional<Map<String, Object>> tryResolveTemplateRowForAuthorizerList(String templateKey) {
		String normalized = templateKey == null ? "" : templateKey.trim();
		if (!StringUtils.hasText(normalized)) {
			return Optional.empty();
		}
		LambdaQueryWrapper<WxappTemplate> w = new LambdaQueryWrapper<>();
		w.eq(WxappTemplate::getKeyName, normalized).last("LIMIT 1");
		WxappTemplate row = wxappTemplateMapper.selectOne(w);
		if (row != null) {
			return Optional.of(wxaTemplateDomainInfoService.toTemplateRowMap(row));
		}
		return buildRowFromWxaTemplateFallbackProperties(normalized);
	}

	public Optional<Map<String, Object>> loadFallbackRowFromPropertiesOnly(String templateKey) {
		String normalized = templateKey == null ? "" : templateKey.trim();
		if (!StringUtils.hasText(normalized)) {
			return Optional.empty();
		}
		return buildRowFromWxaTemplateFallbackProperties(normalized);
	}

	private boolean hasAnyFallbackProperty(String base) {
		return environment.getProperty(base + "template-id") != null
				|| environment.getProperty(base + "version") != null
				|| environment.getProperty(base + "desc") != null
				|| environment.getProperty(base + "tag") != null
				|| environment.getProperty(base + "name") != null
				|| environment.getProperty(base + "domain") != null;
	}

	private LinkedHashMap<String, Object> newEmptyFiveDomainMap() {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		for (String col : DOMAIN_KEYS) {
			m.put(col, new ArrayList<String>());
		}
		return m;
	}

	private Map<String, Object> normalizeParsedDomainMap(Map<String, Object> parsed) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		for (String col : DOMAIN_KEYS) {
			Object raw = parsed != null ? parsed.get(col) : null;
			m.put(col, new ArrayList<>(wxaTemplateDomainInfoService.coerceDomainListProperty(raw)));
		}
		return m;
	}

	private Optional<Map<String, Object>> buildRowFromWxaTemplateFallbackProperties(String normalized) {
		String key = normalized.toLowerCase(Locale.ROOT);
		String base = "ecshopx.wechat.wxa-template-fallback." + key + ".";
		if (!hasAnyFallbackProperty(base)) {
			return Optional.empty();
		}
		String tid = environment.getProperty(base + "template-id");
		String ver = environment.getProperty(base + "version");
		String desc = environment.getProperty(base + "desc");
		String tag = environment.getProperty(base + "tag");
		String name = environment.getProperty(base + "name");
		String domainStr = environment.getProperty(base + "domain");
		int templateIdInt = 0;
		if (StringUtils.hasText(tid)) {
			try {
				templateIdInt = Integer.parseInt(tid.trim());
			} catch (NumberFormatException e) {
				templateIdInt = 0;
			}
		}
		Object versionValue;
		if (!StringUtils.hasText(ver)) {
			versionValue = null;
		} else {
			versionValue = ver.trim();
		}
		String descVal = StringUtils.hasText(desc) ? desc.trim() : "";
		String tagVal = StringUtils.hasText(tag) ? tag.trim() : "";
		String nameVal = StringUtils.hasText(name) ? name.trim() : "";
		Map<String, Object> domainMap;
		if (!StringUtils.hasText(domainStr)) {
			domainMap = newEmptyFiveDomainMap();
		} else {
			try {
				Map<String, Object> parsed = objectMapper.readValue(domainStr.trim(), DOMAIN_TYPE);
				if (parsed == null || parsed.isEmpty()) {
					domainMap = newEmptyFiveDomainMap();
				} else {
					domainMap = normalizeParsedDomainMap(parsed);
				}
			} catch (Exception e) {
				domainMap = newEmptyFiveDomainMap();
			}
		}
		boolean skipRedisMerge = Boolean.parseBoolean(
				environment.getProperty("ecshopx.wechat.wxa-template-fallback.skip-redis-domain-merge", "false"));
		if (!skipRedisMerge) {
			wxaTemplateDomainInfoService.mergeFiveColumnsWithRedis(domainMap);
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("id", null);
		out.put("key_name", normalized);
		out.put("name", nameVal);
		out.put("tag", tagVal);
		out.put("template_id", templateIdInt);
		out.put("template_id_2", 0);
		out.put("version", versionValue);
		out.put("is_only", Boolean.FALSE);
		out.put("description", descVal);
		out.put("domain", domainMap);
		out.put("is_disabled", Boolean.FALSE);
		out.put("created", null);
		out.put("updated", null);
		out.put("desc", descVal);
		return Optional.of(out);
	}

	private Map<String, Object> loadFromProperties(String normalized) {
		String key = normalized.toLowerCase(Locale.ROOT);
		String base = "ecshopx.wechat.wxa-template-fallback." + key + ".";
		String tid = environment.getProperty(base + "template-id");
		String ver = environment.getProperty(base + "version");
		String desc = environment.getProperty(base + "desc");
		String tag = environment.getProperty(base + "tag");
		String domainStr = environment.getProperty(base + "domain");
		if (!StringUtils.hasText(tid)
				|| !StringUtils.hasText(ver)
				|| !StringUtils.hasText(desc)
				|| !StringUtils.hasText(tag)
				|| !StringUtils.hasText(domainStr)) {
			return null;
		}
		return buildRow(tid.trim(), ver.trim(), desc.trim(), tag.trim(), domainStr.trim());
	}

	private Map<String, Object> buildRow(String templateId, String version, String desc, String tag, String domainStr) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("template_id", templateId);
		m.put("version", version == null ? "" : version);
		m.put("desc", desc == null ? "" : desc);
		m.put("tag", tag == null ? "" : tag);
		if (!StringUtils.hasText(domainStr)) {
			throw new BadRequestException("选择的小程序模板不存在");
		}
		try {
			m.put("domain", objectMapper.readValue(domainStr.trim(), DOMAIN_TYPE));
		} catch (Exception e) {
			throw new BadRequestException("选择的小程序模板不存在");
		}
		return m;
	}
}
