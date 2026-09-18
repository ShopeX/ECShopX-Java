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

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.employeepurchase.StoreHomePagePagesTemplateDecorationPort;
import cn.shopex.ecshopx.common.port.employeepurchase.StoreHomePageWeappCustomDecorationPort;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.employeepurchase.domain.StoreHomePage;
import cn.shopex.ecshopx.employeepurchase.mapper.StoreHomePageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class StoreHomePageFrontDetailService {

	private final StoreHomePageMapper storeHomePageMapper;
	private final StoreHomePagePagesTemplateDecorationPort pagesTemplateDecorationPort;
	private final StoreHomePageWeappCustomDecorationPort weappCustomDecorationPort;
	private final LangueProperties langueProperties;

	public StoreHomePageFrontDetailService(
			StoreHomePageMapper storeHomePageMapper,
			StoreHomePagePagesTemplateDecorationPort pagesTemplateDecorationPort,
			StoreHomePageWeappCustomDecorationPort weappCustomDecorationPort,
			LangueProperties langueProperties) {
		this.storeHomePageMapper = storeHomePageMapper;
		this.pagesTemplateDecorationPort = pagesTemplateDecorationPort;
		this.weappCustomDecorationPort = weappCustomDecorationPort;
		this.langueProperties = langueProperties;
	}

	public Map<String, Object> getDetailForFront(
			long companyId, int authDistributorId, long id, long userId, long eActivityId) {
		StoreHomePage row = requireRow(companyId, id);
		StoreHomePageAccess.assertRowMatchesDealer(row, authDistributorId);

		String requestLang = RequestLangTag.current(langueProperties);
		ResolvedPagesTemplate resolved = resolvePagesTemplateForStoreHomeRow(companyId, row, requestLang);

		LinkedHashMap<String, Object> base = new LinkedHashMap<>();
		base.put("store_home_page_id", id);
		base.put("company_id", row.getCompanyId());
		base.put("distributor_id", row.getDistributorId() == null ? 0 : row.getDistributorId());
		base.put("page_name", nullToEmpty(row.getPageName()));
		base.put("page_description", nullToEmpty(row.getPageDescription()));
		base.put("page_share_title", nullToEmpty(row.getPageShareTitle()));
		base.put("page_share_desc", nullToEmpty(row.getPageShareDesc()));
		base.put("page_share_imageUrl", nullToEmpty(row.getPageShareImageUrl()));
		base.put("template_name", nullToEmpty(row.getTemplateName()));
		base.put("is_open", row.getIsOpen() == null ? 0 : row.getIsOpen());
		base.put(
				"weapp_customize_page_id",
				row.getWeappCustomizePageId() == null ? null : row.getWeappCustomizePageId());
		base.put("resolved_pages_template_id", resolved.resolvedPagesTemplateId());
		base.put("template_meta", resolved.templateMeta());
		base.put("pages_template_record", resolved.pagesTemplateRecord());
		base.put("page_template_detail", null);

		String templateName = nullToEmpty(row.getTemplateName());
		long pid = resolved.resolvedPagesTemplateId() == null ? 0L : resolved.resolvedPagesTemplateId();
		int distId = row.getDistributorId() == null ? 0 : row.getDistributorId();
		long customizeId = row.getWeappCustomizePageId() == null ? 0L : row.getWeappCustomizePageId();

		if (StringUtils.hasText(templateName) && (pid > 0L || customizeId > 0L)) {
			String customPageName = weappSettingPageNameForCustomizePage(customizeId);
			if (customPageName != null) {
				for (String tryTemplateName : decorationTemplateNameCandidates(row)) {
					Map<String, Object> detail =
							weappCustomDecorationPort.loadCustomPageDecorationDetail(
									companyId, tryTemplateName, customPageName, distId, authDistributorId, requestLang);
					if (pageTemplateDetailHasNonEmptyList(detail)) {
						base.put("page_template_detail", detail);
						break;
					}
				}
				if (base.get("page_template_detail") == null) {
					base.put("page_template_detail", emptyPageTemplateDetail());
				}
			} else if (pid > 0L) {
				Map<String, Object> detail =
						pagesTemplateDecorationPort.loadPagesTemplateDetail(
								companyId, userId, distId, templateName, pid, eActivityId, requestLang);
				base.put(
						"page_template_detail",
						pageTemplateDetailHasNonEmptyList(detail) ? detail : emptyPageTemplateDetail());
			} else {
				base.put("page_template_detail", emptyPageTemplateDetail());
			}
		}

		if (base.get("page_template_detail") == null) {
			base.put("page_template_detail", emptyPageTemplateDetail());
		}

		return base;
	}

	private ResolvedPagesTemplate resolvePagesTemplateForStoreHomeRow(
			long companyId, StoreHomePage storeHomeRow, String requestLang) {
		String templateName = nullToEmpty(storeHomeRow.getTemplateName());
		if (!StringUtils.hasText(templateName)) {
			return ResolvedPagesTemplate.empty();
		}
		int distributorId = storeHomeRow.getDistributorId() == null ? 0 : storeHomeRow.getDistributorId();

		Map<String, Object> picked = null;
		for (String tryTemplateName : decorationTemplateNameCandidates(storeHomeRow)) {
			for (SearchPlan plan : pagesTemplateListSearchPlans(distributorId)) {
				Map<String, Object> listResult =
						pagesTemplateDecorationPort.listPagesTemplates(
								companyId, plan.distributorId(), plan.weappPages(), requestLang);
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> rows =
						listResult.get("list") instanceof List<?> l
								? (List<Map<String, Object>>) (List<?>) l
								: List.of();
				picked = pickResolvedPagesTemplateRow(rows, tryTemplateName);
				if (picked != null) {
					break;
				}
			}
			if (picked != null) {
				break;
			}
		}

		if (picked == null) {
			return ResolvedPagesTemplate.empty();
		}

		long pid = readLongId(picked.get("pages_template_id"));
		LinkedHashMap<String, Object> templateMeta = new LinkedHashMap<>();
		templateMeta.put("template_title", picked.getOrDefault("template_title", ""));
		templateMeta.put("template_pic", picked.getOrDefault("template_pic", ""));
		templateMeta.put("weapp_pages", picked.getOrDefault("weapp_pages", ""));
		templateMeta.put("status", picked.get("status"));

		return new ResolvedPagesTemplate(pid > 0L ? pid : null, templateMeta, picked);
	}

	public static String weappSettingPageNameForCustomizePage(long weappCustomizePageId) {
		return weappCustomizePageId > 0L ? "custom_" + weappCustomizePageId : null;
	}

	public static List<String> decorationTemplateNameCandidates(StoreHomePage storeHomeRow) {
		List<String> names = new ArrayList<>();
		String primary = nullToEmpty(storeHomeRow.getTemplateName());
		if (StringUtils.hasText(primary)) {
			names.add(primary);
		}
		if (!"yykweishop".equals(primary)) {
			names.add("yykweishop");
		}
		return names.stream().distinct().toList();
	}

	public static List<SearchPlan> pagesTemplateListSearchPlans(int rowDistributorId) {
		if (rowDistributorId > 0) {
			return List.of(
					new SearchPlan(rowDistributorId, "distributor_index"),
					new SearchPlan(rowDistributorId, "index"),
					new SearchPlan(0, "index"));
		}
		return List.of(new SearchPlan(0, "index"));
	}

	public static Map<String, Object> pickResolvedPagesTemplateRow(
			List<Map<String, Object>> pagesTemplateListRows, String templateName) {
		if (!StringUtils.hasText(templateName)) {
			return null;
		}
		List<Map<String, Object>> enabledFirst = new ArrayList<>();
		List<Map<String, Object>> anyMatch = new ArrayList<>();
		for (Map<String, Object> row : pagesTemplateListRows) {
			if (row == null) {
				continue;
			}
			if (!templateName.equals(String.valueOf(row.getOrDefault("template_name", "")))) {
				continue;
			}
			anyMatch.add(row);
			if (readInt(row.get("status")) == 1) {
				enabledFirst.add(row);
			}
		}
		if (!enabledFirst.isEmpty()) {
			return enabledFirst.get(0);
		}
		if (!anyMatch.isEmpty()) {
			return anyMatch.get(0);
		}
		return null;
	}

	public static boolean pageTemplateDetailHasNonEmptyList(Map<String, Object> detail) {
		if (detail == null) {
			return false;
		}
		Object listObj = detail.get("list");
		return listObj instanceof List<?> list && !list.isEmpty();
	}

	private static Map<String, Object> emptyPageTemplateDetail() {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("list", List.of());
		out.put("config", List.of());
		return out;
	}

	private StoreHomePage requireRow(long companyId, long id) {
		StoreHomePage row =
				storeHomePageMapper.selectOne(
						new LambdaQueryWrapper<StoreHomePage>()
								.eq(StoreHomePage::getId, id)
								.eq(StoreHomePage::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (row == null) {
			throw new ResourceException("未查询到数据");
		}
		return row;
	}

	private static String nullToEmpty(String raw) {
		return raw == null ? "" : raw;
	}

	private static long readLongId(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int readInt(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public record SearchPlan(int distributorId, String weappPages) {}

	private record ResolvedPagesTemplate(
			Long resolvedPagesTemplateId, Map<String, Object> templateMeta, Map<String, Object> pagesTemplateRecord) {
		static ResolvedPagesTemplate empty() {
			return new ResolvedPagesTemplate(null, null, null);
		}
	}
}
