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

import cn.shopex.ecshopx.theme.domain.ThemePcTemplateContent;
import cn.shopex.ecshopx.theme.mapper.ThemePcTemplateContentMapper;
import cn.shopex.ecshopx.theme.support.ThemePcTemplateContentRowMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PcTemplateGetHeaderOrFooterService {

	private final ThemePcTemplateContentMapper themePcTemplateContentMapper;
	private final ThemePcTemplateContentRowMapper themePcTemplateContentRowMapper;
	private final ThemePcTemplateContentLangReadService themePcTemplateContentLangReadService;

	public Object getHeaderOrFooter(long companyId, String requestLang, String pageName) {
		List<Long> langIds =
				themePcTemplateContentLangReadService.filterThemePcTemplateContentIdsByNameLangContains(
						(int) companyId, requestLang, pageName);
		LambdaQueryWrapper<ThemePcTemplateContent> w =
				new LambdaQueryWrapper<ThemePcTemplateContent>()
						.eq(ThemePcTemplateContent::getCompanyId, companyId);
		if (!langIds.isEmpty()) {
			w.in(ThemePcTemplateContent::getThemePcTemplateContentId, langIds);
		} else if (pageName == null) {
			w.isNull(ThemePcTemplateContent::getName);
		} else {
			w.eq(ThemePcTemplateContent::getName, pageName);
		}
		w.orderByAsc(ThemePcTemplateContent::getThemePcTemplateContentId).last("LIMIT 1");
		List<ThemePcTemplateContent> list = themePcTemplateContentMapper.selectList(w);
		if (list.isEmpty()) {
			return Collections.emptyList();
		}
		ThemePcTemplateContent entity = list.get(0);
		Map<String, Object> row = themePcTemplateContentRowMapper.toRowMap(entity);
		themePcTemplateContentLangReadService.applyThemePcTemplateContentDetailLangOverlay(companyId, row, requestLang);
		return row;
	}
}
