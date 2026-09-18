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

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.theme.domain.PagesTemplate;
import cn.shopex.ecshopx.theme.domain.PagesTemplateSet;
import cn.shopex.ecshopx.theme.mapper.PagesTemplateMapper;
import cn.shopex.ecshopx.theme.mapper.PagesTemplateSetMapper;
import cn.shopex.ecshopx.theme.service.dto.PagesTemplateFrontDetailQuery;
import cn.shopex.ecshopx.theme.support.DecorationParamsDeepCopy;
import cn.shopex.ecshopx.theme.support.PagesTemplateSetRowMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PagesTemplateFrontDetailService {

	private final PagesTemplateSetMapper pagesTemplateSetMapper;
	private final PagesTemplateMapper pagesTemplateMapper;
	private final PagesTemplateSetRowMapper pagesTemplateSetRowMapper;
	private final PagesTemplateSetOutsideTabBarReadService pagesTemplateSetOutsideTabBarReadService;
	private final PagesTemplateDecoratorContentService pagesTemplateDecoratorContentService;
	private final PagesTemplateDecoratorMemberTagsFilterService pagesTemplateDecoratorMemberTagsFilterService;
	private final PagesTemplateDecoratorHandlerService pagesTemplateDecoratorHandlerService;
	private final PagesTemplateDecoratorFilterService pagesTemplateDecoratorFilterService;
	private final LangueProperties langueProperties;

	public PagesTemplateFrontDetailService(
			PagesTemplateSetMapper pagesTemplateSetMapper,
			PagesTemplateMapper pagesTemplateMapper,
			PagesTemplateSetRowMapper pagesTemplateSetRowMapper,
			PagesTemplateSetOutsideTabBarReadService pagesTemplateSetOutsideTabBarReadService,
			PagesTemplateDecoratorContentService pagesTemplateDecoratorContentService,
			PagesTemplateDecoratorMemberTagsFilterService pagesTemplateDecoratorMemberTagsFilterService,
			PagesTemplateDecoratorHandlerService pagesTemplateDecoratorHandlerService,
			PagesTemplateDecoratorFilterService pagesTemplateDecoratorFilterService,
			LangueProperties langueProperties) {
		this.pagesTemplateSetMapper = pagesTemplateSetMapper;
		this.pagesTemplateMapper = pagesTemplateMapper;
		this.pagesTemplateSetRowMapper = pagesTemplateSetRowMapper;
		this.pagesTemplateSetOutsideTabBarReadService = pagesTemplateSetOutsideTabBarReadService;
		this.pagesTemplateDecoratorContentService = pagesTemplateDecoratorContentService;
		this.pagesTemplateDecoratorMemberTagsFilterService = pagesTemplateDecoratorMemberTagsFilterService;
		this.pagesTemplateDecoratorHandlerService = pagesTemplateDecoratorHandlerService;
		this.pagesTemplateDecoratorFilterService = pagesTemplateDecoratorFilterService;
		this.langueProperties = langueProperties;
	}

	public Object detail(PagesTemplateFrontDetailQuery q, String requestLocaleTag) {
		LambdaQueryWrapper<PagesTemplateSet> setOnlyCompany =
				new LambdaQueryWrapper<PagesTemplateSet>()
						.eq(PagesTemplateSet::getCompanyId, q.companyId())
						.orderByAsc(PagesTemplateSet::getId)
						.last("LIMIT 1");
		PagesTemplateSet setRow = pagesTemplateSetMapper.selectOne(setOnlyCompany);
		if (setRow == null || setRow.getIndexType() == null || setRow.getIndexType() == 0) {
			return Collections.emptyList();
		}

		LambdaQueryWrapper<PagesTemplate> pt = new LambdaQueryWrapper<PagesTemplate>().isNull(PagesTemplate::getDeletedAt);
		pt.eq(PagesTemplate::getCompanyId, q.companyId());
		if (q.pagesTemplateId() > 0L) {
			pt.eq(PagesTemplate::getPagesTemplateId, q.pagesTemplateId());
		} else if (setRow.getIndexType() == 1) {
			pt.eq(PagesTemplate::getRegionauthId, q.regionauthId())
					.eq(PagesTemplate::getDistributorId, 0)
					.eq(PagesTemplate::getStatus, 1)
					.eq(PagesTemplate::getWeappPages, nullToIndex(q.weappPages()));
		} else {
			pt.eq(PagesTemplate::getDistributorId, q.distributorId()).eq(PagesTemplate::getStatus, 1);
		}

		PagesTemplate templateRow = pagesTemplateMapper.selectOne(pt.last("LIMIT 1"));
		if (templateRow == null) {
			if (q.pagesTemplateId() > 0L) {
				throw new ResourceException("模版ID错误");
			}
			return Collections.emptyList();
		}
		long resolvedTemplateId = templateRow.getPagesTemplateId() == null ? 0L : templateRow.getPagesTemplateId();

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		LambdaQueryWrapper<PagesTemplateSet> tabBarQ =
				new LambdaQueryWrapper<PagesTemplateSet>()
						.eq(PagesTemplateSet::getCompanyId, q.companyId())
						.eq(PagesTemplateSet::getPagesTemplateId, resolvedTemplateId)
						.orderByAsc(PagesTemplateSet::getId)
						.last("LIMIT 1");
		PagesTemplateSet tabSetRow = pagesTemplateSetMapper.selectOne(tabBarQ);
		if (tabSetRow != null) {
			Map<String, Object> tabBarMap = buildTabBarRowMap(q.companyId(), tabSetRow, requestLocaleTag);
			Object tb = tabBarMap.get("tab_bar");
			if (tb != null) {
				data.put("tab_bar", tb);
			}
			Object tbl = tabBarMap.get("tab_bar_lang");
			if (tbl != null) {
				data.put("tab_bar_lang", tbl);
			}
		}

		List<Map<String, Object>> list =
				pagesTemplateDecoratorContentService.buildDecoratedComponentRows(
						q.companyId(),
						resolvedTemplateId,
						q.templateName(),
						q.version(),
						requestLocaleTag,
						q.userId(),
						q.distributorId(),
						q.eActivityId(),
						q.weappSettingId(),
						q.goodsGridTabId(),
						q.page(),
						q.pageSize());

		pagesTemplateDecoratorMemberTagsFilterService.apply(q.companyId(), q.userId(), list);
		pagesTemplateDecoratorHandlerService.applyTemplateHandler(q.companyId(), list);

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
				config.add(pagesTemplateDecoratorFilterService.applyTemplateFilter(q.companyId(), copy));
			}
		}
		data.put("list", list);
		data.put("config", config);
		return data;
	}

	private Map<String, Object> buildTabBarRowMap(long companyId, PagesTemplateSet row, String requestLocaleTag) {
		String canonicalLangTag = resolveCanonicalLangTag(requestLocaleTag);
		String modTabBar = null;
		if (!langueProperties.isDefaultLang(requestLocaleTag)
				&& canonicalLangTag != null
				&& row.getId() != null) {
			modTabBar =
					pagesTemplateSetOutsideTabBarReadService.findTabBarOverlay(
							(int) companyId, row.getId().longValue(), requestLocaleTag);
		}
		String baseTabBar = row.getTabBar();
		String displayTabBar;
		if (StringUtils.hasText(modTabBar) && !isUnsetOrEmptyScalar(modTabBar)) {
			displayTabBar = modTabBar;
		} else {
			displayTabBar = baseTabBar;
		}
		Map<String, Object> tabBarLangArg = null;
		if (StringUtils.hasText(modTabBar) && !isUnsetOrEmptyScalar(modTabBar)) {
			LinkedHashMap<String, Object> tabBarLang = new LinkedHashMap<>();
			tabBarLang.put(canonicalLangTag, modTabBar);
			tabBarLangArg = tabBarLang;
		}
		PagesTemplateSet merged = new PagesTemplateSet();
		merged.setId(row.getId());
		merged.setCompanyId(row.getCompanyId());
		merged.setRegionauthId(row.getRegionauthId());
		merged.setIndexType(row.getIndexType());
		merged.setPagesTemplateId(row.getPagesTemplateId());
		merged.setIsEnforceSync(row.getIsEnforceSync());
		merged.setIsOpenRecommend(row.getIsOpenRecommend());
		merged.setIsOpenWechatappLocation(row.getIsOpenWechatappLocation());
		merged.setIsOpenScanQrcode(row.getIsOpenScanQrcode());
		merged.setIsOpenOfficialAccount(row.getIsOpenOfficialAccount());
		merged.setTabBar(displayTabBar);
		return pagesTemplateSetRowMapper.toRowMap(merged, tabBarLangArg);
	}

	private static String nullToIndex(String weappPages) {
		return weappPages == null || weappPages.isBlank() ? "index" : weappPages;
	}

	private static String resolveCanonicalLangTag(String requestLang) {
		if (requestLang == null) {
			return null;
		}
		String t = requestLang.trim();
		if (t.isEmpty()) {
			return null;
		}
		if ("zh-CN".equalsIgnoreCase(t)) {
			return "zh-CN";
		}
		if ("en-CN".equalsIgnoreCase(t)) {
			return "en-CN";
		}
		if ("ar-SA".equalsIgnoreCase(t)) {
			return "ar-SA";
		}
		return null;
	}

	private static boolean isUnsetOrEmptyScalar(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof Boolean b) {
			return !b;
		}
		if (v instanceof String s) {
			String t = s.trim();
			return t.isEmpty() || "0".equals(t);
		}
		if (v instanceof CharSequence cs) {
			String t = cs.toString().trim();
			return t.isEmpty() || "0".equals(t);
		}
		if (v instanceof Number n) {
			return n.doubleValue() == 0.0d;
		}
		return false;
	}
}
