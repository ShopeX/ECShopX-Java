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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.members.admin.AdminMemberSetMemberSalesmanPort;
import cn.shopex.ecshopx.distribution.domain.DistributorUser;
import cn.shopex.ecshopx.distribution.mapper.DistributorUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("adminMemberSetMemberSalesmanPortImpl")
public class AdminMemberSetMemberSalesmanPortImpl implements AdminMemberSetMemberSalesmanPort {

	private final DistributorUserMapper distributorUserMapper;

	public AdminMemberSetMemberSalesmanPortImpl(DistributorUserMapper distributorUserMapper) {
		this.distributorUserMapper = distributorUserMapper;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void setMemberSalesman(long companyId, Long distributorId, List<Long> userIds, long salesmanId) {
		for (Long userId : userIds) {
			LambdaQueryWrapper<DistributorUser> w = new LambdaQueryWrapper<DistributorUser>()
					.eq(DistributorUser::getCompanyId, companyId)
					.eq(DistributorUser::getUserId, userId);
			if (distributorId == null) {
				w.isNull(DistributorUser::getDistributorId);
			} else {
				w.eq(DistributorUser::getDistributorId, distributorId);
			}
			DistributorUser row = distributorUserMapper.selectOne(w.last("LIMIT 1"));
			if (row == null) {
				throw new ResourceException("未查询到更新数据");
			}
			LambdaUpdateWrapper<DistributorUser> uw = new LambdaUpdateWrapper<>();
			uw.eq(DistributorUser::getId, row.getId());
			uw.set(DistributorUser::getSalesmanId, salesmanId);
			uw.set(DistributorUser::getUpdated, System.currentTimeMillis() / 1000L);
			int affected = distributorUserMapper.update(null, uw);
			if (affected != 1) {
				throw new ResourceException("未查询到更新数据");
			}
		}
	}
}
