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

package cn.shopex.ecshopx.goods.service.wxapp;

import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import cn.shopex.ecshopx.goods.service.cart.salesperson.SalespersonCartDistributorSkuReplaceService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * standard 云店列表在编排查询后，用 {@code distributor_items} 覆盖门店库存/门店价（与 PHP 门店 replace 一致）。
 */
@Service
public class WxappStandardDistributorSkuListReplaceService {

	private final ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver;
	private final SalespersonCartDistributorSkuReplaceService salespersonCartDistributorSkuReplaceService;

	public WxappStandardDistributorSkuListReplaceService(
			ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver,
			SalespersonCartDistributorSkuReplaceService salespersonCartDistributorSkuReplaceService) {
		this.itemsCategoryDistributorIdResolver = itemsCategoryDistributorIdResolver;
		this.salespersonCartDistributorSkuReplaceService = salespersonCartDistributorSkuReplaceService;
	}

	public void applyIfNeeded(long companyId, Map<String, Object> params, List<Map<String, Object>> list) {
		if (list == null || list.isEmpty()) {
			return;
		}
		if (!Boolean.TRUE.equals(params.get("is_can_sale"))) {
			return;
		}
		if (!"standard".equals(itemsCategoryDistributorIdResolver.resolveProductModel(companyId))) {
			return;
		}
		long distributorId = resolvePrimaryDistributorIdFromParams(params.get("distributor_id"));
		if (distributorId <= 0) {
			return;
		}
		salespersonCartDistributorSkuReplaceService.apply(companyId, distributorId, list);
		for (Map<String, Object> row : list) {
			long store = longVal(row.get("store"));
			row.put("v_store", store != 0 ? 1 : 0);
		}
	}

	private static long resolvePrimaryDistributorIdFromParams(Object dist) {
		if (dist instanceof List<?> dl && !dl.isEmpty()) {
			return longVal(dl.get(0));
		}
		return longVal(dist);
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}