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
import cn.shopex.ecshopx.distribution.service.distributorvalid.DistributorIsValidSettingService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DistributorWhiteListCheckUserValidService {

	private final DistributorMapper distributorMapper;
	private final DistributorWhiteListMapper distributorWhiteListMapper;
	private final MemberAccountService memberAccountService;
	private final DistributorIsValidSettingService distributorIsValidSettingService;

	public DistributorWhiteListCheckUserValidService(
			DistributorMapper distributorMapper,
			DistributorWhiteListMapper distributorWhiteListMapper,
			MemberAccountService memberAccountService,
			DistributorIsValidSettingService distributorIsValidSettingService) {
		this.distributorMapper = distributorMapper;
		this.distributorWhiteListMapper = distributorWhiteListMapper;
		this.memberAccountService = memberAccountService;
		this.distributorIsValidSettingService = distributorIsValidSettingService;
	}

	public boolean checkUserValidCommon(long distributorId, long userId, long companyId) {
		Map<String, Object> set = distributorIsValidSettingService.getOpenDividedSetting(companyId);
		if (Boolean.TRUE.equals(set.get("status"))) {
			return checkUserValidInDistributor(distributorId, userId, companyId);
		}
		return true;
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
}
