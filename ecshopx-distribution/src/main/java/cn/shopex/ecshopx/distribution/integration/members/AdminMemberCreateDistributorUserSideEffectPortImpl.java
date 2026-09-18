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

import cn.shopex.ecshopx.common.members.admin.AdminMemberCreateDistributorUserSideEffectPort;
import cn.shopex.ecshopx.distribution.domain.DistributorUser;
import cn.shopex.ecshopx.distribution.mapper.DistributorUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

@Service("adminMemberCreateDistributorUserSideEffectPortImpl")
public class AdminMemberCreateDistributorUserSideEffectPortImpl
		implements AdminMemberCreateDistributorUserSideEffectPort {

	private final DistributorUserMapper distributorUserMapper;

	public AdminMemberCreateDistributorUserSideEffectPortImpl(DistributorUserMapper distributorUserMapper) {
		this.distributorUserMapper = distributorUserMapper;
	}

	@Override
	public void createData(long companyId, long userId, long distributorId, long salespersonId, long inviterId) {
		if (distributorId == 0L && salespersonId == 0L && inviterId == 0L) {
			return;
		}
		long salesmanId = salespersonId;
		LambdaQueryWrapper<DistributorUser> w = new LambdaQueryWrapper<DistributorUser>()
				.eq(DistributorUser::getUserId, userId)
				.eq(DistributorUser::getCompanyId, companyId);
		if (distributorId != 0L) {
			w.eq(DistributorUser::getDistributorId, distributorId);
		}
		if (salesmanId != 0L) {
			w.eq(DistributorUser::getSalesmanId, salesmanId);
		}
		w.last("LIMIT 1");
		if (distributorUserMapper.selectOne(w) != null) {
			return;
		}
		DistributorUser row = new DistributorUser();
		row.setCompanyId(companyId);
		row.setUserId(userId);
		row.setDistributorId(distributorId);
		row.setShopId(0L);
		row.setSalesmanId(salesmanId);
		row.setFamily("");
		long now = System.currentTimeMillis() / 1000L;
		row.setCreated(now);
		row.setUpdated(now);
		distributorUserMapper.insert(row);
	}
}
