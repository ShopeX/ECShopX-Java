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
import cn.shopex.ecshopx.distribution.domain.DistributorWhiteList;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.mapper.DistributorWhiteListMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorWhiteListCheckUserValidService {

	private final DistributorMapper distributorMapper;
	private final DistributorWhiteListMapper distributorWhiteListMapper;
	private final MemberAccountService memberAccountService;

	public DistributorWhiteListCheckUserValidService(
			DistributorMapper distributorMapper,
			DistributorWhiteListMapper distributorWhiteListMapper,
			MemberAccountService memberAccountService) {
		this.distributorMapper = distributorMapper;
		this.distributorWhiteListMapper = distributorWhiteListMapper;
		this.memberAccountService = memberAccountService;
	}

	public boolean checkUserValidCommon(long distributorId, long userId, long companyId) {
		return checkUserValidInDistributor(distributorId, userId, companyId);
	}

	public boolean checkUserValidInDistributor(long distributorId, long userId, long companyId) {
		Distributor row =
				distributorMapper.selectOne(
						new LambdaQueryWrapper<Distributor>()
								.eq(Distributor::getDistributorId, distributorId)
								.last("LIMIT 1"));
		if (row == null) {
			return false;
		}
		Long od = row.getOpenDivided();
		if (od == null || od.longValue() == 0L) {
			return true;
		}
		String mobile = memberAccountService.findMobileStored(companyId, userId);
		DistributorWhiteList w =
				distributorWhiteListMapper.selectOne(
						new LambdaQueryWrapper<DistributorWhiteList>()
								.eq(DistributorWhiteList::getDistributorId, distributorId)
								.eq(DistributorWhiteList::getMobile, mobile)
								.last("LIMIT 1"));
		return w != null;
	}

	/** Returns distinct distributor ids on the member mobile whitelist; empty when unsigned-in or no mobile. */
	public List<Long> listWhitelistedDistributorIds(long companyId, long userId) {
		if (userId <= 0L) {
			return List.of();
		}
		String mobile = memberAccountService.findMobileStored(companyId, userId);
		if (!StringUtils.hasText(mobile)) {
			return List.of();
		}
		List<DistributorWhiteList> rows =
				distributorWhiteListMapper.selectList(
						new LambdaQueryWrapper<DistributorWhiteList>()
								.eq(DistributorWhiteList::getCompanyId, companyId)
								.eq(DistributorWhiteList::getMobile, mobile));
		if (rows == null || rows.isEmpty()) {
			return List.of();
		}
		Set<Long> ids = new LinkedHashSet<>();
		for (DistributorWhiteList row : rows) {
			if (row.getDistributorId() != null && row.getDistributorId() > 0L) {
				ids.add(row.getDistributorId());
			}
		}
		return new ArrayList<>(ids);
	}
}
