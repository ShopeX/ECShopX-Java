package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.common.operatorcart.dto.CouponCartItemScope;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 按卡券 {@code use_bound} 计算购物车中可参与优惠的商品及金额。
 *
 * <p>{@code rel_item_ids} 在指定商品/排除商品时为 itemId；指定分类、标签、品牌时分别为类目/标签/品牌 id。
 */
public final class UserDiscountCardMatchedAmount {

	private UserDiscountCardMatchedAmount() {}

	public static long feeFen(
			int useBound, Object relItemIds, Map<Long, Long> itemFees, Map<Long, CouponCartItemScope> scopes) {
		if (itemFees == null || itemFees.isEmpty()) {
			return 0L;
		}
		long sum = 0L;
		for (Long itemId : itemIds(useBound, relItemIds, itemFees.keySet(), scopes)) {
			Long fee = itemFees.get(itemId);
			if (fee != null && fee > 0L) {
				sum += fee;
			}
		}
		return sum;
	}

	public static List<Long> itemIds(
			int useBound,
			Object relItemIds,
			Collection<Long> cartItemIds,
			Map<Long, CouponCartItemScope> scopes) {
		if (cartItemIds == null || cartItemIds.isEmpty()) {
			return List.of();
		}
		if (useBound <= 0 || isRelAll(relItemIds)) {
			List<Long> all = new ArrayList<>();
			for (Long id : cartItemIds) {
				if (id != null && id > 0L) {
					all.add(id);
				}
			}
			return all;
		}
		Set<Long> relIds = parseIds(relItemIds);
		Map<Long, CouponCartItemScope> scopeMap = scopes == null ? Map.of() : scopes;
		List<Long> out = new ArrayList<>();
		for (Long itemId : cartItemIds) {
			if (itemId == null || itemId <= 0L) {
				continue;
			}
			if (matches(useBound, itemId, relIds, scopeMap.get(itemId))) {
				out.add(itemId);
			}
		}
		return out;
	}

	public static int useBoundOf(Map<String, Object> card) {
		if (card == null) {
			return 0;
		}
		Object raw = card.get("use_bound");
		if (raw == null) {
			raw = card.get("useBound");
		}
		return parseInt(raw);
	}

	public static Object relItemIdsOf(Map<String, Object> card) {
		if (card == null) {
			return null;
		}
		if (card.containsKey("rel_item_ids")) {
			return card.get("rel_item_ids");
		}
		return card.get("relItemIds");
	}

	private static boolean matches(int useBound, long itemId, Set<Long> relIds, CouponCartItemScope scope) {
		return switch (useBound) {
			case 1 -> relIds.contains(itemId);
			case 2 -> scope != null && scope.categoryId() != null && relIds.contains(scope.categoryId());
			case 3 -> scope != null && intersects(scope.tagIds(), relIds);
			case 4 -> scope != null && scope.brandId() != null && relIds.contains(scope.brandId().longValue());
			case 5 -> !relIds.contains(itemId);
			default -> relIds.contains(itemId);
		};
	}

	private static boolean intersects(Set<Long> tags, Set<Long> relIds) {
		if (tags == null || tags.isEmpty() || relIds.isEmpty()) {
			return false;
		}
		for (Long tagId : tags) {
			if (tagId != null && relIds.contains(tagId)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isRelAll(Object raw) {
		if (raw instanceof List<?> list) {
			if (list.isEmpty()) {
				return false;
			}
			for (Object o : list) {
				if (o != null && "all".equalsIgnoreCase(o.toString().trim())) {
					return true;
				}
			}
			return false;
		}
		String s = raw == null ? "" : String.valueOf(raw).trim();
		return "all".equalsIgnoreCase(s);
	}

	static Set<Long> parseIds(Object raw) {
		Set<Long> out = new LinkedHashSet<>();
		if (raw instanceof List<?> list) {
			for (Object o : list) {
				long v = parseLong(o);
				if (v > 0L) {
					out.add(v);
				}
			}
			return out;
		}
		if (raw == null) {
			return out;
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty() || "all".equalsIgnoreCase(s)) {
			return out;
		}
		for (String part : s.split(",")) {
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
