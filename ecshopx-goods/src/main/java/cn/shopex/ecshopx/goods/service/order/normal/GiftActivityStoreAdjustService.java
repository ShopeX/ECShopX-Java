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

import cn.shopex.ecshopx.companys.service.setting.GiftSettingRedisService;
import cn.shopex.ecshopx.goods.service.items.ItemAvailableStoreResolver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 满赠 gift_activity / 赠品行软库存调整：仅当 {@code check_gift_store=true} 时，
 * 按与普通商品一致的可用库存解析规则下调 {@code gift_num}；并扣除同 SKU 主品已占用数量。
 */
@Service
public class GiftActivityStoreAdjustService {

	/** 购物车主品行 / gift_activity 上：应送赠品但无货时的展示文案。 */
	public static final String GIFT_UNAVAILABLE_TIP_KEY = "gift_unavailable_tip";

	private static final String DEFAULT_GIFT_UNAVAILABLE_TIP = "赠品暂时无货";

	private final GiftSettingRedisService giftSettingRedisService;
	private final ItemAvailableStoreResolver itemAvailableStoreResolver;

	public GiftActivityStoreAdjustService(
			GiftSettingRedisService giftSettingRedisService,
			ItemAvailableStoreResolver itemAvailableStoreResolver) {
		this.giftSettingRedisService = giftSettingRedisService;
		this.itemAvailableStoreResolver = itemAvailableStoreResolver;
	}

	/**
	 * 购物车/结算：调整店铺下 {@code gift_activity} 列表（无主品占用上下文）。
	 */
	public List<Map<String, Object>> adjustGiftActivities(
			long companyId, String receiptType, List<Map<String, Object>> giftActivities) {
		return adjustGiftActivities(companyId, receiptType, giftActivities, null);
	}

	/**
	 * 购物车/结算：调整店铺下 {@code gift_activity} 列表。
	 * <ul>
	 *   <li>{@code check_gift_store=false}：原样返回</li>
	 *   <li>有 {@code receiptType}：按配送方式解析可用库存</li>
	 *   <li>无 {@code receiptType}（非结算列表）：取快递/非快递路径较大可用库存</li>
	 *   <li>可用库存再扣减 {@code cartLines} 中已勾选主品占用；多赠品依次累计占用</li>
	 * </ul>
	 */
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> adjustGiftActivities(
			long companyId,
			String receiptType,
			List<Map<String, Object>> giftActivities,
			List<Map<String, Object>> cartLines) {
		if (giftActivities == null || giftActivities.isEmpty()) {
			return giftActivities == null ? List.of() : giftActivities;
		}
		if (!checkGiftStoreEnabled(companyId)) {
			return giftActivities;
		}
		Map<Long, Integer> reservedByItemId = buildReservedFromCartLines(cartLines);
		List<Map<String, Object>> out = new ArrayList<>();
		for (Map<String, Object> activity : giftActivities) {
			if (activity == null) {
				continue;
			}
			Map<String, Object> copy = new LinkedHashMap<>(activity);
			Object giftsRaw = copy.get("gifts");
			List<Map<String, Object>> adjustedGifts = new ArrayList<>();
			List<String> removedGiftNames = new ArrayList<>();
			int sourceGiftCount = 0;
			if (giftsRaw instanceof List<?> giftList) {
				for (Object g : giftList) {
					if (!(g instanceof Map<?, ?> gm)) {
						continue;
					}
					sourceGiftCount++;
					Map<String, Object> gift = new LinkedHashMap<>((Map<String, Object>) gm);
					int originalGiftNum = intVal(gift.get("gift_num"), 0);
					if (!gift.containsKey("original_gift_num")) {
						gift.put("original_gift_num", originalGiftNum);
					}
					boolean kept = applyClampToGift(companyId, receiptType, gift, reservedByItemId);
					// 保留 gift_num=0 行，供结算页组装赠品库存提示；前端展示仍按 gift_num>0 过滤
					adjustedGifts.add(gift);
					if (!kept) {
						String name = stringVal(gift.get("item_name"));
						if (!StringUtils.hasText(name)) {
							name = stringVal(gift.get("itemName"));
						}
						if (StringUtils.hasText(name)) {
							removedGiftNames.add(name);
						}
					}
				}
			}
			copy.put("gifts", adjustedGifts);
			boolean allUnavailable =
					sourceGiftCount > 0
							&& adjustedGifts.stream().noneMatch(g -> intVal(g.get("gift_num"), 0) > 0);
			if (allUnavailable) {
				copy.put(GIFT_UNAVAILABLE_TIP_KEY, buildUnavailableTip(removedGiftNames));
			} else {
				copy.remove(GIFT_UNAVAILABLE_TIP_KEY);
			}
			out.add(copy);
		}
		return out;
	}

