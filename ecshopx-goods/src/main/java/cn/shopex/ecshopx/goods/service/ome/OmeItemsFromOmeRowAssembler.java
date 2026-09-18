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

package cn.shopex.ecshopx.goods.service.ome;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OmeItemsFromOmeRowAssembler {

	private final OmeItemsFromOmeCategoryBrandSpecResolver resolver;

	public OmeItemsFromOmeRowAssembler(OmeItemsFromOmeCategoryBrandSpecResolver resolver) {
		this.resolver = resolver;
	}

	/**
	 * OME 接口金额单位为「元」；落库与 {@code PlatformItemsAddService} 一致，以「分」存储。
	 */
	private static int omeMoneyToFen(Object v) {
		if (v == null) {
			return 0;
		}
		BigDecimal yuan = new BigDecimal(v.toString().trim());
		return yuan.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).intValue();
	}

	private static double omeWeight(Object v) {
		if (v == null) {
			return 0.0;
		}
		if (v instanceof Number n) {
			return n.doubleValue();
		}
		try {
			return Double.parseDouble(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}

	private static String str(Object v) {
		return v != null ? String.valueOf(v) : "";
	}

	private static boolean isNospec(Object specInfo) {
		if (specInfo == null) {
			return true;
		}
		if (Boolean.FALSE.equals(specInfo)) {
			return true;
		}
		if (specInfo instanceof Number n && n.doubleValue() == 0.0) {
			return true;
		}
		if (specInfo instanceof String s) {
			return !StringUtils.hasText(s);
		}
		if (specInfo instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		if (specInfo instanceof Iterable<?> it) {
			return !it.iterator().hasNext();
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public void handleRow(
			long companyId,
			Map<String, Object> row,
			AtomicReference<Map<String, Object>> itemInfoRef,
			Map<String, Object> itemMap,
			String[] preGoodsBnHolder) {
		boolean nospec = isNospec(row.get("spec_info"));
		String goodsBnTrim = str(row.get("goods_bn")).trim();
		boolean multiSpec = !nospec;
		boolean isDefault;
		if (multiSpec && preGoodsBnHolder[0] != null && goodsBnTrim.equals(preGoodsBnHolder[0])) {
			isDefault = false;
		} else {
			isDefault = true;
			preGoodsBnHolder[0] = goodsBnTrim;
		}

		if (isDefault) {
			Map<String, Object> itemInfo = new LinkedHashMap<>();
			itemInfo.put("company_id", companyId);
			itemInfo.put("item_type", "normal");
			itemInfo.put("item_name", str(row.get("goods_name")).trim());
			itemInfo.put("nospec", nospec);
			itemInfo.put("item_main_cat_id", resolver.getItemCategoryId(companyId, row));
			itemInfo.put("brand_id", resolver.getBrandId(companyId, row));
			itemInfo.put("item_bn", str(row.get("product_bn")));
			itemInfo.put("weight", omeWeight(row.get("weight")));
			itemInfo.put("barcode", str(row.get("barcode")));
			itemInfo.put("price", omeMoneyToFen(row.get("price")));
			itemInfo.put("cost_price", omeMoneyToFen(row.get("cost")));
			itemInfo.put("market_price", omeMoneyToFen(row.get("mktprice")));
			itemInfo.put("item_unit", str(row.get("unit")));
			itemInfo.put("approve_status", itemMap.get("approve_status") != null ? String.valueOf(itemMap.get("approve_status")) : "instock");
			itemInfo.put("is_default", isDefault);
			itemInfo.put("spec_items", new ArrayList<Map<String, Object>>());
			copyUnsyncedFromItemMap(itemInfo, itemMap);
			itemInfoRef.set(itemInfo);
		}

		if (multiSpec) {
			Map<String, Object> itemInfo = itemInfoRef.get();
			if (itemInfo == null) {
				return;
			}
			List<Map<String, Object>> specItems = (List<Map<String, Object>>) itemInfo.get("spec_items");
			Map<String, Object> specItem = new LinkedHashMap<>();
			specItem.put("item_bn", str(row.get("product_bn")));
			specItem.put("weight", omeWeight(row.get("weight")));
			specItem.put("barcode", str(row.get("barcode")));
			specItem.put("price", omeMoneyToFen(row.get("price")));
			specItem.put("cost_price", omeMoneyToFen(row.get("cost")));
			specItem.put("market_price", omeMoneyToFen(row.get("mktprice")));
			specItem.put("item_unit", str(row.get("unit")));
			specItem.put("approve_status", itemMap.get("approve_status") != null ? String.valueOf(itemMap.get("approve_status")) : "instock");
			specItem.put("is_default", isDefault);
			specItem.put("item_spec", resolver.getItemSpec(companyId, row));
			specItem.put("item_id", itemMap.get("item_id"));
			specItem.put("store", itemMap.get("store") != null ? itemMap.get("store") : 0);
			specItems.add(specItem);
		}
	}

	private static void copyUnsyncedFromItemMap(Map<String, Object> itemInfo, Map<String, Object> itemMap) {
		itemInfo.put("item_id", itemMap.get("item_id"));
		itemInfo.put("store", itemMap.get("store") != null ? itemMap.get("store") : 0);
		itemInfo.put("consume_type", itemMap.get("consume_type"));
		itemInfo.put("brief", itemMap.get("brief"));
		itemInfo.put("sort", itemMap.get("sort"));
		itemInfo.put("templates_id", itemMap.get("templates_id"));
		itemInfo.put("is_show_specimg", itemMap.get("is_show_specimg"));
		itemInfo.put("pics", itemMap.get("pics"));
		itemInfo.put("video_type", itemMap.get("video_type"));
		itemInfo.put("videos", itemMap.get("videos"));
		itemInfo.put("intro", itemMap.get("intro"));
		itemInfo.put("special_type", itemMap.get("special_type"));
		itemInfo.put("purchase_agreement", itemMap.get("purchase_agreement"));
		itemInfo.put("enable_agreement", itemMap.get("enable_agreement"));
		itemInfo.put("item_address_city", itemMap.get("item_address_city"));
		itemInfo.put("item_address_province", itemMap.get("item_address_province"));
		itemInfo.put("date_type", itemMap.get("date_type"));
		itemInfo.put("begin_date", itemMap.get("begin_date"));
		itemInfo.put("end_date", itemMap.get("end_date"));
		itemInfo.put("fixed_term", itemMap.get("fixed_term"));
		itemInfo.put("tax_rate", itemMap.get("tax_rate"));
		itemInfo.put("crossborder_tax_rate", itemMap.get("crossborder_tax_rate"));
		itemInfo.put("origincountry_id", itemMap.get("origincountry_id"));
		itemInfo.put("type", itemMap.get("type"));
		itemInfo.put("distributor_id", itemMap.get("distributor_id"));
		itemInfo.put("item_source", itemMap.get("item_source"));
		itemInfo.put("is_gift", itemMap.get("is_gift"));
		itemInfo.put("is_profit", itemMap.get("is_profit"));
		itemInfo.put("profit_type", itemMap.get("profit_type"));
		itemInfo.put("profit_fee", itemMap.get("profit_fee"));
	}
}
