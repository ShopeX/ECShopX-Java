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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.espier.service.offline.OfflineBankAccountApplicationService;
import cn.shopex.ecshopx.payment.service.front.FrontPaymentSettingReadService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxappOrderOfflineBackaccountService {

	private final FrontPaymentSettingReadService frontPaymentSettingReadService;
	private final OfflineBankAccountApplicationService offlineBankAccountApplicationService;

	public WxappOrderOfflineBackaccountService(
			FrontPaymentSettingReadService frontPaymentSettingReadService,
			OfflineBankAccountApplicationService offlineBankAccountApplicationService) {
		this.frontPaymentSettingReadService = frontPaymentSettingReadService;
		this.offlineBankAccountApplicationService = offlineBankAccountApplicationService;
	}

	public Map<String, Object> getOfflineAccount(long companyId, String countryCodeQueryRaw) {
		String s = countryCodeQueryRaw == null ? "" : countryCodeQueryRaw.trim();
		String lang = (s.isEmpty() || "0".equals(s)) ? "zh-CN" : s;
		Object setting = frontPaymentSettingReadService.getPaymentSetting(companyId, "offline_pay", lang);
		List<Map<String, Object>> list = offlineBankAccountApplicationService.getOfflineAccount(companyId, lang);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("setting", setting);
		data.put("list", list);
		return data;
	}
}
