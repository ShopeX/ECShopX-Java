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

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.repository.DistributorInfoReadRepository;
import org.springframework.stereotype.Service;

/**
 * 读取平台总店（{@code distributor_self=1}）或指定店铺的配送开关。
 * 无总店时默认仅快递（{@link DistributorDeliveryCapability#logisticsOnlyFallback()}）。
 */
@Service
public class DistributorDeliveryCapabilityService {

	private final DistributorInfoReadRepository distributorInfoReadRepository;

	public DistributorDeliveryCapabilityService(DistributorInfoReadRepository distributorInfoReadRepository) {
		this.distributorInfoReadRepository = distributorInfoReadRepository;
	}

	/**
	 * @param requestDistributorId 前台请求店铺；{@code <=0} 时取平台总店配置
	 */
	public DistributorDeliveryCapability resolveForFront(long companyId, long requestDistributorId) {
		if (companyId <= 0L) {
			return DistributorDeliveryCapability.sumFallback();
		}
		if (requestDistributorId > 0L) {
			return fromDistributor(companyId, requestDistributorId);
		}
		return fromPlatformSelf(companyId);
	}

	public DistributorDeliveryCapability fromPlatformSelf(long companyId) {
		return distributorInfoReadRepository
				.loadSelfDistributorForCompany(companyId, null)
				.map(this::fromEntity)
				.orElseGet(DistributorDeliveryCapability::logisticsOnlyFallback);
	}

	public DistributorDeliveryCapability fromDistributor(long companyId, long distributorId) {
		if (distributorId <= 0L) {
			return fromPlatformSelf(companyId);
		}
		return distributorInfoReadRepository
				.findByCompanyAndDistributorId(companyId, distributorId, null)
				.map(this::fromEntity)
				.orElseGet(DistributorDeliveryCapability::sumFallback);
	}

	private DistributorDeliveryCapability fromEntity(Distributor d) {
		if (d == null) {
			return DistributorDeliveryCapability.sumFallback();
		}
		return DistributorDeliveryCapability.of(d.getIsDelivery(), d.getIsSelfDelivery(), d.getIsZiti());
	}
}
