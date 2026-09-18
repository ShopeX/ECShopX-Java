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
import cn.shopex.ecshopx.merchant.service.MerchantBaseSettingSaveService;
import cn.shopex.ecshopx.wechat.domain.WeappSetting;
import cn.shopex.ecshopx.wechat.port.WxaWeappTemplateParamsEnrichPort;
import cn.shopex.ecshopx.wechat.repository.WeappSettingRepository;
import cn.shopex.ecshopx.wechat.support.WeappSettingOutsideLangParamsLoader;
import cn.shopex.ecshopx.wechat.support.WeappSettingLegacySerializeCodec;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WxappMemberCenterSettingService {

	private static final List<String> MEMBER_CENTER_PAGE_NAMES = List.of(
			"member_center_setting",
			"member_center_redirect_setting",
			"member_center_menu_setting");

	private static final List<String> PAGESTEMPLATE_MEMBERCENTER_PAGE_NAMES = List.of(
			"member_center_setting",
			"member_center_redirect_setting");

	private final SuperadminWxappTemplateMetadataService superadminWxappTemplateMetadataService;
	private final WeappSettingRepository weappSettingRepository;
	private final MerchantBaseSettingSaveService merchantBaseSettingSaveService;
	private final WeappSettingOutsideLangParamsLoader weappSettingOutsideLangParamsLoader;
	private final WxaWeappTemplateParamsEnrichPort wxaWeappTemplateParamsEnrichPort;

	@Value("${ecshopx.request-field.oem-shuyun:false}")
	private boolean oemShuyun;

	public WxappMemberCenterSettingService(
			SuperadminWxappTemplateMetadataService superadminWxappTemplateMetadataService,
			WeappSettingRepository weappSettingRepository,
			MerchantBaseSettingSaveService merchantBaseSettingSaveService,
			WeappSettingOutsideLangParamsLoader weappSettingOutsideLangParamsLoader,
			WxaWeappTemplateParamsEnrichPort wxaWeappTemplateParamsEnrichPort) {
		this.superadminWxappTemplateMetadataService = superadminWxappTemplateMetadataService;
		this.weappSettingRepository = weappSettingRepository;
		this.merchantBaseSettingSaveService = merchantBaseSettingSaveService;
		this.weappSettingOutsideLangParamsLoader = weappSettingOutsideLangParamsLoader;
		this.wxaWeappTemplateParamsEnrichPort = wxaWeappTemplateParamsEnrichPort;
	}

	public Map<String, Object> getMemberCenterParamByTempName(
			long companyId,
			long userId,
			String templateName,
			String versionQuery,
			String countryCodeRaw) {
		String templateKey = (templateName == null) ? "" : templateName.trim();
		superadminWxappTemplateMetadataService.assertValidWxappTemplateForPageParams(templateKey);
		Map<String, Object> companyBase = merchantBaseSettingSaveService.loadCompanyBaseSetting(companyId);
		String version = resolveVersionForMemberCenter(versionQuery);
		String localeTag = resolveLangTag(countryCodeRaw);
		List<WeappSetting> rows =
				weappSettingRepository.listForMemberCenterPages(companyId, templateKey, version, MEMBER_CENTER_PAGE_NAMES);
		List<Long> ids = rows.stream().map(WeappSetting::getId).toList();
		Map<Long, String> langById =
				weappSettingOutsideLangParamsLoader.findParamsByLocale(companyId, ids, localeTag);
		LinkedHashMap<String, Map<String, Object>> pageNameToParams = new LinkedHashMap<>();
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
			params.put("user_id", Long.valueOf(userId));
			params.put("distributor_id", 0);
			if (!params.containsKey("e_activity_id")) {
				params.put("e_activity_id", 0L);
			}
			String widgetName = row.getName();
			if (shouldEnrichGoodsWidgetForAdmin(widgetName, params)) {
				Boolean merchantStatusPerItemOrNull;
				if (oemShuyun) {
					merchantStatusPerItemOrNull = null;
				} else {
					Object st = companyBase.get("status");
					merchantStatusPerItemOrNull = (st instanceof Boolean b) ? b : Boolean.FALSE;
				}
				wxaWeappTemplateParamsEnrichPort.enrichAdminRow(
						companyId, widgetName, params, merchantStatusPerItemOrNull);
			}
			String pageNameKey = row.getPageName() == null ? "" : row.getPageName();
			pageNameToParams.put(pageNameKey, params);
		}
		if (!pageNameToParams.containsKey("member_center_menu_setting")) {
			LinkedHashMap<String, Object> defaultData = new LinkedHashMap<>();
			defaultData.put("ziti_order", Boolean.TRUE);
			defaultData.put("ext_info", Boolean.TRUE);
			defaultData.put("group", Boolean.TRUE);
			defaultData.put("boost_activity", Boolean.TRUE);
			defaultData.put("boost_order", Boolean.TRUE);
			defaultData.put("complaint", Boolean.TRUE);
			defaultData.put("activity", Boolean.TRUE);
			defaultData.put("recharge", Boolean.TRUE);
			defaultData.put("member_code", Boolean.TRUE);
			LinkedHashMap<String, Object> defaultParams = new LinkedHashMap<>();
			defaultParams.put("data", defaultData);
			pageNameToParams.put("member_center_menu_setting", defaultParams);
		}
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		for (Map<String, Object> pageParams : pageNameToParams.values()) {
			Object inner = pageParams.get("data");
			if (inner instanceof Map<?, ?> m) {
				for (Map.Entry<?, ?> e : m.entrySet()) {
					merged.put(String.valueOf(e.getKey()), e.getValue());
				}
			}
		}
		LinkedHashMap<String, Object> dataInner = new LinkedHashMap<>(merged);
		LinkedHashMap<String, Object> paramsWrap = new LinkedHashMap<>();
		paramsWrap.put("data", dataInner);
		LinkedHashMap<String, Object> listItem = new LinkedHashMap<>();
		listItem.put("params", paramsWrap);
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("list", List.of(listItem));
		return result;
	}

	public Map<String, Object> getPagestemplateMembercenter(
			long companyId,
			long userId,
			String templateName,
			String versionQuery,
			String countryCodeRaw) {
		String templateKey = templateName == null ? "" : templateName.trim();
		superadminWxappTemplateMetadataService.assertValidWxappTemplateForPageParams(templateKey);
		Map<String, Object> companyBase = merchantBaseSettingSaveService.loadCompanyBaseSetting(companyId);
		String version = resolveVersionForMemberCenter(versionQuery);
		String localeTag = resolveLangTag(countryCodeRaw);
		List<WeappSetting> rows = weappSettingRepository.listForMemberCenterPages(
				companyId, templateKey, version, PAGESTEMPLATE_MEMBERCENTER_PAGE_NAMES);
		List<Long> ids = rows.stream().map(WeappSetting::getId).toList();
		Map<Long, String> langById =
				weappSettingOutsideLangParamsLoader.findParamsByLocale(companyId, ids, localeTag);
		LinkedHashMap<String, Map<String, Object>> pageNameToParams = new LinkedHashMap<>();
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
			params.put("user_id", Long.valueOf(userId));
			params.put("distributor_id", 0);
			if (!params.containsKey("e_activity_id")) {
				params.put("e_activity_id", 0L);
			}
			String widgetName = row.getName();
			if (shouldEnrichGoodsWidgetForAdmin(widgetName, params)) {
				Boolean merchantStatusPerItemOrNull;
				if (oemShuyun) {
					merchantStatusPerItemOrNull = null;
				} else {
					Object st = companyBase.get("status");
					merchantStatusPerItemOrNull = (st instanceof Boolean b) ? b : Boolean.FALSE;
				}
				wxaWeappTemplateParamsEnrichPort.enrichAdminRow(
						companyId, widgetName, params, merchantStatusPerItemOrNull);
			}
			String pageNameKey = row.getPageName() == null ? "" : row.getPageName();
			pageNameToParams.put(pageNameKey, params);
		}
		if (!pageNameToParams.containsKey("member_center_menu_setting")) {
			LinkedHashMap<String, Object> defaultData = new LinkedHashMap<>();
			defaultData.put("ziti_order", Boolean.TRUE);
			defaultData.put("ext_info", Boolean.TRUE);
			defaultData.put("group", Boolean.TRUE);
			defaultData.put("boost_activity", Boolean.TRUE);
			defaultData.put("boost_order", Boolean.TRUE);
			defaultData.put("complaint", Boolean.TRUE);
			defaultData.put("activity", Boolean.TRUE);
			defaultData.put("recharge", Boolean.TRUE);
			defaultData.put("member_code", Boolean.TRUE);
			LinkedHashMap<String, Object> defaultParams = new LinkedHashMap<>();
			defaultParams.put("data", defaultData);
			pageNameToParams.put("member_center_menu_setting", defaultParams);
		}
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		for (Map<String, Object> pageParams : pageNameToParams.values()) {
			Object inner = pageParams.get("data");
			if (inner instanceof Map<?, ?> m) {
				for (Map.Entry<?, ?> e : m.entrySet()) {
					merged.put(String.valueOf(e.getKey()), e.getValue());
				}
			}
		}
		return merged;
	}

	private static String resolveVersionForMemberCenter(String versionQuery) {
		String v = versionQuery == null ? "" : versionQuery.trim();
		return v.isEmpty() ? "v1.0.0" : v;
	}

	private static String resolveLangTag(String countryCodeRaw) {
		if (countryCodeRaw == null) {
			return "zh-CN";
		}
		String trimmed = countryCodeRaw.trim();
		if (trimmed.isEmpty()) {
			return "zh-CN";
		}
		return trimmed;
	}

	private static boolean isNonEmptyDataList(Object dataObj) {
		return dataObj instanceof List<?> list && !list.isEmpty();
	}

	private static boolean shouldEnrichGoodsWidgetForAdmin(
			String widgetName, LinkedHashMap<String, Object> params) {
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
