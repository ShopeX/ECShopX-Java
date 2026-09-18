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
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PagesTemplateSyncDistributorQueryService {

	private final DistributorMapper distributorMapper;

	public PagesTemplateSyncDistributorQueryService(DistributorMapper distributorMapper) {
		this.distributorMapper = distributorMapper;
	}

	public List<Long> listDistributorIds(long companyId, long regionauthId) {
		List<Distributor> rows =
				distributorMapper.selectList(
						new LambdaQueryWrapper<Distributor>()
								.eq(Distributor::getCompanyId, companyId)
								.eq(Distributor::getRegionauthId, regionauthId));
		if (rows == null || rows.isEmpty()) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (Distributor row : rows) {
			Long id = row.getDistributorId();
			if (id != null && id > 0L) {
				out.add(id);
			}
		}
		return out;
	}
}
