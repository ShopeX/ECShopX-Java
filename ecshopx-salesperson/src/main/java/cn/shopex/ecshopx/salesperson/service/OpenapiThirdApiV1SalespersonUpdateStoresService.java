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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.domain.ShopsRelSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import cn.shopex.ecshopx.salesperson.mapper.ShopsRelSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV1SalespersonUpdateStoresService {

	private static final String MSG_QUERY_ERROR = "导购员信息查询错误";
	private static final String MSG_STORE_NOT_FOUND = "未查询到店铺信息";
	private static final String STORE_TYPE_DISTRIBUTOR = "distributor";

	private final ShopSalespersonMapper shopSalespersonMapper;
	private final ShopsRelSalespersonMapper shopsRelSalespersonMapper;
	private final DistributorListQueryService distributorListQueryService;
	private final TransactionTemplate transactionTemplate;

	public OpenapiThirdApiV1SalespersonUpdateStoresService(
			ShopSalespersonMapper shopSalespersonMapper,
			ShopsRelSalespersonMapper shopsRelSalespersonMapper,
			DistributorListQueryService distributorListQueryService,
			TransactionTemplate transactionTemplate) {
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.shopsRelSalespersonMapper = shopsRelSalespersonMapper;
		this.distributorListQueryService = distributorListQueryService;
		this.transactionTemplate = transactionTemplate;
	}

	public void executeUpdateSalespersonStores(long companyId, String employeeNumber, String storeBn) {
		ShopSalesperson row = shopSalespersonMapper.selectOne(
				new LambdaQueryWrapper<ShopSalesperson>()
						.eq(ShopSalesperson::getCompanyId, companyId)
						.eq(ShopSalesperson::getWorkUserid, employeeNumber)
						.last("LIMIT 1")
						.select(ShopSalesperson::getSalespersonId));

		if (row == null) {
			throw new ResourceException(MSG_QUERY_ERROR);
		}

		updateSalespersonStore(companyId, row.getSalespersonId(), storeBn);
	}

	private void updateSalespersonStore(long companyId, long salespersonId, String storeBn) {
		if (isStoreBnLooseEqualZero(storeBn)) {
			shopsRelSalespersonMapper.delete(new LambdaQueryWrapper<ShopsRelSalesperson>()
					.eq(ShopsRelSalesperson::getCompanyId, companyId)
					.eq(ShopsRelSalesperson::getSalespersonId, salespersonId));
			return;
		}

		List<String> shopCodes;
		if (storeBn == null || !StringUtils.hasText(storeBn)) {
			shopCodes = List.of("");
		} else {
			shopCodes = Arrays.stream(storeBn.split(","))
					.map(String::trim)
					.filter(StringUtils::hasText)
					.toList();
			if (shopCodes.isEmpty()) {
				shopCodes = List.of("");
			}
		}

		List<Distributor> distributors = distributorListQueryService
				.listValidByCompanyAndShopCodesOrderedByCreatedDesc(companyId, shopCodes);
		if (distributors.isEmpty()) {
			throw new ResourceException(MSG_STORE_NOT_FOUND);
		}

		List<Long> distributorIds = distributors.stream()
				.map(Distributor::getDistributorId)
				.filter(Objects::nonNull)
				.toList();

		transactionTemplate.executeWithoutResult(status -> {
			shopsRelSalespersonMapper.delete(new LambdaQueryWrapper<ShopsRelSalesperson>()
					.eq(ShopsRelSalesperson::getCompanyId, companyId)
					.eq(ShopsRelSalesperson::getSalespersonId, salespersonId));
			for (Long distributorId : distributorIds) {
				if (distributorId == null) {
					continue;
				}
				ShopsRelSalesperson rel = new ShopsRelSalesperson();
				rel.setShopId(distributorId);
				rel.setSalespersonId(salespersonId);
				rel.setCompanyId(companyId);
				rel.setStoreType(STORE_TYPE_DISTRIBUTOR);
				shopsRelSalespersonMapper.insert(rel);
			}
		});
	}

	private static boolean isStoreBnLooseEqualZero(String storeBn) {
		if (storeBn == null) {
			return false;
		}
		String trimmed = storeBn.trim();
		if ("0".equals(trimmed)) {
			return true;
		}
		try {
			return Double.parseDouble(trimmed) == 0.0;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
