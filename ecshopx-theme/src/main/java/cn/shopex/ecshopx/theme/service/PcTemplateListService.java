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

import cn.shopex.ecshopx.companys.service.CommonLangModReadService;
import cn.shopex.ecshopx.theme.domain.ThemePcTemplate;
import cn.shopex.ecshopx.theme.mapper.ThemePcTemplateMapper;
import cn.shopex.ecshopx.theme.service.dto.PcTemplateListQuery;
import cn.shopex.ecshopx.theme.support.ThemePcTemplateRowMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PcTemplateListService {

	private final ThemePcTemplateMapper themePcTemplateMapper;
	private final ThemePcTemplateRowMapper themePcTemplateRowMapper;
	private final CommonLangModReadService commonLangModReadService;

	public Map<String, Object> lists(PcTemplateListQuery query, String requestLang) {
		LambdaQueryWrapper<ThemePcTemplate> w = new LambdaQueryWrapper<>();
		w.eq(ThemePcTemplate::getCompanyId, query.companyId())
				.eq(ThemePcTemplate::getDistributorId, query.distributorId())
				.isNull(ThemePcTemplate::getDeletedAt);
		String pageType = query.pageTypeFilterOrNull();
		if (pageType != null && StringUtils.hasText(pageType.trim())) {
			w.eq(ThemePcTemplate::getPageType, pageType.trim());
		}
		if (query.statusFilterRawOrNull() != null) {
			w.apply("status = {0}", query.statusFilterRawOrNull());
		}
		w.orderByDesc(ThemePcTemplate::getCreated);

		long total = themePcTemplateMapper.selectCount(w);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		if (total == 0) {
			out.put("list", List.of());
			return out;
		}

		Page<ThemePcTemplate> page = new Page<>(query.pageNo(), query.pageSize(), false);
		themePcTemplateMapper.selectPage(page, w);

		List<Map<String, Object>> listMaps = new ArrayList<>();
		for (ThemePcTemplate entity : page.getRecords()) {
			Map<String, Object> row = themePcTemplateRowMapper.toRowMap(entity);
			commonLangModReadService.applyThemePcTemplateListLangOverlay(query.companyId(), row, requestLang);
			listMaps.add(row);
		}
		out.put("list", listMaps);
		return out;
	}
}
