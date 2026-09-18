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

import cn.shopex.ecshopx.companys.service.operatorcart.OperatorCartAdminSubmitDataService;
import cn.shopex.ecshopx.kaquan.service.discount.dto.CartItemMoneyRow;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminUserCardListFacadeService {

	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

	private final OperatorCartAdminSubmitDataService operatorCartAdminSubmitDataService;
	private final UserDiscountNewCardListService userDiscountNewCardListService;
	private final UserDiscountCardValidCheckService userDiscountCardValidCheckService;

	public AdminUserCardListFacadeService(OperatorCartAdminSubmitDataService operatorCartAdminSubmitDataService,
			UserDiscountNewCardListService userDiscountNewCardListService,
			UserDiscountCardValidCheckService userDiscountCardValidCheckService) {
		this.operatorCartAdminSubmitDataService = operatorCartAdminSubmitDataService;
		this.userDiscountNewCardListService = userDiscountNewCardListService;
		this.userDiscountCardValidCheckService = userDiscountCardValidCheckService;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> buildList(long companyId, long operatorId, long userId, long distributorId, String code,
			String cardId, String amountRaw, boolean hasItemsParam, int pageNo, int pageSize) {
		return buildList(companyId, operatorId, userId, distributorId, code, cardId, amountRaw, hasItemsParam, pageNo,
				pageSize, null);
	}

	/**
	 * @param lineItemsInsteadOfOperatorCart when non-null, item-scope coupon checks use these lines and the operator
	 *        cart is not loaded (wxapp member checkout uses {@code orders_cart}, not {@code companys_operator_cart}).
	 *        Pass an empty map when there are no lines to avoid operator submit semantics.
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> buildList(long companyId, long operatorId, long userId, long distributorId, String code,
			String cardId, String amountRaw, boolean hasItemsParam, int pageNo, int pageSize,
			Map<Long, CartItemMoneyRow> lineItemsInsteadOfOperatorCart) {
		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("user_id", userId);
		filter.put("distributor_id", distributorId);
		filter.put("card_type", List.of("discount", "cash"));
		filter.put("use_platform", "mall");
		filter.put("status", List.of(1, 4));
		filter.put("begin_date|lte", (int) Instant.now().getEpochSecond());
		filter.put("end_date|gt", (int) Instant.now().getEpochSecond());
		if (code != null && !code.isBlank()) {
			filter.put("code", code.trim());
		}
		if (cardId != null && !cardId.isBlank()) {
			filter.put("card_id", cardId.trim());
		}
		if (amountRaw != null && !amountRaw.isBlank()) {
			try {
				filter.put("least_cost|lte", Integer.parseInt(amountRaw.trim()));
			} catch (NumberFormatException e) {
				filter.put("least_cost|lte", amountRaw.trim());
			}
		}
		Map<Long, CartItemMoneyRow> items = new LinkedHashMap<>();
		if (lineItemsInsteadOfOperatorCart != null) {
			items.putAll(lineItemsInsteadOfOperatorCart);
		} else {
			Map<String, Object> cartResult = operatorCartAdminSubmitDataService.getCartDataList(companyId, distributorId,
					operatorId, userId, true);
			Object validCartObj = cartResult.get("valid_cart");
			if (validCartObj instanceof List<?> validCart && !validCart.isEmpty()) {
				Object first = validCart.get(0);
				if (first instanceof Map<?, ?> g) {
					Object listObj = g.get("list");
					if (listObj instanceof List<?> lines) {
						for (Object lineObj : lines) {
							if (lineObj instanceof Map<?, ?> line) {
								long iid = toLong(line.get("item_id"));
								long num = toLong(line.get("num"));
								long fee = toLong(line.get("total_fee"));
								items.put(iid, new CartItemMoneyRow(num, fee));
							}
						}
					}
				}
			}
		}
		if (!items.isEmpty()) {
			filter.put("item_id", new ArrayList<>(items.keySet()));
		}
		Map<String, Object> cardLists = userDiscountNewCardListService.loadPage(filter, pageNo, pageSize);
		Object listObj = cardLists.get("list");
		List<?> list = listObj instanceof List<?> l ? l : List.of();
		if (list.isEmpty()) {
			return new HashMap<>(Map.of("list", List.of(), "count", 0));
		}
		long thresholdAmount = parseAmountThreshold(amountRaw);
		for (Object rowObj : list) {
			if (!(rowObj instanceof Map<?, ?> card)) {
				continue;
			}
			Map<String, Object> cardMap = (Map<String, Object>) card;
			cardMap.put("valid", true);
			cardMap.put("locked", false);
			int status = intVal(firstKey(cardMap, "status"));
			if (status == 4) {
				cardMap.put("locked", true);
			}
			int begin = intVal(firstKey(cardMap, "begin_date"));
			int end = intVal(firstKey(cardMap, "end_date"));
			if (begin > 0) {
				cardMap.put("begin_date", DATE_FMT.format(Instant.ofEpochSecond(begin)));
			}
			if (end > 0) {
				cardMap.put("end_date", DATE_FMT.format(Instant.ofEpochSecond(end)));
			}
			Map<String, Object> coupon = new LinkedHashMap<>();
			coupon.put("card_id", firstKey(cardMap, "card_id"));
			coupon.put("title", firstKey(cardMap, "title"));
			coupon.put("code", firstKey(cardMap, "code"));
			coupon.put("card_type", firstKey(cardMap, "card_type"));
			coupon.put("valid", true);
			String ct = str(firstKey(cardMap, "card_type"));
			if ("cash".equals(ct)) {
				coupon.put("least_cost", firstKey(cardMap, "least_cost"));
				coupon.put("reduce_cost", firstKey(cardMap, "reduce_cost"));
			} else if ("discount".equals(ct)) {
				coupon.put("discount", firstKey(cardMap, "discount"));
			}
			cardMap.put("coupon", coupon);
			if (thresholdAmount >= 0 && !hasItemsParam && amountRaw != null && !amountRaw.isBlank()) {
				int leastCost = intVal(firstKey(cardMap, "least_cost"));
				if (leastCost < thresholdAmount) {
					cardMap.put("valid", false);
					coupon.put("valid", false);
				}
			}
		}
		userDiscountCardValidCheckService.apply(companyId, cardLists, distributorId, items);
		return cardLists;
	}

	private static long parseAmountThreshold(String amountRaw) {
		if (amountRaw == null || amountRaw.isBlank()) {
			return -1L;
		}
		try {
			return Long.parseLong(amountRaw.trim());
		} catch (NumberFormatException e) {
			return -1L;
		}
	}

	private static Object firstKey(Map<String, Object> m, String key) {
		if (m.containsKey(key)) {
			return m.get(key);
		}
		for (Map.Entry<String, Object> e : m.entrySet()) {
			if (e.getKey() != null && e.getKey().equalsIgnoreCase(key)) {
				return e.getValue();
			}
		}
		return null;
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o instanceof String s && !s.isBlank()) {
			return Long.parseLong(s.trim());
		}
		return 0L;
	}

	private static int intVal(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		if (o == null) {
			return 0;
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}
}
