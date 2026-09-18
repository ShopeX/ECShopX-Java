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
import cn.shopex.ecshopx.members.integration.admin.AdminMemberGetInfoDistributorDisplayNamePort;
import java.util.Collection;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service("adminMemberGetInfoDistributorDisplayNamePortImpl")
public class AdminMemberGetInfoDistributorDisplayNamePortImpl implements AdminMemberGetInfoDistributorDisplayNamePort {

	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;

	public AdminMemberGetInfoDistributorDisplayNamePortImpl(
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService) {
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
	}

	@Override
	public String resolve(long companyId, long distributorId) {
		Map<String, Object> row = distributorRepositoryGetInfoSimpleService.getInfoSimpleByDistributorId(companyId, distributorId);
		if (row == null || row.isEmpty()) {
			return "";
		}
		Object n = row.get("name");
		return n == null ? "" : String.valueOf(n);
	}

	@Override
	public Map<Long, String> resolveBatch(long companyId, Collection<Long> distributorIds) {
		return distributorRepositoryGetInfoSimpleService.getNameByDistributorIds(companyId, distributorIds);
	}
}
