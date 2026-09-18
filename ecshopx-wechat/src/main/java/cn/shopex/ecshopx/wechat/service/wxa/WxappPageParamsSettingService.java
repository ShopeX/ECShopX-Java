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

package cn.shopex.ecshopx.wechat.service.wxa;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.goods.port.WxappCategoryTreeForPageParamsPort;
import cn.shopex.ecshopx.merchant.service.MerchantBaseSettingSaveService;
import cn.shopex.ecshopx.wechat.domain.WeappSetting;
import cn.shopex.ecshopx.wechat.port.WxaWeappTemplateParamsEnrichPort;
import cn.shopex.ecshopx.wechat.repository.WeappSettingRepository;
import cn.shopex.ecshopx.wechat.service.CustomizePageService;
import cn.shopex.ecshopx.wechat.support.WeappSettingOutsideLangParamsLoader;
import cn.shopex.ecshopx.wechat.support.WeappSettingLegacySerializeCodec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WxappPageParamsSettingService {

	private final SuperadminWxappTemplateMetadataService superadminWxappTemplateMetadataService;
	private final WeappSettingRepository weappSettingRepository;
	private final WeappSettingOutsideLangParamsLoader weappSettingOutsideLangParamsLoader;
	private final MerchantBaseSettingSaveService merchantBaseSettingSaveService;
	private final WxaWeappTemplateParamsEnrichPort wxaWeappTemplateParamsEnrichPort;
	private final CustomizePageService customizePageService;
	private final WxappCategoryTreeForPageParamsPort wxappCategoryTreeForPageParamsPort;

	@Value("${ecshopx.request-field.oem-shuyun:false}")
	private boolean oemShuyun;

	public WxappPageParamsSettingService(
			SuperadminWxappTemplateMetadataService superadminWxappTemplateMetadataService,
			WeappSettingRepository weappSettingRepository,
			WeappSettingOutsideLangParamsLoader weappSettingOutsideLangParamsLoader,
			MerchantBaseSettingSaveService merchantBaseSettingSaveService,
			WxaWeappTemplateParamsEnrichPort wxaWeappTemplateParamsEnrichPort,
			CustomizePageService customizePageService,
			WxappCategoryTreeForPageParamsPort wxappCategoryTreeForPageParamsPort) {
		this.superadminWxappTemplateMetadataService = superadminWxappTemplateMetadataService;
		this.weappSettingRepository = weappSettingRepository;
		this.weappSettingOutsideLangParamsLoader = weappSettingOutsideLangParamsLoader;
		this.merchantBaseSettingSaveService = merchantBaseSettingSaveService;
		this.wxaWeappTemplateParamsEnrichPort = wxaWeappTemplateParamsEnrichPort;
		this.customizePageService = customizePageService;
		this.wxappCategoryTreeForPageParamsPort = wxappCategoryTreeForPageParamsPort;
	}

	public Object getParamByTempName(
			long companyId,
			long userId,
			String templateName,
			String nameQuery,
			String pageNameQuery,
			String versionQuery,
			long distributorId,
			long regionauthId,
			String countryCode,
			long jwtDistributorId) {
		String pageName =
				pageNameQuery == null || pageNameQuery.isBlank() ? "index" : pageNameQuery.trim();
		boolean customFlag = false;
		long customIdForShare = 0L;

		if (pageName.contains("custom_")) {
			String suffix = pageName.substring("custom_".length());
			long customId;
			if ("salesperson".equals(suffix)) {
				customId = customizePageService.resolveFrontSalespersonCustomPageId(companyId, templateName);
			} else if ("my".equals(suffix)) {
				customId = customizePageService.resolveFrontMyCustomPageId(companyId, regionauthId);
			} else {
				customId = parseLongOrZero(suffix);
			}
			if (customId == 0L) {
				LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
				empty.put("list", new ArrayList<>());
				empty.put("config", new ArrayList<>());
				return empty;
			}
			pageName = "custom_" + customId;
			customFlag = true;
			customIdForShare = customId;
		}

		superadminWxappTemplateMetadataService.assertValidWxappTemplateForPageParams(templateName);
		String configNameFilter = isAggregateModeName(nameQuery) ? null : nameQuery.trim();
		List<WeappSetting> rows =
				weappSettingRepository.listForAdminPageParams(
						companyId,
						templateName,
						pageName,
						resolveFrontVersion(versionQuery),
						configNameFilter);
		List<Long> ids = rows.stream().map(WeappSetting::getId).collect(Collectors.toList());
		String localeTag =
				countryCode == null || countryCode.isBlank() ? "zh-CN" : countryCode.trim();
		Map<Long, String> langById =
				weappSettingOutsideLangParamsLoader.findParamsByLocale(companyId, ids, localeTag);
		List<Map<String, Object>> list = new ArrayList<>();
		Map<String, Object> companyBaseForMerchantStatus = null;
		for (WeappSetting row : rows) {
			Object paramsObj =
					WeappSettingLegacySerializeCodec.decode(row.getParams() == null ? "" : row.getParams());
			String langRaw = langById.get(row.getId());
			if (langRaw != null && !langRaw.isBlank()) {
				paramsObj = WeappSettingLegacySerializeCodec.decode(langRaw);
			}
			if (!(paramsObj instanceof Map<?, ?> rawMap)) {
				throw new ResourceException("装修配置格式无效");
			}
			LinkedHashMap<String, Object> params = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : rawMap.entrySet()) {
				params.put(String.valueOf(e.getKey()), e.getValue());
			}
			String widgetName = row.getName();
			if (shouldEnrichGoodsWidget(widgetName, params)) {
				Boolean merchantStatusPerItemOrNull;
				if (oemShuyun) {
					merchantStatusPerItemOrNull = null;
				} else {
					if (companyBaseForMerchantStatus == null) {
						companyBaseForMerchantStatus =
								merchantBaseSettingSaveService.loadCompanyBaseSetting(companyId);
					}
					Object st = companyBaseForMerchantStatus.get("status");
					merchantStatusPerItemOrNull = (st instanceof Boolean b) ? b : Boolean.FALSE;
				}
				wxaWeappTemplateParamsEnrichPort.enrichAdminRow(
						companyId, widgetName, params, merchantStatusPerItemOrNull);
			}
			if (params.containsKey("is_open") && isOpenLooselyFalse(params.get("is_open"))) {
				params.put("data", new ArrayList<>());
			}
			String pageNameOut =
					row.getPageName() == null || row.getPageName().isBlank()
							? "index"
							: row.getPageName();
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("id", row.getId());
			item.put("template_name", row.getTemplateName());
			item.put("company_id", row.getCompanyId());
			item.put("name", row.getName());
			item.put("page_name", pageNameOut);
			item.put("params", params);
			list.add(item);
		}

		if ("category".equals(pageName) && distributorId != 0L && !list.isEmpty()) {
			Object p0 = list.get(0).get("params");
			if (p0 instanceof Map<?, ?>) {
				@SuppressWarnings("unchecked")
				Map<String, Object> paramsWritable = (Map<String, Object>) (Map<?, ?>) p0;
				List<Map<String, Object>> tree =
						wxappCategoryTreeForPageParamsPort.loadTreeForWxappPageParams(
								companyId, distributorId, localeTag, jwtDistributorId);
				if (tree != null && !tree.isEmpty()) {
					paramsWritable.put("data", tree);
				}
			}
		}

		if (isAggregateModeName(nameQuery)) {
			LinkedHashMap<String, Object> out = new LinkedHashMap<>();
			out.put("list", list);
			out.put("config", WxaPageParamsSettingService.buildConfigFromList(list));
			if (customFlag) {
				out.put("share", customizePageService.getCustomizePageInfo(customIdForShare, localeTag));
			}
			return out;
		}
		return list;
	}

	private static String resolveFrontVersion(String versionQuery) {
		String v = versionQuery == null ? "" : versionQuery.trim();
		return v.isEmpty() ? "v1.0.0" : v;
	}

	private static boolean isAggregateModeName(String nameQuery) {
		return nameQuery == null || nameQuery.isBlank() || "0".equals(nameQuery);
	}

	private static long parseLongOrZero(String s) {
		if (s == null || s.isBlank()) {
			return 0L;
		}
		try {
			return Long.parseLong(s.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static boolean isOpenLooselyFalse(Object v) {
		if (v instanceof Boolean b) {
			return !b;
		}
		if (v instanceof Number n) {
			return n.longValue() == 0L;
		}
		if (v instanceof String s) {
			return "0".equals(s.trim());
		}
		return false;
	}

	private static boolean isNonEmptyDataList(Object dataObj) {
		return dataObj instanceof List<?> l && !l.isEmpty();
	}

	private static boolean shouldEnrichGoodsWidget(String widgetName, LinkedHashMap<String, Object> params) {
		if (widgetName == null) {
			return false;
		}
		return switch (widgetName) {
			case "goodsScroll", "goodsGrid" -> isNonEmptyDataList(params.get("data"));
			case "goodsGridTab" -> hasGoodsGridTabListContainer(params.get("list"));
			default -> false;
		};
	}

	private static boolean hasGoodsGridTabListContainer(Object listObj) {
		if (listObj == null) {
			return false;
		}
		return listObj instanceof Map<?, ?> || listObj instanceof List<?>;
	}
}
