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

import cn.shopex.ecshopx.common.members.port.MemberDistributionShopListByIdsPort;
import cn.shopex.ecshopx.distribution.service.DistributorH5ListShopByIdsService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MemberDistributionShopListByIdsPortImpl implements MemberDistributionShopListByIdsPort {

	private final DistributorH5ListShopByIdsService distributorH5ListShopByIdsService;

	public MemberDistributionShopListByIdsPortImpl(
			DistributorH5ListShopByIdsService distributorH5ListShopByIdsService) {
		this.distributorH5ListShopByIdsService = distributorH5ListShopByIdsService;
	}

	@Override
	public Map<String, Object> listValidShopByDistributorIds(
			long companyId, List<Long> distributorIdsOrdered, String requestLang) {
		return distributorH5ListShopByIdsService.listValidShopByDistributorIds(
				companyId, distributorIdsOrdered, requestLang);
	}
}
