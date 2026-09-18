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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappPromotionArticlesService {

	private final SuperadminWxappTemplateMetadataService superadminWxappTemplateMetadataService;
	private final WeappSettingRepository weappSettingRepository;
	private final MerchantBaseSettingSaveService merchantBaseSettingSaveService;
	private final WeappSettingOutsideLangParamsLoader weappSettingOutsideLangParamsLoader;
	private final WxaWeappTemplateParamsEnrichPort wxaWeappTemplateParamsEnrichPort;

	@Value("${ecshopx.request-field.oem-shuyun:false}")
	private boolean oemShuyun;

	public WxappPromotionArticlesService(
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

	public List<Map<String, Object>> getPromotionArticles(
			long companyId,
			String templateNameRaw,
			String nameOrNull,
			String pageNameRaw,
			String countryCodeRaw) {
		String tn = templateNameRaw == null ? "" : templateNameRaw.trim();
		superadminWxappTemplateMetadataService.assertValidWxappTemplateForPageParams(tn);
		String pageName = (pageNameRaw == null || pageNameRaw.isBlank()) ? "index" : pageNameRaw.trim();
		String configNameFilter =
				(nameOrNull != null && StringUtils.hasText(nameOrNull.trim())) ? nameOrNull.trim() : null;
		List<WeappSetting> rows =
				weappSettingRepository.listForAdminPageParams(companyId, tn, pageName, "v1.0.0", configNameFilter);
		if (rows.isEmpty()) {
			return new ArrayList<>();
		}
		String langTag = resolveLangTag(countryCodeRaw);
		List<Long> ids = rows.stream().map(WeappSetting::getId).toList();
		Map<Long, String> langById =
				weappSettingOutsideLangParamsLoader.findParamsByLocale(companyId, ids, langTag);
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
			if (shouldEnrichGoodsWidgetForAdmin(widgetName, params)) {
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
		return list;
	}

	public Object getPromotionArticlesInfo(
			long companyId,
			String templateNameRaw,
			String nameOrNull,
			String pageNameRaw,
			String countryCodeRaw,
			String indexRaw) {
		List<Map<String, Object>> list =
				getPromotionArticles(companyId, templateNameRaw, nameOrNull, pageNameRaw, countryCodeRaw);
		if (list.isEmpty()) {
			return new ArrayList<>();
		}
		Map<String, Object> rawBlock = extractPromotionArticleParamsItemByIndex(list, indexRaw);
		if (rawBlock == null) {
			return null;
		}
		return stripViewContentOnlyCopy(rawBlock);
	}

	private static Map<String, Object> extractPromotionArticleParamsItemByIndex(
			List<Map<String, Object>> list, String indexRaw) {
		if (list == null || list.isEmpty()) {
			return null;
		}
		Map<String, Object> first = list.get(0);
		if (first == null) {
			return null;
		}
		Object paramsObj = first.get("params");
		if (paramsObj instanceof List<?> rawList) {
			int i = resolveListIndexFromRaw(indexRaw);
			if (i < 0 || i >= rawList.size()) {
				return null;
			}
			Object el = rawList.get(i);
			return el instanceof Map<?, ?> m ? shallowStringKeyMapCopy(m) : null;
		}
		if (paramsObj instanceof Map<?, ?> rawMap) {
			Object el = rawMap.get(indexRaw);
			if (el == null && indexRaw != null) {
				el = rawMap.get(String.valueOf(indexRaw));
			}
			if (el == null && indexRaw != null) {
				try {
					el = rawMap.get(Integer.parseInt(indexRaw.trim()));
				} catch (NumberFormatException ignored) {
					// keep el null
				}
			}
			return el instanceof Map<?, ?> m ? shallowStringKeyMapCopy(m) : null;
		}
		return null;
	}

	private static int resolveListIndexFromRaw(String indexRaw) {
		if (indexRaw == null) {
			return -1;
		}
		try {
			return Integer.parseInt(indexRaw.trim());
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private static LinkedHashMap<String, Object> shallowStringKeyMapCopy(Map<?, ?> m) {
		LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : m.entrySet()) {
			copy.put(String.valueOf(e.getKey()), e.getValue());
		}
		return copy;
	}

	private static LinkedHashMap<String, Object> stripViewContentOnlyCopy(Map<String, Object> block) {
		LinkedHashMap<String, Object> copy = new LinkedHashMap<>(block);
		copy.remove("viewcontent");
		return copy;
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

	private static boolean isNonEmptyDataList(Object dataObj) {
		return dataObj instanceof List<?> list && !list.isEmpty();
	}

	private static boolean hasGoodsGridTabListContainer(Object listObj) {
		if (listObj == null) {
			return false;
		}
		return listObj instanceof Map<?, ?> || listObj instanceof List<?>;
	}
}
