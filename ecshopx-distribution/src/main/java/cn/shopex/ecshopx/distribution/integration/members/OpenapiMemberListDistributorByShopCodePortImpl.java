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

import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.members.integration.distribution.OpenapiMemberListDistributorByShopCodePort;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiMemberListDistributorByShopCodePortImpl implements OpenapiMemberListDistributorByShopCodePort {

	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;

	public OpenapiMemberListDistributorByShopCodePortImpl(
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService) {
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
	}

	@Override
	public Optional<Long> resolveDistributorIdByShopCode(long companyId, String shopCode) {
		if (!StringUtils.hasText(shopCode)) {
			return Optional.empty();
		}
		Map<String, Object> info =
				distributorRepositoryGetInfoSimpleService.getInfoSimpleByShopCode(companyId, shopCode.trim());
		Object distId = info.get("distributor_id");
		if (distId == null) {
			return Optional.empty();
		}
		long id = longVal(distId);
		return id > 0L ? Optional.of(id) : Optional.empty();
	}

	private static long longVal(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
