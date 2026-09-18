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

package cn.shopex.ecshopx.bootstrap.wechat;

import cn.shopex.ecshopx.common.wechat.WxappPagestemplateBaseinfoSupportFacade;
import cn.shopex.ecshopx.companys.service.setting.WxShopsSettingGetService;
import cn.shopex.ecshopx.theme.service.PagesTemplateSetGetInfoService;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class WxappPagestemplateBaseinfoSupportFacadeImpl implements WxappPagestemplateBaseinfoSupportFacade {

	private final PagesTemplateSetGetInfoService pagesTemplateSetGetInfoService;
	private final WxShopsSettingGetService wxShopsSettingGetService;

	public WxappPagestemplateBaseinfoSupportFacadeImpl(
			PagesTemplateSetGetInfoService pagesTemplateSetGetInfoService,
			WxShopsSettingGetService wxShopsSettingGetService) {
		this.pagesTemplateSetGetInfoService = pagesTemplateSetGetInfoService;
		this.wxShopsSettingGetService = wxShopsSettingGetService;
	}

	@Override
	public Optional<Map<String, Object>> pagesTemplateSetRow(
			long companyId, String requestLang, long regionauthId) {
		return pagesTemplateSetGetInfoService.getInfo(companyId, requestLang, 0L, regionauthId);
	}

	@Override
	public String wxShopsBrandNameForTitle(long companyId, String countryCode) {
		Map<String, Object> m = wxShopsSettingGetService.getWxShopsSetting(companyId, countryCode);
		Object v = m.get("brand_name");
		return v == null ? "" : Objects.toString(v, "");
	}
}
