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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class DistributorEmployeeListLookupService {

	private final DistributorMapper distributorMapper;

	public DistributorEmployeeListLookupService(DistributorMapper distributorMapper) {
		this.distributorMapper = distributorMapper;
	}

	public Map<Integer, String> distributorIdToName(long companyId, Set<Integer> distributorIds, int limit) {
		if (distributorIds == null || distributorIds.isEmpty()) {
			return new LinkedHashMap<>();
		}
		List<Long> longIds = distributorIds.stream().map(Integer::longValue).toList();
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId);
		w.in(Distributor::getDistributorId, longIds);
		w.orderByDesc(Distributor::getCreated);
		Page<Distributor> page = new Page<>(1, limit);
		Page<Distributor> result = distributorMapper.selectPage(page, w);
		Map<Integer, String> map = new LinkedHashMap<>();
		for (Distributor d : result.getRecords()) {
			if (d.getDistributorId() == null) {
				continue;
			}
			int id = d.getDistributorId().intValue();
			String name = d.getName();
			map.put(id, name != null ? name : "");
		}
		return map;
	}
}
