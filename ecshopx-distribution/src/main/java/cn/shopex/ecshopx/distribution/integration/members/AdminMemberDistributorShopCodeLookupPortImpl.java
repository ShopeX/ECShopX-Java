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

package cn.shopex.ecshopx.distribution.integration.members;

import cn.shopex.ecshopx.common.members.admin.AdminMemberDistributorShopCodeLookupPort;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminMemberDistributorShopCodeLookupPortImpl implements AdminMemberDistributorShopCodeLookupPort {

	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;

	public AdminMemberDistributorShopCodeLookupPortImpl(DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService) {
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
	}

	@Override
	public Optional<String> findShopCodeByDistributorId(long companyId, long distributorId) {
		if (distributorId <= 0L) {
			return Optional.empty();
		}
		Map<String, Object> info = distributorRepositoryGetInfoSimpleService.getInfoSimpleByDistributorId(companyId, distributorId);
		Object sc = info.get("shop_code");
		if (sc == null) {
			return Optional.empty();
		}
		String trimmed = String.valueOf(sc).trim();
		return StringUtils.hasText(trimmed) ? Optional.of(trimmed) : Optional.empty();
	}
}
