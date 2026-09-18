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

package cn.shopex.ecshopx.theme.integration.employeepurchase;

import cn.shopex.ecshopx.common.port.employeepurchase.StoreHomePageDecorationConstants;
import cn.shopex.ecshopx.common.port.employeepurchase.StoreHomePagePagesTemplateDecorationPort;
import cn.shopex.ecshopx.theme.service.PagesTemplateFrontDetailService;
import cn.shopex.ecshopx.theme.service.PagesTemplateListService;
import cn.shopex.ecshopx.theme.service.dto.PagesTemplateFrontDetailQuery;
import cn.shopex.ecshopx.theme.service.dto.PagesTemplateListQuery;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class StoreHomePagePagesTemplateDecorationPortImpl implements StoreHomePagePagesTemplateDecorationPort {

	private final PagesTemplateListService pagesTemplateListService;
	private final PagesTemplateFrontDetailService pagesTemplateFrontDetailService;

	public StoreHomePagePagesTemplateDecorationPortImpl(
			PagesTemplateListService pagesTemplateListService,
			PagesTemplateFrontDetailService pagesTemplateFrontDetailService) {
		this.pagesTemplateListService = pagesTemplateListService;
		this.pagesTemplateFrontDetailService = pagesTemplateFrontDetailService;
	}

	@Override
	public Map<String, Object> listPagesTemplates(
			long companyId, int distributorId, String weappPages, String requestLang) {
		PagesTemplateListQuery query =
				new PagesTemplateListQuery(companyId, distributorId, weappPages, false, null, 1, 100);
		return pagesTemplateListService.lists(query, requestLang);
	}

	@Override
	public Map<String, Object> loadPagesTemplateDetail(
			long companyId,
			long userId,
			int distributorId,
			String templateName,
			long pagesTemplateId,
			long eActivityId,
			String requestLang) {
		PagesTemplateFrontDetailQuery query =
				new PagesTemplateFrontDetailQuery(
						companyId,
						userId,
						0L,
						distributorId,
						distributorId > 0 ? "distributor_index" : "index",
						templateName,
						StoreHomePageDecorationConstants.INDEX_DECORATION_SETTING_VERSION,
						1,
						50,
						null,
						null,
						pagesTemplateId,
						eActivityId);
		Object detail = pagesTemplateFrontDetailService.detail(query, requestLang);
		if (detail instanceof Map<?, ?> map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> typed = (Map<String, Object>) (Map<?, ?>) map;
			return normalizePageTemplateDetail(typed);
		}
		return emptyPageTemplateDetail();
	}

	private static Map<String, Object> normalizePageTemplateDetail(Map<String, Object> detail) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("list", detail.getOrDefault("list", List.of()));
		out.put("config", detail.getOrDefault("config", List.of()));
		return out;
	}

	private static Map<String, Object> emptyPageTemplateDetail() {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("list", List.of());
		out.put("config", List.of());
		return out;
	}
}
