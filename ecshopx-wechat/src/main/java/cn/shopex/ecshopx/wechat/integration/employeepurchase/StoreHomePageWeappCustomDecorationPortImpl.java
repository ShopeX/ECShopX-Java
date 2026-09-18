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

package cn.shopex.ecshopx.wechat.integration.employeepurchase;

import cn.shopex.ecshopx.common.port.employeepurchase.StoreHomePageWeappCustomDecorationPort;
import cn.shopex.ecshopx.common.port.employeepurchase.StoreHomePageDecorationConstants;
import cn.shopex.ecshopx.wechat.service.wxa.WxaPageParamsSettingService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class StoreHomePageWeappCustomDecorationPortImpl implements StoreHomePageWeappCustomDecorationPort {

	private final WxaPageParamsSettingService wxaPageParamsSettingService;

	public StoreHomePageWeappCustomDecorationPortImpl(WxaPageParamsSettingService wxaPageParamsSettingService) {
		this.wxaPageParamsSettingService = wxaPageParamsSettingService;
	}

	@Override
	public Map<String, Object> loadCustomPageDecorationDetail(
			long companyId,
			String templateName,
			String customPageName,
			int rowDistributorId,
			int authDistributorId,
			String requestLang) {
		for (String version : customDecorationSettingVersionCandidates(rowDistributorId, authDistributorId)) {
			Object raw =
					wxaPageParamsSettingService.getParamByTempName(
							companyId,
							templateName,
							null,
							customPageName,
							rowDistributorId > 0 ? String.valueOf(rowDistributorId) : null,
							version,
							requestLang);
			Map<String, Object> detail = coercePageTemplateDetail(raw);
			if (pageTemplateDetailHasNonEmptyList(detail)) {
				return detail;
			}
		}
		return emptyPageTemplateDetail();
	}

	private static List<String> customDecorationSettingVersionCandidates(int rowDistributorId, int authDistributorId) {
		int distributorId = rowDistributorId > 0 ? rowDistributorId : authDistributorId;
		List<String> versions = new ArrayList<>();
		if (distributorId > 0) {
			versions.add("shop_" + distributorId);
		}
		versions.add(StoreHomePageDecorationConstants.CUSTOM_DECORATION_SETTING_VERSION);
		return versions.stream().distinct().toList();
	}

	private static Map<String, Object> coercePageTemplateDetail(Object raw) {
		if (raw instanceof Map<?, ?> map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> typed = (Map<String, Object>) (Map<?, ?>) map;
			return normalizePageTemplateDetail(typed);
		}
		if (raw instanceof List<?> list && !list.isEmpty()) {
			LinkedHashMap<String, Object> out = new LinkedHashMap<>();
			out.put("list", list);
			out.put("config", WxaPageParamsSettingService.buildConfigFromList(castListMap(list)));
			return out;
		}
		return emptyPageTemplateDetail();
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> castListMap(List<?> list) {
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object item : list) {
			if (item instanceof Map<?, ?> m) {
				out.add((Map<String, Object>) (Map<?, ?>) m);
			}
		}
		return out;
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

	private static boolean pageTemplateDetailHasNonEmptyList(Map<String, Object> detail) {
		if (detail == null) {
			return false;
		}
		Object listObj = detail.get("list");
		return listObj instanceof List<?> list && !list.isEmpty();
	}
}
