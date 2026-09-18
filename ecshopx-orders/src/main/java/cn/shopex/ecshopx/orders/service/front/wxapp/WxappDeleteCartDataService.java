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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.orders.domain.Cart;
import cn.shopex.ecshopx.orders.mapper.CartMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WxappDeleteCartDataService {

	private static final Logger log = LoggerFactory.getLogger(WxappDeleteCartDataService.class);

	private final CartMapper cartMapper;

	public WxappDeleteCartDataService(CartMapper cartMapper) {
		this.cartMapper = cartMapper;
	}

	public boolean deleteCartData(
			long companyId, long authUserId, Map<String, Object> merged, Long shopIdFilterOrNull) {
		long buyUserId = longVal(merged.get("buy_user_id"));
		long effectiveUserId = (buyUserId != 0L) ? buyUserId : authUserId;

		LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(Cart::getCompanyId, companyId).eq(Cart::getUserId, effectiveUserId);
		if (shopIdFilterOrNull != null) {
			wrapper.eq(Cart::getShopId, shopIdFilterOrNull.longValue());
		}
		if (merged.containsKey("cart_id") && merged.get("cart_id") != null) {
			Object v = merged.get("cart_id");
			if (v instanceof Number n) {
				wrapper.eq(Cart::getCartId, n.longValue());
			} else if (v instanceof String s) {
				String t = s.trim();
				if (t.isEmpty()) {
					wrapper.apply("cart_id = {0}", "");
				} else {
					try {
						long parsed = Long.parseLong(t);
						wrapper.eq(Cart::getCartId, parsed);
					} catch (NumberFormatException e) {
						wrapper.apply("cart_id = {0}", t);
					}
				}
			} else {
				wrapper.apply("cart_id = {0}", String.valueOf(v).trim());
			}
		}

		List<Cart> list = cartMapper.selectList(wrapper);
		if (list == null || list.isEmpty()) {
			return true;
		}
		for (Cart c : list) {
			cartMapper.deleteById(c.getCartId());
		}
		return true;
	}

	public boolean deleteCartDataBat(long companyId, Map<String, Object> merged) {
		Object cartIdListRaw =
				merged.containsKey("cart_id_list") ? merged.get("cart_id_list") : Boolean.FALSE;
		String explodeInput = toCommaJoinedCartIdString(cartIdListRaw);
		String[] segments = explodeInput.split(",", -1);

		boolean userIdFilter = buyUserIdFilterActive(merged.get("buy_user_id"));
		Map<String, Object> logPayload = new LinkedHashMap<>();
		logPayload.put("companyId", companyId);
		logPayload.put("userIdFilter", userIdFilter);
		logPayload.put("cartIdListRaw", cartIdListRaw);
		logPayload.put("segments", Arrays.asList(segments));
		log.info(":cart:deleteCartDataBat:del: {}", logPayload);

		LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(Cart::getCompanyId, companyId);
		wrapper.nested(w -> {
			applyOneSegment(w, segments[0]);
			for (int i = 1; i < segments.length; i++) {
				w.or();
				applyOneSegment(w, segments[i]);
			}
		});
		if (userIdFilter) {
			wrapper.eq(Cart::getUserId, longVal(merged.get("buy_user_id")));
		}

		List<Cart> list = cartMapper.selectList(wrapper);
		if (list == null || list.isEmpty()) {
			return true;
		}
		for (Cart c : list) {
			cartMapper.deleteById(c.getCartId());
		}
		return true;
	}

	private static void applyOneSegment(LambdaQueryWrapper<Cart> w, String segment) {
		String t = segment == null ? "" : segment.trim();
		if (t.isEmpty()) {
			w.apply("cart_id = {0}", "");
		} else {
			try {
				long parsed = Long.parseLong(t);
				w.eq(Cart::getCartId, parsed);
			} catch (NumberFormatException e) {
				w.apply("cart_id = {0}", t);
			}
		}
	}

	private static boolean buyUserIdFilterActive(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof Boolean b) {
			return b;
		}
		if (o instanceof Number n) {
			return n.longValue() != 0L;
		}
		if (o instanceof String s) {
			String t = s.trim();
			if (t.isEmpty() || "0".equals(t)) {
				return false;
			}
			try {
				return Long.parseLong(t) != 0L;
			} catch (NumberFormatException e) {
				return true;
			}
		}
		return false;
	}

	private static String toCommaJoinedCartIdString(Object raw) {
		if (raw == null) {
			return "";
		}
		if (raw instanceof Boolean b) {
			return b ? "1" : "";
		}
		if (raw instanceof String s) {
			return s;
		}
		if (raw instanceof Number n) {
			return Long.toString(n.longValue());
		}
		if (raw instanceof Collection<?> c) {
			return joinCartIdListElements(c);
		}
		if (raw.getClass().isArray()) {
			return cartIdListFromArray(raw);
		}
		return Objects.toString(raw, "");
	}

	private static String joinCartIdListElements(Collection<?> c) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Object el : c) {
			if (!first) {
				sb.append(',');
			}
			first = false;
			sb.append(cartIdListElementToString(el));
		}
		return sb.toString();
	}

	private static String cartIdListFromArray(Object raw) {
		if (raw instanceof Object[] a) {
			return joinCartIdListElements(Arrays.asList(a));
		}
		if (raw instanceof int[] a) {
			return joinPrimitiveArray(a.length, i -> cartIdListElementToString(a[i]));
		}
		if (raw instanceof long[] a) {
			return joinPrimitiveArray(a.length, i -> cartIdListElementToString(a[i]));
		}
		if (raw instanceof short[] a) {
			return joinPrimitiveArray(a.length, i -> cartIdListElementToString(a[i]));
		}
		if (raw instanceof byte[] a) {
			return joinPrimitiveArray(a.length, i -> cartIdListElementToString(a[i]));
		}
		if (raw instanceof char[] a) {
			return joinPrimitiveArray(a.length, i -> cartIdListElementToString(a[i]));
		}
		if (raw instanceof float[] a) {
			return joinPrimitiveArray(a.length, i -> cartIdListElementToString(a[i]));
		}
		if (raw instanceof double[] a) {
			return joinPrimitiveArray(a.length, i -> cartIdListElementToString(a[i]));
		}
		if (raw instanceof boolean[] a) {
			return joinPrimitiveArray(a.length, i -> cartIdListElementToString(a[i]));
		}
		return Objects.toString(raw, "");
	}

	@FunctionalInterface
	private interface IntToString {
		String at(int i);
	}

	private static String joinPrimitiveArray(int len, IntToString getter) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < len; i++) {
			if (i > 0) {
				sb.append(',');
			}
			sb.append(getter.at(i));
		}
		return sb.toString();
	}

	private static String cartIdListElementToString(Object el) {
		if (el == null) {
			return "";
		}
		if (el instanceof Boolean b) {
			return b ? "1" : "";
		}
		if (el instanceof String s) {
			return s;
		}
		if (el instanceof Number n) {
			return Long.toString(n.longValue());
		}
		if (el instanceof Collection<?> || el.getClass().isArray()) {
			return Objects.toString(el, "");
		}
		return Objects.toString(el, "");
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
