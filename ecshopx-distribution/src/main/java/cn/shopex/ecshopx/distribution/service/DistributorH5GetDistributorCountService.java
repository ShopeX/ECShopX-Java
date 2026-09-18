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

package cn.shopex.ecshopx.distribution.service;

import java.util.Collections;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DistributorH5GetDistributorCountService {

	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final DistributeCountReadService distributeCountReadService;

	public DistributorH5GetDistributorCountService(
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			DistributeCountReadService distributeCountReadService) {
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.distributeCountReadService = distributeCountReadService;
	}

	/**
	 * Resolves the company shop without an explicit distributor id, then returns either an empty list (no shop) or
	 * Redis-backed distribution metrics keyed in {@link DistributeCountReadService} order.
	 */
	public Object getDistributorCount(long companyId) {
		Map<String, Object> shop =
				distributorRepositoryGetInfoSimpleService.getDistributorInfoForCompanyShop(companyId, 0L);
		if (shop == null || shop.isEmpty()) {
			return Collections.emptyList();
		}
		long distributorId = resolveDistributorIdFromShop(shop);
		return distributeCountReadService.getDistributorCount(distributorId);
	}

	private static long resolveDistributorIdFromShop(Map<String, Object> shop) {
		Object v = shop.get("distributor_id");
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
	}
}
