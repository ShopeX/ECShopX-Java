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

package cn.shopex.ecshopx.companys.service.merchant;

import cn.shopex.ecshopx.companys.service.domain.CompanyDomainLookupService;
import cn.shopex.ecshopx.merchant.port.MerchantWxappLoginCompanyIdResolverPort;
import cn.shopex.ecshopx.merchant.service.wxapp.MerchantWxappLoginService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
public class CompanysMerchantWxappLoginCompanyIdResolver implements MerchantWxappLoginCompanyIdResolverPort {

	private final CompanyDomainLookupService companyDomainLookupService;

	public CompanysMerchantWxappLoginCompanyIdResolver(CompanyDomainLookupService companyDomainLookupService) {
		this.companyDomainLookupService = companyDomainLookupService;
	}

	@Override
	public void resolveCompanyId(Map<String, Object> credentials) {
		if (!MerchantWxappLoginService.isEmptyCompanyId(credentials.get("company_id"))) {
			return;
		}
		Object originObj = credentials.get("origin");
		if (originObj == null || !StringUtils.hasText(String.valueOf(originObj))) {
			return;
		}
		companyDomainLookupService.resolveAndPutCompanyId(credentials);
	}
}
