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

import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.salesperson.domain.ShopsRelSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopsRelSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ShopSalespersonCheckDistributorIsValidService {

	private final ShopsRelSalespersonMapper shopsRelSalespersonMapper;
	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;

	public ShopSalespersonCheckDistributorIsValidService(ShopsRelSalespersonMapper shopsRelSalespersonMapper,
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService) {
		this.shopsRelSalespersonMapper = shopsRelSalespersonMapper;
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
	}

	public boolean checkDistributorIsValid(long companyId, String salespersonIdRaw, String distributorIdRaw) {
		long salespersonId;
		long distributorId;
		try {
			salespersonId = Long.parseLong(salespersonIdRaw.trim());
			distributorId = Long.parseLong(distributorIdRaw.trim());
		} catch (NumberFormatException e) {
			return false;
		}
		if (salespersonId < 0L || distributorId < 0L) {
			return false;
		}

		ShopsRelSalesperson rel = shopsRelSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopsRelSalesperson>()
				.eq(ShopsRelSalesperson::getCompanyId, companyId)
				.eq(ShopsRelSalesperson::getSalespersonId, salespersonId)
				.eq(ShopsRelSalesperson::getShopId, distributorId)
				.last("LIMIT 1"));
		if (rel == null) {
			return false;
		}

		Map<String, Object> distributorInfo =
				distributorRepositoryGetInfoSimpleService.getDistributorInfoForCompanyShop(companyId, distributorId);
		return !distributorInfo.isEmpty();
	}
}
