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
import org.springframework.stereotype.Component;

/**
 * 按配送方式解析可用库存，与结算页普通商品 clamp 规则一致：
 * <ul>
 *   <li>{@code receipt_type=logistics} 且供应商商品：供应商库存（无 supplier_item_id 时回退 logistics_store）</li>
 *   <li>非快递：本地库存（is_total_store=false 用 store；否则优先 store - logistics_store）</li>
 *   <li>其他：{@code store}</li>
 * </ul>
 */
@Component
public class ItemAvailableStoreResolver {

	private final SupplierItemStoreService supplierItemStoreService;

	public ItemAvailableStoreResolver(SupplierItemStoreService supplierItemStoreService) {
		this.supplierItemStoreService = supplierItemStoreService;
	}

	/**
	 * @param ctx SKU 或行上下文，读取 supplier_id / supplier_item_id / store / logistics_store / is_total_store
	 * @return 可用库存（≥0）
	 */
	public int resolveAvailable(long companyId, String receiptType, Map<String, Object> ctx) {
		if (ctx == null) {
			return 0;
		}
		if (isLogistics(receiptType)) {
			long supplierId = longVal(ctx.get("supplier_id"), 0L);
			if (supplierId > 0L) {
				long supplierItemId = longVal(ctx.get("supplier_item_id"), 0L);
				if (supplierItemId > 0L) {
					return Math.max(0, supplierItemStoreService.resolveSupplierItemStore(companyId, supplierItemId));
				}
				return Math.max(0, intVal(ctx.get("logistics_store"), 0));
			}
			return Math.max(0, intVal(ctx.get("store"), 0));
		}
		if (!resolveTotalStore(ctx)) {
			return Math.max(0, intVal(ctx.get("store"), 0));
		}
		int store = intVal(ctx.get("store"), 0);
		int logistics = intVal(ctx.get("logistics_store"), 0);
		int local = store - logistics;
		if (local < 0) {
			local = store;
		}
		return Math.max(0, local);
	}

	/**
	 * 非结算购物车无 {@code receipt_type} 时：取快递与非快递路径可用库存的较大值，
	 * 避免仅因未选定配送方式而误隐藏仍有库存的赠品。
	 */
	public int resolveAvailableForCartDisplay(long companyId, Map<String, Object> ctx) {
		int logistics = resolveAvailable(companyId, "logistics", ctx);
		int local = resolveAvailable(companyId, "ziti", ctx);
		return Math.max(logistics, local);
	}

	/**
	 * 结算 clamp 写入行上的 logistics_store 展示值：快递供应商场景用 available；否则用上下文 logistics_store。
	 */
	public int resolveLogisticsStoreForDisplay(String receiptType, Map<String, Object> ctx, int available) {
		if (isLogistics(receiptType) && longVal(ctx != null ? ctx.get("supplier_id") : null, 0L) > 0L) {
			return Math.max(0, available);
		}
		return Math.max(0, intVal(ctx != null ? ctx.get("logistics_store") : null, 0));
	}

	private static boolean isLogistics(String receiptType) {
		return "logistics".equalsIgnoreCase(stringVal(receiptType));
	}

	private static boolean resolveTotalStore(Map<String, Object> ctx) {
		if (ctx.containsKey("is_total_store")) {
			return !Boolean.FALSE.equals(ctx.get("is_total_store"));
		}
		return true;
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
