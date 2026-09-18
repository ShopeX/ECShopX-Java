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
import cn.shopex.ecshopx.selfservice.domain.FormTemplate;
import cn.shopex.ecshopx.selfservice.mapper.FormTemplateMapper;
import cn.shopex.ecshopx.selfservice.service.multilang.FormTemplateOutsideMultiLangReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FormTemplateDatalistService {

	private static final int DEFAULT_PAGE_SIZE = 10;

	private final FormTemplateMapper formTemplateMapper;
	private final FormTemplateApiRowAssembler formTemplateApiRowAssembler;
	private final FormTemplateOutsideMultiLangReadService formTemplateOutsideMultiLangReadService;

	public FormTemplateDatalistService(
			FormTemplateMapper formTemplateMapper,
			FormTemplateApiRowAssembler formTemplateApiRowAssembler,
			FormTemplateOutsideMultiLangReadService formTemplateOutsideMultiLangReadService) {
		this.formTemplateMapper = formTemplateMapper;
		this.formTemplateApiRowAssembler = formTemplateApiRowAssembler;
		this.formTemplateOutsideMultiLangReadService = formTemplateOutsideMultiLangReadService;
	}

	public Map<String, Object> getDatalist(
			long companyId,
			String pageRaw,
			String pageSizeRaw,
			String temTypeRaw,
			String isValidRaw,
			String temNameRaw,
			String countryCode) {
		String requestLangTag = resolveLangTag(countryCode);
		int page = resolvePage(pageRaw);
		int pageSize = resolvePageSize(pageSizeRaw);
		long offset = (long) page - 1L;
		long byteOffset = offset * (long) pageSize;
		if (byteOffset < 0L) {
			byteOffset = 0L;
		}

		LambdaQueryWrapper<FormTemplate> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(FormTemplate::getCompanyId, companyId);
		applyIsValidFilter(wrapper, isValidRaw);
		if (isTruthyTemplateParam(temNameRaw)) {
			wrapper.eq(FormTemplate::getTemName, temNameRaw);
		}
		if (isTruthyTemplateParam(temTypeRaw)) {
			wrapper.eq(FormTemplate::getTemType, temTypeRaw);
		}
		wrapper.orderByDesc(FormTemplate::getId);

		long total = formTemplateMapper.selectCount(wrapper);
		List<Map<String, Object>> list;
		if (total == 0L) {
			list = new ArrayList<>();
		} else {
			List<FormTemplate> entities =
					formTemplateMapper.selectList(wrapper.last("LIMIT " + pageSize + " OFFSET " + byteOffset));
			list = new ArrayList<>(entities.size());
			for (FormTemplate entity : entities) {
				list.add(formTemplateApiRowAssembler.toRow(entity));
			}
			if (!list.isEmpty()) {
				formTemplateOutsideMultiLangReadService.applyListLangOverrides(companyId, list, requestLangTag);
			}
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

	private static void applyIsValidFilter(LambdaQueryWrapper<FormTemplate> wrapper, String isValidRaw) {
		if (isValidRaw == null || !StringUtils.hasText(isValidRaw.trim())) {
			return;
		}
		String t = isValidRaw.trim();
		if ("0".equals(t)) {
			return;
		}
		int status = (int) LeadingNumberParser.parseAsLong(t);
		wrapper.eq(FormTemplate::getStatus, status);
	}

	private static boolean isTruthyTemplateParam(String raw) {
		if (raw == null || raw.isEmpty() || "0".equals(raw)) {
			return false;
		}
		return true;
	}
}
