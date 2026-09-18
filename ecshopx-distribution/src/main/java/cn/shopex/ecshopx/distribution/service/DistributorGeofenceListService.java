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

import cn.shopex.ecshopx.distribution.mapper.DistributorGeofenceMapper;
import cn.shopex.ecshopx.distribution.service.dto.DistributorGeofenceJoinRow;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DistributorGeofenceListService {

	private final DistributorGeofenceMapper distributorGeofenceMapper;
	private final DistributorGeofenceResponseMapper distributorGeofenceResponseMapper;

	public DistributorGeofenceListService(
			DistributorGeofenceMapper distributorGeofenceMapper,
			DistributorGeofenceResponseMapper distributorGeofenceResponseMapper) {
		this.distributorGeofenceMapper = distributorGeofenceMapper;
		this.distributorGeofenceResponseMapper = distributorGeofenceResponseMapper;
	}

	public Map<String, Object> get(
			long companyId,
			long distributorId,
			int page,
			int pageSize,
			boolean filterByGeofenceId,
			Long geofenceId) {
		Page<DistributorGeofenceJoinRow> p = new Page<>(page, pageSize);
		distributorGeofenceMapper.selectJoinPage(p, companyId, distributorId, filterByGeofenceId, geofenceId);
		List<Map<String, Object>> list = new ArrayList<>();
		for (DistributorGeofenceJoinRow row : p.getRecords()) {
			list.add(distributorGeofenceResponseMapper.toListItem(row));
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", list);
		out.put("total_count", p.getTotal());
		return out;
	}
}
