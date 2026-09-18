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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.repository.DistributorWriteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DistributorDefaultSetService {

	private final DistributorWriteRepository distributorWriteRepository;

	public DistributorDefaultSetService(DistributorWriteRepository distributorWriteRepository) {
		this.distributorWriteRepository = distributorWriteRepository;
	}

	@Transactional(rollbackFor = Exception.class)
	public void setDefaultDistributor(long companyId, long distributorId) {
		if (distributorWriteRepository.selectSimpleByCompanyAndId(companyId, distributorId).isEmpty()) {
			throw new ResourceException("店铺不存在或无权限");
		}
		distributorWriteRepository.clearIsDefaultForCompanyMainDistributors(companyId);
		int n = distributorWriteRepository.setIsDefaultForDistributor(companyId, distributorId, 1);
		if (n == 0) {
			throw new ResourceException("店铺不存在或无权限");
		}
	}
}
