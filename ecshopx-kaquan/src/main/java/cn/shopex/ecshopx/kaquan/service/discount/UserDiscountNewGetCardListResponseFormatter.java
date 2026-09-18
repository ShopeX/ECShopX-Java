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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class UserDiscountNewGetCardListResponseFormatter {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(SHANGHAI);

	public List<Map<String, Object>> formatList(List<Map<String, Object>> cards) {
		if (cards == null || cards.isEmpty()) {
			return List.of();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Map<String, Object> card : cards) {
			out.add(formatCard(card));
		}
		return out;
	}

	public Map<String, Object> formatCur(Map<String, Object> cur) {
		if (cur == null || cur.isEmpty()) {
			return new LinkedHashMap<>();
		}
		Map<String, Object> m = new LinkedHashMap<>();
		putStringIfPresent(m, "id", cur.get("id"));
		putStringIfPresent(m, "company_id", first(cur, "company_id", "companyId"));
		putIfPresent(m, "currency", cur.get("currency"));
		putIfPresent(m, "title", cur.get("title"));
		putIfPresent(m, "symbol", cur.get("symbol"));
		Object rate = cur.get("rate");
		if (rate != null) {
			m.put("rate", rate);
		}
		Object isDefault = cur.get("is_default");
		if (isDefault == null) {
			isDefault = cur.get("isDefault");
		}
		if (isDefault != null) {
			m.put("is_default", isDefault);
		}
		Object usePlatform = first(cur, "use_platform", "usePlatform");
		if (usePlatform != null) {
			m.put("use_platform", usePlatform);
		}
		return m;
	}

	public String formatTotalCount(long total) {
		return String.valueOf(total);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> formatCard(Map<String, Object> row) {
		Map<String, Object> m = new LinkedHashMap<>();
		putStringIfPresent(m, "id", first(row, "id"));
		putStringIfPresent(m, "user_id", first(row, "user_id", "userId"));
		putStringIfPresent(m, "company_id", first(row, "company_id", "companyId"));
		putStringIfPresent(m, "card_id", first(row, "card_id", "cardId"));
		putIfPresent(m, "code", first(row, "code"));
		putIfPresent(m, "source_type", resolveSourceType(row));
		putStringIfPresent(m, "status", first(row, "status"));
		putIfPresent(m, "card_type", stringValue(first(row, "card_type", "cardType")));
		putIfPresent(m, "use_platform", stringValue(first(row, "use_platform", "usePlatform")));
		putIfPresent(m, "begin_date", formatYmd(first(row, "begin_date", "beginDate")));
		putIfPresent(m, "end_date", formatYmd(first(row, "end_date", "endDate")));
		putStringIfPresent(m, "get_date", first(row, "get_date", "getDate"));
		putIfPresent(m, "title", stringValue(first(row, "title")));
		putIfPresent(m, "color", stringValue(first(row, "color")));
		putStringIfPresent(m, "discount", first(row, "discount"));
		putStringIfPresent(m, "least_cost", first(row, "least_cost", "leastCost"));
		putStringIfPresent(m, "reduce_cost", first(row, "reduce_cost", "reduceCost"));
		putIfPresent(m, "rel_shops_ids", stringValue(first(row, "rel_shops_ids", "relShopsIds")));
		m.put("rel_item_ids", formatRelItemIds(first(row, "rel_item_ids", "relItemIds")));
		putIfPresent(m, "rel_distributor_ids", stringValue(first(row, "rel_distributor_ids", "relDistributorIds")));
		putNullField(m, "consume_source", row, "consume_source", "consumeSource");
		putNullField(m, "get_outer_str", row, "get_outer_str", "getOuterStr");
		putNullField(m, "location_name", row, "location_name", "locationName");
		putNullField(m, "staff_open_id", row, "staff_open_id", "staffOpenId");
		putNullField(m, "verify_code", row, "verify_code", "verifyCode");
		putNullField(m, "remark_amount", row, "remark_amount", "remarkAmount");
		putNullField(m, "consume_outer_str", row, "consume_outer_str", "consumeOuterStr");
		putNullField(m, "trans_id", row, "trans_id", "transId");
		putNullField(m, "fee", row, "fee");
		putNullField(m, "original_fee", row, "original_fee", "originalFee");
		putNullField(m, "location_id", row, "location_id", "locationId");
		putIfPresent(m, "use_scenes", stringValue(first(row, "use_scenes", "useScenes")));
		putStringIfPresent(m, "most_cost", first(row, "most_cost", "mostCost"));
		putIfPresent(m, "use_condition", first(row, "use_condition", "useCondition"));
		putStringFlag(m, "is_give_by_friend", first(row, "is_give_by_friend", "isGiveByFriend"));
		putNullField(m, "old_code", row, "old_code", "oldCode");
		putNullField(m, "friend_open_id", row, "friend_open_id", "friendOpenId");
		putStringFlag(m, "is_return_back", first(row, "is_return_back", "isReturnBack"));
		putStringFlag(m, "is_chat_room", first(row, "is_chat_room", "isChatRoom"));
		putStringIfPresent(m, "salesperson_id", first(row, "salesperson_id", "salespersonId"));
		putIfPresent(m, "salesperson_code", stringValue(first(row, "salesperson_code", "salespersonCode")));
		putStringIfPresent(m, "use_limited", first(row, "use_limited", "useLimited"));
		putStringIfPresent(m, "remain_times", first(row, "remain_times", "remainTimes"));
		putStringIfPresent(m, "use_bound", first(row, "use_bound", "useBound"));
		putNullField(m, "rel_category_ids", row, "rel_category_ids", "relCategoryIds");
		putIfPresent(m, "apply_scope", stringValue(first(row, "apply_scope", "applyScope")));
		putStringIfPresent(m, "used_time", first(row, "used_time", "usedTime"));
		putStringIfPresent(m, "expired_time", first(row, "expired_time", "expiredTime"));
		putNullField(m, "activity_name", row, "activity_name", "activityName");
		putNullField(m, "dm_card_code", row, "dm_card_code", "dmCardCode");
		putIfPresent(m, "description", stringValue(first(row, "description")));
		putStringIfPresent(m, "source_id", resolveSourceId(row));
		m.put("valid", row.get("valid"));
		m.put("coupon", formatCoupon(row.get("coupon")));
		m.put("itemIfall", row.get("itemIfall"));
		m.put("itemList", formatItemList(row.get("itemList")));
		m.put("storeList", row.get("storeList") instanceof List<?> list ? list : List.of());
		m.put("ifall", row.get("ifall"));
		String tagClass = stringValue(row.get("tagClass"));
		if (StringUtils.hasText(tagClass)) {
			m.put("tagClass", tagClass);
		}
		String invalidDesc = stringValue(row.get("invalid_desc"));
		if (StringUtils.hasText(invalidDesc)) {
			m.put("invalid_desc", invalidDesc);
		}
		return m;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> formatCoupon(Object raw) {
		if (!(raw instanceof Map<?, ?> map)) {
			return new LinkedHashMap<>();
		}
		Map<String, Object> coupon = new LinkedHashMap<>();
		putStringIfPresent(coupon, "card_id", first((Map<String, Object>) map, "card_id", "cardId"));
		putIfPresent(coupon, "title", stringValue(first((Map<String, Object>) map, "title")));
		putIfPresent(coupon, "code", stringValue(first((Map<String, Object>) map, "code")));
		putIfPresent(coupon, "card_type", stringValue(first((Map<String, Object>) map, "card_type", "cardType")));
		coupon.put("valid", map.get("valid"));
		Object leastCost = first((Map<String, Object>) map, "least_cost", "leastCost");
		if (leastCost != null) {
			coupon.put("least_cost", String.valueOf(leastCost));
		}
		Object reduceCost = first((Map<String, Object>) map, "reduce_cost", "reduceCost");
		if (reduceCost != null) {
			coupon.put("reduce_cost", String.valueOf(reduceCost));
		}
		Object discount = first((Map<String, Object>) map, "discount");
		if (discount != null) {
			coupon.put("discount", String.valueOf(discount));
		}
		return coupon;
	}

	private static List<Object> formatItemList(Object raw) {
		if (!(raw instanceof List<?> list)) {
			return List.of();
		}
		return new ArrayList<>(list);
	}

	private static Object resolveSourceType(Map<String, Object> row) {
		Object template = first(row, "card_template_source_type", "cardTemplateSourceType");
		if (template != null) {
			return stringValue(template);
		}
		return stringValue(first(row, "source_type", "sourceType"));
	}

	private static Object resolveSourceId(Map<String, Object> row) {
		Object template = first(row, "card_template_source_id", "cardTemplateSourceId");
		if (template != null) {
			return String.valueOf(template);
		}
		return stringifyOrNull(first(row, "source_id", "sourceId"));
	}

	private static List<String> formatRelItemIds(Object raw) {
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
		if (!StringUtils.hasText(s) || "all".equalsIgnoreCase(s)) {
			return List.of("all");
		}
		List<String> out = new ArrayList<>();
		for (String part : s.split(",")) {
			if (StringUtils.hasText(part.trim())) {
				out.add(part.trim());
			}
		}
		return out.isEmpty() ? List.of("all") : out;
	}

	private static String formatYmd(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.matches("\\d{4}-\\d{2}-\\d{2}")) {
				return t;
			}
		}
		long epoch = parseLong(raw);
		if (epoch <= 0L) {
			return null;
		}
		return YMD.format(Instant.ofEpochSecond(epoch));
	}

	private static void putStringFlag(Map<String, Object> target, String key, Object raw) {
		if (raw == null) {
			target.put(key, "0");
			return;
		}
		if (raw instanceof Boolean b) {
			target.put(key, b ? "1" : "0");
			return;
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty() || "0".equals(s) || "false".equalsIgnoreCase(s)) {
			target.put(key, "0");
		} else {
			target.put(key, "1");
		}
	}

	private static void putNullField(Map<String, Object> target, String key, Map<String, Object> row, String... keys) {
		Object v = first(row, keys);
		target.put(key, v);
	}

	private static void putStringIfPresent(Map<String, Object> target, String key, Object raw) {
		String s = stringifyOrNull(raw);
		if (s != null) {
			target.put(key, s);
		}
	}

	private static String stringifyOrNull(Object raw) {
		if (raw == null) {
			return null;
		}
		return String.valueOf(raw);
	}

	private static void putIfPresent(Map<String, Object> target, String key, Object val) {
		if (val != null) {
			target.put(key, val);
		}
	}

	private static Object first(Map<String, Object> row, String... keys) {
		for (String key : keys) {
			if (row.containsKey(key) && row.get(key) != null) {
				return row.get(key);
			}
		}
		return null;
	}

	private static String stringValue(Object raw) {
		return raw == null ? "" : String.valueOf(raw).trim();
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
