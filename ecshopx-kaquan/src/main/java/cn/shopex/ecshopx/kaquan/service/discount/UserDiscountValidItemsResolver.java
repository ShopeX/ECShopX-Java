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

import cn.shopex.ecshopx.common.cart.WxappUserCartListForCouponCheckoutPort;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.operatorcart.OperatorCartAdminSubmitDataService;
import cn.shopex.ecshopx.kaquan.service.discount.dto.UserDiscountNewGetCardListRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class UserDiscountValidItemsResolver {

	private final WxappUserCartListForCouponCheckoutPort wxappUserCartListForCouponCheckoutPort;
	private final OperatorCartAdminSubmitDataService operatorCartAdminSubmitDataService;
	private final ObjectMapper objectMapper;

	public UserDiscountValidItemsResolver(WxappUserCartListForCouponCheckoutPort wxappUserCartListForCouponCheckoutPort,
			OperatorCartAdminSubmitDataService operatorCartAdminSubmitDataService,
			ObjectMapper objectMapper) {
		this.wxappUserCartListForCouponCheckoutPort = wxappUserCartListForCouponCheckoutPort;
		this.operatorCartAdminSubmitDataService = operatorCartAdminSubmitDataService;
		this.objectMapper = objectMapper;
	}

	@SuppressWarnings("unchecked")
	public Map<Long, Map<String, Object>> resolve(long companyId, long userId, UserDiscountNewGetCardListRequest req) {
		Map<Long, Map<String, Object>> payloadItems = fromItemsPayload(req.parseItemsOrEmpty(objectMapper));
		if (!payloadItems.isEmpty()) {
			return payloadItems;
		}
		if (req.isCheckoutRequired()) {
			Map<Long, Map<String, Object>> items;
			if ("cxd".equalsIgnoreCase(nullSafeTrim(req.getCartType())) && req.parseCxdidOrZero() > 0L) {
				items = resolveFromSalespersonCart(companyId, userId, req);
			} else {
				items = resolveFromCommonCart(companyId, userId, req);
			}
			if (items.isEmpty()) {
				throw new ResourceException("购物车为空");
			}
			return items;
		}
		Map<Long, Map<String, Object>> items = new LinkedHashMap<>();
		String itemId = nullSafeTrim(req.getItemId());
		long singleId = parseLongOrZero(itemId);
		if (singleId > 0L) {
			Map<String, Object> line = new LinkedHashMap<>();
			line.put("item_id", singleId);
			items.put(singleId, line);
		}
		return items;
	}

	private Map<Long, Map<String, Object>> resolveFromSalespersonCart(
			long companyId, long userId, UserDiscountNewGetCardListRequest req) {
		Map<String, Object> cart = operatorCartAdminSubmitDataService.getCartDataList(
				companyId,
				req.parseDistributorIdOrZero(),
				req.parseCxdidOrZero(),
				userId,
				true);
		return fromCartPayload(cart);
	}

	private Map<Long, Map<String, Object>> resolveFromCommonCart(
			long companyId, long userId, UserDiscountNewGetCardListRequest req) {
		String cartType = nullSafeTrim(req.getCartType());
		if (cartType.isEmpty()) {
			cartType = "cart";
		}
		long shopId = req.parseDistributorIdOrZero();
		Map<String, Object> cart = wxappUserCartListForCouponCheckoutPort.getCartList(
				companyId,
				userId,
				shopId,
				cartType,
				intQueryFlag(req.getIscrossborder()),
				intQueryFlag(req.getIsShopScreen()));
		return fromCartPayload(cart);
	}

	@SuppressWarnings("unchecked")
	private static Map<Long, Map<String, Object>> fromCartPayload(Map<String, Object> cart) {
		Map<Long, Map<String, Object>> out = new LinkedHashMap<>();
		Object validCart = cart.get("valid_cart");
		if (!(validCart instanceof List<?> groups)) {
			return out;
		}
		for (Object g : groups) {
			if (!(g instanceof Map<?, ?> group)) {
				continue;
			}
			Object linesObj = group.get("list");
			if (!(linesObj instanceof List<?> lines)) {
				continue;
			}
			for (Object lineObj : lines) {
				if (!(lineObj instanceof Map<?, ?> lineRaw)) {
					continue;
				}
				Map<String, Object> line = (Map<String, Object>) lineRaw;
				long itemId = parseLongOrZero(String.valueOf(line.get("item_id")));
				if (itemId <= 0L) {
					continue;
				}
				out.put(itemId, new LinkedHashMap<>(line));
			}
		}
		return out;
	}

	private static Map<Long, Map<String, Object>> fromItemsPayload(List<Map<String, Object>> lines) {
		Map<Long, Map<String, Object>> out = new LinkedHashMap<>();
		for (Map<String, Object> line : lines) {
			if (line == null) {
				continue;
			}
			Object itemIdRaw = line.containsKey("item_id") ? line.get("item_id") : line.get("itemId");
			long itemId = 0L;
			if (itemIdRaw instanceof Number n) {
				itemId = n.longValue();
			} else if (itemIdRaw != null) {
				itemId = parseLongOrZero(String.valueOf(itemIdRaw));
			}
			if (itemId <= 0L) {
				continue;
			}
			Map<String, Object> normalized = new LinkedHashMap<>();
			normalized.put("item_id", itemId);
			normalized.put("num", line.getOrDefault("num", 1));
			normalized.put("total_fee", resolveLineTotalFeeFen(line));
			out.put(itemId, normalized);
		}
		return out;
	}

	private static long resolveLineTotalFeeFen(Map<String, Object> line) {
		long totalFee = parseDecimalToLong(line.getOrDefault("total_fee", line.get("totalFee")));
		if (totalFee > 0L) {
			return totalFee;
		}
		BigDecimal price = toDecimal(line.getOrDefault("price", line.get("item_fee")));
		if (price.compareTo(BigDecimal.ZERO) <= 0) {
			return 0L;
		}
		BigDecimal num = toDecimal(line.getOrDefault("num", 1));
		if (num.compareTo(BigDecimal.ZERO) <= 0) {
			num = BigDecimal.ONE;
		}
		return price.multiply(num).multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
	}

	private static long parseDecimalToLong(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return new BigDecimal(s).setScale(0, RoundingMode.DOWN).longValue();
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static BigDecimal toDecimal(Object raw) {
		if (raw == null) {
			return BigDecimal.ZERO;
		}
		if (raw instanceof BigDecimal bd) {
			return bd;
		}
		if (raw instanceof Number n) {
			return new BigDecimal(n.toString());
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(s);
		} catch (NumberFormatException e) {
			return BigDecimal.ZERO;
		}
	}

	private static String nullSafeTrim(String value) {
		return value == null ? "" : value.trim();
	}

	private static int intQueryFlag(String raw) {
		if (raw == null || raw.isBlank()) {
			return 0;
		}
		String s = raw.trim();
		if ("true".equalsIgnoreCase(s)) {
			return 1;
		}
		if ("false".equalsIgnoreCase(s)) {
			return 0;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long parseLongOrZero(String raw) {
		if (raw == null || raw.isBlank()) {
			return 0L;
		}
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
