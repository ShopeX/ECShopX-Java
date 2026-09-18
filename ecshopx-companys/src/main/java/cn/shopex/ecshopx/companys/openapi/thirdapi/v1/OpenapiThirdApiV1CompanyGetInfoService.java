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

package cn.shopex.ecshopx.companys.openapi.thirdapi.v1;

import cn.shopex.ecshopx.companys.service.setting.WxShopsSettingLangSliceHelper;
import cn.shopex.ecshopx.companys.service.setting.WxShopsSettingRedisReadService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV1CompanyGetInfoService {

	private final WxShopsSettingRedisReadService wxShopsSettingRedisReadService;

	public OpenapiThirdApiV1CompanyGetInfoService(
			WxShopsSettingRedisReadService wxShopsSettingRedisReadService) {
		this.wxShopsSettingRedisReadService = wxShopsSettingRedisReadService;
	}

	public Map<String, Object> getCompanyInfo(long companyId, String countryCode) {
		String lang = (countryCode == null || countryCode.isBlank())
				? "zh-CN"
				: countryCode.trim();
		Map<String, Object> loaded = wxShopsSettingRedisReadService.load(companyId);
		Map<String, Object> slice = WxShopsSettingLangSliceHelper.innerMapForLang(loaded, lang);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(slice);
		out.put("company_id", companyId);
		return out;
	}
}
