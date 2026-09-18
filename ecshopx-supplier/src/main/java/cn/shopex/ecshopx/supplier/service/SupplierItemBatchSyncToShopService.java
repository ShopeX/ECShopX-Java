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

package cn.shopex.ecshopx.supplier.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.integration.SupplierItemsSyncToShopExecutor;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SupplierItemBatchSyncToShopService {

	private final SupplierItemsSyncToShopExecutor supplierItemsSyncToShopExecutor;

	public SupplierItemBatchSyncToShopService(SupplierItemsSyncToShopExecutor supplierItemsSyncToShopExecutor) {
		this.supplierItemsSyncToShopExecutor = supplierItemsSyncToShopExecutor;
	}

	public List<Long> batchSyncToShop(long companyIdFromJwt, long distributorIdFromJwt, Map<String, Object> jwt, Map<String, Object> body) {
		String operatorType = Objects.toString(jwt.get("operator_type"), "").trim();
		if (!"distributor".equalsIgnoreCase(operatorType)) {
			throw new ForbiddenException("仅店铺可同步供应商商品");
		}
		if (distributorIdFromJwt <= 0L) {
			throw new BadRequestException("店铺信息无效");
		}

		String itemIdsStr = Objects.toString(body.get("item_ids"), "").trim();
		if (!StringUtils.hasText(itemIdsStr)) {
			throw new BadRequestException("请选择需要同步的商品");
		}

		List<Long> idList = new ArrayList<>();
		for (String segment : itemIdsStr.split(",")) {
			String s = segment == null ? "" : segment.trim();
			if (!StringUtils.hasText(s)) {
				continue;
			}
			try {
				idList.add(Long.parseLong(s));
			} catch (NumberFormatException e) {
				throw new BadRequestException("item_ids 格式错误");
			}
		}
		if (idList.isEmpty()) {
			throw new BadRequestException("请选择需要同步的商品");
		}

		List<Long> result = new ArrayList<>();
		for (long pathItemId : new LinkedHashSet<>(idList)) {
			supplierItemsSyncToShopExecutor.syncToShop(companyIdFromJwt, distributorIdFromJwt, pathItemId);
			result.add(pathItemId);
		}
		return result;
	}
}
