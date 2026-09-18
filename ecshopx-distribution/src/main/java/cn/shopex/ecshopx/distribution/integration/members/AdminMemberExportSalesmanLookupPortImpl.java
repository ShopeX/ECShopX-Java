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

import cn.shopex.ecshopx.distribution.domain.DistributorSalesman;
import cn.shopex.ecshopx.distribution.domain.DistributorUser;
import cn.shopex.ecshopx.distribution.mapper.DistributorSalesmanMapper;
import cn.shopex.ecshopx.distribution.mapper.DistributorUserMapper;
import cn.shopex.ecshopx.members.service.export.port.AdminMemberExportSalesmanLookupPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminMemberExportSalesmanLookupPortImpl implements AdminMemberExportSalesmanLookupPort {

	private final DistributorUserMapper distributorUserMapper;
	private final DistributorSalesmanMapper distributorSalesmanMapper;

	public AdminMemberExportSalesmanLookupPortImpl(
			DistributorUserMapper distributorUserMapper, DistributorSalesmanMapper distributorSalesmanMapper) {
		this.distributorUserMapper = distributorUserMapper;
		this.distributorSalesmanMapper = distributorSalesmanMapper;
	}

	@Override
	public Map<String, Object> getSalesmanInfo(long companyId, Map<String, Object> memberRow) {
		long userId = longVal(memberRow.get("user_id"));

		DistributorUser du =
				distributorUserMapper.selectOne(
						new LambdaQueryWrapper<DistributorUser>()
								.eq(DistributorUser::getUserId, userId)
								.eq(DistributorUser::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (du != null && du.getSalesmanId() != null && du.getSalesmanId() > 0L) {
			Map<String, Object> sp = loadSalesmanRow(companyId, du.getSalesmanId());
			if (sp != null) {
				return sp;
			}
		}

		DistributorSalesman selfSales = findSalesmanByBoundUserId(companyId, userId);
		if (selfSales != null) {
			Map<String, Object> selfRow = salesmanToMobileMap(selfSales);
			return selfRow != null ? selfRow : Map.of();
		}

		long inviterId = longVal(memberRow.get("inviter_id"));
		if (inviterId <= 0L) {
			return Map.of();
		}

		DistributorSalesman invSales = findSalesmanByBoundUserId(companyId, inviterId);
		if (invSales != null) {
			return salesmanToMobileMap(invSales);
		}

		DistributorUser duInv =
				distributorUserMapper.selectOne(
						new LambdaQueryWrapper<DistributorUser>()
								.eq(DistributorUser::getUserId, inviterId)
								.eq(DistributorUser::getCompanyId, companyId)
								.last("LIMIT 1"));
		long salesmanId = 0L;
		if (duInv != null && duInv.getSalesmanId() != null) {
			salesmanId = duInv.getSalesmanId();
		}
		if (salesmanId > 0L) {
			Map<String, Object> sp = loadSalesmanRow(companyId, salesmanId);
			if (sp != null) {
				return sp;
			}
		}
		return Map.of();
	}

	private Map<String, Object> loadSalesmanRow(long companyId, long salesmanId) {
		DistributorSalesman e =
				distributorSalesmanMapper.selectOne(
						new LambdaQueryWrapper<DistributorSalesman>()
								.eq(DistributorSalesman::getSalesmanId, salesmanId)
								.eq(DistributorSalesman::getCompanyId, companyId)
								.last("LIMIT 1"));
		return salesmanToMobileMap(e);
	}

	private static Map<String, Object> salesmanToMobileMap(DistributorSalesman e) {
		if (e == null) {
			return null;
		}
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("mobile", e.getMobile());
		m.put("salesperson_id", e.getSalesmanId());
		return m;
	}

	private DistributorSalesman findSalesmanByBoundUserId(long companyId, long memberUserId) {
		return distributorSalesmanMapper.selectOne(
				new LambdaQueryWrapper<DistributorSalesman>()
						.eq(DistributorSalesman::getCompanyId, companyId)
						.eq(DistributorSalesman::getUserId, String.valueOf(memberUserId))
						.last("LIMIT 1"));
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
