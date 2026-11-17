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
import org.springframework.util.StringUtils;

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
			Map<String, Object> sku = skuByItem != null ? skuByItem.get(itemId) : null;
			applyOne(companyId, shopId, itemId, line, sku, false);
		}
	}

	/**
	 * 满赠/加价购等已加载的平台 SKU map：按店铺关联叠店仓（与结算主品 {@link #applyForLines} 规则一致）。
	 * {@code distributorId&lt;=0} 或无关联行时不改写。缺 {@code approve_status} 时仍写入 {@code logistics_store}。
	 */
	public void applyForSkuMaps(long companyId, long distributorId, List<Map<String, Object>> skuMaps) {
		if (skuMaps == null || skuMaps.isEmpty() || companyId <= 0L || distributorId <= 0L) {
			return;
		}
		for (Map<String, Object> row : skuMaps) {
			if (row == null) {
				continue;
			}
			long itemId = longVal(row.get("item_id"), 0L);
			if (itemId <= 0L) {
				continue;
			}
			applyOne(companyId, distributorId, itemId, row, null, true);
		}
	}

	private void applyOne(
			long companyId,
			long distributorId,
			long itemId,
			Map<String, Object> target,
			Map<String, Object> skuFallback,
			boolean writeLogisticsWhenApproveMissing) {
		DistributorItems di = distributorItemSkuInfoForStoreService.findRow(companyId, itemId, distributorId);
		if (di == null) {
			return;
		}
		boolean totalStore = Boolean.TRUE.equals(di.getIsTotalStore());
		target.put("is_total_store", totalStore);
		if (totalStore) {
			return;
		}
		int platformStore =
				skuFallback != null ? intVal(skuFallback.get("store"), 0) : intVal(target.get("store"), 0);
		String approve = stringVal(target.get("approve_status"));
		if (!StringUtils.hasText(approve) && skuFallback != null) {
			approve = stringVal(skuFallback.get("approve_status"));
		}
		boolean writeLogistics =
				"onsale".equals(approve)
						|| "offline_sale".equals(approve)
						|| (writeLogisticsWhenApproveMissing && !StringUtils.hasText(approve));
		if (writeLogistics) {
			target.put("logistics_store", platformStore);
		}
		if (di.getStore() != null) {
			target.put("store", di.getStore().intValue());
		}
		if (di.getPrice() != null && di.getPrice() > 0L) {
			target.put("price", di.getPrice().intValue());
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
