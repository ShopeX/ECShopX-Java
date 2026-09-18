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
import cn.shopex.ecshopx.theme.domain.PagesTemplate;
import cn.shopex.ecshopx.theme.mapper.PagesTemplateMapper;
import cn.shopex.ecshopx.theme.support.DecorationParamsDeepCopy;
import cn.shopex.ecshopx.theme.support.PagesTemplateRowMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PagesTemplateDetailService {

	private final PagesTemplateMapper pagesTemplateMapper;
	private final PagesTemplateRowMapper pagesTemplateRowMapper;
	private final PagesTemplateDecoratorContentService decoratorContentService;
	private final PagesTemplateDecoratorFilterService decoratorFilterService;
	private final PagesTemplateDecoratorHandlerService decoratorHandlerService;
	private final CommonLangModReadService commonLangModReadService;

	public PagesTemplateDetailService(
			PagesTemplateMapper pagesTemplateMapper,
			PagesTemplateRowMapper pagesTemplateRowMapper,
			PagesTemplateDecoratorContentService decoratorContentService,
			PagesTemplateDecoratorFilterService decoratorFilterService,
			PagesTemplateDecoratorHandlerService decoratorHandlerService,
			CommonLangModReadService commonLangModReadService) {
		this.pagesTemplateMapper = pagesTemplateMapper;
		this.pagesTemplateRowMapper = pagesTemplateRowMapper;
		this.decoratorContentService = decoratorContentService;
		this.decoratorFilterService = decoratorFilterService;
		this.decoratorHandlerService = decoratorHandlerService;
		this.commonLangModReadService = commonLangModReadService;
	}

	public Object detail(long companyId, Long pagesTemplateId, String version, String requestLocaleTag) {
		if (pagesTemplateId == null) {
			return Collections.emptyList();
		}
		String v = (version == null || version.isBlank()) ? "v1.0.2" : version.trim();
		PagesTemplate row =
				pagesTemplateMapper.selectOne(
						new LambdaQueryWrapper<PagesTemplate>()
								.eq(PagesTemplate::getCompanyId, companyId)
								.eq(PagesTemplate::getPagesTemplateId, pagesTemplateId)
								.isNull(PagesTemplate::getDeletedAt)
								.last("LIMIT 1"));
		if (row == null) {
			return Collections.emptyList();
		}
		Map<String, Object> result = pagesTemplateRowMapper.toRowMap(row);
		commonLangModReadService.applyPagesTemplateDetailLangOverlay(companyId, result, requestLocaleTag);
		result.remove("template_content");
		Object tn = result.get("template_name");
		String templateName = tn == null ? "" : String.valueOf(tn);
		List<Map<String, Object>> list =
				decoratorContentService.buildDecoratedComponentRows(
						companyId, pagesTemplateId, templateName, v, requestLocaleTag);
		for (Map<String, Object> item : list) {
			Object po = item.get("params");
			Map<String, Object> pm = castToMutableStringKeyMap(po);
			item.put("params", decoratorFilterService.applyTemplateFilter(companyId, pm));
		}
		decoratorHandlerService.applyTemplateHandler(companyId, list);
		Map<String, Object> content = new LinkedHashMap<>();
		content.put("list", list);
		List<Object> config = new ArrayList<>();
		for (Map<String, Object> item : list) {
			Object po = item.get("params");
			if (!(po instanceof Map<?, ?> raw)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> p = (Map<String, Object>) (Map<?, ?>) raw;
			if (p.containsKey("name") && p.containsKey("base")) {
				Map<String, Object> copy = DecorationParamsDeepCopy.copy(p);
				config.add(decoratorFilterService.applyTemplateFilter(companyId, copy));
			}
		}
		content.put("config", config);
		result.put("template_content", content);
		return result;
	}

	private static Map<String, Object> castToMutableStringKeyMap(Object po) {
		if (!(po instanceof Map<?, ?> raw)) {
			return new LinkedHashMap<>();
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : raw.entrySet()) {
			out.put(String.valueOf(e.getKey()), e.getValue());
		}
		return out;
	}
}
