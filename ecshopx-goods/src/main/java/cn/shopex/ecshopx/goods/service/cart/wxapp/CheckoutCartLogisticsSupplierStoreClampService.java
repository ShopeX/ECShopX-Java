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

import cn.shopex.ecshopx.goods.service.items.ItemAvailableStoreResolver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 结算页按配送方式限制可用库存（仅内存参与计价，不回写购物车）：
 * <ul>
 *   <li>{@code receipt_type=logistics}：供应商商品仅可用供应商库存</li>
 *   <li>其他配送方式：仅可用平台/店铺本地库存</li>
 * </ul>
 */
@Service
public class CheckoutCartLogisticsSupplierStoreClampService {

	private final ItemAvailableStoreResolver itemAvailableStoreResolver;

	public CheckoutCartLogisticsSupplierStoreClampService(ItemAvailableStoreResolver itemAvailableStoreResolver) {
		this.itemAvailableStoreResolver = itemAvailableStoreResolver;
	}

	/**
	 * @return 因库存不足被下调购买数量的明细（不写 DB）；无调整时返回空列表
	 */
	public List<Map<String, Object>> clampIfNeeded(
			long companyId,
			boolean isCheckout,
			Map<String, Object> inputData,
			List<Map<String, Object>> validLines,
			List<Map<String, Object>> invalidLines,
			Map<Long, Map<String, Object>> skuByItem) {
		if (!isCheckout || inputData == null || validLines == null || validLines.isEmpty()) {
			return List.of();
		}
		List<Map<String, Object>> adjustments = new ArrayList<>();
		String receiptType = stringVal(inputData.get("receipt_type"));
		if ("logistics".equals(receiptType)) {
			clampToSupplierStore(companyId, validLines, invalidLines, skuByItem, adjustments, receiptType);
		} else {
			clampToLocalStore(companyId, validLines, invalidLines, skuByItem, adjustments, receiptType);
		}
		return adjustments;
	}

	/** 快递：供应商商品按供应商剩余库存参与计价。 */
	private void clampToSupplierStore(
			long companyId,
			List<Map<String, Object>> validLines,
			List<Map<String, Object>> invalidLines,
			Map<Long, Map<String, Object>> skuByItem,
			List<Map<String, Object>> adjustments,
			String receiptType) {
		Iterator<Map<String, Object>> it = validLines.iterator();
		while (it.hasNext()) {
			Map<String, Object> line = it.next();
			long itemId = longVal(line.get("item_id"), 0L);
			Map<String, Object> sku = skuByItem != null ? skuByItem.get(itemId) : null;
			if (sku == null) {
				continue;
			}
			long supplierId = longVal(sku.get("supplier_id"), 0L);
			if (supplierId <= 0L) {
				continue;
			}
			int available = itemAvailableStoreResolver.resolveAvailable(companyId, receiptType, sku);
			int logisticsDisplay =
					itemAvailableStoreResolver.resolveLogisticsStoreForDisplay(receiptType, sku, available);
			applyClamp(line, it, invalidLines, available, logisticsDisplay, adjustments, receiptType);
		}
	}

	/**
	 * 非快递：按平台/店铺本地库存参与计价。SKU 的 {@code store} 经前台 enrich 后可能是本地、共享或二者之和；
	 * 本地库存优先按 {@code store - logistics_store} 估算，若为负则回退为当前 {@code store}。
	 */
	private void clampToLocalStore(
			long companyId,
			List<Map<String, Object>> validLines,
			List<Map<String, Object>> invalidLines,
			Map<Long, Map<String, Object>> skuByItem,
			List<Map<String, Object>> adjustments,
			String receiptType) {
		Iterator<Map<String, Object>> it = validLines.iterator();
		while (it.hasNext()) {
			Map<String, Object> line = it.next();
			long itemId = longVal(line.get("item_id"), 0L);
			Map<String, Object> sku = skuByItem != null ? skuByItem.get(itemId) : null;
			if (sku == null && !line.containsKey("store")) {
				continue;
			}
			Map<String, Object> ctx = mergeLineSkuContext(line, sku);
			int available = itemAvailableStoreResolver.resolveAvailable(companyId, receiptType, ctx);
			int logisticsDisplay =
					itemAvailableStoreResolver.resolveLogisticsStoreForDisplay(receiptType, ctx, available);
			applyClamp(line, it, invalidLines, available, logisticsDisplay, adjustments, receiptType);
		}
	}

