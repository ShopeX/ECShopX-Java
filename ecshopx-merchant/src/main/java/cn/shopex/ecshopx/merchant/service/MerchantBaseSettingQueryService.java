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

package cn.shopex.ecshopx.merchant.service;

import cn.shopex.ecshopx.merchant.port.CompanyDomainInfoRead;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MerchantBaseSettingQueryService {

	private final MerchantBaseSettingSaveService merchantBaseSettingSaveService;
	private final CompanyDomainInfoRead companyDomainInfoRead;

	public MerchantBaseSettingQueryService(
			MerchantBaseSettingSaveService merchantBaseSettingSaveService,
			CompanyDomainInfoRead companyDomainInfoRead) {
		this.merchantBaseSettingSaveService = merchantBaseSettingSaveService;
		this.companyDomainInfoRead = companyDomainInfoRead;
	}

	public Map<String, Object> getBaseSetting(long companyId) {
		Map<String, Object> inputData = merchantBaseSettingSaveService.loadCompanyBaseSetting(companyId);
		Map<String, Object> domainInfo = companyDomainInfoRead.getDomainInfo(companyId);

		Object h5DomainObj = domainInfo.get("h5_domain");
		String h5Domain = h5DomainObj != null ? String.valueOf(h5DomainObj).trim() : "";
		Object defObj = domainInfo.get("h5_default_domain");
		String h5Default = defObj != null ? String.valueOf(defObj) : "";
		String h5urlDomain = StringUtils.hasText(h5Domain) ? h5Domain : h5Default;

		inputData.put("h5url", "https://" + h5urlDomain + "/subpages/merchant/login");
		return inputData;
	}
}
