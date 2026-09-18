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

package cn.shopex.ecshopx.distribution.integration;

import cn.shopex.ecshopx.common.port.distribution.DmCrmTradeFinishGuideDistributorReadPort;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DmCrmTradeFinishGuideDistributorReadPortImpl implements DmCrmTradeFinishGuideDistributorReadPort {

	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;

	public DmCrmTradeFinishGuideDistributorReadPortImpl(
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService) {
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
	}

	@Override
	public Optional<Map<String, Object>> loadGuideDistributorRow(long companyId, long distributorId) {
		if (distributorId <= 0L) {
			return Optional.empty();
		}
		Map<String, Object> raw =
				distributorRepositoryGetInfoSimpleService.getInfoSimpleByDistributorId(companyId, distributorId);
		if (raw == null || raw.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(raw);
	}
}
