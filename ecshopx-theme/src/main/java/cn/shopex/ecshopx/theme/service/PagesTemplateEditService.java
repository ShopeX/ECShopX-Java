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

package cn.shopex.ecshopx.theme.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.theme.domain.PagesTemplate;
import cn.shopex.ecshopx.theme.mapper.PagesTemplateMapper;
import cn.shopex.ecshopx.wechat.domain.WeappSetting;
import cn.shopex.ecshopx.wechat.mapper.WeappSettingMapper;
import cn.shopex.ecshopx.wechat.support.WeappSettingLegacySerializeCodec;
import cn.shopex.ecshopx.wechat.support.WeappSettingOutsideLangParamsWriter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
@Service
@Transactional(rollbackFor = Exception.class)
public class PagesTemplateEditService {

	private static final String PAGE_NAME = "index";

	private static final String VERSION = "v1.0.2";

	private final PagesTemplateMapper pagesTemplateMapper;

	private final WeappSettingMapper weappSettingMapper;

	private final ObjectMapper objectMapper;

	private final WeappSettingOutsideLangParamsWriter weappSettingOutsideLangParamsWriter;

	public PagesTemplateEditService(
			PagesTemplateMapper pagesTemplateMapper,
			WeappSettingMapper weappSettingMapper,
			ObjectMapper objectMapper,
			WeappSettingOutsideLangParamsWriter weappSettingOutsideLangParamsWriter) {
		this.pagesTemplateMapper = pagesTemplateMapper;
		this.weappSettingMapper = weappSettingMapper;
		this.objectMapper = objectMapper;
		this.weappSettingOutsideLangParamsWriter = weappSettingOutsideLangParamsWriter;
	}

	public Map<String, Object> handleEdit(long companyId, JsonNode body) {
		if (body == null || body.isNull()) {
			body = objectMapper.createObjectNode();
		}
		if (!body.isObject()) {
			throw new BadRequestException("请求体格式错误");
		}
		Long pagesTemplateId = resolvePagesTemplateId(body);
		if (isMetaKeyPresent(body, "template_title")
				|| isMetaKeyPresent(body, "template_pic")
				|| isMetaKeyPresent(body, "regionauth_id")) {
			updateInfoBranch(companyId, pagesTemplateId, body);
			return Map.of("status", Boolean.TRUE);
		}
		editBranch(companyId, pagesTemplateId, body);
		return Map.of("status", Boolean.TRUE);
	}

