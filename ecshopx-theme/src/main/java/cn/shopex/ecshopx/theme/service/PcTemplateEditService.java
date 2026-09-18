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
import cn.shopex.ecshopx.theme.api.admin.v1.dto.PcTemplateEditRequest;
import cn.shopex.ecshopx.theme.domain.ThemePcTemplate;
import cn.shopex.ecshopx.theme.mapper.ThemePcTemplateMapper;
import cn.shopex.ecshopx.theme.support.ThemePcTemplateRowMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class PcTemplateEditService {

	private static final Logger log = LoggerFactory.getLogger(PcTemplateEditService.class);
	private static final ObjectMapper JSON_LOG = new ObjectMapper();

	private final ThemePcTemplateMapper themePcTemplateMapper;
	private final ThemePcTemplateRowMapper themePcTemplateRowMapper;
	private final CommonLangModWriteService commonLangModWriteService;
	private final LangueProperties langueProperties;

	public Map<String, Object> edit(long companyId, String requestLang, PcTemplateEditRequest req) {
		logEditParams(req);

		long themePcTemplateId = parseThemePcTemplateId(req);

		ThemePcTemplate row =
				themePcTemplateMapper.selectOne(
						new LambdaQueryWrapper<ThemePcTemplate>()
								.eq(ThemePcTemplate::getCompanyId, companyId)
								.eq(ThemePcTemplate::getThemePcTemplateId, themePcTemplateId));
		if (row == null) {
			throw new ResourceException("模板页面不存在");
		}

		String effectivePageType = req.getPageType() != null ? req.getPageType() : row.getPageType();

		Integer status = req.getStatus();
		int distributorIdForCheck =
				req.getDistributorId() != null
						? Math.max(0, req.getDistributorId())
						: (row.getDistributorId() == null ? 0 : Math.max(0, row.getDistributorId()));
		if (status != null && status.intValue() != 0) {
			checkIndexTemplateStatusForEdit(
					companyId, effectivePageType, status.intValue(), themePcTemplateId, distributorIdForCheck);
		}

		boolean defaultLang = langueProperties.isDefaultLang(requestLang);

		String trimmedTitle = null;
		String trimmedDesc = null;
		String trimmedPageType = null;
		boolean setTitle = false;
		boolean setDesc = false;
		if (defaultLang) {
			if (isNonEmptyStringField(req.getTemplateTitle())) {
				trimmedTitle = req.getTemplateTitle().trim();
				setTitle = true;
			}
			if (isNonEmptyStringField(req.getTemplateDescription())) {
				trimmedDesc = req.getTemplateDescription().trim();
				setDesc = true;
			}
		}
		boolean setPageTypeColumn = false;
		if (isNonEmptyStringField(req.getPageType())) {
			trimmedPageType = req.getPageType().trim();
			setPageTypeColumn = true;
		}
		boolean setStatusColumn = status != null && status.intValue() != 0;
		boolean setDistributorId = req.getDistributorId() != null;
		Integer distributorIdToSet = setDistributorId ? Math.max(0, req.getDistributorId()) : null;

		LambdaUpdateWrapper<ThemePcTemplate> uw =
				new LambdaUpdateWrapper<ThemePcTemplate>()
						.eq(ThemePcTemplate::getCompanyId, companyId)
						.eq(ThemePcTemplate::getThemePcTemplateId, themePcTemplateId);

		int now = (int) (System.currentTimeMillis() / 1000L);
		if (setTitle) {
			uw.set(ThemePcTemplate::getTemplateTitle, trimmedTitle);
		}
		if (setDesc) {
			uw.set(ThemePcTemplate::getTemplateDescription, trimmedDesc);
		}
		if (setStatusColumn) {
			uw.set(ThemePcTemplate::getStatus, status);
		}
		if (setPageTypeColumn) {
			uw.set(ThemePcTemplate::getPageType, trimmedPageType);
		}
		if (setDistributorId) {
			uw.set(ThemePcTemplate::getDistributorId, distributorIdToSet);
		}

		boolean anyMainFieldSet =
				setTitle || setDesc || setStatusColumn || setPageTypeColumn || setDistributorId;

		Map<String, String> langBag = new HashMap<>();
		if (!defaultLang) {
			if (isNonEmptyStringField(req.getTemplateTitle())) {
				langBag.put("template_title", req.getTemplateTitle().trim());
			}
			if (isNonEmptyStringField(req.getTemplateDescription())) {
				langBag.put("template_description", req.getTemplateDescription().trim());
			}
		}

		boolean hasLangUpdate = !langBag.isEmpty();

		if (anyMainFieldSet) {
			uw.set(ThemePcTemplate::getUpdated, now);
			int affected = themePcTemplateMapper.update(null, uw);
			if (affected != 1) {
				throw new ResourceException("未查询到更新数据");
			}
		}

		if (hasLangUpdate) {
			if (!anyMainFieldSet) {
				ThemePcTemplate still = themePcTemplateMapper.selectById(themePcTemplateId);
				if (still == null || !Objects.equals(still.getCompanyId(), companyId)) {
					throw new ResourceException("未查询到更新数据");
				}
			}
			commonLangModWriteService.updateLangData(
					(int) companyId,
					langBag,
					"theme_pc_template",
					(int) themePcTemplateId,
					"theme_pc_template",
					requestLang);
		}

		ThemePcTemplate fresh = themePcTemplateMapper.selectById(themePcTemplateId);
		if (fresh == null || !Objects.equals(fresh.getCompanyId(), companyId)) {
			throw new ResourceException("未查询到更新数据");
		}
		return themePcTemplateRowMapper.toRowMap(fresh);
	}

	private static boolean isNonEmptyStringField(String v) {
		if (v == null) {
			return false;
		}
		if (v.isEmpty()) {
			return false;
		}
		if ("0".equals(v)) {
			return false;
		}
		return true;
	}

	private long parseThemePcTemplateId(PcTemplateEditRequest req) {
		String raw = req.getThemePcTemplateId();
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			throw new BadRequestException("缺少theme_pc_template_id");
		}
		try {
			long id = Long.parseLong(raw.trim());
			if (id <= 0L) {
				throw new ResourceException("模板页面不存在");
			}
			return id;
		} catch (NumberFormatException ex) {
			throw new ResourceException("模板页面不存在");
		}
	}

	private void checkIndexTemplateStatusForEdit(
			long companyId,
			String pageType,
			int status,
			long excludeThemePcTemplateId,
			int distributorId) {
		if (!"index".equals(pageType) || status != 1) {
			return;
		}
		Long cnt =
				themePcTemplateMapper.selectCount(
						new LambdaQueryWrapper<ThemePcTemplate>()
								.eq(ThemePcTemplate::getCompanyId, companyId)
								.eq(ThemePcTemplate::getDistributorId, distributorId)
								.eq(ThemePcTemplate::getPageType, "index")
								.eq(ThemePcTemplate::getStatus, 1)
								.ne(ThemePcTemplate::getThemePcTemplateId, excludeThemePcTemplateId)
								.isNull(ThemePcTemplate::getDeletedAt));
		if (cnt != null && cnt > 0) {
			throw new ResourceException("已有启用的模版");
		}
	}

	private void logEditParams(PcTemplateEditRequest req) {
		try {
			log.info("edit::params====>{}", JSON_LOG.writeValueAsString(req));
		} catch (JsonProcessingException e) {
			log.info("edit::params====>{}", req);
		}
	}
}
