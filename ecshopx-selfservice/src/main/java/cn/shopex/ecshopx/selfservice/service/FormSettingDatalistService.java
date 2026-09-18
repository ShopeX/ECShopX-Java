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

package cn.shopex.ecshopx.selfservice.service;

import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.selfservice.domain.FormSetting;
import cn.shopex.ecshopx.selfservice.mapper.FormSettingMapper;
import cn.shopex.ecshopx.selfservice.service.multilang.FormSettingOutsideMultiLangReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Builds the admin form-setting datalist response: filtered query, optional multi-language field titles,
 * and pagination metadata ({@code total_count}, {@code list}).
 * <p>
 * Pagination: blank or missing {@code page} is treated as {@code 0}. Blank or invalid {@code pageSize}
 * (non-numeric, zero, or negative) falls back to {@link #DEFAULT_PAGE_SIZE}. SQL {@code OFFSET} is
 * {@code (page - 1) * pageSize}, clamped to {@code 0} when that product would be negative (for example
 * {@code page == 0}).
 */
@Service
public class FormSettingDatalistService {

	/** Used when {@code pageSize} is missing, not an integer, or not a positive integer. */
	private static final int DEFAULT_PAGE_SIZE = 20;

	private final FormSettingMapper formSettingMapper;
	private final FormSettingOutsideMultiLangReadService multiLangReadService;
	private final FormSettingApiRowAssembler rowAssembler;

	public FormSettingDatalistService(
			FormSettingMapper formSettingMapper,
			FormSettingOutsideMultiLangReadService multiLangReadService,
			FormSettingApiRowAssembler rowAssembler) {
		this.formSettingMapper = formSettingMapper;
		this.multiLangReadService = multiLangReadService;
		this.rowAssembler = rowAssembler;
	}

	public Map<String, Object> getDatalist(
			long companyId,
			String pageRaw,
			String pageSizeRaw,
			String formElement,
			String fieldTitle,
			String isValidRaw,
			String countryCode) {
		String langTag = resolveLangTag(countryCode);
		int page = resolvePage(pageRaw);
		int pageSize = resolvePageSize(pageSizeRaw);
		long offset = (long) page - 1L;
		long byteOffset = offset * (long) pageSize;
		if (byteOffset < 0L) {
			byteOffset = 0L;
		}

		LambdaQueryWrapper<FormSetting> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(FormSetting::getCompanyId, companyId);
		applyIsValidFilter(wrapper, isValidRaw);
		if (StringUtils.hasText(formElement)) {
			wrapper.eq(FormSetting::getFormElement, formElement.trim());
		}
		if (StringUtils.hasText(fieldTitle)) {
			wrapper.like(FormSetting::getFieldTitle, fieldTitle.trim());
		}
		wrapper.orderByDesc(FormSetting::getId);

		long total = formSettingMapper.selectCount(wrapper);
		List<Map<String, Object>> list;
		if (total == 0L) {
			list = new ArrayList<>();
		} else {
			List<FormSetting> entities = formSettingMapper.selectList(
					wrapper.last("LIMIT " + pageSize + " OFFSET " + byteOffset));
			list = new ArrayList<>(entities.size());
			for (FormSetting entity : entities) {
				list.add(rowAssembler.toRow(entity));
			}
			multiLangReadService.applyFieldTitleOverrides(companyId, list, langTag);
		}

		Map<String, Object> out = new LinkedHashMap<>();
		int totalCount = total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
		out.put("total_count", totalCount);
		out.put("list", list);
		return out;
	}

	private static String resolveLangTag(String countryCode) {
		if (countryCode == null || !StringUtils.hasText(countryCode.trim())) {
			return "zh-CN";
		}
		String t = countryCode.trim();
		return StringUtils.hasText(t) ? t : "zh-CN";
	}

	private static int resolvePage(String pageRaw) {
		if (pageRaw == null || !StringUtils.hasText(pageRaw.trim())) {
			return 0;
		}
		String trimmed = pageRaw.trim();
		if ("0".equals(trimmed)) {
			return 0;
		}
		long p = LeadingNumberParser.parseAsLong(trimmed);
		if (p > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		if (p < Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}
		return (int) p;
	}

	private static int resolvePageSize(String pageSizeRaw) {
		if (pageSizeRaw == null || !StringUtils.hasText(pageSizeRaw.trim())) {
			return DEFAULT_PAGE_SIZE;
		}
		try {
			int n = Integer.parseInt(pageSizeRaw.trim());
			if (n > 0) {
				return n;
			}
		} catch (NumberFormatException ignored) {
			return DEFAULT_PAGE_SIZE;
		}
		return DEFAULT_PAGE_SIZE;
	}

	private static void applyIsValidFilter(LambdaQueryWrapper<FormSetting> wrapper, String isValidRaw) {
		if (isValidRaw == null || !StringUtils.hasText(isValidRaw.trim())) {
			return;
		}
		String t = isValidRaw.trim();
		if ("0".equals(t)) {
			return;
		}
		int v;
		try {
			v = Integer.parseInt(t);
		} catch (NumberFormatException ex) {
			v = 0;
		}
		wrapper.eq(FormSetting::getStatus, v);
	}
}
