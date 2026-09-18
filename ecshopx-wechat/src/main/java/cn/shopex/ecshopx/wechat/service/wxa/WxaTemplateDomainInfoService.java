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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.superadmin.domain.WxappTemplate;
import cn.shopex.ecshopx.superadmin.mapper.WxappTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxaTemplateDomainInfoService {

	private static final TypeReference<Map<String, Object>> DOMAIN_JSON_TYPE = new TypeReference<>() {};

	private static final List<String> DOMAIN_COLUMNS = List.of(
			"requestdomain",
			"wsrequestdomain",
			"uploaddomain",
			"downloaddomain",
			"webviewdomain");

	private final WxappTemplateMapper wxappTemplateMapper;
	private final ObjectMapper objectMapper;
	private final Environment environment;
	private final StringRedisTemplate sharedStringRedisTemplate;
	private final boolean systemIsSaas;

	public WxaTemplateDomainInfoService(
			WxappTemplateMapper wxappTemplateMapper,
			ObjectMapper objectMapper,
			Environment environment,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			@Value("${common.system-is-saas:false}") boolean systemIsSaas) {
		this.wxappTemplateMapper = wxappTemplateMapper;
		this.objectMapper = objectMapper;
		this.environment = environment;
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.systemIsSaas = systemIsSaas;
	}

	public void setDomain(WxappTemplateDomainRedisPayload payload) {
		if (systemIsSaas) {
			throw new ResourceException("当前系统无权限");
		}
		String rawRequest = payload.requestdomain();
		if (rawRequest == null) {
			throw new ResourceException("request合法域名必填");
		}
		String requestTrimmed = rawRequest.trim();
		if (requestTrimmed.isEmpty() || "0".equals(requestTrimmed)) {
			throw new ResourceException("request合法域名必填");
		}
		String wsTrimmed = payload.wsrequestdomain() == null ? "" : payload.wsrequestdomain().trim();
		String uploadTrimmed = payload.uploaddomain() == null ? "" : payload.uploaddomain().trim();
		String downloadTrimmed = payload.downloaddomain() == null ? "" : payload.downloaddomain().trim();
		String webviewTrimmed = payload.webviewdomain() == null ? "" : payload.webviewdomain().trim();
		String[] values = new String[] {
				requestTrimmed,
				wsTrimmed,
				uploadTrimmed,
				downloadTrimmed,
				webviewTrimmed
		};
		for (int i = 0; i < DOMAIN_COLUMNS.size(); i++) {
			String col = DOMAIN_COLUMNS.get(i);
			sharedStringRedisTemplate.opsForValue().set(redisKeyForColumn(col), values[i]);
		}
	}

	public LinkedHashMap<String, String> getDomain() {
		LinkedHashMap<String, String> data = new LinkedHashMap<>();
		for (String col : DOMAIN_COLUMNS) {
			String v = sharedStringRedisTemplate.opsForValue().get(redisKeyForColumn(col));
			data.put(col, v == null ? "" : v.trim());
		}
		return data;
	}

	public LinkedHashMap<String, Object> requireEnabledTemplateDetailByTemplateId(Integer templateId) {
		LambdaQueryWrapper<WxappTemplate> w = new LambdaQueryWrapper<>();
		w.eq(WxappTemplate::getTemplateId, templateId)
				.eq(WxappTemplate::getIsDisabled, false)
				.last("LIMIT 1");
		WxappTemplate row = wxappTemplateMapper.selectOne(w);
		if (row == null) {
			throw new ResourceException("小程序模板不存在或已禁用");
		}
		return toTemplateRowMap(row);
	}

	public Map<String, Object> buildLocalDomainStructure(String templateName) {
		String key = templateName == null ? "" : templateName.trim();
		LambdaQueryWrapper<WxappTemplate> w = new LambdaQueryWrapper<>();
		w.eq(WxappTemplate::getKeyName, key).eq(WxappTemplate::getIsDisabled, false).last("LIMIT 1");
		WxappTemplate row = wxappTemplateMapper.selectOne(w);

		if (row == null) {
			return buildFromEnvironmentFallback(key);
		}
		if (StringUtils.hasText(row.getDomain())) {
			Map<String, Object> domainMap = readDomainJsonOrEmpty(row.getDomain().trim());
			mergeFiveColumnsWithRedis(domainMap);
			return domainMap;
		}
		Map<String, Object> domainMap = new LinkedHashMap<>();
		mergeFiveColumnsWithRedis(domainMap);
		return domainMap;
	}

	private Map<String, Object> buildFromEnvironmentFallback(String normalizedKey) {
		String lc = normalizedKey.toLowerCase(Locale.ROOT);
		String base = "ecshopx.wechat.wxa-template-fallback." + lc + ".";
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
			return new LinkedHashMap<>();
		}
		Map<String, Object> domainMap;
		try {
			domainMap = objectMapper.readValue(domainStr.trim(), DOMAIN_JSON_TYPE);
		} catch (Exception e) {
			domainMap = new LinkedHashMap<>();
		}
		return domainMap != null ? domainMap : new LinkedHashMap<>();
	}

	private Map<String, Object> readDomainJsonOrEmpty(String json) {
		try {
			Map<String, Object> m = objectMapper.readValue(json, DOMAIN_JSON_TYPE);
			return m != null ? m : new LinkedHashMap<>();
		} catch (Exception e) {
			return new LinkedHashMap<>();
		}
	}

	/** 模板表一行转列表 Map；{@code domain} 经 Redis 五列合并。 */
	LinkedHashMap<String, Object> toTemplateRowMap(WxappTemplate row) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", row.getId());
		m.put("key_name", row.getKeyName());
		m.put("name", row.getName());
		m.put("tag", row.getTag());
		m.put("template_id", row.getTemplateId() == null ? 0 : row.getTemplateId());
		m.put("template_id_2", row.getTemplateId2() == null ? 0 : row.getTemplateId2());
		m.put("version", row.getVersion());
		m.put("is_only", Boolean.TRUE.equals(row.getIsOnly()));
		m.put("description", row.getDescription());
		Map<String, Object> domainMap;
		if (StringUtils.hasText(row.getDomain())) {
			domainMap = readDomainJsonOrEmpty(row.getDomain().trim());
		} else {
			domainMap = new LinkedHashMap<>();
		}
		mergeFiveColumnsWithRedis(domainMap);
		m.put("domain", domainMap);
		m.put("is_disabled", Boolean.TRUE.equals(row.getIsDisabled()));
		m.put("created", row.getCreated());
		m.put("updated", row.getUpdated());
		m.put("desc", row.getDescription());
		return m;
	}

	public Map<String, LinkedHashMap<String, Object>> listWxappTemplatesMergedAsDataList() {
		LambdaQueryWrapper<WxappTemplate> w = new LambdaQueryWrapper<>();
		w.eq(WxappTemplate::getIsDisabled, false)
				.orderByDesc(WxappTemplate::getCreated)
				.last("LIMIT 100");
		List<WxappTemplate> rows = wxappTemplateMapper.selectList(w);
		LinkedHashMap<String, LinkedHashMap<String, Object>> result = new LinkedHashMap<>();
		if (rows == null) {
			return result;
		}
		for (WxappTemplate row : rows) {
			String keyName = row.getKeyName();
			if (keyName == null || keyName.trim().isEmpty()) {
				continue;
			}
			result.put(keyName.trim(), toTemplateRowMap(row));
		}
		return result;
	}

	/** Coerce a JSON domain column value to a string list (same rules as Redis merge). */
	List<String> coerceDomainListProperty(Object raw) {
		return extractTokenList(raw);
	}

	void mergeFiveColumnsWithRedis(Map<String, Object> domainMap) {
		for (String col : DOMAIN_COLUMNS) {
			List<String> oldList = extractTokenList(domainMap.get(col));
			List<String> newList = splitRedisTokens(sharedStringRedisTemplate.opsForValue().get(redisKeyForColumn(col)));
			LinkedHashSet<String> merged = new LinkedHashSet<>();
			merged.addAll(oldList);
			merged.addAll(newList);
			domainMap.put(col, new ArrayList<>(merged));
		}
	}

	private static String redisKeyForColumn(String col) {
		return switch (col) {
			case "requestdomain" -> "wxappTemplateRequestdomain";
			case "wsrequestdomain" -> "wxappTemplateWsrequestdomain";
			case "uploaddomain" -> "wxappTemplateUploaddomain";
			case "downloaddomain" -> "wxappTemplateDownloaddomain";
			case "webviewdomain" -> "wxappTemplateWebViewdomain";
			default -> "";
		};
	}

	private static List<String> extractTokenList(Object raw) {
		List<String> out = new ArrayList<>();
		if (raw == null) {
			return out;
		}
		if (raw instanceof String s) {
			return splitWhitespaceTokens(normalizeNewlinesToSpace(s));
		}
		if (raw instanceof List<?> list) {
			for (Object o : list) {
				if (o == null) {
					continue;
				}
				String t = String.valueOf(o).trim();
				if (StringUtils.hasText(t)) {
					out.add(t);
				}
			}
			return out;
		}
		return out;
	}

	private static List<String> splitRedisTokens(String redisRaw) {
		if (!StringUtils.hasText(redisRaw)) {
			return new ArrayList<>();
		}
		return splitWhitespaceTokens(normalizeNewlinesToSpace(redisRaw));
	}

	private static String normalizeNewlinesToSpace(String s) {
		return s.replaceAll("\\R+", " ").trim();
	}

	private static List<String> splitWhitespaceTokens(String normalized) {
		List<String> out = new ArrayList<>();
		if (!StringUtils.hasText(normalized)) {
			return out;
		}
		for (String part : normalized.split("\\s+")) {
			if (StringUtils.hasText(part)) {
				out.add(part);
			}
		}
		return out;
	}
}
