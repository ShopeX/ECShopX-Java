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

package cn.shopex.ecshopx.goods.integration;

import cn.shopex.ecshopx.common.distribution.DistributionPopularizeProfitApplyPort;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import org.springframework.stereotype.Service;

@Service
public class DistributionPopularizeProfitApplyPortImpl implements DistributionPopularizeProfitApplyPort {

	private final ItemsRepository itemsRepository;

	public DistributionPopularizeProfitApplyPortImpl(ItemsRepository itemsRepository) {
		this.itemsRepository = itemsRepository;
	}

	@Override
	public void applyPopularizeSellerScaleForDefaultItems(long companyId, String profitScalePlain) {
		itemsRepository.updateProfitByCompanyAndProfitType(companyId, 0, profitScalePlain);
	}
}
