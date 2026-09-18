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

package cn.shopex.ecshopx.companys.integration;

import cn.shopex.ecshopx.common.port.companys.CompanyOpenapiIdentityReadPort;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CompanyOpenapiIdentityReadPortImpl implements CompanyOpenapiIdentityReadPort {

	private final CompanysMapper companysMapper;

	public CompanyOpenapiIdentityReadPortImpl(CompanysMapper companysMapper) {
		this.companysMapper = companysMapper;
	}

	@Override
	public Optional<Identity> findByCompanyId(long companyId) {
		Companys company = companysMapper.selectById(companyId);
		if (company == null) {
			return Optional.empty();
		}
		return Optional.of(new Identity(company.getPassportUid(), company.getEid()));
	}
}
