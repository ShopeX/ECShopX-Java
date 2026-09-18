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
import cn.shopex.ecshopx.merchant.service.MerchantDisabledDistributorIdsQueryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class HandleValidCartValidShopIdsService {

	private final DistributorMapper distributorMapper;
	private final MerchantDisabledDistributorIdsQueryService merchantDisabledDistributorIdsQueryService;

	public HandleValidCartValidShopIdsService(
			DistributorMapper distributorMapper,
			MerchantDisabledDistributorIdsQueryService merchantDisabledDistributorIdsQueryService) {
		this.distributorMapper = distributorMapper;
		this.merchantDisabledDistributorIdsQueryService = merchantDisabledDistributorIdsQueryService;
	}

	public List<Long> resolveValidShopIdsAfterDistributorAndMerchantRules(long companyId, List<Long> distinctShopIdsFromCart) {
		if (distinctShopIdsFromCart == null || distinctShopIdsFromCart.isEmpty()) {
			return List.of();
		}
		LinkedHashSet<Long> shopIds = new LinkedHashSet<>();
		for (Long id : distinctShopIdsFromCart) {
			if (id != null && id > 0L) {
				shopIds.add(id);
			}
		}
		if (shopIds.isEmpty()) {
			return List.of();
		}
		List<Long> shopIdList = new ArrayList<>(shopIds);
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId)
				.in(Distributor::getDistributorId, shopIdList)
				.eq(Distributor::getIsValid, "true");
		List<Distributor> validShopList = distributorMapper.selectList(w);
		List<Long> validShopIds = new ArrayList<>();
		for (Distributor d : validShopList) {
			if (d.getDistributorId() != null) {
				validShopIds.add(d.getDistributorId());
			}
		}
		if (validShopIds.isEmpty()) {
			return List.of();
		}
		List<Long> disabledLinked = merchantDisabledDistributorIdsQueryService.listDistributorIdsLinkedToDisabledMerchants(companyId);
		if (disabledLinked == null || disabledLinked.isEmpty()) {
			return List.copyOf(validShopIds);
		}
		Set<Long> disabledSet = new LinkedHashSet<>(disabledLinked);
		List<Long> out = new ArrayList<>();
		for (Long sid : validShopIds) {
			if (!disabledSet.contains(sid)) {
				out.add(sid);
			}
		}
		return out;
	}
}
