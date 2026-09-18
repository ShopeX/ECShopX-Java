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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorWhiteListMemberShopListService {

	private static final String SQL_IS_VALID_ACTIVE =
			"(is_valid = 1 OR LOWER(TRIM(CAST(is_valid AS CHAR))) IN ('true','1'))";

	private final DistributorWhiteListMapper distributorWhiteListMapper;
	private final DistributorMapper distributorMapper;
	private final MemberAccountService memberAccountService;
	private final ObjectMapper objectMapper;

	public DistributorWhiteListMemberShopListService(
			DistributorWhiteListMapper distributorWhiteListMapper,
			DistributorMapper distributorMapper,
			MemberAccountService memberAccountService,
			ObjectMapper objectMapper) {
		this.distributorWhiteListMapper = distributorWhiteListMapper;
		this.distributorMapper = distributorMapper;
		this.memberAccountService = memberAccountService;
		this.objectMapper = objectMapper;
	}

	public List<Map<String, Object>> listShopsForMember(long companyId, long userId) {
		String mobile = memberAccountService.findMobileStored(companyId, userId);
		if (!StringUtils.hasText(mobile)) {
			return List.of();
		}
		List<DistributorWhiteList> rows =
				distributorWhiteListMapper.selectList(
						new LambdaQueryWrapper<DistributorWhiteList>()
								.eq(DistributorWhiteList::getCompanyId, companyId)
								.eq(DistributorWhiteList::getMobile, mobile));
		if (rows.isEmpty()) {
			return List.of();
		}
		LinkedHashMap<Long, Boolean> orderedDistributorIds = new LinkedHashMap<>();
		for (DistributorWhiteList r : rows) {
			Long distributorId = r.getDistributorId();
			if (distributorId != null && distributorId > 0L) {
				orderedDistributorIds.put(distributorId, Boolean.TRUE);
			}
		}
		if (orderedDistributorIds.isEmpty()) {
			return List.of();
		}
		List<Distributor> shops =
				distributorMapper.selectList(
						new LambdaQueryWrapper<Distributor>()
								.eq(Distributor::getCompanyId, companyId)
								.in(Distributor::getDistributorId, orderedDistributorIds.keySet())
								.apply(SQL_IS_VALID_ACTIVE)
								.orderByAsc(Distributor::getDistributorId));
		List<Map<String, Object>> out = new ArrayList<>();
		for (Distributor d : shops) {
			out.add(new LinkedHashMap<>(DistributorRowMaps.toApiRow(d, objectMapper)));
		}
		return out;
	}
}
