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

package cn.shopex.ecshopx.goods.service.order.normal;

import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.goods.service.items.ItemsCategoryPathByItemService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderCheckoutPlusBuyService {

	private final ItemsCategoryPathByItemService itemsCategoryPathByItemService;

	public OrderCheckoutPlusBuyService(ItemsCategoryPathByItemService itemsCategoryPathByItemService) {
		this.itemsCategoryPathByItemService = itemsCategoryPathByItemService;
	}

	@SuppressWarnings("unchecked")
	public void applyPlusBuyActivitiesFromCheckoutMeta(NormalOrderCreateParams p, Map<String, Object> checkoutMeta) {
		Object raw = checkoutMeta.get("plus_buy_activity");
		if (!(raw instanceof List<?> activities) || activities.isEmpty()) {
			return;
		}
		Map<String, Object> od = p.getOrderData();
		long companyId = longVal(od.get("company_id"), 0L);
		if (companyId <= 0L) {
			return;
		}
		Object itemsRaw = od.get("items");
		if (!(itemsRaw instanceof List<?> itemList)) {
			return;
		}
		List<Map<String, Object>> orderItems = new ArrayList<>();
		for (Object o : itemList) {
			if (o instanceof Map<?, ?> m) {
				orderItems.add(new LinkedHashMap<>((Map<String, Object>) m));
			}
		}
		List<Map<String, Object>> itemsPromotion = copyItemsPromotionList(od.get("items_promotion"));
		long orderDiscountFee = longVal(od.get("discount_fee"), 0L);
		long orderItemFee = longVal(od.get("item_fee"), 0L);
		long orderTotalFee = longVal(od.get("total_fee"), 0L);
		int totalItemNum = intVal(od.get("totalItemNum"), 0);

		for (Object actObj : activities) {
			if (!(actObj instanceof Map<?, ?> actRaw)) {
				continue;
			}
			Map<String, Object> activityData = new LinkedHashMap<>((Map<String, Object>) actRaw);
			long activityId = longVal(activityData.get("activity_id"), 0L);
			Object plusItemRaw = activityData.get("plus_item");
			if (activityId <= 0L || !(plusItemRaw instanceof Map<?, ?>)) {
				continue;
			}
			Map<String, Object> itemInfo = new LinkedHashMap<>((Map<String, Object>) plusItemRaw);
			Set<Long> activityItemIds = parseItemIdSet(activityData.get("activity_item_ids"));
			List<Map<String, Object>> activityItems = new ArrayList<>();
			List<Map<String, Object>> remaining = new ArrayList<>();
			for (Map<String, Object> line : orderItems) {
				long lineItemId = longVal(line.get("item_id"), 0L);
				if (!activityItemIds.isEmpty() && activityItemIds.contains(lineItemId)) {
					activityItems.add(line);
				} else {
					remaining.add(line);
				}
			}
			Map<String, Object> plusLine = buildPlusBuyOrderLine(activityData, itemInfo, od, companyId);
			int lineDiscount = intVal(plusLine.get("discount_fee"), 0);
			int lineItemFee = intVal(plusLine.get("item_fee"), 0);
			int lineTotalFee = intVal(plusLine.get("total_fee"), 0);
			int lineNum = intVal(plusLine.get("num"), 0);
			itemsPromotion.add(buildPlusBuyItemsPromotionRow(od, plusLine, activityData));
			orderDiscountFee += lineDiscount;
			orderItemFee += lineItemFee;
			orderTotalFee += lineTotalFee;
			totalItemNum += lineNum;
			Map<String, Object> discountInfo = resolveDiscountDesc(activityData);
			discountInfo.put("discount_fee", lineDiscount);
			appendOrderPlusBuyDiscountInfo(od, discountInfo);
			activityItems.add(plusLine);
			for (int i = 0; i < activityItems.size(); i++) {
				Map<String, Object> row = activityItems.get(i);
				Map<String, Object> lineDiscountInfo = new LinkedHashMap<>(discountInfo);
				lineDiscountInfo.put(
						"discount_fee",
						longVal(row.get("item_id"), 0L) == longVal(plusLine.get("item_id"), -1L)
								? lineDiscount
								: 0L);
				appendLineDiscountInfo(row, lineDiscountInfo);
				activityItems.set(i, row);
			}
			remaining.addAll(activityItems);
			orderItems = remaining;
		}

		od.put("items", orderItems);
		od.put("discount_fee", (int) Math.min(orderDiscountFee, Integer.MAX_VALUE));
		od.put("item_fee", String.valueOf(orderItemFee));
		od.put("total_fee", orderTotalFee);
		if (totalItemNum > 0) {
			od.put("totalItemNum", totalItemNum);
		}
		if (!itemsPromotion.isEmpty()) {
			od.put("items_promotion", itemsPromotion);
		}
	}

	private Map<String, Object> buildPlusBuyOrderLine(
			Map<String, Object> activityData,
			Map<String, Object> itemInfo,
			Map<String, Object> od,
			long companyId) {
		long price = longVal(itemInfo.get("price"), 0L);
		long plusPrice = longVal(itemInfo.get("plus_price"), 0L);
		int giftNum = intVal(itemInfo.get("gift_num"), 1);
		if (giftNum <= 0) {
			giftNum = 1;
		}
		long lineDiscount = (price - plusPrice) * giftNum;
		long lineItemFee = price * giftNum;
		long lineTotalFee = plusPrice * giftNum;
		Map<String, Object> line = new LinkedHashMap<>();
		line.put("order_id", od.get("order_id"));
		line.put("item_id", longVal(itemInfo.get("item_id"), 0L));
		line.put("goods_id", longVal(itemInfo.get("goods_id"), longVal(itemInfo.get("item_id"), 0L)));
		line.put("item_bn", firstNonBlank(itemInfo, "itemBn", "item_bn"));
		line.put("goods_bn", stringVal(itemInfo.get("goods_bn")));
		line.put("company_id", od.get("company_id"));
		line.put("user_id", od.get("user_id"));
		line.put("item_name", firstNonBlank(itemInfo, "itemName", "item_name"));
		line.put("templates_id", longVal(itemInfo.get("templates_id"), 0L));
		line.put("pic", firstPic(itemInfo.get("pics")));
		line.put("num", giftNum);
		line.put("price", (int) Math.min(price, Integer.MAX_VALUE));
		line.put("activity_price", (int) Math.min(plusPrice, Integer.MAX_VALUE));
		line.put("discount_fee", (int) Math.min(lineDiscount, Integer.MAX_VALUE));
		line.put("discount_info", new ArrayList<>());
		line.put("item_fee", (int) Math.min(lineItemFee, Integer.MAX_VALUE));
		line.put("cost_fee", 0);
		line.put("item_unit", stringVal(itemInfo.get("item_unit")));
		line.put("total_fee", (int) Math.min(lineTotalFee, Integer.MAX_VALUE));
		line.put("rebate", 0);
		line.put("total_rebate", 0);
		line.put("distributor_id", longVal(od.get("distributor_id"), 0L));
		line.put("mobile", stringVal(od.get("mobile")));
		line.put("is_total_store", !Boolean.FALSE.equals(itemInfo.get("is_total_store")));
		line.put("shop_id", longVal(od.get("shop_id"), 0L));
		line.put("fee_rate", od.get("fee_rate"));
		line.put("fee_type", od.get("fee_type"));
		line.put("fee_symbol", od.get("fee_symbol"));
		line.put("order_item_type", "plus_buy");
		line.put("is_gift", isGiftFlag(itemInfo.get("is_gift")));
		line.put("item_spec_desc", stringVal(itemInfo.get("item_spec_desc")));
		line.put("volume", 0);
		line.put("activity_id", longVal(activityData.get("activity_id"), 0L));
		line.put("item_category_main", resolveItemCategoryMain(itemInfo, companyId));
		line.put("cost_price", stringVal(itemInfo.get("cost_price")));
		line.put("market_price", stringVal(itemInfo.get("market_price")));
		line.put("supplier_id", stringVal(itemInfo.get("supplier_id")));
		line.put("is_logistics", false);
		return line;
	}

	private static Map<String, Object> buildPlusBuyItemsPromotionRow(
			Map<String, Object> od, Map<String, Object> plusLine, Map<String, Object> activityData) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("company_id", od.get("company_id"));
		row.put("user_id", od.get("user_id"));
		row.put("shop_id", longVal(od.get("distributor_id"), 0L));
		row.put("item_id", plusLine.get("item_id"));
		row.put("item_name", plusLine.get("item_name"));
		row.put("item_type", "normal");
		row.put("order_type", "normal");
		row.put("activity_id", activityData.get("activity_id"));
		Map<String, Object> discountDesc = resolveDiscountDesc(activityData);
		row.put("activity_type", stringVal(discountDesc.get("type"), "plus_price_buy"));
		row.put("activity_name", stringVal(discountDesc.get("info")));
		row.put("activity_tag", List.of());
		row.put("activity_desc", new LinkedHashMap<>(activityData));
		row.put("activity_rule", stringVal(discountDesc.get("rule")));
		return row;
	}

	private List<Map<String, Object>> resolveItemCategoryMain(Map<String, Object> itemInfo, long companyId) {
		long mainCatId = longVal(itemInfo.get("item_main_cat_id"), 0L);
		if (mainCatId <= 0L) {
			mainCatId = longVal(itemInfo.get("item_category"), 0L);
		}
		if (mainCatId <= 0L) {
			return List.of();
		}
		List<Map<String, Object>> path =
				itemsCategoryPathByItemService.getCategoryPathById(companyId, mainCatId, true);
		return path == null ? List.of() : path;
	}

	@SuppressWarnings("unchecked")
	private static void appendOrderPlusBuyDiscountInfo(Map<String, Object> od, Map<String, Object> discountInfo) {
		Object existing = od.get("discount_info");
		if (existing instanceof Map<?, ?> map) {
			Map<String, Object> merged = new LinkedHashMap<>((Map<String, Object>) map);
			String key = stringVal(discountInfo.get("type")) + stringVal(discountInfo.get("id"));
			merged.put(key, discountInfo);
			od.put("discount_info", merged);
			return;
		}
		List<Map<String, Object>> list = new ArrayList<>();
		if (existing instanceof List<?> lst) {
			for (Object el : lst) {
				if (el instanceof Map<?, ?> m) {
					list.add(new LinkedHashMap<>((Map<String, Object>) m));
				}
			}
		}
		list.add(discountInfo);
		od.put("discount_info", list);
	}

	@SuppressWarnings("unchecked")
	private static void appendLineDiscountInfo(Map<String, Object> item, Map<String, Object> discountInfo) {
		Object existing = item.get("discount_info");
		List<Map<String, Object>> list;
		if (existing instanceof List<?> lst) {
			list = new ArrayList<>();
			for (Object el : lst) {
				if (el instanceof Map<?, ?> m) {
					list.add(new LinkedHashMap<>((Map<String, Object>) m));
				}
			}
		} else {
			list = new ArrayList<>();
		}
		list.add(new LinkedHashMap<>(discountInfo));
		item.put("discount_info", list);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> resolveDiscountDesc(Map<String, Object> activityData) {
		Object raw = activityData.get("discount_desc");
		if (raw instanceof Map<?, ?> m) {
			return new LinkedHashMap<>((Map<String, Object>) m);
		}
		return new LinkedHashMap<>();
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> copyItemsPromotionList(Object raw) {
		if (!(raw instanceof List<?> list)) {
			return new ArrayList<>();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object el : list) {
			if (el instanceof Map<?, ?> m) {
				out.add(new LinkedHashMap<>((Map<String, Object>) m));
			}
		}
		return out;
	}

	private static Set<Long> parseItemIdSet(Object raw) {
		Set<Long> ids = new LinkedHashSet<>();
		if (!(raw instanceof List<?> list)) {
			return ids;
		}
		for (Object el : list) {
			long id = longVal(el, 0L);
			if (id > 0L) {
				ids.add(id);
			}
		}
		return ids;
	}

	private static boolean isGiftFlag(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		return "true".equals(String.valueOf(raw));
	}

	private static String firstPic(Object pics) {
		if (pics == null) {
			return "";
		}
		if (pics instanceof String s) {
			return s.trim();
		}
		if (pics instanceof List<?> lst && !lst.isEmpty()) {
			Object first = lst.get(0);
			return first == null ? "" : String.valueOf(first).trim();
		}
		return "";
	}

	private static String firstNonBlank(Map<String, Object> m, String... keys) {
		for (String key : keys) {
			String s = stringVal(m.get(key));
			if (StringUtils.hasText(s)) {
				return s;
			}
		}
		return "";
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

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString();
	}

	private static String stringVal(Object primary, String fallback) {
		String s = stringVal(primary);
		return StringUtils.hasText(s) ? s : fallback;
	}
}
