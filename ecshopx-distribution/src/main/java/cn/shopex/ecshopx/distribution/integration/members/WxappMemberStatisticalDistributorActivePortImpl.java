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

package cn.shopex.ecshopx.distribution.integration.members;

import cn.shopex.ecshopx.common.members.port.WxappMemberStatisticalDistributorActivePort;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

@Service("wxappMemberStatisticalDistributorActivePortImpl")
public class WxappMemberStatisticalDistributorActivePortImpl
		implements WxappMemberStatisticalDistributorActivePort {

	private final DistributorMapper distributorMapper;

	public WxappMemberStatisticalDistributorActivePortImpl(DistributorMapper distributorMapper) {
		this.distributorMapper = distributorMapper;
	}

	@Override
	public boolean hasActiveDistributor(long companyId, long distributorId) {
		LambdaQueryWrapper<Distributor> w =
				new LambdaQueryWrapper<Distributor>()
						.eq(Distributor::getCompanyId, companyId)
						.eq(Distributor::getDistributorId, distributorId)
						.eq(Distributor::getIsValid, "true");
		Long c = distributorMapper.selectCount(w);
		return c != null && c > 0;
	}
}
