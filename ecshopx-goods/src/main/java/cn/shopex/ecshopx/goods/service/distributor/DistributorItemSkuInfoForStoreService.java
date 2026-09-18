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

package cn.shopex.ecshopx.goods.service.distributor;

import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.mapper.DistributorItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

@Service
public class DistributorItemSkuInfoForStoreService {

	private final DistributorItemsMapper distributorItemsMapper;

	public DistributorItemSkuInfoForStoreService(DistributorItemsMapper distributorItemsMapper) {
		this.distributorItemsMapper = distributorItemsMapper;
	}

	/**
	 * When there is no distributor-item row or the flag is unset, treat stock as headquarters (total-store) scope.
	 */
	public boolean isTotalStore(long companyId, long itemId, long distributorId) {
		if (distributorId <= 0L) {
			return true;
		}
		DistributorItems row = distributorItemsMapper.selectOne(new LambdaQueryWrapper<DistributorItems>()
				.eq(DistributorItems::getCompanyId, companyId)
				.eq(DistributorItems::getDistributorId, distributorId)
				.eq(DistributorItems::getItemId, itemId)
				.last("LIMIT 1"));
		if (row == null || row.getIsTotalStore() == null) {
			return true;
		}
		return Boolean.TRUE.equals(row.getIsTotalStore());
	}

	public DistributorItems findRow(long companyId, long itemId, long distributorId) {
		if (distributorId <= 0L) {
			return null;
		}
		return distributorItemsMapper.selectOne(new LambdaQueryWrapper<DistributorItems>()
				.eq(DistributorItems::getCompanyId, companyId)
				.eq(DistributorItems::getDistributorId, distributorId)
				.eq(DistributorItems::getItemId, itemId)
				.last("LIMIT 1"));
	}
}
