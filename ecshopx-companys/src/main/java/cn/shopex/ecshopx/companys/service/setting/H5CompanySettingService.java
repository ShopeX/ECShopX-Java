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

package cn.shopex.ecshopx.companys.service.setting;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class H5CompanySettingService {

	private static final String MSG_LANG_CONFIG_MISSING = "不存在的多语言配置，请在多语言模块（langue）中补齐后再试";

	private final WxShopsSettingRedisReadService wxShopsSettingRedisReadService;
	private final CompanyBaseSettingService companyBaseSettingService;
	private final OpenDistributorDividedSettingRedisService openDistributorDividedSettingRedisService;
	private final LangueProperties langueProperties;

	public H5CompanySettingService(
			WxShopsSettingRedisReadService wxShopsSettingRedisReadService,
			CompanyBaseSettingService companyBaseSettingService,
			OpenDistributorDividedSettingRedisService openDistributorDividedSettingRedisService,
			LangueProperties langueProperties) {
		this.wxShopsSettingRedisReadService = wxShopsSettingRedisReadService;
		this.companyBaseSettingService = companyBaseSettingService;
		this.openDistributorDividedSettingRedisService = openDistributorDividedSettingRedisService;
		this.langueProperties = langueProperties;
	}

	public Map<String, Object> getCompanySetting(long companyId, String requestLang) {
		Map<String, Object> loaded = wxShopsSettingRedisReadService.load(companyId);
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();

		if (!loaded.isEmpty()) {
			List<String> langList = langueProperties.getList();
			if (langList == null || langList.isEmpty()) {
				throw new ResourceException(MSG_LANG_CONFIG_MISSING);
			}
			Map<String, Object> wx = WxShopsSettingLangSliceHelper.innerMapForLang(loaded, requestLang);
			if (!wx.isEmpty()) {
				result.putAll(wx);
			}
		}

		Map<String, Object> settingOnly =
				companyBaseSettingService.getSetting(companyId, null, null, null, "true");
		Object cs = settingOnly.get("customer_switch");
		int customerSwitch = (cs instanceof Number n) ? n.intValue() : 0;
		result.put("customer_switch", customerSwitch);

		Map<String, Object> openDivided =
				openDistributorDividedSettingRedisService.getOpenDistributorDivided(companyId);
		result.put("open_divided", openDivided);

		return result;
	}
}
