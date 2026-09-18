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

import cn.shopex.ecshopx.common.wechat.WxappPagestemplateBaseinfoSupportFacade;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappPagestemplateBaseinfoService {

	private final WxappPageParamsSettingService wxappPageParamsSettingService;
	private final WxappPagestemplateBaseinfoSupportFacade supportFacade;

	public WxappPagestemplateBaseinfoService(
			WxappPageParamsSettingService wxappPageParamsSettingService,
			WxappPagestemplateBaseinfoSupportFacade supportFacade) {
		this.wxappPageParamsSettingService = wxappPageParamsSettingService;
		this.supportFacade = supportFacade;
	}

	public Map<String, Object> getPagestemplateBaseinfo(
			long companyId,
			long userId,
			long jwtDistributorId,
			String templateName,
			String versionQuery,
			String regionauthIdRaw,
			String countryCodeRaw) {
		String locale =
				countryCodeRaw == null || countryCodeRaw.isBlank()
						? "zh-CN"
						: countryCodeRaw.trim();
		Object raw =
				wxappPageParamsSettingService.getParamByTempName(
						companyId,
						userId,
						templateName,
						"",
						"color_style",
						versionQuery,
						0L,
						0L,
						locale,
						jwtDistributorId);

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("color_style", extractColorStyle(raw));

		long regionauthIdForQuery = resolveRegionauthForPagesTemplate(regionauthIdRaw);
		supportFacade
				.pagesTemplateSetRow(companyId, locale, regionauthIdForQuery)
				.ifPresent(merged::putAll);

		merged.put("title", supportFacade.wxShopsBrandNameForTitle(companyId, locale));

		return merged;
	}

	private static LinkedHashMap<String, Object> extractColorStyle(Object raw) {
		String primary = "";
		String accent = "";
		String marketing = "";
		if (!(raw instanceof Map<?, ?> rawMap)) {
			return buildColorStyle(primary, accent, marketing);
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) (Map<?, ?>) rawMap;
		Object listObj = map.get("list");
		if (!(listObj instanceof List<?> list) || list.isEmpty()) {
			return buildColorStyle(primary, accent, marketing);
		}
		Object first = list.get(0);
		if (!(first instanceof Map<?, ?> firstRow)) {
			return buildColorStyle(primary, accent, marketing);
		}
		Object paramsObj = firstRow.get("params");
		if (!(paramsObj instanceof Map<?, ?> paramsMap)) {
			return buildColorStyle(primary, accent, marketing);
		}
		Object dataObj = paramsMap.get("data");
		if (!(dataObj instanceof List<?> dataList) || dataList.isEmpty()) {
			return buildColorStyle(primary, accent, marketing);
		}
		Object dataFirst = dataList.get(0);
		if (!(dataFirst instanceof Map<?, ?> dataMap)) {
			return buildColorStyle(primary, accent, marketing);
		}
		primary = strOrEmpty(dataMap.get("primary"));
		accent = strOrEmpty(dataMap.get("accent"));
		marketing = strOrEmpty(dataMap.get("marketing"));
		return buildColorStyle(primary, accent, marketing);
	}

	private static LinkedHashMap<String, Object> buildColorStyle(
			String primary, String accent, String marketing) {
		LinkedHashMap<String, Object> colorStyle = new LinkedHashMap<>();
		colorStyle.put("primary", primary);
		colorStyle.put("accent", accent);
		colorStyle.put("marketing", marketing);
		return colorStyle;
	}

	private static String strOrEmpty(Object v) {
		if (v == null) {
			return "";
		}
		return String.valueOf(v);
	}

	private static long resolveRegionauthForPagesTemplate(String regionauthIdRaw) {
		return parsePositiveLongOrZero(regionauthIdRaw);
	}

	private static long parsePositiveLongOrZero(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s && StringUtils.hasText(s)) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
