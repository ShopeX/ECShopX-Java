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
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.companys.service.CommonLangModWriteService;
import cn.shopex.ecshopx.theme.api.admin.v1.dto.PcTemplateHeaderFooterSaveRequest;
import cn.shopex.ecshopx.theme.domain.ThemePcTemplateContent;
import cn.shopex.ecshopx.theme.mapper.ThemePcTemplateContentMapper;
import cn.shopex.ecshopx.theme.support.ThemePcTemplateContentRowMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class PcTemplateHeaderFooterSaveService {

	private static final String TABLE = "theme_pc_template_content";
	private static final String MODULE = "theme_pc_template_content";

	private final ThemePcTemplateContentMapper themePcTemplateContentMapper;
	private final LangueProperties langueProperties;
	private final CommonLangModWriteService commonLangModWriteService;
	private final ThemePcTemplateContentRowMapper themePcTemplateContentRowMapper;
	private final ObjectMapper objectMapper;

	public Map<String, Object> saveHeaderOrFooter(
			long companyId, String requestLang, PcTemplateHeaderFooterSaveRequest req) {
		String raw = req.getPageName();
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			throw new ResourceException("未查询到更新数据");
		}
		String pageName = raw.trim();
		if (pageName.length() > 20) {
			throw new BadRequestException("page_name 长度不能超过 20");
		}

		String paramsString = resolveParamsString(req.getConfig());

		LambdaQueryWrapper<ThemePcTemplateContent> w =
				new LambdaQueryWrapper<ThemePcTemplateContent>()
						.eq(ThemePcTemplateContent::getCompanyId, companyId)
						.eq(ThemePcTemplateContent::getName, pageName)
						.orderByAsc(ThemePcTemplateContent::getThemePcTemplateContentId)
						.last("LIMIT 1");
		List<ThemePcTemplateContent> list = themePcTemplateContentMapper.selectList(w);
		ThemePcTemplateContent existing = list.isEmpty() ? null : list.get(0);

		int now = (int) (System.currentTimeMillis() / 1000L);
		Long contentId;

		if (langueProperties.isDefaultLang(requestLang)) {
			if (existing == null) {
				contentId = insertDefaultLang(companyId, pageName, paramsString, now);
			} else {
				updateDefaultLang(existing, paramsString, now);
				contentId = existing.getThemePcTemplateContentId();
			}
			// PHP RepositoryLangInterceptor: default-lang save also syncs outside_item_multi_lang_mod_lang_*.
			syncDefaultLangMod(companyId, contentId, pageName, paramsString, requestLang);
		} else {
			if (existing == null) {
				contentId = insertNonDefaultLang(companyId, pageName, paramsString, requestLang, now);
			} else {
				updateNonDefaultLang(companyId, paramsString, requestLang, now, existing);
				contentId = existing.getThemePcTemplateContentId();
			}
		}

		ThemePcTemplateContent fresh = themePcTemplateContentMapper.selectById(contentId);
		if (fresh == null) {
			throw new ResourceException("未查询到更新数据");
		}
		return themePcTemplateContentRowMapper.toRowMap(fresh);
	}

	private String resolveParamsString(JsonNode configNode) {
		if (configNode == null || configNode.isNull()) {
			return null;
		}
		if (configNode.isTextual()) {
			return configNode.asText();
		}
		try {
			return objectMapper.writeValueAsString(configNode);
		} catch (JsonProcessingException ex) {
			throw new BadRequestException("config 不是合法 JSON");
		}
	}

	private Long insertDefaultLang(long companyId, String pageName, String paramsString, int now) {
		ThemePcTemplateContent entity = new ThemePcTemplateContent();
		entity.setCompanyId(companyId);
		entity.setThemePcTemplateId(0L);
		entity.setName(pageName);
		entity.setParams(paramsString);
		entity.setSortBy(null);
		entity.setCreated(now);
		entity.setUpdated(now);
		int rows = themePcTemplateContentMapper.insert(entity);
		if (rows != 1 || entity.getThemePcTemplateContentId() == null) {
			throw new ResourceException("未查询到更新数据");
		}
		return entity.getThemePcTemplateContentId();
	}

	private Long insertNonDefaultLang(
			long companyId, String pageName, String paramsString, String requestLang, int now) {
		ThemePcTemplateContent entity = new ThemePcTemplateContent();
		entity.setCompanyId(companyId);
		entity.setThemePcTemplateId(0L);
		entity.setName(pageName);
		entity.setParams(null);
		entity.setSortBy(null);
		entity.setCreated(now);
		entity.setUpdated(now);
		int rows = themePcTemplateContentMapper.insert(entity);
		if (rows != 1 || entity.getThemePcTemplateContentId() == null) {
			throw new ResourceException("未查询到更新数据");
		}
		Map<String, String> langBag = new HashMap<>();
		langBag.put("name", pageName);
		langBag.put("params", paramsString == null ? "" : paramsString);
		commonLangModWriteService.saveLang(
				(int) companyId,
				langBag,
				TABLE,
				entity.getThemePcTemplateContentId().intValue(),
				MODULE,
				requestLang);
		return entity.getThemePcTemplateContentId();
	}

	private void updateDefaultLang(ThemePcTemplateContent existing, String paramsString, int now) {
		Long id = existing.getThemePcTemplateContentId();
		if (id == null) {
			throw new ResourceException("未查询到更新数据");
		}
		ThemePcTemplateContent patch = new ThemePcTemplateContent();
		patch.setThemePcTemplateContentId(id);
		patch.setParams(paramsString);
		patch.setUpdated(now);
		int affected = themePcTemplateContentMapper.updateById(patch);
		if (affected != 1) {
			throw new ResourceException("未查询到更新数据");
		}
	}

	private void syncDefaultLangMod(
			long companyId,
			long contentId,
			String pageName,
			String paramsString,
			String requestLang) {
		Map<String, String> langBag = new HashMap<>();
		if (StringUtils.hasText(pageName)) {
			langBag.put("name", pageName);
		}
		if (StringUtils.hasText(paramsString)) {
			langBag.put("params", paramsString);
		}
		if (langBag.isEmpty()) {
			return;
		}
		commonLangModWriteService.updateLangData(
				(int) companyId,
				langBag,
				TABLE,
				(int) contentId,
				MODULE,
				requestLang);
	}

	private void updateNonDefaultLang(
			long companyId,
			String paramsString,
			String requestLang,
			int now,
			ThemePcTemplateContent existing) {
		Long id = existing.getThemePcTemplateContentId();
		if (id == null) {
			throw new ResourceException("未查询到更新数据");
		}
		LambdaUpdateWrapper<ThemePcTemplateContent> uw =
				new LambdaUpdateWrapper<ThemePcTemplateContent>()
						.eq(ThemePcTemplateContent::getThemePcTemplateContentId, id)
						.set(ThemePcTemplateContent::getParams, null)
						.set(ThemePcTemplateContent::getUpdated, now);
		int affected = themePcTemplateContentMapper.update(null, uw);
		if (affected != 1) {
			throw new ResourceException("未查询到更新数据");
		}
		Map<String, String> langBag = new HashMap<>();
		langBag.put("params", paramsString == null ? "" : paramsString);
		commonLangModWriteService.updateLangData(
				(int) companyId,
				langBag,
				TABLE,
				existing.getThemePcTemplateContentId().intValue(),
				MODULE,
				requestLang);
	}
}
