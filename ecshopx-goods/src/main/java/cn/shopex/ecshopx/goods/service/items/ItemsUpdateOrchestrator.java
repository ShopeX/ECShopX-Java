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

import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ItemsUpdateOrchestrator {

	private final PlatformItemsAddService platformItemsAddService;
	private final SupplierItemsAddService supplierItemsAddService;
	private final DistributorGoodsSyncService distributorGoodsSyncService;
	private final SupplierItemsReviewGoodsService supplierItemsReviewGoodsService;

	public ItemsUpdateOrchestrator(
			PlatformItemsAddService platformItemsAddService,
			SupplierItemsAddService supplierItemsAddService,
			DistributorGoodsSyncService distributorGoodsSyncService,
			SupplierItemsReviewGoodsService supplierItemsReviewGoodsService) {
		this.platformItemsAddService = platformItemsAddService;
		this.supplierItemsAddService = supplierItemsAddService;
		this.distributorGoodsSyncService = distributorGoodsSyncService;
		this.supplierItemsReviewGoodsService = supplierItemsReviewGoodsService;
	}

	public void updateItems(Map<String, Object> params, long pathItemId, boolean isSupplierGoods) {
		String op = str(params.get("operator_type"));
		if ("supplier".equals(op)) {
			params.put("supplier_id", toLong(params.get("operator_id")));
			Object audit = params.get("audit_status");
			if (audit == null || !"processing".equals(audit.toString())) {
				params.put("audit_status", "submitting");
			}
			supplierItemsAddService.addItemsTransactional(params);
			return;
		}
		if (isSupplierGoods) {
			String auditStatus = str(params.get("audit_status"));
			if ("approved".equals(auditStatus)) {
				supplierItemsAddService.addItemsTransactional(params);
			}
			supplierItemsReviewGoodsService.reviewGoods(params, pathItemId);
			return;
		}
		long defaultItemId = platformItemsAddService.addItemsTransactional(params);
		long companyId = toLong(params.get("company_id"));
		distributorGoodsSyncService.syncGoods(companyId, defaultItemId);
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}
}
