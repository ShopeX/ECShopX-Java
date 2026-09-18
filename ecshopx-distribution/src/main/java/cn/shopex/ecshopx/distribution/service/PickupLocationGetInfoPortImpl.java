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

import cn.shopex.ecshopx.common.distribution.PickupLocationGetInfoPort;
import cn.shopex.ecshopx.distribution.domain.PickupLocation;
import cn.shopex.ecshopx.distribution.mapper.PickupLocationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PickupLocationGetInfoPortImpl implements PickupLocationGetInfoPort {

	private final PickupLocationMapper pickupLocationMapper;

	public PickupLocationGetInfoPortImpl(PickupLocationMapper pickupLocationMapper) {
		this.pickupLocationMapper = pickupLocationMapper;
	}

	@Override
	public Map<String, Object> getInfo(long companyId, long pickupLocationId) {
		if (companyId <= 0L || pickupLocationId <= 0L) {
			return Collections.emptyMap();
		}
		PickupLocation row =
				pickupLocationMapper.selectOne(
						new LambdaQueryWrapper<PickupLocation>()
								.eq(PickupLocation::getCompanyId, companyId)
								.eq(PickupLocation::getId, pickupLocationId)
								.last("LIMIT 1"));
		if (row == null) {
			return Collections.emptyMap();
		}
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", row.getId());
		m.put("company_id", row.getCompanyId());
		m.put("distributor_id", row.getDistributorId());
		m.put("name", row.getName());
		m.put("lng", row.getLng());
		m.put("lat", row.getLat());
		m.put("province", row.getProvince());
		m.put("city", row.getCity());
		m.put("area", row.getArea());
		m.put("address", row.getAddress());
		m.put("contract_phone", row.getContractPhone());
		m.put("hours", row.getHours());
		m.put("workdays", row.getWorkdays());
		return m;
	}
}
