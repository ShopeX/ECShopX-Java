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

package cn.shopex.ecshopx.orders.repository;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.mapper.SupplierItemDeleteOrderGuardMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class SupplierItemDeleteOrderGuardRepository {

	private static final String MSG_BOTH =
			"存在未完结的订单和未完成的售后单，不能删除";
	private static final String MSG_ORDERS_ONLY = "存在未完结的订单，不能删除";
	private static final String MSG_AFTERSALES_ONLY = "存在未完成的售后单，不能删除";

	private final SupplierItemDeleteOrderGuardMapper mapper;

	public SupplierItemDeleteOrderGuardRepository(SupplierItemDeleteOrderGuardMapper mapper) {
		this.mapper = mapper;
	}

	public void assertNoBlockingOrdersOrAftersales(long companyId, long supplierId, List<Long> poolItemIds) {
		if (poolItemIds == null || poolItemIds.isEmpty()) {
			return;
		}
		int nowSeconds = (int) (System.currentTimeMillis() / 1000L);
		long orderCount = mapper.countUnfinishedOrders(companyId, supplierId, nowSeconds, poolItemIds);
		long aftersalesCount = mapper.countUnfinishedAftersales(companyId, supplierId, poolItemIds);
		if (orderCount > 0 && aftersalesCount > 0) {
			throw new ResourceException(MSG_BOTH);
		}
		if (orderCount > 0) {
			throw new ResourceException(MSG_ORDERS_ONLY);
		}
		if (aftersalesCount > 0) {
			throw new ResourceException(MSG_AFTERSALES_ONLY);
		}
	}
}
