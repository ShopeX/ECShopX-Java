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
import cn.shopex.ecshopx.theme.api.admin.v1.dto.PcTemplateAddRequest;
import cn.shopex.ecshopx.theme.domain.ThemePcTemplate;
import cn.shopex.ecshopx.theme.mapper.ThemePcTemplateMapper;
import cn.shopex.ecshopx.theme.support.ThemePcTemplateRowMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class PcTemplateAddService {

	private static final Set<String> ALLOWED_PAGE_TYPES = Set.of("index", "custom", "product_list");

	private final ThemePcTemplateMapper themePcTemplateMapper;
	private final ThemePcTemplateRowMapper themePcTemplateRowMapper;
	private final CommonLangModWriteService commonLangModWriteService;
	private final LangueProperties langueProperties;

	public Map<String, Object> add(long companyId, String requestLang, PcTemplateAddRequest req) {
		String v = req.getVersion();
		if (v == null || !StringUtils.hasText(v.trim())) {
			v = "v1.0.1";
		} else {
			v = v.trim();
		}
		if (!StringUtils.hasText(v)) {
			throw new BadRequestException("缺少版本号");
		}

		Integer s = req.getStatus();
		if (s == null) {
			s = 2;
		}

		String pageTypeRaw = req.getPageType();
		if (pageTypeRaw == null || !StringUtils.hasText(pageTypeRaw.trim())) {
			throw new BadRequestException("缺少页面类型");
		}
		String pageType = pageTypeRaw.trim();
		if (!ALLOWED_PAGE_TYPES.contains(pageType)) {
			throw new BadRequestException("缺少页面类型");
		}

		String titleRaw = req.getTemplateTitle();
		if (titleRaw == null || !StringUtils.hasText(titleRaw.trim())) {
			throw new BadRequestException("缺少页面名称");
		}
		String titleTrim = titleRaw.trim();

		String descRaw = req.getTemplateDescription();
		if (descRaw == null || !StringUtils.hasText(descRaw.trim())) {
			throw new BadRequestException("缺少页面描述");
		}
		String descTrim = descRaw.trim();

		int distributorId = req.getDistributorId() == null ? 0 : Math.max(0, req.getDistributorId());

		checkIndexTemplateStatus(companyId, pageType, s, distributorId);

		int now = (int) (System.currentTimeMillis() / 1000L);
		ThemePcTemplate entity = new ThemePcTemplate();
		entity.setCompanyId(companyId);
		entity.setDistributorId(distributorId);
		entity.setPageType(pageType);
		entity.setVersion(v);
		entity.setStatus(s);
		entity.setCreated(now);
		entity.setUpdated(now);
		entity.setDeletedAt(null);

		if (langueProperties.isDefaultLang(requestLang)) {
			entity.setTemplateTitle(titleTrim);
			entity.setTemplateDescription(descTrim);
		} else {
			entity.setTemplateTitle(null);
			entity.setTemplateDescription(null);
		}

		int rows = themePcTemplateMapper.insert(entity);
		if (rows != 1) {
			throw new ResourceException("未查询到更新数据");
		}
		if (entity.getThemePcTemplateId() == null) {
			throw new ResourceException("未查询到更新数据");
		}

		if (!langueProperties.isDefaultLang(requestLang)) {
			Map<String, String> langBag = new HashMap<>();
			langBag.put("template_title", titleTrim);
			langBag.put("template_description", descTrim);
			commonLangModWriteService.saveLang(
					(int) companyId,
					langBag,
					"theme_pc_template",
					entity.getThemePcTemplateId().intValue(),
					"theme_pc_template",
					requestLang);
		}

		return themePcTemplateRowMapper.toRowMap(entity);
	}

	private void checkIndexTemplateStatus(
			long companyId, String pageType, Integer status, int distributorId) {
		if (!"index".equals(pageType) || status == null || status.intValue() != 1) {
			return;
		}
		Long cnt =
				themePcTemplateMapper.selectCount(
						new LambdaQueryWrapper<ThemePcTemplate>()
								.eq(ThemePcTemplate::getCompanyId, companyId)
								.eq(ThemePcTemplate::getDistributorId, distributorId)
								.eq(ThemePcTemplate::getPageType, "index")
								.eq(ThemePcTemplate::getStatus, 1)
								.isNull(ThemePcTemplate::getDeletedAt));
		if (cnt != null && cnt > 0) {
			throw new ResourceException("已有启用的模版");
		}
	}
}
