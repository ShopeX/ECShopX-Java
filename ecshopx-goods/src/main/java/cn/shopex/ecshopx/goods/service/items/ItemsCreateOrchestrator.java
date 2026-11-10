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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.goods.search.sync.GoodsSearchIndexDirtyFanout;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ItemsCreateOrchestrator {

	private final PlatformItemsAddService platformItemsAddService;
	private final SupplierItemsAddService supplierItemsAddService;
	private final DistributorGoodsSyncService distributorGoodsSyncService;
	private final GoodsSearchIndexDirtyFanout goodsSearchIndexDirtyFanout;

	public ItemsCreateOrchestrator(
			PlatformItemsAddService platformItemsAddService,
			SupplierItemsAddService supplierItemsAddService,
			DistributorGoodsSyncService distributorGoodsSyncService,
			GoodsSearchIndexDirtyFanout goodsSearchIndexDirtyFanout) {
		this.platformItemsAddService = platformItemsAddService;
		this.supplierItemsAddService = supplierItemsAddService;
		this.distributorGoodsSyncService = distributorGoodsSyncService;
		this.goodsSearchIndexDirtyFanout = goodsSearchIndexDirtyFanout;
	}

	public void createItems(Map<String, Object> params) {
		long supplierId = params.get("supplier_id") != null ? toLong(params.get("supplier_id")) : 0L;
		if (supplierId > 0) {
			supplierItemsAddService.addItemsTransactional(params);
			return;
		}
		long defaultItemId = platformItemsAddService.addItemsTransactional(params);
		long companyId = toLong(params.get("company_id"));
		distributorGoodsSyncService.syncGoods(companyId, defaultItemId);
		if (defaultItemId > 0) {
			goodsSearchIndexDirtyFanout.scheduleUpsertForDefaultItem(companyId, defaultItemId);
		}
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}
}
