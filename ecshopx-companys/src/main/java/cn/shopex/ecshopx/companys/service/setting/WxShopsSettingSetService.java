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

import cn.shopex.ecshopx.common.distribution.SelfDistributorDisplayUpdatePort;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxShopsSettingSetService {

	private final SelfDistributorDisplayUpdatePort selfDistributorDisplayUpdatePort;
	private final WxShopsSettingRedisReadService wxShopsSettingRedisReadService;

	public WxShopsSettingSetService(
			SelfDistributorDisplayUpdatePort selfDistributorDisplayUpdatePort,
			WxShopsSettingRedisReadService wxShopsSettingRedisReadService) {
		this.selfDistributorDisplayUpdatePort = selfDistributorDisplayUpdatePort;
		this.wxShopsSettingRedisReadService = wxShopsSettingRedisReadService;
	}

	public void setWxShopsSetting(long companyId, Map<String, Object> merged) {
		if (merged == null) {
			throw new BadRequestException("内部参数错误");
		}
		Object cc = merged.get("country_code");
		String raw = cc == null ? "" : cc.toString().trim();
		String langKey = StringUtils.hasText(raw) ? raw : "zh-CN";

		Map<String, Object> inner = new LinkedHashMap<>();
		inner.put("logo", merged.get("logo"));
		inner.put("intro", merged.get("intro"));
		inner.put("brand_name", merged.get("brand_name"));
		inner.put("background", merged.get("background"));

		Map<String, Object> outer = new LinkedHashMap<>();
		outer.put(langKey, inner);

		Object bn = merged.get("brand_name");
		String brandNameForPort = bn == null ? null : String.valueOf(bn);
		Object lg = merged.get("logo");
		String logoForPort = lg == null ? null : String.valueOf(lg);
		selfDistributorDisplayUpdatePort.syncSelfDistributorBrandAndLogoIfPresent(
				companyId, brandNameForPort, logoForPort, langKey);

		wxShopsSettingRedisReadService.saveWxShopsSettingJson(companyId, outer);
	}
}
