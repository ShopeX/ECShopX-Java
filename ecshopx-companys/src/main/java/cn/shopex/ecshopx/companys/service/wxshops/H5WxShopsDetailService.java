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

package cn.shopex.ecshopx.companys.service.wxshops;

import cn.shopex.ecshopx.companys.service.setting.WxShopsSettingGetService;
import cn.shopex.ecshopx.companys.service.setting.WxShopsSettingLangSliceHelper;
import cn.shopex.ecshopx.companys.service.setting.WxShopsSettingRedisReadService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class H5WxShopsDetailService {

	private final WxShopsDetailService wxShopsDetailService;
	private final WxShopsSettingGetService wxShopsSettingGetService;
	private final WxShopsSettingRedisReadService wxShopsSettingRedisReadService;

	public H5WxShopsDetailService(
			WxShopsDetailService wxShopsDetailService,
			WxShopsSettingGetService wxShopsSettingGetService,
			WxShopsSettingRedisReadService wxShopsSettingRedisReadService) {
		this.wxShopsDetailService = wxShopsDetailService;
		this.wxShopsSettingGetService = wxShopsSettingGetService;
		this.wxShopsSettingRedisReadService = wxShopsSettingRedisReadService;
	}

	public Map<String, Object> getWxShopsDetail(long wxShopId, String countryCodeForWxSetting) {
		Map<String, Object> core = wxShopsDetailService.getWxShopsDetail(wxShopId);
		LinkedHashMap<String, Object> result = new LinkedHashMap<>(core);
		Object cid = result.get("company_id");
		if (!companyIdTruthy(cid)) {
			result.put("base_setting", emptyBaseSetting());
		} else {
			Long companyId = resolveCompanyIdForSetting(cid);
			if (companyId == null) {
				result.put("base_setting", emptyBaseSetting());
			} else {
				String lang = effectiveLangForWxSetting(countryCodeForWxSetting);
				Map<String, Object> loaded = wxShopsSettingRedisReadService.load(companyId);
				Map<String, Object> slice = WxShopsSettingLangSliceHelper.innerMapForLang(loaded, lang);
				if (slice.isEmpty() && !loaded.isEmpty() && !"zh-CN".equals(lang)) {
					result.put("base_setting", Collections.emptyList());
				} else {
					result.put(
							"base_setting",
							wxShopsSettingGetService.getWxShopsSetting(companyId, countryCodeForWxSetting));
				}
			}
		}
		return result;
	}

	/** 与 {@link WxShopsSettingGetService#getWxShopsSetting} 使用的语言键一致。 */
	private static String effectiveLangForWxSetting(String countryCodeRaw) {
		return (countryCodeRaw == null || countryCodeRaw.isBlank()) ? "zh-CN" : countryCodeRaw.trim();
	}

	private static LinkedHashMap<String, Object> emptyBaseSetting() {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("logo", "");
		m.put("intro", "");
		return m;
	}

	/** 判断 {@code company_id} 是否应触发门店基础配置加载：0、空串、{@code "0"} 为否；非数字非空串为是。 */
	private static boolean companyIdTruthy(Object cid) {
		if (cid == null) {
			return false;
		}
		if (cid instanceof Number n) {
			return n.longValue() != 0L;
		}
		String s = cid.toString().trim();
		if (s.isEmpty() || "0".equals(s)) {
			return false;
		}
		try {
			return Long.parseLong(s) != 0L;
		} catch (NumberFormatException e) {
			return true;
		}
	}

	private static Long resolveCompanyIdForSetting(Object cid) {
		try {
			if (cid instanceof Number n) {
				return n.longValue();
			}
			return Long.parseLong(cid.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
