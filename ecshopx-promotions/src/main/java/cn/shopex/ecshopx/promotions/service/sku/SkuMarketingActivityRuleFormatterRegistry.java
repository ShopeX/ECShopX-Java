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

package cn.shopex.ecshopx.promotions.service.sku;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SkuMarketingActivityRuleFormatterRegistry {

	public String formatRules(String marketingType, String conditionType, Object conditionValue) {
		if (!StringUtils.hasText(marketingType)) {
			return "";
		}
		String mt = marketingType.trim();
		String ct = conditionType == null ? "" : conditionType.trim();
		if (conditionValue instanceof List<?> list && !list.isEmpty()) {
			return switch (mt) {
				case "full_minus" -> formatFullMinusPhpRules(ct, list);
				case "full_discount" -> formatFullDiscountPhpRules(ct, list);
				case "full_gift" -> formatFullGiftPhpRules(ct, list);
				case "plus_price_buy" -> formatPlusPriceBuyPhpRules(ct, list);
				case "self_select" -> "";
				default -> "";
			};
		}
		Map<String, Object> cv = conditionValue instanceof Map<?, ?> m ? castStringObjectMap(m) : Map.of();
		return switch (mt) {
			case "full_minus" -> formatFullMinus(ct, cv);
			case "full_discount" -> formatFullDiscount(ct, cv);
			case "full_gift" -> formatFullGift(ct, cv);
			case "self_select" -> formatSelfSelect(ct, cv);
			case "plus_price_buy" -> formatPlusPriceBuy(ct, cv);
			default -> "";
		};
	}

	private static String formatFullMinusPhpRules(String conditionType, List<?> rulesArr) {
		StringBuilder sb = new StringBuilder();
		for (Object raw : rulesArr) {
			if (!(raw instanceof Map<?, ?> rule)) {
				continue;
			}
			Object full = rule.get("full");
			Object minus = rule.get("minus");
			if (full == null || minus == null) {
				continue;
			}
			if ("quantity".equalsIgnoreCase(conditionType)) {
				sb.append("购买满").append(full).append("件，减").append(minus).append("元;");
			} else if ("totalfee".equalsIgnoreCase(conditionType)) {
				sb.append("消费满").append(full).append("元，减").append(minus).append("元;");
			}
		}
		return sb.toString();
	}

	private static String formatFullDiscountPhpRules(String conditionType, List<?> rulesArr) {
		StringBuilder sb = new StringBuilder();
		for (Object raw : rulesArr) {
			if (!(raw instanceof Map<?, ?> rule)) {
				continue;
			}
			Object full = rule.get("full");
			Object discount = rule.get("discount");
			if (full == null || discount == null) {
				continue;
			}
			if ("quantity".equalsIgnoreCase(conditionType)) {
				sb.append("购买满").append(full).append("件，减").append(phpConcatScalar(discount)).append("%优惠;");
			} else if ("totalfee".equalsIgnoreCase(conditionType)) {
				sb.append("消费满").append(full).append("元，减").append(phpConcatScalar(discount)).append("%优惠;");
			}
		}
		return sb.toString();
	}

	private static String formatFullGiftPhpRules(String conditionType, List<?> rulesArr) {
		StringBuilder sb = new StringBuilder();
		for (Object raw : rulesArr) {
			if (!(raw instanceof Map<?, ?> rule)) {
				continue;
			}
			Object full = rule.get("full");
			if (full == null) {
				continue;
			}
			if ("quantity".equalsIgnoreCase(conditionType)) {
				sb.append("购买满").append(full).append("件，送赠品;");
			} else if ("totalfee".equalsIgnoreCase(conditionType)) {
				sb.append("消费满").append(full).append("元，送赠品;");
			}
		}
		return sb.toString();
	}

	private static String formatPlusPriceBuyPhpRules(String conditionType, List<?> rulesArr) {
		StringBuilder sb = new StringBuilder();
		for (Object raw : rulesArr) {
			if (!(raw instanceof Map<?, ?> rule)) {
				continue;
			}
			Object full = rule.get("full");
			Object price = rule.get("price");
			if (full == null || price == null) {
				continue;
			}
			if ("quantity".equalsIgnoreCase(conditionType)) {
				sb.append("购买满").append(full).append("件，加价").append(price).append("元换购商品");
			} else if ("totalfee".equalsIgnoreCase(conditionType)) {
				sb.append("消费满").append(full).append("元，加价").append(price).append("元换购商品");
			}
		}
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> castStringObjectMap(Map<?, ?> m) {
		Map<String, Object> out = new java.util.LinkedHashMap<>();
		for (Map.Entry<?, ?> e : m.entrySet()) {
			if (e.getKey() != null) {
				out.put(e.getKey().toString(), e.getValue());
			}
		}
		return out;
	}

	private static String formatFullMinus(String conditionType, Map<String, Object> conditionValue) {
		List<Map.Entry<Long, Long>> tiers = parseNumericTiers(conditionValue);
		if (tiers.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<Long, Long> e : tiers) {
			if (!sb.isEmpty()) {
				sb.append("；");
			}
			long threshold = e.getKey();
			long off = e.getValue();
			if ("quantity".equalsIgnoreCase(conditionType)) {
				sb.append("满").append(threshold).append("件减").append(formatMoneyYuan(off)).append("元");
			} else {
				sb.append("满").append(formatMoneyYuan(threshold)).append("元减").append(formatMoneyYuan(off)).append("元");
			}
		}
		return sb.toString();
	}

	private static String formatFullDiscount(String conditionType, Map<String, Object> conditionValue) {
		List<Map.Entry<Long, Long>> tiers = parseNumericTiers(conditionValue);
		if (tiers.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<Long, Long> e : tiers) {
			if (!sb.isEmpty()) {
				sb.append("；");
			}
			long threshold = e.getKey();
			long discountPermille = e.getValue();
			if ("quantity".equalsIgnoreCase(conditionType)) {
				sb.append("满").append(threshold).append("件打").append(formatDiscountLabel(discountPermille));
			} else {
				sb.append("满").append(formatMoneyYuan(threshold)).append("元打").append(formatDiscountLabel(discountPermille));
			}
		}
		return sb.toString();
	}

	private static String formatFullGift(String conditionType, Map<String, Object> conditionValue) {
		List<Map.Entry<Long, Long>> tiers = parseNumericTiers(conditionValue);
		if (tiers.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<Long, Long> e : tiers) {
			if (!sb.isEmpty()) {
				sb.append("；");
			}
			long threshold = e.getKey();
			if ("quantity".equalsIgnoreCase(conditionType)) {
				sb.append("满").append(threshold).append("件享赠品");
			} else {
				sb.append("满").append(formatMoneyYuan(threshold)).append("元享赠品");
			}
		}
		return sb.toString();
	}

	private static String formatSelfSelect(String conditionType, Map<String, Object> conditionValue) {
		return formatFullMinus(conditionType, conditionValue);
	}

	private static String formatPlusPriceBuy(String conditionType, Map<String, Object> conditionValue) {
		return formatFullMinus(conditionType, conditionValue);
	}

	private static List<Map.Entry<Long, Long>> parseNumericTiers(Map<String, Object> conditionValue) {
		List<Map.Entry<Long, Long>> out = new ArrayList<>();
		for (Map.Entry<String, Object> en : conditionValue.entrySet()) {
			Long k = parseLongLenient(en.getKey());
			Long v = parseLongLenient(en.getValue());
			if (k != null && v != null) {
				out.add(Map.entry(k, v));
			}
		}
		out.sort(Comparator.comparingLong(Map.Entry::getKey));
		return out;
	}

	private static String phpConcatScalar(Object raw) {
		if (raw instanceof Number n) {
			double d = n.doubleValue();
			if (d == Math.rint(d)) {
				return String.valueOf((long) d);
			}
		}
		return String.valueOf(raw);
	}

	private static Long parseLongLenient(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String formatMoneyYuan(long cents) {
		if (cents % 100L == 0L) {
			return String.valueOf(cents / 100L);
		}
		return String.format(java.util.Locale.ROOT, "%.2f", cents / 100.0);
	}

	private static String formatDiscountLabel(long permille) {
		if (permille <= 0L) {
			return "";
		}
		if (permille % 100L == 0L) {
			return (permille / 100L) + "折";
		}
		return String.format(java.util.Locale.ROOT, "%.1f折", permille / 1000.0 * 10.0);
	}
}
