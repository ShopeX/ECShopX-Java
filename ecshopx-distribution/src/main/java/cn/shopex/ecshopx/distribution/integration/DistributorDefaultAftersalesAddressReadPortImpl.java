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

package cn.shopex.ecshopx.distribution.integration;

import cn.shopex.ecshopx.common.port.distribution.DistributorDefaultAftersalesAddressReadPort;
import cn.shopex.ecshopx.distribution.domain.DistributorAftersalesAddress;
import cn.shopex.ecshopx.distribution.mapper.DistributorAftersalesAddressMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DistributorDefaultAftersalesAddressReadPortImpl implements DistributorDefaultAftersalesAddressReadPort {

	private final DistributorAftersalesAddressMapper distributorAftersalesAddressMapper;

	public DistributorDefaultAftersalesAddressReadPortImpl(DistributorAftersalesAddressMapper distributorAftersalesAddressMapper) {
		this.distributorAftersalesAddressMapper = distributorAftersalesAddressMapper;
	}

	@Override
	public Optional<Map<String, Object>> findDefaultAddress(long companyId, long distributorId) {
		if (distributorId <= 0L) {
			return Optional.empty();
		}
		DistributorAftersalesAddress row =
				distributorAftersalesAddressMapper.selectOne(
						new LambdaQueryWrapper<DistributorAftersalesAddress>()
								.eq(DistributorAftersalesAddress::getCompanyId, companyId)
								.eq(DistributorAftersalesAddress::getDistributorId, distributorId)
								.eq(DistributorAftersalesAddress::getIsDefault, 1)
								.last("LIMIT 1"));
		if (row == null) {
			return Optional.empty();
		}
		Map<String, Object> nested = new LinkedHashMap<>();
		nested.put("aftersales_address_id", row.getAddressId());
		nested.put("aftersales_address", str(row.getAddress()));
		nested.put("aftersales_contact", str(row.getContact()));
		nested.put("aftersales_mobile", str(row.getMobile()));
		nested.put("is_default", row.getIsDefault() != null && row.getIsDefault() == 1 ? 1 : 0);
		return Optional.of(nested);
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}
}