	/** line 优先（is_total_store / store），其余字段回退 sku。 */
	private static Map<String, Object> mergeLineSkuContext(Map<String, Object> line, Map<String, Object> sku) {
		Map<String, Object> ctx = new HashMap<>();
		if (sku != null) {
			ctx.putAll(sku);
		}
		if (line != null) {
			if (line.containsKey("store")) {
				ctx.put("store", line.get("store"));
			}
			if (line.containsKey("logistics_store")) {
				ctx.put("logistics_store", line.get("logistics_store"));
			}
			if (line.containsKey("is_total_store")) {
				ctx.put("is_total_store", line.get("is_total_store"));
			}
			if (line.containsKey("supplier_id")) {
				ctx.put("supplier_id", line.get("supplier_id"));
			}
			if (line.containsKey("supplier_item_id")) {
				ctx.put("supplier_item_id", line.get("supplier_item_id"));
			}
		}
		return ctx;
	}

	private static void applyClamp(
			Map<String, Object> line,
			Iterator<Map<String, Object>> it,
			List<Map<String, Object>> invalidLines,
			int available,
			int logisticsStore,
			List<Map<String, Object>> adjustments,
			String receiptType) {
		int num = intVal(line.get("num"), 0);
		line.put("cart_num", num);
		line.put("logistics_store", Math.max(logisticsStore, 0));
		if (num <= available) {
			line.put("store", Math.max(available, 0));
			return;
		}
		int clamped = Math.max(available, 0);
		// 仅勾选主品写入结算弹窗明细；未勾选行（含仅作赠品 SKU 的失效行）静默下调，避免赠品无货误弹窗
		if (adjustments != null && isCheckedLine(line)) {
			Map<String, Object> tip = new LinkedHashMap<>();
			tip.put("item_id", longVal(line.get("item_id"), 0L));
			tip.put("item_name", stringVal(line.get("item_name")));
			tip.put("original_num", num);
			tip.put("available_num", clamped);
			tip.put("receipt_type", receiptType == null ? "" : receiptType);
			adjustments.add(tip);
		}
		line.put("num", clamped);
		line.put("store", clamped);
		if (clamped <= 0) {
			it.remove();
			if (invalidLines != null) {
				invalidLines.add(line);
			}
		}
	}

	/** 结算弹窗只针对勾选行；缺省视为勾选（兼容未带 is_checked 的线下/快速购路径）。 */
	private static boolean isCheckedLine(Map<String, Object> line) {
		if (line == null || !line.containsKey("is_checked")) {
			return true;
		}
		Object v = line.get("is_checked");
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = v == null ? "" : v.toString().trim();
		return !"0".equals(s) && !"false".equalsIgnoreCase(s);
	}

	/** 结算页弹窗文案：全部无库存时引导切换配送，否则提示调整数量。 */
	public static String buildStoreQuantityAdjustTip(
			List<Map<String, Object>> adjustments, boolean blockCheckout, String suggestedReceiptType) {
		if (adjustments == null || adjustments.isEmpty()) {
			return "";
		}
		StringBuilder lines = new StringBuilder();
		for (Map<String, Object> item : adjustments) {
			if (item == null) {
				continue;
			}
			String name = stringVal(item.get("item_name"));
			if (name.isEmpty()) {
				name = "商品" + stringVal(item.get("item_id"));
			}
			int from = intVal(item.get("original_num"), 0);
			int to = intVal(item.get("available_num"), 0);
			if (lines.length() > 0) {
				lines.append('\n');
			}
			if (to <= 0) {
				lines.append('「').append(name).append("」暂无库存");
			} else {
				lines.append('「').append(name).append("」：").append(from).append("件 → ").append(to).append('件');
			}
		}
		if (blockCheckout) {
			String suggestLabel = receiptTypeLabel(suggestedReceiptType);
			if (!suggestLabel.isEmpty()) {
				return lines + "\n确认后将切换为" + suggestLabel;
			}
			return lines + "\n当前配送方式下暂无可结算商品，请返回购物车或切换配送方式";
		}
		return "当前配送方式下部分商品库存不足，确认后将调整本单购买数量：\n" + lines;
	}

	private static String receiptTypeLabel(String receiptType) {
		if (receiptType == null) {
			return "";
		}
		return switch (receiptType.trim()) {
			case "ziti" -> "自提";
			case "logistics" -> "快递";
			case "merchant" -> "同城配送";
			default -> "";
		};
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
