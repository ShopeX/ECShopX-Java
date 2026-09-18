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
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DistributorH5ListShopByIdsService {

	private final DistributorMapper distributorMapper;
	private final SelfDeliverySettingReadService selfDeliverySettingReadService;
	private final DistributorListRowFormatService distributorListRowFormatService;
	private final ObjectMapper objectMapper;

	public DistributorH5ListShopByIdsService(
			DistributorMapper distributorMapper,
			SelfDeliverySettingReadService selfDeliverySettingReadService,
			DistributorListRowFormatService distributorListRowFormatService,
			ObjectMapper objectMapper) {
		this.distributorMapper = distributorMapper;
		this.selfDeliverySettingReadService = selfDeliverySettingReadService;
		this.distributorListRowFormatService = distributorListRowFormatService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> listShopByDistributorIds(
			long companyId, List<Long> distributorIdsOrdered, String requestLang) {
		if (distributorIdsOrdered == null || distributorIdsOrdered.isEmpty()) {
			LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
			empty.put("total_count", 0L);
			empty.put("list", List.of());
			return empty;
		}
		List<Long> uniqueForIn =
				distributorIdsOrdered.stream()
						.filter(Objects::nonNull)
						.filter(id -> id > 0L)
						.distinct()
						.collect(Collectors.toList());
		if (uniqueForIn.isEmpty()) {
			LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
			empty.put("total_count", 0L);
			empty.put("list", List.of());
			return empty;
		}

		LambdaQueryWrapper<Distributor> base = Wrappers.lambdaQuery();
		base.eq(Distributor::getCompanyId, companyId).in(Distributor::getDistributorId, uniqueForIn);
		long total = distributorMapper.selectCount(base);

		List<Distributor> fetched = distributorMapper.selectList(base);
		Map<Long, Distributor> byId =
				fetched.stream()
						.collect(Collectors.toMap(Distributor::getDistributorId, d -> d, (a, b) -> a));

		List<Map<String, Object>> rows = new ArrayList<>();
		for (Long id : distributorIdsOrdered) {
			if (id == null || id <= 0L) {
				continue;
			}
			Distributor d = byId.get(id);
			if (d == null) {
				continue;
			}
			int distributorSelf = d.getDistributorSelf() == null ? 0 : d.getDistributorSelf().intValue();
			Map<String, Object> setting =
					selfDeliverySettingReadService.getSetting(companyId, d.getDistributorId(), distributorSelf);
			rows.add(distributorListRowFormatService.formatStoreRow(d, setting, objectMapper));
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", rows);
		return out;
	}

	public Map<String, Object> listValidShopByDistributorIds(
			long companyId, List<Long> distributorIdsOrdered, String requestLang) {
		if (distributorIdsOrdered == null || distributorIdsOrdered.isEmpty()) {
			LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
			empty.put("total_count", 0L);
			empty.put("list", List.of());
			return empty;
		}
		List<Long> uniqueForIn =
				distributorIdsOrdered.stream()
						.filter(Objects::nonNull)
						.filter(id -> id > 0L)
						.distinct()
						.collect(Collectors.toList());
		if (uniqueForIn.isEmpty()) {
			LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
			empty.put("total_count", 0L);
			empty.put("list", List.of());
			return empty;
		}

		LambdaQueryWrapper<Distributor> base = Wrappers.lambdaQuery();
		base.eq(Distributor::getCompanyId, companyId)
				.in(Distributor::getDistributorId, uniqueForIn)
				.eq(Distributor::getIsValid, "true");
		long total = distributorMapper.selectCount(base);

		List<Distributor> fetched = distributorMapper.selectList(base);
		Map<Long, Distributor> byId =
				fetched.stream()
						.collect(Collectors.toMap(Distributor::getDistributorId, d -> d, (a, b) -> a));

		List<Map<String, Object>> rows = new ArrayList<>();
		for (Long id : distributorIdsOrdered) {
			if (id == null || id <= 0L) {
				continue;
			}
			Distributor d = byId.get(id);
			if (d == null) {
				continue;
			}
			int distributorSelf = d.getDistributorSelf() == null ? 0 : d.getDistributorSelf().intValue();
			Map<String, Object> setting =
					selfDeliverySettingReadService.getSetting(companyId, d.getDistributorId(), distributorSelf);
			rows.add(distributorListRowFormatService.formatStoreRow(d, setting, objectMapper));
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", rows);
		return out;
	}
}
