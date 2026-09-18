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

import cn.shopex.ecshopx.companys.service.protocol.ShopProtocolSetService;
import cn.shopex.ecshopx.companys.service.setting.WxShopsSettingLangSliceHelper;
import cn.shopex.ecshopx.companys.service.setting.WxShopsSettingRedisReadService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxShopsBaseInfoService {

	/**
	 * storefront wx shop Redis buckets and protocol hashes follow the same country code as stored
	 * settings; the query parameter defaults to zh-CN when absent or blank.
	 */
	public static String resolveCountryCodeForWxSetting(HttpServletRequest request) {
		String cc = request.getParameter("country_code");
		if (cc == null) {
			return "zh-CN";
		}
		String t = cc.trim();
		if (t.isEmpty()) {
			return "zh-CN";
		}
		return t;
	}

	private final WxShopsSettingRedisReadService wxShopsSettingRedisReadService;
	private final ShopProtocolSetService shopProtocolSetService;

	public WxShopsBaseInfoService(
			WxShopsSettingRedisReadService wxShopsSettingRedisReadService,
			ShopProtocolSetService shopProtocolSetService) {
		this.wxShopsSettingRedisReadService = wxShopsSettingRedisReadService;
		this.shopProtocolSetService = shopProtocolSetService;
	}

	public Map<String, Object> getBaseInfo(long companyId, String requestLang) {
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		Map<String, Object> loaded = wxShopsSettingRedisReadService.load(companyId);
		if (!loaded.isEmpty()) {
			Map<String, Object> slice = WxShopsSettingLangSliceHelper.innerMapForLang(loaded, requestLang);
			if (!slice.isEmpty()) {
				data.putAll(slice);
			}
		}
		String lang = requestLang.trim();
		Map<String, Object> mrBlock = shopProtocolSetService.get(companyId, "member_register", lang);
		Map<String, Object> prBlock = shopProtocolSetService.get(companyId, "privacy", lang);
		Object mrInner = mrBlock.get("member_register");
		Object prInner = prBlock.get("privacy");
		String memberRegisterTitle = titleFromBlock(mrInner);
		String privacyTitle = titleFromBlock(prInner);
		LinkedHashMap<String, String> protocol = new LinkedHashMap<>();
		protocol.put("member_register", memberRegisterTitle);
		protocol.put("privacy", privacyTitle);
		data.put("protocol", protocol);
		return data;
	}

	private static String titleFromBlock(Object o) {
		if (o instanceof Map<?, ?> m) {
			Object t = m.get("title");
			return t == null ? "" : String.valueOf(t);
		}
		return "";
	}
}
