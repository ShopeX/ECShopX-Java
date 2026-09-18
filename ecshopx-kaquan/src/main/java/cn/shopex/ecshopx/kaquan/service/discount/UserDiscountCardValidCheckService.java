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

import cn.shopex.ecshopx.kaquan.service.discount.dto.CartItemMoneyRow;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class UserDiscountCardValidCheckService {

	private static final int POINT_FEE_FEN = 0;

	@SuppressWarnings("unchecked")
	public void apply(long companyId, Map<String, Object> cardLists, long distributorId, Map<Long, CartItemMoneyRow> items) {
		if (items == null || items.isEmpty()) {
			return;
		}
		Object listObj = cardLists.get("list");
		if (!(listObj instanceof List<?> rawList)) {
			return;
		}
		List<Long> itemIds = new ArrayList<>(items.keySet());
		for (Object el : rawList) {
			if (!(el instanceof Map<?, ?> card)) {
				continue;
			}
			Map<String, Object> cardMap = (Map<String, Object>) card;
			Object relItem = cardMap.get("rel_item_ids");
			long amount = countItemAmount(itemIds, relItem, items);
			if (amount == 0) {
				setInvalid(cardMap);
				continue;
			}
			int leastCost = intVal(cardMap.get("least_cost"));
			String cardType = str(cardMap.get("card_type"));
			if ("discount".equals(cardType) && leastCost > 0 && leastCost > amount - POINT_FEE_FEN) {
				setInvalid(cardMap);
			} else if ("cash".equals(cardType) && leastCost > amount - POINT_FEE_FEN) {
				setInvalid(cardMap);
			}
			if (relItem instanceof String rs && !rs.isBlank() && !"all".equals(rs)) {
				String[] parts = rs.split(",");
				List<String> filtered = new ArrayList<>();
				for (String p : parts) {
					if (p != null && !p.isBlank()) {
						filtered.add(p.trim());
					}
				}
				cardMap.put("rel_item_ids", filtered);
			}
			Object ub = cardMap.get("use_bound");
			int useBound = ub instanceof Number n ? n.intValue() : 0;
			Object relAfter = cardMap.get("rel_item_ids");
			if (relAfter instanceof List<?> relList && useBound == 5) {
				Set<String> hash = new HashSet<>();
				for (Object x : relList) {
					if (x != null) {
						hash.add(String.valueOf(x));
					}
				}
				boolean valid = false;
				for (Long id : itemIds) {
					if (!hash.contains(String.valueOf(id))) {
						valid = true;
						break;
					}
				}
				if (!valid) {
					setInvalid(cardMap);
				}
			}
			Object rdist = cardMap.get("rel_distributor_ids");
			if (rdist instanceof List<?> dlist && distributorId > 0) {
				boolean contains = false;
				for (Object x : dlist) {
					if (x != null && String.valueOf(x).equals(String.valueOf(distributorId))) {
						contains = true;
						break;
					}
				}
				if (!contains) {
					setInvalid(cardMap);
				}
			}
		}
	}

	private static void setInvalid(Map<String, Object> cardMap) {
		cardMap.put("valid", false);
		Object coupon = cardMap.get("coupon");
		if (coupon instanceof Map<?, ?> cm) {
			((Map<String, Object>) cm).put("valid", false);
		}
	}

	private static long countItemAmount(List<Long> itemList, Object cardItem, Map<Long, CartItemMoneyRow> inputItem) {
		long amount = 0;
		if (cardItem instanceof List<?> arr) {
			for (Long itemId : itemList) {
				String sid = String.valueOf(itemId);
				for (Object x : arr) {
					if (x != null && sid.equals(String.valueOf(x))) {
						CartItemMoneyRow row = inputItem.get(itemId);
						if (row != null && row.getTotalFeeFen() > 0) {
							amount += row.getTotalFeeFen();
						}
						break;
					}
				}
			}
		} else {
			String s = cardItem == null ? "" : String.valueOf(cardItem).trim();
			if (s.isEmpty() || "all".equalsIgnoreCase(s)) {
				for (Long itemId : itemList) {
					CartItemMoneyRow row = inputItem.get(itemId);
					if (row != null && row.getTotalFeeFen() > 0) {
						amount += row.getTotalFeeFen();
					}
				}
				return amount;
			}
			for (Long itemId : itemList) {
				if (s.contains("," + itemId + ",")) {
					CartItemMoneyRow row = inputItem.get(itemId);
					if (row != null && row.getTotalFeeFen() > 0) {
						amount += row.getTotalFeeFen();
					}
				}
			}
		}
		return amount;
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
