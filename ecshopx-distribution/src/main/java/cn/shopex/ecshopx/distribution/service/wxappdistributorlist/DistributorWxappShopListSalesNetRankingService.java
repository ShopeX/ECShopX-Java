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

package cn.shopex.ecshopx.distribution.service.wxappdistributorlist;

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.repository.WxappDistributorNetSalesBatchJdbcRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DistributorWxappShopListSalesNetRankingService {

	private final DistributorMapper distributorMapper;
	private final WxappDistributorNetSalesBatchJdbcRepository netSalesBatchJdbcRepository;

	public DistributorWxappShopListSalesNetRankingService(
			DistributorMapper distributorMapper, WxappDistributorNetSalesBatchJdbcRepository netSalesBatchJdbcRepository) {
		this.distributorMapper = distributorMapper;
		this.netSalesBatchJdbcRepository = netSalesBatchJdbcRepository;
	}

	public List<Long> buildFieldOrderDistributorIds(int sortType, long companyId, List<Long> allCompanyDistributorIds, long nowEpochSecond) {
		if (sortType != 3 && sortType != 4) {
			return List.of();
		}
		Map<Long, Long> net = buildNetSalesMapInternal(companyId, allCompanyDistributorIds, nowEpochSecond);
		if (net.isEmpty()) {
			return List.of();
		}
		List<Map.Entry<Long, Long>> entries = new ArrayList<>(net.entrySet());
		if (sortType == 3) {
			entries.sort(Comparator.<Map.Entry<Long, Long>>comparingLong(Map.Entry::getValue).reversed());
		} else {
			entries.sort(Comparator.comparingLong(Map.Entry::getValue));
		}
		return entries.stream().map(Map.Entry::getKey).collect(Collectors.toList());
	}

	public Map<Long, Long> buildNetSalesMapForCompany(long companyId, long nowEpochSecond) {
		List<Long> allIds = listAllDistributorIds(companyId);
		return buildNetSalesMapInternal(companyId, allIds, nowEpochSecond);
	}

	private Map<Long, Long> buildNetSalesMapInternal(long companyId, List<Long> distributorIds, long nowEpochSecond) {
		if (distributorIds == null || distributorIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, Long> orders = netSalesBatchJdbcRepository.sumDoneOrderItemQtyByDistributorIds(companyId, distributorIds, nowEpochSecond);
		Map<Long, Long> afters = netSalesBatchJdbcRepository.sumDoneAftersalesItemQtyByDistributorIds(companyId, distributorIds, nowEpochSecond);
		Map<Long, Long> net = new LinkedHashMap<>();
		for (Long id : distributorIds) {
			long o = orders.getOrDefault(id, 0L);
			long a = afters.getOrDefault(id, 0L);
			net.put(id, Long.valueOf(o - a));
		}
		return net;
	}

	private List<Long> listAllDistributorIds(long companyId) {
		List<Distributor> rows =
				distributorMapper.selectList(
						new LambdaQueryWrapper<Distributor>()
								.eq(Distributor::getCompanyId, companyId)
								.select(Distributor::getDistributorId));
		List<Long> ids = new ArrayList<>();
		for (Distributor d : rows) {
			if (d.getDistributorId() != null) {
				ids.add(d.getDistributorId());
			}
		}
		return ids;
	}
}
