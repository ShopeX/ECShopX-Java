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

package cn.shopex.ecshopx.members.service.h5;

import cn.shopex.ecshopx.companys.service.auth.CompanysUserAuthChecker;
import cn.shopex.ecshopx.companys.service.domain.CompanyDomainLookupService;
import cn.shopex.ecshopx.members.service.h5.auth.H5AuthStrategy;
import cn.shopex.ecshopx.members.service.h5.auth.H5AuthType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class EspierLocalUserResolver {

	private final CompanyDomainLookupService companyDomainLookupService;

	private final Map<H5AuthType, H5AuthStrategy> strategyByType;

	private final MembersProtocolLogOnLoginService membersProtocolLogOnLoginService;

	private final CompanysUserAuthChecker companysUserAuthChecker;

	public EspierLocalUserResolver(
			CompanyDomainLookupService companyDomainLookupService,
			List<H5AuthStrategy> strategies,
			MembersProtocolLogOnLoginService membersProtocolLogOnLoginService,
			CompanysUserAuthChecker companysUserAuthChecker) {
		this.companyDomainLookupService = companyDomainLookupService;
		this.membersProtocolLogOnLoginService = membersProtocolLogOnLoginService;
		this.companysUserAuthChecker = companysUserAuthChecker;
		Map<H5AuthType, H5AuthStrategy> map = new EnumMap<>(H5AuthType.class);
		for (H5AuthStrategy s : strategies) {
			map.put(s.type(), s);
		}
		this.strategyByType = map;
	}

	public Optional<H5GenericUser> retrieve(Map<String, Object> credentials) {
		Object existingCompany = credentials.get("company_id");
		if (existingCompany == null || !StringUtils.hasText(String.valueOf(existingCompany).trim())) {
			companyDomainLookupService.resolveAndPutCompanyId(credentials);
		}
		H5AuthType authType = H5AuthType.fromCredentials(credentials);
		H5AuthStrategy strategy = strategyByType.get(authType);
		if (strategy == null) {
			strategy = strategyByType.get(H5AuthType.UNKNOWN);
		}
		Optional<H5GenericUser> user = strategy.resolve(credentials);
		if (user.isEmpty()) {
			return Optional.empty();
		}
		H5GenericUser u = user.get();
		if (u.getUserId() > 0) {
			membersProtocolLogOnLoginService.appendAcceptedProtocolsIfNeeded(u, credentials);
		}
		companysUserAuthChecker.checkUserAuth(u.getAttributes());
		return Optional.of(u);
	}
}
