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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.goods.service.distributor.DistributorItemsPagedAddRunner;
import cn.shopex.ecshopx.goods.service.distributor.DistributorItemsPagedAddRunner.AddJobParams;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DistributorGoodsSyncService {

	private final DistributorMapper distributorMapper;
	private final DistributorItemsPagedAddRunner pagedAddRunner;

	public DistributorGoodsSyncService(
			DistributorMapper distributorMapper, DistributorItemsPagedAddRunner pagedAddRunner) {
		this.distributorMapper = distributorMapper;
		this.pagedAddRunner = pagedAddRunner;
	}

	public void syncGoods(long companyId, long defaultItemId) {
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId)
				.eq(Distributor::getAutoSyncGoods, true)
				.and(q ->
						q.isNull(Distributor::getIsValid).or().apply("LOWER(TRIM(is_valid)) <> {0}", "delete"));
		List<Distributor> distributors = distributorMapper.selectList(w);
		if (distributors.isEmpty()) {
			return;
		}
		List<Long> itemFilter = List.of(defaultItemId);
		int pageSize = 100;
		boolean async = distributors.size() > 3;
		for (Distributor d : distributors) {
			var params = new AddJobParams(companyId, d.getDistributorId(), itemFilter, false, 1, pageSize);
			if (async) {
				pagedAddRunner.enqueueAsyncFirstPage(params);
			} else {
				pagedAddRunner.runAllPagesSync(params);
			}
		}
	}
}
