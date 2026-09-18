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

package cn.shopex.ecshopx.wechat.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.regionauth.RegionauthNameMapFacade;
import cn.shopex.ecshopx.common.wechat.WeappCustomizePageLangWriteFacade;
import cn.shopex.ecshopx.common.wechat.WeappCustomizePageListLangFacade;
import cn.shopex.ecshopx.common.wechat.WechatCustomizePageCategoryBindFacade;
import cn.shopex.ecshopx.wechat.domain.WeappCustomizePage;
import cn.shopex.ecshopx.wechat.domain.WeappSetting;
import cn.shopex.ecshopx.wechat.repository.WeappCustomizePageRepository;
import cn.shopex.ecshopx.wechat.repository.WeappSettingRepository;
import cn.shopex.ecshopx.wechat.support.WeappSettingLegacySerializeCodec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CustomizePageService {

	private static final Set<String> ALLOWED_PAGE_TYPES =
			Set.of("normal", "salesperson", "category", "my", "task_share", "enterprise_store_home");

	private final WeappCustomizePageRepository weappCustomizePageRepository;
	private final WeappSettingRepository weappSettingRepository;
	private final WeappCustomizePageListLangFacade weappCustomizePageListLangFacade;
	private final WeappCustomizePageLangWriteFacade weappCustomizePageLangWriteFacade;
	private final RegionauthNameMapFacade regionauthNameMapFacade;
	private final WechatCustomizePageCategoryBindFacade wechatCustomizePageCategoryBindFacade;

	public CustomizePageService(
			WeappCustomizePageRepository weappCustomizePageRepository,
			WeappSettingRepository weappSettingRepository,
			WeappCustomizePageListLangFacade weappCustomizePageListLangFacade,
			WeappCustomizePageLangWriteFacade weappCustomizePageLangWriteFacade,
			RegionauthNameMapFacade regionauthNameMapFacade,
			WechatCustomizePageCategoryBindFacade wechatCustomizePageCategoryBindFacade) {
		this.weappCustomizePageRepository = weappCustomizePageRepository;
		this.weappSettingRepository = weappSettingRepository;
		this.weappCustomizePageListLangFacade = weappCustomizePageListLangFacade;
		this.weappCustomizePageLangWriteFacade = weappCustomizePageLangWriteFacade;
		this.regionauthNameMapFacade = regionauthNameMapFacade;
		this.wechatCustomizePageCategoryBindFacade = wechatCustomizePageCategoryBindFacade;
	}

	public Map<String, Object> getCustomizepageList(
			long companyId,
			long jwtDistributorId,
			boolean templateNameQueryKeyPresent,
			String templateName,
			String pageRaw,
			String pageSizeRaw,
			String pageTypeRaw,
			boolean regionauthQueryKeyPresent,
			String regionauthIdRaw,
			String requestLang) {
		String pageTypeResolved = resolvePageTypeWithDefaultNormal(pageTypeRaw);
		int page = isPageQueryParamFalsy(pageRaw) ? 1 : parseIntLeadingDigits(pageRaw);
		int pageSize = isPageQueryParamFalsy(pageSizeRaw) ? 20 : parseIntLeadingDigits(pageSizeRaw);
		long regionauthFilterId = 0L;
		if (regionauthQueryKeyPresent) {
			regionauthFilterId = parseLongOrZeroRegionauth(regionauthIdRaw);
		}
		long totalCount = weappCustomizePageRepository.countCustomizePageList(companyId, templateNameQueryKeyPresent, templateName,
				pageTypeResolved, regionauthQueryKeyPresent, regionauthFilterId);
		List<Map<String, Object>> list;
		if (totalCount == 0L) {
			list = List.of();
		} else {
			List<WeappCustomizePage> entities = weappCustomizePageRepository.pageCustomizePageList(companyId,
					templateNameQueryKeyPresent, templateName, pageTypeResolved, regionauthQueryKeyPresent, regionauthFilterId, page,
					pageSize);
			list = new ArrayList<>();
			for (WeappCustomizePage e : entities) {
				list.add(toCustomizePageListRow(e));
			}
		}
		if (!list.isEmpty()) {
			weappCustomizePageListLangFacade.enrichWeappCustomizePageListRows(companyId, list, requestLang);
			LinkedHashSet<Long> regionIds = new LinkedHashSet<>();
			for (Map<String, Object> row : list) {
				long rid = parseRowLongId(row.get("regionauth_id"));
				if (rid > 0L) {
					regionIds.add(rid);
				}
			}
			Map<Long, String> nameMap = regionauthNameMapFacade.mapRegionauthIdToName(companyId, regionIds);
			for (Map<String, Object> row : list) {
				long rowRid = parseRowLongId(row.get("regionauth_id"));
				row.put("regionauth_name", nameMap.getOrDefault(rowRid, ""));
			}
			if ("category".equalsIgnoreCase(pageTypeResolved)) {
				List<Long> pageIds = new ArrayList<>();
				for (Map<String, Object> row : list) {
					long pid = parseRowLongId(row.get("id"));
					if (pid > 0L) {
						pageIds.add(pid);
					}
				}
				List<Map<String, Object>> bindRows = wechatCustomizePageCategoryBindFacade.listCategoriesForCustomizePageList(companyId,
						jwtDistributorId, pageIds);
				Map<Long, Map<String, Object>> categoryByPageId = new LinkedHashMap<>();
				for (Map<String, Object> br : bindRows) {
					if (br == null) {
						continue;
					}
					long cpid = parseRowLongId(br.get("customize_page_id"));
					if (cpid <= 0L) {
						continue;
					}
					categoryByPageId.put(cpid, br);
				}
				for (Map<String, Object> row : list) {
					long pageId = parseRowLongId(row.get("id"));
					Object pt = row.get("page_type");
					String ptStr = pt == null ? "" : String.valueOf(pt).trim();
					boolean rowIsCategory = "category".equalsIgnoreCase(ptStr);
					Map<String, Object> bind = categoryByPageId.get(pageId);
					if (!rowIsCategory || bind == null) {
						continue;
					}
					if (bind.containsKey("category_id")) {
						row.put("category_id", bind.get("category_id"));
					}
					if (bind.containsKey("category_name")) {
						row.put("category_name", bind.get("category_name"));
					}
				}
			}
		}
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", totalCount);
		data.put("list", list);
		return data;
	}

	public Map<String, Object> getCustomizePageInfo(long pageId, String requestLang) {
		Optional<WeappCustomizePage> opt = weappCustomizePageRepository.findById(pageId);
		if (opt.isEmpty()) {
			throw new ResourceException("自定义页面不存在");
		}
		WeappCustomizePage entity = opt.get();
		Map<String, Object> row = toCustomizePageListRow(entity);
		long langCompanyId = parseRowLongId(row.get("company_id"));
		weappCustomizePageListLangFacade.enrichWeappCustomizePageListRows(langCompanyId, List.of(row), requestLang);
		return row;
	}

	public long resolveFrontSalespersonCustomPageId(long companyId, String templateName) {
		String trimmed = templateName == null ? "" : templateName.trim();
		return weappCustomizePageRepository
				.findOneByCompanyIdAndPageTypeAndTemplateName(companyId, "salesperson", trimmed)
				.map(WeappCustomizePage::getId)
				.filter(id -> id != null && id > 0L)
				.orElse(0L);
	}

	public long resolveFrontMyCustomPageId(long companyId, long regionauthId) {
		return weappCustomizePageRepository
				.findTopOpenMyPageForFront(companyId, regionauthId)
				.map(WeappCustomizePage::getId)
				.filter(id -> id != null && id > 0L)
				.orElse(0L);
	}

	public Map<String, Object> getSalespersonCustomizePage(long companyId, String templateName, String requestLang) {
		String trimmed = templateName == null ? "" : templateName.trim();
		if (!StringUtils.hasText(trimmed)) {
			throw new BadRequestException("模版名称不能为空", 422);
		}
		Optional<WeappCustomizePage> opt =
				weappCustomizePageRepository.findOneByCompanyIdAndPageTypeAndTemplateName(companyId, "salesperson", trimmed);
		if (opt.isPresent()) {
			WeappCustomizePage entity = opt.get();
			LinkedHashMap<String, Object> out = new LinkedHashMap<>();
			out.put("id", entity.getId());
			return out;
		}
		WeappCustomizePage entity = new WeappCustomizePage();
		entity.setCompanyId(companyId);
		entity.setPageType("salesperson");
		entity.setTemplateName(trimmed);
		entity.setPageName("导购货架首页");
		entity.setPageDescription("导购货架首页");
		entity.setPageShareTitle("导购货架");
		entity.setPageShareDesc("导购货架");
		entity.setIsOpen(1);
		weappCustomizePageRepository.insertCustomizePage(entity);
		LinkedHashMap<String, String> langBag = new LinkedHashMap<>();
		langBag.put("page_name", "导购货架首页");
		langBag.put("page_description", "导购货架首页");
		langBag.put("page_share_title", "导购货架");
		langBag.put("page_share_desc", "导购货架");
		weappCustomizePageLangWriteFacade.saveInitialSalespersonCustomizePageLangIfNeeded(companyId, entity.getId(), requestLang,
				langBag);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("id", entity.getId());
		return out;
	}

	public CustomizePageCreateOutcome createCustomizePage(long companyId, Map<String, Object> mergedInput) {
		String pageTypeNorm = normalizePageType(mergedInput.get("page_type"));

		String templateTrim = trimToEmpty(mergedInput.get("template_name"));
		if (!StringUtils.hasText(templateTrim)) {
			throw new BadRequestException("模版名称不能为空", 422);
		}
		String pageNameTrim = trimToEmpty(mergedInput.get("page_name"));
		if (!StringUtils.hasText(pageNameTrim)) {
			throw new BadRequestException("自定义页面名称不能为空", 422);
		}
		String pageDescTrim = trimToEmpty(mergedInput.get("page_description"));
		if (!StringUtils.hasText(pageDescTrim)) {
			throw new BadRequestException("页面描述不能为空", 422);
		}
		if (!ALLOWED_PAGE_TYPES.contains(pageTypeNorm)) {
			throw new BadRequestException("页面类型不能为空", 422);
		}

		boolean openFlag = resolveIsOpen(mergedInput);

		if (openFlag && "my".equals(pageTypeNorm)) {
			String templateFilter = null;
			if (mergedInput.containsKey("template_name")) {
				String t = trimToEmpty(mergedInput.get("template_name"));
				if (StringUtils.hasText(t)) {
					templateFilter = t;
				}
			}
			long cnt = weappCustomizePageRepository.countOpenMyForCompany(companyId, templateFilter);
			if (cnt > 0) {
				return CustomizePageCreateOutcome.forDuplicateMy();
			}
		}

		WeappCustomizePage entity = new WeappCustomizePage();
		entity.setCompanyId(companyId);
		entity.setTemplateName(templateTrim);
		entity.setPageName(pageNameTrim);
		entity.setPageDescription(pageDescTrim);
		entity.setPageType(pageTypeNorm);
		entity.setIsOpen(openFlag ? 1 : 0);

		if (mergedInput.containsKey("page_share_title") && mergedInput.get("page_share_title") != null) {
			entity.setPageShareTitle(String.valueOf(mergedInput.get("page_share_title")));
		}
		if (mergedInput.containsKey("page_share_desc") && mergedInput.get("page_share_desc") != null) {
			entity.setPageShareDesc(String.valueOf(mergedInput.get("page_share_desc")));
		}
		if (mergedInput.containsKey("page_share_imageUrl") && mergedInput.get("page_share_imageUrl") != null) {
			entity.setPageShareImageUrl(String.valueOf(mergedInput.get("page_share_imageUrl")));
		}

		if (mergedInput.containsKey("regionauth_id") && mergedInput.get("regionauth_id") != null) {
			try {
				entity.setRegionauthId(Long.parseLong(String.valueOf(mergedInput.get("regionauth_id")).trim()));
			} catch (NumberFormatException e) {
				throw new BadRequestException("regionauth_id 格式不正确", 422);
			}
		}

		weappCustomizePageRepository.insertCustomizePage(entity);

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", entity.getId());
		row.put("template_name", entity.getTemplateName());
		row.put("company_id", entity.getCompanyId());
		row.put("regionauth_id", entity.getRegionauthId());
		row.put("page_name", entity.getPageName());
		row.put("page_description", entity.getPageDescription());
		row.put("page_share_title", entity.getPageShareTitle());
		row.put("page_share_desc", entity.getPageShareDesc());
		row.put("page_share_imageUrl", entity.getPageShareImageUrl());
		row.put("is_open", entity.getIsOpen());
		row.put("page_type", entity.getPageType());

		return CustomizePageCreateOutcome.ok(row);
	}

	@Transactional(rollbackFor = Exception.class)
	public CustomizePageUpdateOutcome updateCustomizePage(
			long companyId,
			long pageId,
			Object pageTypeFromRequestOrNull,
			Map<String, Object> mergedKeysFromRequest) {
		boolean effectiveIsOpen = resolveIsOpen(mergedKeysFromRequest);
		WeappCustomizePage entity;

		if (effectiveIsOpen) {
			Optional<WeappCustomizePage> currentOpt =
					weappCustomizePageRepository.findByIdAndCompanyId(pageId, companyId);
			if (currentOpt.isEmpty()) {
				throw new ResourceException("自定义页面不存在");
			}
			WeappCustomizePage current = currentOpt.get();

			String effectivePageType;
			if (pageTypeFromRequestOrNull == null) {
				effectivePageType = current.getPageType();
			} else {
				String trimmed = String.valueOf(pageTypeFromRequestOrNull).trim();
				effectivePageType = StringUtils.hasText(trimmed) ? trimmed : current.getPageType();
			}

			if ("my".equals(effectivePageType)) {
				boolean currentIsOpen = current.getIsOpen() != null && current.getIsOpen().intValue() == 1;
				if (!currentIsOpen) {
					String templateFilter = null;
					if (mergedKeysFromRequest.containsKey("template_name")) {
						String t = trimToEmpty(mergedKeysFromRequest.get("template_name"));
						if (StringUtils.hasText(t)) {
							templateFilter = t;
						}
					}
					if (templateFilter == null && StringUtils.hasText(current.getTemplateName())) {
						templateFilter = current.getTemplateName();
					}
					long cnt = weappCustomizePageRepository.countOpenMyForCompanyExcludingId(
							companyId, pageId, templateFilter);
					if (cnt > 0) {
						return CustomizePageUpdateOutcome.forDuplicateMy();
					}
				}
			}
			entity = current;
		} else {
			Optional<WeappCustomizePage> loadOpt =
					weappCustomizePageRepository.findByIdAndCompanyId(pageId, companyId);
			if (loadOpt.isEmpty()) {
				throw new ResourceException("未查询到更新数据");
			}
			entity = loadOpt.get();
		}

		if (mergedKeysFromRequest.containsKey("template_name") && mergedKeysFromRequest.get("template_name") != null) {
			entity.setTemplateName(String.valueOf(mergedKeysFromRequest.get("template_name")));
		}
		if (mergedKeysFromRequest.containsKey("page_name") && mergedKeysFromRequest.get("page_name") != null) {
			entity.setPageName(String.valueOf(mergedKeysFromRequest.get("page_name")));
		}
		if (mergedKeysFromRequest.containsKey("page_description") && mergedKeysFromRequest.get("page_description") != null) {
			entity.setPageDescription(String.valueOf(mergedKeysFromRequest.get("page_description")));
		}
		if (mergedKeysFromRequest.containsKey("page_share_title") && mergedKeysFromRequest.get("page_share_title") != null) {
			entity.setPageShareTitle(String.valueOf(mergedKeysFromRequest.get("page_share_title")));
		}
		if (mergedKeysFromRequest.containsKey("page_share_desc") && mergedKeysFromRequest.get("page_share_desc") != null) {
			entity.setPageShareDesc(String.valueOf(mergedKeysFromRequest.get("page_share_desc")));
		}
		if (mergedKeysFromRequest.containsKey("page_share_imageUrl") && mergedKeysFromRequest.get("page_share_imageUrl") != null) {
			entity.setPageShareImageUrl(String.valueOf(mergedKeysFromRequest.get("page_share_imageUrl")));
		}
		if (mergedKeysFromRequest.containsKey("regionauth_id") && mergedKeysFromRequest.get("regionauth_id") != null) {
			try {
				entity.setRegionauthId(
						Long.parseLong(String.valueOf(mergedKeysFromRequest.get("regionauth_id")).trim()));
			} catch (NumberFormatException e) {
				throw new BadRequestException("regionauth_id 格式不正确");
			}
		}
		entity.setIsOpen(effectiveIsOpen ? 1 : 0);

		int updated = weappCustomizePageRepository.updateById(entity);
		if (updated != 1) {
			throw new ResourceException("未查询到更新数据");
		}

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", entity.getId());
		row.put("template_name", entity.getTemplateName());
		row.put("company_id", entity.getCompanyId());
		row.put("regionauth_id", entity.getRegionauthId());
		row.put("page_name", entity.getPageName());
		row.put("page_description", entity.getPageDescription());
		row.put("page_share_title", entity.getPageShareTitle());
		row.put("page_share_desc", entity.getPageShareDesc());
		row.put("page_share_imageUrl", entity.getPageShareImageUrl());
		row.put("is_open", entity.getIsOpen());
		row.put("page_type", entity.getPageType());
		return CustomizePageUpdateOutcome.ok(row);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> deleteCustomizePage(long companyId, long pageId) {
		Optional<WeappCustomizePage> pageOpt = weappCustomizePageRepository.findById(pageId);
		if (pageOpt.isEmpty()) {
			throw new ResourceException("自定义页面不存在");
		}
		WeappCustomizePage page = pageOpt.get();
		String templateName = page.getTemplateName() == null ? "" : page.getTemplateName();
		String pageNameKey = "custom_" + pageId;
		weappSettingRepository.deleteHardByCompanyTemplateAndPageName(companyId, templateName, pageNameKey);
		weappCustomizePageRepository.deleteByIdAndCompanyId(pageId, companyId);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("status", Boolean.TRUE);
		return out;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> copy(long companyId, long sourcePageId) {
		try {
			Optional<WeappCustomizePage> srcOpt =
					weappCustomizePageRepository.findByIdAndCompanyId(sourcePageId, companyId);
			if (srcOpt.isEmpty()) {
				throw new ResourceException("模版不存在");
			}
			WeappCustomizePage src = srcOpt.get();
			WeappCustomizePage copyEntity = new WeappCustomizePage();
			copyEntity.setCompanyId(src.getCompanyId());
			copyEntity.setTemplateName(src.getTemplateName());
			copyEntity.setRegionauthId(src.getRegionauthId() == null ? 0L : src.getRegionauthId());
			copyEntity.setPageName(src.getPageName());
			copyEntity.setPageDescription(src.getPageDescription());
			copyEntity.setPageShareTitle(src.getPageShareTitle());
			copyEntity.setPageShareDesc(src.getPageShareDesc());
			copyEntity.setPageShareImageUrl(src.getPageShareImageUrl());
			copyEntity.setPageType(src.getPageType() == null ? "normal" : src.getPageType());
			copyEntity.setIsOpen(0);

			weappCustomizePageRepository.insertCustomizePage(copyEntity);

			String sourcePageNameKey = "custom_" + sourcePageId;
			List<WeappSetting> rows = weappSettingRepository.listByCompanyTemplateAndPageName(
					companyId, src.getTemplateName(), sourcePageNameKey);

			for (WeappSetting row : rows) {
				String rawParams = row.getParams();
				Object decoded = WeappSettingLegacySerializeCodec.decode(rawParams == null ? "" : rawParams);
				LinkedHashMap<String, Object> paramsMap;
				if (decoded instanceof Map<?, ?> m) {
					paramsMap = new LinkedHashMap<>();
					for (Map.Entry<?, ?> e : m.entrySet()) {
						paramsMap.put(String.valueOf(e.getKey()), e.getValue());
					}
				} else if (decoded instanceof List<?> list) {
					paramsMap = new LinkedHashMap<>();
					for (int i = 0; i < list.size(); i++) {
						paramsMap.put(String.valueOf(i), list.get(i));
					}
				} else {
					throw new ResourceException(
							decoded == null ? "params deserialize failed"
									: ("params deserialize failed: " + decoded.getClass().getSimpleName()));
				}
				weappSettingRepository.insertWithParamsIdRewrite(
						row.getCompanyId(),
						row.getTemplateName(),
						"custom_" + copyEntity.getId(),
						row.getName(),
						paramsMap,
						row.getVersion(),
						row.getPagesTemplateId(),
						row.getSortBy());
			}

			Map<String, Object> row = new LinkedHashMap<>();
			row.put("id", copyEntity.getId());
			row.put("template_name", copyEntity.getTemplateName());
			row.put("company_id", copyEntity.getCompanyId());
			row.put("regionauth_id", copyEntity.getRegionauthId());
			row.put("page_name", copyEntity.getPageName());
			row.put("page_description", copyEntity.getPageDescription());
			row.put("page_share_title", copyEntity.getPageShareTitle());
			row.put("page_share_desc", copyEntity.getPageShareDesc());
			row.put("page_share_imageUrl", copyEntity.getPageShareImageUrl());
			row.put("is_open", copyEntity.getIsOpen());
			row.put("page_type", copyEntity.getPageType());
			return row;
		} catch (RuntimeException e) {
			if (e instanceof ResourceException re) {
				throw re;
			}
			throw new ResourceException(e.getMessage());
		}
	}

	/**
	 * 从查询参数字符串解析整数：去除前导空白；可选一位正负号；读取连续十进制数字；遇首个非数字即停止；
	 * 若未读到任何数字则返回 0。
	 */
	private static int parseIntLeadingDigits(String raw) {
		if (raw == null) {
			return 0;
		}
		String s = raw.stripLeading();
		if (s.isEmpty()) {
			return 0;
		}
		int idx = 0;
		int sign = 1;
		char c0 = s.charAt(0);
		if (c0 == '+') {
			idx = 1;
		} else if (c0 == '-') {
			sign = -1;
			idx = 1;
		}
		long acc = 0L;
		boolean anyDigit = false;
		while (idx < s.length()) {
			char c = s.charAt(idx++);
			if (c < '0' || c > '9') {
				break;
			}
			anyDigit = true;
			acc = acc * 10L + (c - '0');
			if (acc > 0x7FFFFFFFL + 1L) {
				return sign >= 0 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
			}
		}
		if (!anyDigit) {
			return 0;
		}
		long v = sign * acc;
		if (v > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		if (v < Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}
		return (int) v;
	}

	private static boolean isPageQueryParamFalsy(String raw) {
		return raw == null || raw.isEmpty() || "0".equals(raw);
	}

	/**
	 * 解析列表查询的 page_type：空串、{@code "0"} 与缺失语义一致时视为未指定，使用默认 {@code normal}。
	 */
	private static String resolvePageTypeWithDefaultNormal(String raw) {
		if (raw == null) {
			return "normal";
		}
		if (raw.isEmpty()) {
			return "normal";
		}
		if ("0".equals(raw)) {
			return "normal";
		}
		return raw;
	}

	private static long parseLongOrZeroRegionauth(String raw) {
		if (raw == null) {
			return 0L;
		}
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Map<String, Object> toCustomizePageListRow(WeappCustomizePage e) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("id", e.getId() == null ? 0L : e.getId());
		row.put("template_name", e.getTemplateName());
		row.put("company_id", e.getCompanyId());
		row.put("regionauth_id", e.getRegionauthId() == null ? 0L : e.getRegionauthId());
		row.put("page_name", e.getPageName());
		row.put("page_description", e.getPageDescription());
		row.put("page_share_title", e.getPageShareTitle());
		row.put("page_share_desc", e.getPageShareDesc());
		row.put("page_share_imageUrl", e.getPageShareImageUrl());
		row.put("is_open", e.getIsOpen());
		row.put("page_type", e.getPageType());
		return row;
	}

	private static long parseRowLongId(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String normalizePageType(Object raw) {
		if (raw == null) {
			return "normal";
		}
		if (raw instanceof Number n && n.intValue() == 0) {
			return "normal";
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty() || "0".equals(s)) {
			return "normal";
		}
		return s;
	}

	private static String trimToEmpty(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}

	private static boolean resolveIsOpen(Map<String, Object> mergedInput) {
		if (!mergedInput.containsKey("is_open")) {
			return true;
		}
		Object v = mergedInput.get("is_open");
		if (v == null) {
			return true;
		}
		if (Boolean.FALSE.equals(v)) {
			return false;
		}
		if (v instanceof Number n && n.intValue() == 0) {
			return false;
		}
		String s = String.valueOf(v).trim();
		if (s.equalsIgnoreCase("false") || "0".equals(s)) {
			return false;
		}
		return true;
	}

	public static final class CustomizePageCreateOutcome {
		private final boolean duplicateMy;
		private final Map<String, Object> successData;

		private CustomizePageCreateOutcome(boolean duplicateMy, Map<String, Object> successData) {
			this.duplicateMy = duplicateMy;
			this.successData = successData;
		}

		public static CustomizePageCreateOutcome forDuplicateMy() {
			return new CustomizePageCreateOutcome(true, null);
		}

		public static CustomizePageCreateOutcome ok(Map<String, Object> row) {
			return new CustomizePageCreateOutcome(false, row);
		}

		public boolean duplicateMy() {
			return duplicateMy;
		}

		public Map<String, Object> successData() {
			return successData;
		}
	}

	public static final class CustomizePageUpdateOutcome {
		private final boolean duplicateMy;
		private final Map<String, Object> successData;

		private CustomizePageUpdateOutcome(boolean duplicateMy, Map<String, Object> successData) {
			this.duplicateMy = duplicateMy;
			this.successData = successData;
		}

		public static CustomizePageUpdateOutcome forDuplicateMy() {
			return new CustomizePageUpdateOutcome(true, null);
		}

		public static CustomizePageUpdateOutcome ok(Map<String, Object> row) {
			return new CustomizePageUpdateOutcome(false, row);
		}

		public boolean duplicateMy() {
			return duplicateMy;
		}

		public Map<String, Object> successData() {
			return successData;
		}
	}
}
