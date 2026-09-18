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
import cn.shopex.ecshopx.goods.service.items.ItemAvailableStoreResolver;
import cn.shopex.ecshopx.goods.service.items.ItemsCategoryPathByItemService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderCheckoutFullGiftService {

	private final GiftActivityStoreAdjustService giftActivityStoreAdjustService;
	private final ItemAvailableStoreResolver itemAvailableStoreResolver;
	private final ItemsCategoryPathByItemService itemsCategoryPathByItemService;
	private final StringRedisTemplate companysRedisTemplate;

	public OrderCheckoutFullGiftService(
			GiftActivityStoreAdjustService giftActivityStoreAdjustService,
			ItemAvailableStoreResolver itemAvailableStoreResolver,
			ItemsCategoryPathByItemService itemsCategoryPathByItemService,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.giftActivityStoreAdjustService = giftActivityStoreAdjustService;
		this.itemAvailableStoreResolver = itemAvailableStoreResolver;
		this.itemsCategoryPathByItemService = itemsCategoryPathByItemService;
		this.companysRedisTemplate = companysRedisTemplate;
	}

	@SuppressWarnings("unchecked")
	public void applyGiftActivitiesFromCheckoutMeta(
			NormalOrderCreateParams p, Map<String, Object> checkoutMeta) {
		Object raw = checkoutMeta.get("gift_activity");
		if (!(raw instanceof List<?> activities) || activities.isEmpty()) {
			return;
		}
		Map<String, Object> od = p.getOrderData();
		long companyId = longVal(od.get("company_id"), 0L);
		long userId = longVal(od.get("user_id"), 0L);
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
		// 主品占用 + 已分配赠品占用，跨活动累计，避免同 SKU 超卖
		Map<Long, Integer> reservedByItemId =
				GiftActivityStoreAdjustService.buildReservedFromOrderItems(orderItems);
		String receiptType = resolveReceiptType(od, p.getParams());
		List<Map<String, Object>> giftStoreAdjustments = new ArrayList<>();

		for (Object actObj : activities) {
			if (!(actObj instanceof Map<?, ?> actRaw)) {
				continue;
			}
			Map<String, Object> activityData = new LinkedHashMap<>((Map<String, Object>) actRaw);
			long activityId = longVal(activityData.get("activity_id"), 0L);
			Object giftsRaw = activityData.get("gifts");
			if (activityId <= 0L || !(giftsRaw instanceof List<?> giftList) || giftList.isEmpty()) {
				continue;
			}
			long maxLimit = resolveMaxLimit(activityData);
			if (marketingJoinCount(companyId, activityId, userId) >= maxLimit) {
				continue;
			}
			GiftMergeResult merged =
					mergeGiftLines(
							activityData,
							giftList,
							od,
							companyId,
							receiptType,
							reservedByItemId,
							giftStoreAdjustments);
			if (merged.giftLines.isEmpty()) {
				continue;
			}
			// 与购物车一致：赠品紧挨关联主品（同一活动取最后一个关联主品）之后
			insertGiftLinesAfterLinkedMain(orderItems, activityData, merged.giftLines);
			orderDiscountFee += merged.totalDiscountFee;
			orderItemFee += merged.totalDiscountFee;
			appendOrderFullGiftDiscountInfo(od, activityData, merged.totalDiscountFee);
			for (Map<String, Object> giftLine : merged.giftLines) {
				itemsPromotion.add(buildGiftItemsPromotionRow(od, giftLine, activityData));
			}
		}

		od.put("items", orderItems);
		od.put("discount_fee", (int) Math.min(orderDiscountFee, Integer.MAX_VALUE));
		od.put("goods_discount", od.get("discount_fee"));
		od.put("item_fee", String.valueOf(orderItemFee));
		long totalFee = longVal(od.get("total_fee"), 0L);
		if (totalFee <= 0L && orderItemFee > 0L) {
			od.put("total_fee", totalFee);
		}
		if (!itemsPromotion.isEmpty()) {
			od.put("items_promotion", itemsPromotion);
		}
		if (!giftStoreAdjustments.isEmpty()) {
			od.put("gift_store_adjustments", giftStoreAdjustments);
			od.put("gift_store_adjust_tip", buildGiftStoreAdjustTip(giftStoreAdjustments));
		}
	}

	@SuppressWarnings("unchecked")
	public void allocateFullGiftFees(NormalOrderCreateParams p) {
		Map<String, Object> od = p.getOrderData();
		if (!"normal".equals(stringVal(od.get("order_type")))) {
			return;
		}
		Object promoRaw = od.get("items_promotion");
		if (!(promoRaw instanceof List<?> promoList) || promoList.isEmpty()) {
			return;
		}
		Object itemsRaw = od.get("items");
		if (!(itemsRaw instanceof List<?> itemList) || itemList.isEmpty()) {
			return;
		}
		List<Map<String, Object>> items = new ArrayList<>();
		for (Object o : itemList) {
			if (o instanceof Map<?, ?> m) {
				items.add((Map<String, Object>) m);
			}
		}
		Map<Long, Map<String, Object>> giftActivityById = new LinkedHashMap<>();
		for (Object o : promoList) {
			if (!(o instanceof Map<?, ?> row)) {
				continue;
			}
			Map<String, Object> promo = (Map<String, Object>) row;
			if (!"full_gift".equals(stringVal(promo.get("activity_type")))) {
				continue;
			}
			long activityId = longVal(promo.get("activity_id"), 0L);
			if (activityId <= 0L) {
				continue;
			}
			Object desc = promo.get("activity_desc");
			if (desc instanceof Map<?, ?> descMap) {
				giftActivityById.putIfAbsent(activityId, (Map<String, Object>) descMap);
			}
		}
		for (Map.Entry<Long, Map<String, Object>> entry : giftActivityById.entrySet()) {
			allocateOneGiftActivity(items, entry.getKey(), entry.getValue());
		}
		od.put("items", items);
	}

	private void allocateOneGiftActivity(
			List<Map<String, Object>> items, long activityId, Map<String, Object> giftActivity) {
		List<Long> activityItemIds = parseItemIdList(giftActivity.get("activity_item_ids"));
		Map<Integer, Long> feeByIndex = new LinkedHashMap<>();
		long totalGiftFee = 0L;
		for (int i = 0; i < items.size(); i++) {
			Map<String, Object> item = items.get(i);
			String orderItemType = stringVal(item.get("order_item_type"));
			if ("normal".equals(orderItemType) && activityItemIds.contains(itemIdOf(item))) {
				feeByIndex.put(i, longVal(item.get("total_fee"), 0L));
			}
			if ("gift".equals(orderItemType) && longVal(item.get("activity_id"), 0L) == activityId) {
				long itemFee = longVal(item.get("item_fee"), 0L);
				feeByIndex.put(i, itemFee);
				totalGiftFee += itemFee;
			}
		}
		if (feeByIndex.isEmpty() || totalGiftFee <= 0L) {
			return;
		}
		long sumFees = feeByIndex.values().stream().mapToLong(Long::longValue).sum();
		if (sumFees <= 0L) {
			return;
		}
		BigDecimal percent =
				BigDecimal.valueOf(totalGiftFee).divide(BigDecimal.valueOf(sumFees), 5, RoundingMode.DOWN);
		List<Integer> sortedKeys = new ArrayList<>(feeByIndex.keySet());
		sortedKeys.sort((a, b) -> Long.compare(feeByIndex.get(a), feeByIndex.get(b)));
		long remainingGiftFee = totalGiftFee;
		int i = 0;
		int count = sortedKeys.size();
		Map<String, Object> discountDescTemplate = resolveDiscountDesc(giftActivity);
		for (Integer key : sortedKeys) {
			i++;
			long itemTotalFee = feeByIndex.get(key);
			long discountFee;
			if (i == count) {
				discountFee = remainingGiftFee;
			} else {
				discountFee =
						BigDecimal.valueOf(itemTotalFee)
								.multiply(percent)
								.setScale(0, RoundingMode.DOWN)
								.longValue();
				remainingGiftFee -= discountFee;
			}
			Map<String, Object> item = items.get(key);
			if ("normal".equals(stringVal(item.get("order_item_type")))) {
				item.put("discount_fee", intVal(item.get("discount_fee"), 0) + (int) Math.min(discountFee, Integer.MAX_VALUE));
				item.put(
						"total_fee",
						(int)
								Math.max(
										0L,
										longVal(item.get("total_fee"), 0L) - discountFee));
			}
			if ("gift".equals(stringVal(item.get("order_item_type")))) {
				item.put("discount_fee", (int) Math.min(discountFee, Integer.MAX_VALUE));
				item.put(
						"total_fee",
						(int)
								Math.max(
										0L,
										longVal(item.get("item_fee"), 0L) - discountFee));
			}
			appendLineDiscountInfo(item, discountDescTemplate, discountFee);
		}
	}

	/**
	 * 将赠品插入到活动关联的最后一个主品之后；若该主品后已有赠品行，则追加在这些赠品之后。
	 * 找不到关联主品时回退为列表末尾。
	 */
	private static void insertGiftLinesAfterLinkedMain(
			List<Map<String, Object>> orderItems,
			Map<String, Object> activityData,
			List<Map<String, Object>> giftLines) {
		if (orderItems == null || giftLines == null || giftLines.isEmpty()) {
			return;
		}
		List<Long> activityItemIds = parseItemIdList(activityData.get("activity_item_ids"));
		int insertAfter = -1;
		for (int i = 0; i < orderItems.size(); i++) {
			Map<String, Object> line = orderItems.get(i);
			if ("gift".equals(stringVal(line.get("order_item_type")))) {
				continue;
			}
			if (activityItemIds.contains(itemIdOf(line))) {
				insertAfter = i;
			}
		}
		if (insertAfter < 0) {
			orderItems.addAll(giftLines);
			return;
		}
		int cursor = insertAfter + 1;
		while (cursor < orderItems.size()
				&& "gift".equals(stringVal(orderItems.get(cursor).get("order_item_type")))) {
			cursor++;
		}
		orderItems.addAll(cursor, giftLines);
	}

	@SuppressWarnings("unchecked")
	private GiftMergeResult mergeGiftLines(
			Map<String, Object> activityData,
			List<?> giftList,
			Map<String, Object> od,
			long companyId,
			String receiptType,
			Map<Long, Integer> reservedByItemId,
			List<Map<String, Object>> giftStoreAdjustments) {
		List<Map<String, Object>> giftLines = new ArrayList<>();
		long totalDiscountFee = 0L;
		for (Object giftObj : giftList) {
			if (!(giftObj instanceof Map<?, ?> giftRaw)) {
				continue;
			}
			Map<String, Object> itemInfo = new LinkedHashMap<>((Map<String, Object>) giftRaw);
			itemInfo.put("company_id", companyId);
			int originalGiftNum =
					intVal(
							itemInfo.get("original_gift_num"),
							intVal(itemInfo.get("gift_num"), 0));
			if (originalGiftNum <= 0) {
				continue;
			}
			boolean kept =
					giftActivityStoreAdjustService.adjustSingleGift(
							companyId, receiptType, itemInfo, reservedByItemId);
			int availableNum = intVal(itemInfo.get("gift_num"), 0);
			if (availableNum <= 0) {
				if (giftStoreAdjustments != null) {
					giftStoreAdjustments.add(
							buildGiftStoreAdjustment(
									itemInfo,
									activityData,
									receiptType,
									companyId,
									originalGiftNum,
									0,
									reservedByItemId));
				}
				continue;
			}
			if (!kept) {
				continue;
			}
			Map<String, Object> giftLine = buildGiftOrderLine(activityData, itemInfo, od, companyId);
			int giftNum = intVal(giftLine.get("num"), 0);
			if (giftNum > 0) {
				long lineFee = longVal(giftLine.get("item_fee"), 0L);
				totalDiscountFee += lineFee;
				giftLines.add(giftLine);
			}
		}
		return new GiftMergeResult(giftLines, totalDiscountFee);
	}

	private Map<String, Object> buildGiftStoreAdjustment(
			Map<String, Object> itemInfo,
			Map<String, Object> activityData,
			String receiptType,
			long companyId,
			int originalGiftNum,
			int availableNum,
			Map<Long, Integer> reservedByItemId) {
		String itemName = firstNonBlank(itemInfo, "itemName", "item_name");
		long itemId = longVal(itemInfo.get("item_id"), 0L);
		long supplierId = longVal(itemInfo.get("supplier_id"), 0L);
		String otherReceipt = otherReceiptType(receiptType);
		int otherRaw =
				StringUtils.hasText(otherReceipt)
						? itemAvailableStoreResolver.resolveAvailable(companyId, otherReceipt, itemInfo)
						: 0;
		int reserved =
				itemId > 0L && reservedByItemId != null
						? Math.max(0, reservedByItemId.getOrDefault(itemId, 0))
						: 0;
		// 另一配送可用需扣除主品已占用（reserved 含本单主品；当前路径未分到赠品时 reserved 即主品占用）
		int otherRemain = Math.max(0, otherRaw - reserved);
		boolean canSwitch = availableNum <= 0 && otherRemain > 0 && supplierId > 0L;
		Map<String, Object> tip = new LinkedHashMap<>();
		tip.put("item_id", itemId);
		tip.put("item_name", itemName);
		tip.put("activity_id", longVal(activityData.get("activity_id"), 0L));
		tip.put("original_num", originalGiftNum);
		tip.put("available_num", availableNum);
		tip.put("receipt_type", receiptType == null ? "" : receiptType);
		tip.put("other_receipt_available", otherRemain);
		tip.put("can_switch_receipt", canSwitch);
		if (canSwitch) {
			tip.put("suggested_receipt_type", otherReceipt);
		}
		tip.put("message", buildGiftStoreAdjustMessage(itemName, originalGiftNum, canSwitch));
		return tip;
	}

	private static String buildGiftStoreAdjustTip(List<Map<String, Object>> adjustments) {
		if (adjustments == null || adjustments.isEmpty()) {
			return "";
		}
		boolean canSwitch =
				adjustments.stream().anyMatch(a -> Boolean.TRUE.equals(a.get("can_switch_receipt")));
		StringBuilder lines = new StringBuilder();
		for (Map<String, Object> item : adjustments) {
			if (item == null) {
				continue;
			}
			String name = stringVal(item.get("item_name"));
			if (!StringUtils.hasText(name)) {
				name = "赠品" + stringVal(item.get("item_id"));
			}
			int from = intVal(item.get("original_num"), 0);
			if (lines.length() > 0) {
				lines.append('\n');
			}
			lines.append("赠品「").append(name).append("」应赠").append(from).append("件，当前配送方式无货");
		}
		String header = "本单赠品在当前配送方式下库存不足：";
		String footer =
				canSwitch
						? "该赠品在其他配送方式下仍有库存，可切换配送方式后领取，或继续结算（本单不送该赠品）。"
						: "继续结算将不送无货赠品。";
		return header + "\n" + lines + "\n" + footer;
	}

	private static String buildGiftStoreAdjustMessage(
			String itemName, int originalGiftNum, boolean canSwitch) {
		String name = StringUtils.hasText(itemName) ? itemName : "赠品";
		String base = "赠品「" + name + "」应赠" + originalGiftNum + "件，当前配送方式无货";
		return canSwitch ? base + "，其他配送方式仍有库存" : base;
	}

	private static String otherReceiptType(String receiptType) {
		if ("logistics".equals(receiptType)) {
			return "ziti";
		}
		if ("ziti".equals(receiptType) || "merchant".equals(receiptType) || "dada".equals(receiptType)) {
			return "logistics";
		}
		return "";
	}

	private static String resolveReceiptType(Map<String, Object> od, Map<String, Object> params) {
		String fromOd = stringVal(od != null ? od.get("receipt_type") : null);
		if (StringUtils.hasText(fromOd)) {
			return fromOd.trim();
		}
		String fromParams = stringVal(params != null ? params.get("receipt_type") : null);
		return fromParams == null ? "" : fromParams.trim();
	}

	private Map<String, Object> buildGiftOrderLine(
			Map<String, Object> activityData,
			Map<String, Object> itemInfo,
			Map<String, Object> od,
			long companyId) {
		long price = longVal(itemInfo.get("price"), 0L);
		int giftNum = intVal(itemInfo.get("gift_num"), 0);
		long lineFee = price * giftNum;
		Map<String, Object> line = new LinkedHashMap<>();
		line.put("order_id", od.get("order_id"));
		line.put("item_id", longVal(itemInfo.get("item_id"), 0L));
		line.put("goods_id", longVal(itemInfo.get("goods_id"), longVal(itemInfo.get("item_id"), 0L)));
		line.put("item_bn", firstNonBlank(itemInfo, "itemBn", "item_bn"));
		line.put("goods_bn", stringVal(itemInfo.get("goods_bn")));
		line.put("supplier_id", stringVal(itemInfo.get("supplier_id")));
		line.put("company_id", od.get("company_id"));
		line.put("user_id", od.get("user_id"));
		line.put("item_name", firstNonBlank(itemInfo, "itemName", "item_name"));
		line.put("templates_id", longVal(itemInfo.get("templates_id"), 0L));
		line.put("pic", firstPic(itemInfo.get("pics")));
		line.put("num", giftNum);
		line.put("price", (int) Math.min(price, Integer.MAX_VALUE));
		line.put("activity_price", 0);
		line.put("discount_fee", (int) Math.min(lineFee, Integer.MAX_VALUE));
		line.put("discount_info", new ArrayList<>());
		line.put("item_fee", (int) Math.min(lineFee, Integer.MAX_VALUE));
		line.put("cost_fee", 0);
		line.put("item_unit", stringVal(itemInfo.get("item_unit")));
		line.put("total_fee", 0);
		line.put("rebate", 0);
		line.put("total_rebate", 0);
		line.put("distributor_id", longVal(od.get("distributor_id"), 0L));
		line.put("mobile", stringVal(od.get("mobile")));
		line.put("is_total_store", !Boolean.FALSE.equals(itemInfo.get("is_total_store")));
		line.put("shop_id", longVal(od.get("shop_id"), 0L));
		line.put("fee_rate", od.get("fee_rate"));
		line.put("fee_type", od.get("fee_type"));
		line.put("fee_symbol", od.get("fee_symbol"));
		line.put("order_item_type", "gift");
		line.put("is_gift", isGiftFlag(itemInfo.get("is_gift")));
		line.put("item_spec_desc", stringVal(itemInfo.get("item_spec_desc")));
		line.put("volume", multiplyNum(itemInfo.get("volume"), giftNum));
		line.put("weight", multiplyNum(itemInfo.get("weight"), giftNum));
		line.put("item_category_main", resolveItemCategoryMain(itemInfo, companyId));
		line.put("is_profit", itemInfo.get("is_profit") != null ? itemInfo.get("is_profit") : "0");
		line.put("cost_price", stringVal(itemInfo.get("cost_price")));
		line.put("market_price", stringVal(itemInfo.get("market_price")));
		line.put("activity_id", longVal(activityData.get("activity_id"), 0L));
		line.put("is_logistics", false);
		return line;
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

	private static Map<String, Object> buildGiftItemsPromotionRow(
			Map<String, Object> od, Map<String, Object> giftLine, Map<String, Object> activityData) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("company_id", od.get("company_id"));
		row.put("user_id", od.get("user_id"));
		row.put("shop_id", longVal(od.get("distributor_id"), 0L));
		row.put("item_id", giftLine.get("item_id"));
		row.put("item_name", giftLine.get("item_name"));
		row.put("item_type", "normal");
		row.put("order_type", "normal");
		row.put("activity_id", activityData.get("activity_id"));
		Map<String, Object> discountDesc = resolveDiscountDesc(activityData);
		row.put("activity_type", stringVal(discountDesc.get("type"), "full_gift"));
		row.put("activity_name", stringVal(discountDesc.get("info")));
		row.put("activity_tag", List.of());
		row.put("activity_desc", new LinkedHashMap<>(activityData));
		row.put("activity_rule", stringVal(discountDesc.get("rule")));
		return row;
	}

	@SuppressWarnings("unchecked")
	private static void appendOrderFullGiftDiscountInfo(
			Map<String, Object> od, Map<String, Object> activityData, long totalDiscountFee) {
		Map<String, Object> discountInfo = new LinkedHashMap<>(resolveDiscountDesc(activityData));
		discountInfo.put("discount_fee", totalDiscountFee);
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
	private static void appendLineDiscountInfo(
			Map<String, Object> item, Map<String, Object> discountDescTemplate, long discountFee) {
		Map<String, Object> itemDiscountInfo = new LinkedHashMap<>(discountDescTemplate);
		itemDiscountInfo.put("discount_fee", discountFee);
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
		list.add(itemDiscountInfo);
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

	private int marketingJoinCount(long companyId, long marketingId, long userId) {
		String key = "MarketingUserJoinNum:" + companyId + ":" + marketingId;
		Object hv = companysRedisTemplate.opsForHash().get(key, "user_" + userId);
		if (hv == null || !StringUtils.hasText(hv.toString())) {
			return 0;
		}
		try {
			return Integer.parseInt(hv.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long resolveMaxLimit(Map<String, Object> activityData) {
		Map<String, Object> discountDesc = resolveDiscountDesc(activityData);
		long maxLimit = longVal(discountDesc.get("max_limit"), Long.MAX_VALUE);
		if (maxLimit <= 0L) {
			return Long.MAX_VALUE;
		}
		return maxLimit;
	}

	private static List<Long> parseItemIdList(Object raw) {
		List<Long> ids = new ArrayList<>();
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

	private static long itemIdOf(Map<String, Object> item) {
		return longVal(item.get("item_id"), 0L);
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

	private static Object multiplyNum(Object unit, int num) {
		if (unit == null) {
			return 0;
		}
		if (unit instanceof Number n) {
			return n.doubleValue() * num;
		}
		try {
			return Double.parseDouble(unit.toString().trim()) * num;
		} catch (NumberFormatException e) {
			return 0;
		}
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

	private record GiftMergeResult(List<Map<String, Object>> giftLines, long totalDiscountFee) {}
}
