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

import cn.shopex.ecshopx.theme.domain.PagesTemplate;
import cn.shopex.ecshopx.theme.domain.PagesTemplateSet;
import cn.shopex.ecshopx.theme.mapper.PagesTemplateMapper;
import cn.shopex.ecshopx.theme.mapper.PagesTemplateSetMapper;
import cn.shopex.ecshopx.theme.service.dto.PagesTemplateFrontShopDetailQuery;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PagesTemplateFrontShopDetailService {

	private final PagesTemplateMapper pagesTemplateMapper;
	private final PagesTemplateSetMapper pagesTemplateSetMapper;
	private final PagesTemplateDecoratorContentService pagesTemplateDecoratorContentService;
	private final PagesTemplateDecoratorMemberTagsFilterService pagesTemplateDecoratorMemberTagsFilterService;

	public PagesTemplateFrontShopDetailService(
			PagesTemplateMapper pagesTemplateMapper,
			PagesTemplateSetMapper pagesTemplateSetMapper,
			PagesTemplateDecoratorContentService pagesTemplateDecoratorContentService,
			PagesTemplateDecoratorMemberTagsFilterService pagesTemplateDecoratorMemberTagsFilterService) {
		this.pagesTemplateMapper = pagesTemplateMapper;
		this.pagesTemplateSetMapper = pagesTemplateSetMapper;
		this.pagesTemplateDecoratorContentService = pagesTemplateDecoratorContentService;
		this.pagesTemplateDecoratorMemberTagsFilterService = pagesTemplateDecoratorMemberTagsFilterService;
	}

	public Map<String, Object> shopDetail(PagesTemplateFrontShopDetailQuery q, String requestLocaleTag) {
		String raw = q.weappPagesRaw();
		String resolvedWeappPages = (raw == null || raw.isBlank()) ? "index" : raw.trim();
		String weappPagesForDb =
				Objects.equals(resolvedWeappPages, "index") ? "distributor_index" : resolvedWeappPages;

		int distributorIdInt = safeIntDistributorId(q.distributorId());
		LambdaQueryWrapper<PagesTemplate> first =
				new LambdaQueryWrapper<PagesTemplate>()
						.eq(PagesTemplate::getCompanyId, q.companyId())
						.eq(PagesTemplate::getStatus, 1)
						.eq(PagesTemplate::getWeappPages, weappPagesForDb)
						.isNull(PagesTemplate::getDeletedAt)
						.eq(PagesTemplate::getDistributorId, distributorIdInt);
		PagesTemplate templateRow = pagesTemplateMapper.selectOne(first.last("LIMIT 1"));
		if (templateRow == null) {
			LambdaQueryWrapper<PagesTemplate> second =
					new LambdaQueryWrapper<PagesTemplate>()
							.eq(PagesTemplate::getCompanyId, q.companyId())
							.eq(PagesTemplate::getStatus, 1)
							.eq(PagesTemplate::getWeappPages, weappPagesForDb)
							.isNull(PagesTemplate::getDeletedAt)
							.eq(PagesTemplate::getRegionauthId, q.regionauthId())
							.eq(PagesTemplate::getDistributorId, 0);
			templateRow = pagesTemplateMapper.selectOne(second.last("LIMIT 1"));
		}
		if (templateRow == null) {
			LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
			empty.put("list", "");
			empty.put("config", "");
			return empty;
		}

		long resolvedTemplateId = templateRow.getPagesTemplateId() == null ? 0L : templateRow.getPagesTemplateId();
		if (resolvedTemplateId <= 0L) {
			LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
			empty.put("list", "");
			empty.put("config", "");
			return empty;
		}

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		PagesTemplateSet setRow =
				pagesTemplateSetMapper.selectOne(
						new LambdaQueryWrapper<PagesTemplateSet>()
								.eq(PagesTemplateSet::getCompanyId, q.companyId())
								.eq(PagesTemplateSet::getPagesTemplateId, resolvedTemplateId)
								.orderByAsc(PagesTemplateSet::getId)
								.last("LIMIT 1"));
		if (setRow != null) {
			String tb = setRow.getTabBar();
			data.put("tab_bar", tb == null ? "" : tb);
		}

		List<Map<String, Object>> list =
				pagesTemplateDecoratorContentService.buildDecoratedComponentRows(
						q.companyId(), Long.valueOf(resolvedTemplateId), q.templateName(), q.version(), requestLocaleTag);
		pagesTemplateDecoratorMemberTagsFilterService.apply(q.companyId(), q.userId(), list);
		data.put("list", list);

		List<Object> config = new ArrayList<>();
		for (Map<String, Object> widget : list) {
			Object po = widget.get("params");
			if (po instanceof Map<?, ?> pm && pm.containsKey("name") && pm.containsKey("base")) {
				@SuppressWarnings("unchecked")
				Map<String, Object> ref = (Map<String, Object>) po;
				config.add(ref);
			}
		}
		data.put("config", config);
		return data;
	}

	private static int safeIntDistributorId(long distributorId) {
		if (distributorId <= 0L || distributorId > Integer.MAX_VALUE) {
			return 0;
		}
		return (int) distributorId;
	}
}
