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

import cn.shopex.ecshopx.common.inventory.InventoryDeductTarget;
import cn.shopex.ecshopx.common.inventory.ItemInventoryLineContext;
import org.springframework.stereotype.Component;

@Component
public class InventoryDeductTargetResolver {

	/**
	 * 对齐 PHP {@code ItemStoreService::minusItemStore}：仅 {@code is_total_store=false} 且
	 * {@code distributor_id>0} 时用店前缀 key；快递供应商商品优先扣供应商池。
	 */
	public InventoryDeductTarget resolve(ItemInventoryLineContext ctx) {
		if (ctx == null) {
			return InventoryDeductTarget.PLATFORM_ITEMS;
		}
		if (isLogisticsReceipt(ctx.receiptType()) && ctx.supplierId() > 0L) {
			return InventoryDeductTarget.SUPPLIER_ITEMS;
		}
		if (!ctx.isTotalStore() && ctx.distributorId() > 0L) {
			return InventoryDeductTarget.SHOP_DISTRIBUTOR_ITEMS;
		}
		return InventoryDeductTarget.PLATFORM_ITEMS;
	}

	public static boolean isLogisticsReceipt(String receiptType) {
		return "logistics".equalsIgnoreCase(stringVal(receiptType));
	}

	private static String stringVal(String v) {
		return v == null ? "" : v.trim();
	}
}