	/**
	 * 下单 format：就地调整单个赠品 map 的 gift_num/store；返回是否仍保留（gift_num&gt;0）。
	 * 开关关闭时不改数量并返回 true（保留原 gift_num，即使为 0 也由调用方决定）。
	 */
	public boolean adjustSingleGift(long companyId, String receiptType, Map<String, Object> gift) {
		return adjustSingleGift(companyId, receiptType, gift, null);
	}

	/**
	 * 下单 format：就地调整；{@code reservedByItemId} 为可变占用表（主品占用 + 已分配赠品）。
	 */
	public boolean adjustSingleGift(
			long companyId,
			String receiptType,
			Map<String, Object> gift,
			Map<Long, Integer> reservedByItemId) {
		if (gift == null) {
			return false;
		}
		if (!checkGiftStoreEnabled(companyId)) {
			return intVal(gift.get("gift_num"), 0) > 0;
		}
		if (!StringUtils.hasText(receiptType)) {
			return intVal(gift.get("gift_num"), 0) > 0;
		}
		Map<Long, Integer> reserved =
				reservedByItemId != null ? reservedByItemId : new HashMap<>();
		return applyClampToGift(companyId, receiptType, gift, reserved);
	}

	/**
	 * 从购物车行汇总已勾选主品占用（排除赠品行）。
	 */
	public static Map<Long, Integer> buildReservedFromCartLines(List<Map<String, Object>> cartLines) {
		Map<Long, Integer> reserved = new HashMap<>();
		if (cartLines == null || cartLines.isEmpty()) {
			return reserved;
		}
		for (Map<String, Object> line : cartLines) {
			if (line == null || !isChecked(line.get("is_checked"))) {
				continue;
			}
			if (isGiftLine(line)) {
				continue;
			}
			long itemId = longVal(line.get("item_id"), 0L);
			int num = intVal(line.get("num"), 0);
			if (itemId <= 0L || num <= 0) {
				continue;
			}
			reserved.merge(itemId, num, Integer::sum);
		}
		return reserved;
	}

	/**
	 * 从订单行汇总非赠品占用。
	 */
	public static Map<Long, Integer> buildReservedFromOrderItems(List<Map<String, Object>> orderItems) {
		Map<Long, Integer> reserved = new HashMap<>();
		if (orderItems == null || orderItems.isEmpty()) {
			return reserved;
		}
		for (Map<String, Object> line : orderItems) {
			if (line == null || isGiftLine(line)) {
				continue;
			}
			long itemId = longVal(line.get("item_id"), 0L);
			int num = intVal(line.get("num"), 0);
			if (itemId <= 0L || num <= 0) {
				continue;
			}
			reserved.merge(itemId, num, Integer::sum);
		}
		return reserved;
	}

	private boolean applyClampToGift(
			long companyId,
			String receiptType,
			Map<String, Object> gift,
			Map<Long, Integer> reservedByItemId) {
		int giftNum = intVal(gift.get("gift_num"), 0);
		if (!gift.containsKey("original_gift_num")) {
			gift.put("original_gift_num", giftNum);
		}
		int available =
				StringUtils.hasText(receiptType)
						? itemAvailableStoreResolver.resolveAvailable(companyId, receiptType, gift)
						: itemAvailableStoreResolver.resolveAvailableForCartDisplay(companyId, gift);
		long itemId = longVal(gift.get("item_id"), 0L);
		int reserved = itemId > 0L ? Math.max(0, reservedByItemId.getOrDefault(itemId, 0)) : 0;
		int remain = Math.max(0, available - reserved);
		int clamped = Math.min(giftNum, remain);
		if (clamped < 0) {
			clamped = 0;
		}
		gift.put("gift_num", clamped);
		gift.put("store", remain);
		if (clamped > 0 && itemId > 0L) {
			reservedByItemId.merge(itemId, clamped, Integer::sum);
		}
		return clamped > 0;
	}

	private static boolean isGiftLine(Map<String, Object> line) {
		String orderItemType = stringVal(line.get("order_item_type"));
		if ("gift".equalsIgnoreCase(orderItemType)) {
			return true;
		}
		if (Boolean.TRUE.equals(line.get("is_gift"))) {
			return true;
		}
		String goodType = stringVal(line.get("goodType"));
		return "gift".equalsIgnoreCase(goodType);
	}

	private static boolean isChecked(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = raw.toString().trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}

	private static String buildUnavailableTip(List<String> giftNames) {
		if (giftNames == null || giftNames.isEmpty()) {
			return DEFAULT_GIFT_UNAVAILABLE_TIP;
		}
		if (giftNames.size() == 1) {
			return "赠品「" + giftNames.get(0) + "」暂时无货";
		}
		return "赠品「" + String.join("、", giftNames) + "」暂时无货";
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}

	private boolean checkGiftStoreEnabled(long companyId) {
		Map<String, Object> setting = giftSettingRedisService.getGiftSetting(companyId);
		return truthy(setting != null ? setting.get("check_gift_store") : null);
	}

	private static boolean truthy(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = v.toString().trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s);
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
}
