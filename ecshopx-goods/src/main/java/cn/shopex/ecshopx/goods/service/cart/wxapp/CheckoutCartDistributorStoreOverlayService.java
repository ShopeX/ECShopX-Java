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

package cn.shopex.ecshopx.goods.service.cart.wxapp;

import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.goods.service.distributor.DistributorItemSkuInfoForStoreService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 结算购物车行店铺库存覆盖，对齐 PHP {@code DistributorItemsService::replaceItemInfo}：
 * {@code is_total_store=false} 时 {@code store} 为店铺库存，{@code logistics_store} 为总部可补货库存。
 */
@Service
public class CheckoutCartDistributorStoreOverlayService {

	private final DistributorItemSkuInfoForStoreService distributorItemSkuInfoForStoreService;

	public CheckoutCartDistributorStoreOverlayService(
			DistributorItemSkuInfoForStoreService distributorItemSkuInfoForStoreService) {
		this.distributorItemSkuInfoForStoreService = distributorItemSkuInfoForStoreService;
	}

	public void applyForLines(
			long companyId,
			String shopType,
			List<Map<String, Object>> lines,
			Map<Long, Map<String, Object>> skuByItem) {
		if (lines == null || lines.isEmpty() || companyId <= 0L) {
			return;
		}
		String st = shopType == null ? "" : shopType.trim();
		if (!"distributor".equals(st) && !"drug".equals(st)) {
			return;
		}
		for (Map<String, Object> line : lines) {
			long shopId = longVal(line.get("shop_id"), 0L);
			long itemId = longVal(line.get("item_id"), 0L);
			if (shopId <= 0L || itemId <= 0L) {
				continue;
			}
			DistributorItems di = distributorItemSkuInfoForStoreService.findRow(companyId, itemId, shopId);
			if (di == null) {
				continue;
			}
			boolean totalStore = Boolean.TRUE.equals(di.getIsTotalStore());
			line.put("is_total_store", totalStore);
			if (totalStore) {
				continue;
			}
			Map<String, Object> sku = skuByItem != null ? skuByItem.get(itemId) : null;
			int platformStore = sku != null ? intVal(sku.get("store"), 0) : intVal(line.get("store"), 0);
			String approve = stringVal(line.get("approve_status"));
			if ("onsale".equals(approve) || "offline_sale".equals(approve)) {
				line.put("logistics_store", platformStore);
			}
			if (di.getStore() != null) {
				line.put("store", di.getStore().intValue());
			}
			if (di.getPrice() != null && di.getPrice() > 0L) {
				line.put("price", di.getPrice().intValue());
			}
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}

	private static long longVal(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static int intVal(Object v, int def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