	private void updateInfoBranch(long companyId, Long pagesTemplateId, JsonNode body) {
		String templateTitle = "";
		if (isMetaKeyPresent(body, "template_title")) {
			templateTitle = metaTextOrEmpty(body, "template_title");
		}
		String templatePic = "";
		if (isMetaKeyPresent(body, "template_pic")) {
			templatePic = metaTextOrEmpty(body, "template_pic");
		}
		long regionauthId = 0L;
		if (isMetaKeyPresent(body, "regionauth_id")) {
			regionauthId = parseRegionauthForUpdate(body);
		}
		int rows =
				pagesTemplateMapper.update(
						null,
						new LambdaUpdateWrapper<PagesTemplate>()
								.set(PagesTemplate::getTemplateTitle, templateTitle)
								.set(PagesTemplate::getTemplatePic, templatePic)
								.set(PagesTemplate::getRegionauthId, regionauthId)
								.eq(PagesTemplate::getPagesTemplateId, pagesTemplateId)
								.eq(PagesTemplate::getCompanyId, companyId)
								.isNull(PagesTemplate::getDeletedAt));
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据");
		}
	}

	private void editBranch(long companyId, Long pagesTemplateId, JsonNode body) {
		String templateName = resolveTemplateName(body);
		JsonNode templateContentRoot = parseTemplateContentRoot(body);
		Integer elementEditStatus = parseElementEditStatus(body);
		boolean weappSyncNeeded = isWeappSyncNeeded(templateContentRoot);
		if (weappSyncNeeded) {
			syncWeappSettings(companyId, pagesTemplateId, templateName, templateContentRoot);
		}
		String contentJson;
		try {
			if (templateContentRoot == null || templateContentRoot.isNull()) {
				contentJson = "null";
			} else {
				contentJson = objectMapper.writeValueAsString(templateContentRoot);
			}
		} catch (JsonProcessingException e) {
			throw new ResourceException("template_content 序列化失败");
		}
		var uw =
				new LambdaUpdateWrapper<PagesTemplate>()
						.eq(PagesTemplate::getPagesTemplateId, pagesTemplateId)
						.eq(PagesTemplate::getCompanyId, companyId)
						.isNull(PagesTemplate::getDeletedAt);
		uw.set(PagesTemplate::getTemplateContent, contentJson);
		uw.set(PagesTemplate::getElementEditStatus, elementEditStatus);
		int rows = pagesTemplateMapper.update(null, uw);
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据");
		}
	}

	private static String resolveTemplateName(JsonNode body) {
		if (!body.has("template_name") || body.get("template_name").isNull()) {
			return "yykweishop";
		}
		JsonNode n = body.get("template_name");
		if (!n.isTextual()) {
			throw new BadRequestException("缺少模板展示类型");
		}
		String templateName = n.asText().trim();
		if (!StringUtils.hasText(templateName)) {
			throw new BadRequestException("缺少模板展示类型");
		}
		return templateName;
	}

	private JsonNode parseTemplateContentRoot(JsonNode body) {
		if (!body.has("template_content")) {
			return null;
		}
		JsonNode raw = body.get("template_content");
		if (raw.isNull() || raw.isMissingNode()) {
			return null;
		}
		if (raw.isObject() || raw.isArray()) {
			return raw;
		}
		if (raw.isTextual()) {
			String text = raw.asText();
			if (!StringUtils.hasText(text)) {
				return null;
			}
			try {
				JsonNode parsed = objectMapper.readTree(text);
				if (parsed == null || parsed.isNull()) {
					return null;
				}
				return parsed;
			} catch (JsonProcessingException e) {
				return null;
			}
		}
		return null;
	}

	private static Integer parseElementEditStatus(JsonNode body) {
		JsonNode n = body.path("element_edit_status");
		if (n.isMissingNode() || n.isNull()) {
			return null;
		}
		if (!n.isIntegralNumber()) {
			throw new BadRequestException("element_edit_status 格式错误");
		}
		return n.intValue();
	}

	private boolean isWeappSyncNeeded(JsonNode decoded) {
		if (decoded == null || decoded.isNull()) {
			return false;
		}
		if (decoded.isArray()) {
			if (decoded.isEmpty()) {
				return false;
			}
			throw new ResourceException("template_content 结构错误：挂件同步需以 JSON 对象为根且包含 content 数组");
		}
		if (!decoded.isObject()) {
			return false;
		}
		if (decoded.isEmpty()) {
			return false;
		}
		if (!decoded.has("content")) {
			throw new ResourceException("template_content 结构错误：缺少 content 数组");
		}
		JsonNode content = decoded.get("content");
		if (content.isNull() || !content.isArray()) {
			throw new ResourceException("template_content 结构错误：content 须为数组");
		}
		return content.size() > 0;
	}

	private void syncWeappSettings(
			long companyId, Long pagesTemplateId, String templateName, JsonNode templateContentRoot) {
		JsonNode content = templateContentRoot.get("content");
		Integer pagesTemplateIdForDb =
				pagesTemplateId == null ? null : Math.toIntExact(pagesTemplateId);
		LambdaQueryWrapper<WeappSetting> q =
				new LambdaQueryWrapper<WeappSetting>()
						.eq(WeappSetting::getCompanyId, companyId)
						.eq(WeappSetting::getTemplateName, templateName)
						.eq(WeappSetting::getPageName, PAGE_NAME)
						.eq(WeappSetting::getVersion, VERSION)
						.orderByAsc(WeappSetting::getSortBy)
						.orderByAsc(WeappSetting::getId);
		if (pagesTemplateIdForDb == null) {
			q.isNull(WeappSetting::getPagesTemplateId);
		} else {
			q.eq(WeappSetting::getPagesTemplateId, pagesTemplateIdForDb);
		}
		List<WeappSetting> oldRows = weappSettingMapper.selectList(q);
		List<Long> oldIds = new ArrayList<>();
		if (oldRows != null) {
			for (WeappSetting r : oldRows) {
				if (r.getId() != null) {
					oldIds.add(r.getId());
				}
			}
		}
		Set<Long> editIds = new HashSet<>();
		for (int i = 0; i < content.size(); i++) {
			JsonNode item = content.get(i);
			if (item == null || !item.isObject()) {
				continue;
			}
			int sortIndex = i + 1;
			int rowId = parseRowId(item);
			LinkedHashMap<String, Object> rowMap = rowToParamsMap(item);
			if (rowId > 0) {
				WeappSetting existing = weappSettingMapper.selectById((long) rowId);
				if (existing == null
						|| !Long.valueOf(companyId).equals(existing.getCompanyId())
						|| !templateName.equals(existing.getTemplateName())
						|| !PAGE_NAME.equals(existing.getPageName())
						|| !VERSION.equals(existing.getVersion())
						|| !weappPagesTemplateMatches(existing.getPagesTemplateId(), pagesTemplateIdForDb)) {
					throw new BadRequestException("挂件 id 与模板不匹配");
				}
				Object decoded;
				try {
					decoded = WeappSettingLegacySerializeCodec.decode(existing.getParams());
				} catch (RuntimeException ex) {
					throw new ResourceException("装修配置格式无效");
				}
				LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
				if (decoded instanceof Map<?, ?> dm) {
					for (Map.Entry<?, ?> e : dm.entrySet()) {
						merged.put(String.valueOf(e.getKey()), e.getValue());
					}
				}
				merged.putAll(rowMap);
				if (item.has("name") && item.get("name").isTextual()) {
					existing.setName(item.get("name").asText());
				}
				String serializedParams = WeappSettingLegacySerializeCodec.serialize(merged);
				existing.setParams(serializedParams);
				existing.setSortBy(sortIndex);
				weappSettingMapper.updateById(existing);
				weappSettingOutsideLangParamsWriter.syncDefaultLangParams(
						companyId, existing.getId(), serializedParams);
				editIds.add(existing.getId());
			} else {
				WeappSetting ins = new WeappSetting();
				ins.setCompanyId(companyId);
				ins.setTemplateName(templateName);
				ins.setPageName(PAGE_NAME);
				ins.setName(item.has("name") && item.get("name").isTextual() ? item.get("name").asText() : "");
				ins.setVersion(VERSION);
				ins.setPagesTemplateId(pagesTemplateIdForDb);
				ins.setSortBy(sortIndex);
				String initialParams = WeappSettingLegacySerializeCodec.serialize(rowMap);
				ins.setParams(initialParams);
				weappSettingMapper.insert(ins);
				Long newId = ins.getId();
				if (newId == null) {
					throw new ResourceException("挂件保存失败");
				}
				rowMap.put("id", newId.intValue());
				String serializedParams = WeappSettingLegacySerializeCodec.serialize(rowMap);
				ins.setParams(serializedParams);
				weappSettingMapper.updateById(ins);
				weappSettingOutsideLangParamsWriter.syncDefaultLangParams(
						companyId, newId, serializedParams);
				editIds.add(newId);
			}
		}
		List<Long> diffIds = new ArrayList<>();
		for (Long oid : oldIds) {
			if (!editIds.contains(oid)) {
				diffIds.add(oid);
			}
		}
		if (diffIds.isEmpty()) {
			return;
		}
		weappSettingOutsideLangParamsWriter.deleteLangParamsForWeappSettingIds(companyId, diffIds);
		weappSettingMapper.delete(
				new LambdaQueryWrapper<WeappSetting>()
						.eq(WeappSetting::getCompanyId, companyId)
						.in(WeappSetting::getId, diffIds));
	}

	private static int parseRowId(JsonNode item) {
		if (!item.has("id") || item.get("id").isNull()) {
			return 0;
		}
		JsonNode idN = item.get("id");
		if (idN.isIntegralNumber()) {
			return idN.intValue();
		}
		if (idN.isTextual()) {
			try {
				return Integer.parseInt(idN.asText().trim());
			} catch (NumberFormatException e) {
				return 0;
			}
		}
		return 0;
	}

	private LinkedHashMap<String, Object> rowToParamsMap(JsonNode item) {
		try {
			return objectMapper.convertValue(item, new TypeReference<LinkedHashMap<String, Object>>() {});
		} catch (IllegalArgumentException e) {
			throw new BadRequestException("挂件配置格式错误");
		}
	}

	private static boolean isMetaKeyPresent(JsonNode body, String key) {
		if (body == null || !body.isObject() || !body.has(key)) {
			return false;
		}
		JsonNode v = body.get(key);
		return !(v.isBoolean() && !v.booleanValue());
	}

	private static String metaTextOrEmpty(JsonNode body, String key) {
		JsonNode n = body.get(key);
		if (n == null || n.isNull()) {
			return "";
		}
		String t = n.asText();
		return t != null ? t : "";
	}

	private long parseRegionauthForUpdate(JsonNode body) {
		JsonNode n = body.get("regionauth_id");
		if (n == null || n.isNull()) {
			return 0L;
		}
		if (n.isIntegralNumber()) {
			return n.longValue();
		}
		if (n.isFloatingPointNumber()) {
			return (long) n.asDouble();
		}
		if (n.isTextual()) {
			try {
				return Long.parseLong(n.asText().trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("regionauth_id 无效");
			}
		}
		throw new BadRequestException("regionauth_id 无效");
	}

	private static Long resolvePagesTemplateId(JsonNode body) {
		if (body == null || !body.isObject() || !body.has("pages_template_id")) {
			return null;
		}
		JsonNode n = body.get("pages_template_id");
		if (n == null || n.isNull() || n.isMissingNode()) {
			return null;
		}
		if (n.isIntegralNumber()) {
			return n.longValue();
		}
		if (n.isFloatingPointNumber()) {
			double d = n.asDouble();
			if (Double.isNaN(d) || Double.isInfinite(d)) {
				return null;
			}
			long lv = (long) d;
			if (lv != d) {
				return null;
			}
			return lv;
		}
		if (n.isTextual()) {
			String s = n.asText().trim();
			if (!StringUtils.hasText(s)) {
				return null;
			}
			try {
				return Long.parseLong(s);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	private static boolean weappPagesTemplateMatches(Integer existingPid, Integer expectedPid) {
		return Objects.equals(existingPid, expectedPid);
	}
}
