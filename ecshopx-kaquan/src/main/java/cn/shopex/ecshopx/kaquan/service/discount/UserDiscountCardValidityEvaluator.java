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

package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.kaquan.service.discount.dto.UserDiscountNewGetCardListRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserDiscountCardValidityEvaluator {

	private final WxShopsListForUserDiscountService wxShopsListForUserDiscountService;

	public UserDiscountCardValidityEvaluator(WxShopsListForUserDiscountService wxShopsListForUserDiscountService) {
		this.wxShopsListForUserDiscountService = wxShopsListForUserDiscountService;
	}

	public void evaluate(long companyId, long userId, UserDiscountNewGetCardListRequest req, Map<Long, Map<String, Object>> items,
			List<Map<String, Object>> cards) {
		if (cards == null || cards.isEmpty()) {
			return;
		}
		long nowEpoch = System.currentTimeMillis() / 1000L;
		long distributorId = req.parseDistributorIdOrZero();
		long shopId = parseLong(req.getShopId());
		long orderAmountFen = parseLong(req.parseAmountLteOrNull());
		for (Map<String, Object> card : cards) {
			String tagClass = computeTagClass(card, nowEpoch);
			boolean valid = isCardCurrentlyValid(card, nowEpoch);
			String invalidDesc = initialInvalidDesc(tagClass);
			card.put("valid", valid);
			if (StringUtils.hasText(tagClass)) {
				card.put("tagClass", tagClass);
			}

			Object relItemRaw = first(card, "rel_item_ids", "relItemIds");
			boolean relAll = isRelAllItems(relItemRaw);
			List<Long> relItemIds = parseCsvIds(relItemRaw);
			List<Long> reqItemIds = new ArrayList<>(items.keySet());
			List<Long> amountItemIds;
			List<Long> itemList;
			boolean itemIfall = true;
			if (relAll) {
				amountItemIds = reqItemIds;
				itemList = List.of();
			} else if (!reqItemIds.isEmpty()) {
				Set<Long> reqSet = reqItemIds.stream().collect(Collectors.toSet());
				amountItemIds = relItemIds.stream().filter(reqSet::contains).toList();
				itemList = amountItemIds;
				itemIfall = itemList.isEmpty();
			} else {
				amountItemIds = relItemIds;
				itemList = List.of();
				itemIfall = relItemIds.isEmpty();
			}
			card.put("rel_item_ids", toRelItemIdTokens(relItemRaw));
			card.put("itemList", itemList);
			card.put("itemIfall", itemIfall);

			Object relShops = card.get("rel_shops_ids");
			Map<String, Object> poi = wxShopsListForUserDiscountService.listShopsPoi(companyId, relShops);
			Object shopListObj = poi.get("list");
			card.put("storeList", shopListObj instanceof List<?> list ? list : List.of());
			card.put("ifall", isAllShops(relShops));
			if (valid) {
				// Non-list rel_item_ids (tag/category/brand scope ids as CSV) count full cart fee
				long matchedAmount = (relItemRaw instanceof List<?> && !relAll)
						? countMatchedItemAmount(amountItemIds, false, items)
						: countMatchedItemAmount(reqItemIds, true, items);
				if (matchedAmount <= 0L) {
					valid = false;
					invalidDesc = "订单金额需有大于0元";
				}
				int leastCost = parseInt(first(card, "least_cost", "leastCost"));
				String cardType = stringValue(first(card, "card_type", "cardType"));
				if (valid && ("discount".equals(cardType) || "cash".equals(cardType)) && leastCost > 0
						&& matchedAmount < leastCost) {
					valid = false;
					invalidDesc = "订单金额需满" + fenToYuan(leastCost) + "元";
				}
				if (valid && orderAmountFen > 0L && reqItemIds.isEmpty() && leastCost > orderAmountFen) {
					valid = false;
					invalidDesc = "订单金额需满" + fenToYuan(leastCost) + "元";
				}
				if (valid && distributorId > 0L && !isDistributorApplicable(card, distributorId)) {
					valid = false;
					invalidDesc = "不适用于该店铺";
				}
				if (valid && shopId > 0L && isShopExcluded(card, shopId)) {
					valid = false;
					invalidDesc = "不适用于该门店";
				}
			}
			card.put("valid", valid);
			if (!valid && StringUtils.hasText(invalidDesc)) {
				card.put("invalid_desc", invalidDesc);
			}
			Map<String, Object> coupon = buildCoupon(card, valid);
			card.put("coupon", coupon);
		}
	}

	private static Map<String, Object> buildCoupon(Map<String, Object> row, boolean valid) {
		Map<String, Object> coupon = new LinkedHashMap<>();
		coupon.put("card_id", first(row, "card_id", "cardId"));
		coupon.put("title", first(row, "title"));
		coupon.put("code", first(row, "code"));
		String cardType = stringValue(first(row, "card_type", "cardType"));
		coupon.put("card_type", cardType);
		coupon.put("valid", valid);
		if ("cash".equals(cardType)) {
			coupon.put("least_cost", first(row, "least_cost", "leastCost"));
			coupon.put("reduce_cost", first(row, "reduce_cost", "reduceCost"));
		} else if ("discount".equals(cardType)) {
			coupon.put("discount", first(row, "discount"));
		}
		return coupon;
	}

	private static String computeTagClass(Map<String, Object> row, long nowEpoch) {
		int status = parseInt(first(row, "status"));
		if (status == 2) {
			return "used";
		}
		long begin = parseLong(first(row, "begin_date", "beginDate"));
		long end = parseLong(first(row, "end_date", "endDate"));
		if (begin > nowEpoch) {
			return "notstarted";
		}
		if (end > 0L && end <= nowEpoch) {
			return "overdue";
		}
		return "";
	}

	private static String initialInvalidDesc(String tagClass) {
		return switch (tagClass) {
			case "used" -> "已使用";
			case "notstarted" -> "未到使用时间";
			case "overdue" -> "已过期";
			default -> "";
		};
	}

	private static String fenToYuan(long fen) {
		return BigDecimal.valueOf(fen)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN)
				.stripTrailingZeros()
				.toPlainString();
	}

	private static long countMatchedItemAmount(List<Long> matchedItemIds, boolean relAll, Map<Long, Map<String, Object>> items) {
		long amount = 0L;
		if (relAll) {
			for (Map<String, Object> row : items.values()) {
				long fee = parseLong(first(row, "total_fee", "totalFee"));
				if (fee > 0L) {
					amount += fee;
				}
			}
			return amount;
		}
		for (Long itemId : matchedItemIds) {
			Map<String, Object> row = items.get(itemId);
			if (row == null) {
				continue;
			}
			long fee = parseLong(first(row, "total_fee", "totalFee"));
			if (fee > 0L) {
				amount += fee;
			}
		}
		return amount;
	}

	private static boolean isRelAllItems(Object raw) {
		if (raw instanceof List<?> list) {
			for (Object o : list) {
				if (o != null && "all".equalsIgnoreCase(o.toString().trim())) {
					return true;
				}
			}
			return false;
		}
		return "all".equalsIgnoreCase(stringValue(raw));
	}

	private static List<String> toRelItemIdTokens(Object raw) {
		if (raw instanceof List<?> list) {
			List<String> out = new ArrayList<>();
			for (Object o : list) {
				if (o != null) {
					String s = o.toString().trim();
					if (!s.isEmpty()) {
						out.add(s);
					}
				}
			}
			return out;
		}
		String s = stringValue(raw);
		if (!s.isEmpty() && !"all".equalsIgnoreCase(s)) {
			List<String> out = new ArrayList<>();
			for (String part : s.split(",")) {
				if (part != null && !part.isBlank()) {
					out.add(part.trim());
				}
			}
			return out.isEmpty() ? List.of("all") : out;
		}
		return List.of("all");
	}

	private static boolean isDistributorApplicable(Map<String, Object> row, long distributorId) {
		Object rel = first(row, "rel_distributor_ids", "relDistributorIds");
		if (rel == null) {
			return true;
		}
		String s = stringValue(rel);
		if (s.isEmpty() || "all".equalsIgnoreCase(s)) {
			return true;
		}
		String token = "," + distributorId + ",";
		return s.contains(token);
	}

	private static boolean isShopExcluded(Map<String, Object> row, long shopId) {
		Object rel = first(row, "rel_shops_ids", "relShopsIds");
		if (!(rel instanceof String s) || s.isBlank() || "all".equalsIgnoreCase(s.trim())) {
			return false;
		}
		String token = "," + shopId + ",";
		return s.contains(token);
	}

	private static boolean isCardCurrentlyValid(Map<String, Object> row, long nowEpoch) {
		int status = parseInt(first(row, "status"));
		long begin = parseLong(first(row, "begin_date", "beginDate"));
		long end = parseLong(first(row, "end_date", "endDate"));
		return (status == 1 || status == 4) && begin <= nowEpoch && end > nowEpoch;
	}

	private static boolean isAllShops(Object relShops) {
		if (relShops instanceof String s) {
			return "all".equalsIgnoreCase(s.trim());
		}
		return relShops == null;
	}

	private static List<Long> parseCsvIds(Object raw) {
		if (!(raw instanceof String s)) {
			return List.of();
		}
		String trimmed = s.trim();
		if (trimmed.isEmpty() || "all".equalsIgnoreCase(trimmed)) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (String part : trimmed.split(",")) {
			if (part == null || part.isBlank()) {
				continue;
			}
			long v = parseLong(part.trim());
			if (v > 0L) {
				out.add(v);
			}
		}
		return out;
	}

	private static Object first(Map<String, Object> row, String... keys) {
		for (String key : keys) {
			if (row.containsKey(key)) {
				return row.get(key);
			}
		}
		return null;
	}

	private static String stringValue(Object raw) {
		return raw == null ? "" : String.valueOf(raw).trim();
	}

	private static int parseInt(Object raw) {
		if (raw instanceof Number n) {
			return n.intValue();
		}
		if (raw == null) {
			return 0;
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long parseLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw == null) {
			return 0L;
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
