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

package cn.shopex.ecshopx.distribution.service.distributorvalid.append;

import cn.shopex.ecshopx.distribution.mapper.DistributionStoreMarketingActivityMapper;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DistributorIsValidMarketingActivityReadService {

	private final DistributionStoreMarketingActivityMapper distributionStoreMarketingActivityMapper;

	public DistributorIsValidMarketingActivityReadService(
			DistributionStoreMarketingActivityMapper distributionStoreMarketingActivityMapper) {
		this.distributionStoreMarketingActivityMapper = distributionStoreMarketingActivityMapper;
	}

	public void appendMarketingActivityList(long companyId, Map<String, Object> result) {
		Object did = result.get("distributor_id");
		if (!(did instanceof Number n)) {
			return;
		}
		long distributorId = n.longValue();
		long nowTs = Instant.now().getEpochSecond();
		List<Map<String, Object>> list =
				distributionStoreMarketingActivityMapper.selectOngoingMarketingActivitiesForDistributor(
						companyId, distributorId, nowTs);
		if (list == null) {
			result.put("marketingActivityList", Collections.emptyList());
		} else {
			result.put("marketingActivityList", list);
		}
	}
}
