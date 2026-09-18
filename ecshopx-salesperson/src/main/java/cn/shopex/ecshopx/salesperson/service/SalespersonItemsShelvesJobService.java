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

package cn.shopex.ecshopx.salesperson.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SalespersonItemsShelvesJobService {

	private final List<SalespersonItemsShelvesSyncContributor> contributors;

	public SalespersonItemsShelvesJobService(List<SalespersonItemsShelvesSyncContributor> contributors) {
		this.contributors = contributors;
	}

	public void execute(long companyId, long activityId, String activityType) {
		if (companyId <= 0L || activityId <= 0L || !StringUtils.hasText(activityType)) {
			return;
		}
		for (SalespersonItemsShelvesSyncContributor contributor : contributors) {
			if (contributor.supports(activityType)) {
				contributor.sync(companyId, activityId);
				return;
			}
		}
	}
}
