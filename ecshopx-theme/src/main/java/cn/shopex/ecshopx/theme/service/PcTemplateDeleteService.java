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
import cn.shopex.ecshopx.theme.domain.ThemePcTemplate;
import cn.shopex.ecshopx.theme.domain.ThemePcTemplateContent;
import cn.shopex.ecshopx.theme.mapper.ThemePcTemplateContentMapper;
import cn.shopex.ecshopx.theme.mapper.ThemePcTemplateMapper;
import cn.shopex.ecshopx.theme.support.PcTemplateContentPhysicalIdReader;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PcTemplateDeleteService {

	private final ThemePcTemplateMapper themePcTemplateMapper;
	private final ThemePcTemplateContentMapper themePcTemplateContentMapper;
	private final CommonLangModWriteService commonLangModWriteService;
	private final LangueProperties langueProperties;
	private final PcTemplateContentPhysicalIdReader pcTemplateContentPhysicalIdReader;

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> delete(long companyId, String themePcTemplateIdPath) {
		String p = themePcTemplateIdPath == null ? "" : themePcTemplateIdPath.trim();
		if (!StringUtils.hasText(p)) {
			throw new BadRequestException("缺少theme_pc_template_id");
		}
		long themePcTemplateId;
		try {
			themePcTemplateId = Long.parseLong(p);
		} catch (NumberFormatException ex) {
			throw new ResourceException("模板页面不存在");
		}
		if (themePcTemplateId <= 0L) {
			throw new ResourceException("模板页面不存在");
		}

		ThemePcTemplate row =
				themePcTemplateMapper.selectOne(
						new LambdaQueryWrapper<ThemePcTemplate>()
								.eq(ThemePcTemplate::getCompanyId, companyId)
								.eq(ThemePcTemplate::getThemePcTemplateId, themePcTemplateId)
								.isNull(ThemePcTemplate::getDeletedAt));
		if (row == null) {
			throw new ResourceException("模板页面不存在");
		}

		List<String> langs = langueProperties.getList();
		if (langs != null) {
			for (String lang : langs) {
				if (StringUtils.hasText(lang)) {
					commonLangModWriteService.deleteLang(
							(int) companyId, "theme_pc_template", themePcTemplateId, "theme_pc_template", lang);
				}
			}
		}

		int mainRemoved =
				themePcTemplateMapper.delete(
						new LambdaQueryWrapper<ThemePcTemplate>()
								.eq(ThemePcTemplate::getCompanyId, companyId)
								.eq(ThemePcTemplate::getThemePcTemplateId, themePcTemplateId));
		if (mainRemoved != 1) {
			throw new ResourceException("模板页面不存在");
		}

		List<Long> contentIds = pcTemplateContentPhysicalIdReader.listIdsByCompanyAndTemplate(companyId, themePcTemplateId);
		for (Long cid : contentIds) {
			if (cid == null) {
				continue;
			}
			if (langs != null) {
				for (String lang : langs) {
					if (StringUtils.hasText(lang)) {
						commonLangModWriteService.deleteLang(
								(int) companyId,
								"theme_pc_template_content",
								cid.longValue(),
								"theme_pc_template_content",
								lang);
					}
				}
			}
		}

		themePcTemplateContentMapper.delete(
				new LambdaQueryWrapper<ThemePcTemplateContent>()
						.eq(ThemePcTemplateContent::getCompanyId, companyId)
						.eq(ThemePcTemplateContent::getThemePcTemplateId, themePcTemplateId));

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return data;
	}
}
