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

package cn.shopex.ecshopx.distribution.repository;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.PickupLocation;
import cn.shopex.ecshopx.distribution.mapper.PickupLocationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Repository;

@Repository
public class PickupLocationRelDistributorRepository {

	private final PickupLocationMapper pickupLocationMapper;
	private final DistributorWriteRepository distributorWriteRepository;

	public PickupLocationRelDistributorRepository(
			PickupLocationMapper pickupLocationMapper,
			DistributorWriteRepository distributorWriteRepository) {
		this.pickupLocationMapper = pickupLocationMapper;
		this.distributorWriteRepository = distributorWriteRepository;
	}

	public void relDistributor(long companyId, long pickupLocationId, long newDistributorId) {
		LambdaQueryWrapper<PickupLocation> q = new LambdaQueryWrapper<>();
		q.eq(PickupLocation::getCompanyId, companyId).eq(PickupLocation::getId, pickupLocationId).eq(PickupLocation::getDistributorId, 0L);
		PickupLocation row = pickupLocationMapper.selectOne(q);
		if (row == null) {
			throw new ResourceException("自提点不存在或已绑定店铺");
		}
		if (newDistributorId > 0) {
			distributorWriteRepository
					.selectSimpleByCompanyAndId(companyId, newDistributorId)
					.orElseThrow(() -> new ResourceException("店铺不存在"));
		}
		LambdaUpdateWrapper<PickupLocation> u = new LambdaUpdateWrapper<>();
		u.eq(PickupLocation::getCompanyId, companyId).eq(PickupLocation::getId, pickupLocationId).eq(PickupLocation::getDistributorId, 0L);
		u.set(PickupLocation::getRelDistributorId, newDistributorId);
		pickupLocationMapper.update(null, u);
	}
}
