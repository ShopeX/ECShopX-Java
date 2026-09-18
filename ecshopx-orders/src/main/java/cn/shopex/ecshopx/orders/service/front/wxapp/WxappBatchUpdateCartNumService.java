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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.orders.domain.Cart;
import cn.shopex.ecshopx.orders.mapper.CartMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappBatchUpdateCartNumService {

	private final CartMapper cartMapper;

	public WxappBatchUpdateCartNumService(CartMapper cartMapper) {
		this.cartMapper = cartMapper;
	}

	public List<Map<String, Object>> batchUpdateCartNum(
			long companyId, long userId, List<Map<String, Object>> cartItems) {
		List<Map<String, Object>> result = new ArrayList<>(cartItems.size());
		for (Map<String, Object> entry : cartItems) {
			validateCartItem(entry);
			long parsedCartId = parseCartId(entry.get("cart_id"));
			LambdaQueryWrapper<Cart> w =
					new LambdaQueryWrapper<Cart>()
							.eq(Cart::getCompanyId, companyId)
							.eq(Cart::getUserId, userId)
							.eq(Cart::getCartId, parsedCartId);
			applyOptionalItemIdCondition(w, entry);
			Cart entity = cartMapper.selectOne(w);
			if (entity == null) {
				throw new ResourceException("未查询到更新数据");
			}
			int v = cartNumToStrictInt(entry.get("num"));
			if (v < 0) {
				Cart refreshed = cartMapper.selectById(entity.getCartId());
				result.add(OrdersCartColumnNamesMapSupport.toColumnNamesMap(refreshed));
				continue;
			}
			entity.setNum(v);
			entity.setUpdated((int) (System.currentTimeMillis() / 1000L));
			int affected = cartMapper.updateById(entity);
			if (affected == 0) {
				throw new ResourceException("未查询到更新数据");
			}
			Cart refreshed = cartMapper.selectById(parsedCartId);
			Map<String, Object> row = OrdersCartColumnNamesMapSupport.toColumnNamesMap(refreshed);
			row.put("num", entry.get("num"));
			result.add(row);
		}
		return result;
	}

	private static void validateCartItem(Map<String, Object> entry) {
		if (!entry.containsKey("cart_id")
				|| entry.get("cart_id") == null
				|| (entry.get("cart_id") instanceof String s && !StringUtils.hasText(s))) {
			throw new BadRequestException("购物车id必填");
		}
		if (!entry.containsKey("num") || entry.get("num") == null) {
			throw new BadRequestException("购物车商品数量必填");
		}
		Object numRaw = entry.get("num");
		if (numRaw instanceof String ns && !StringUtils.hasText(ns.trim())) {
			throw new BadRequestException("购物车商品数量必填");
		}
	}

	private static long parseCartId(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException("购物车id必填");
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("购物车id必填");
		}
	}

	private static void applyOptionalItemIdCondition(LambdaQueryWrapper<Cart> w, Map<String, Object> entry) {
		if (!entry.containsKey("item_id") || entry.get("item_id") == null) {
			return;
		}
		Object raw = entry.get("item_id");
		if (raw instanceof Number n) {
			if (n.longValue() == 0L) {
				return;
			}
			w.eq(Cart::getItemId, n.longValue());
			return;
		}
		if (raw instanceof String str) {
			String t = str.trim();
			if (t.isEmpty() || "0".equals(t)) {
				return;
			}
			try {
				w.eq(Cart::getItemId, Long.parseLong(t));
			} catch (NumberFormatException e) {
				throw new BadRequestException("参数非法");
			}
			return;
		}
		if (raw instanceof Boolean b) {
			if (!b) {
				return;
			}
			w.eq(Cart::getItemId, 1L);
			return;
		}
		throw new BadRequestException("参数非法");
	}

	private static int cartNumToStrictInt(Object o) {
		if (o instanceof Integer || o instanceof Short || o instanceof Byte) {
			return ((Number) o).intValue();
		}
		if (o instanceof Long l) {
			long lv = l;
			if (lv > Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			}
			if (lv < Integer.MIN_VALUE) {
				return Integer.MIN_VALUE;
			}
			return (int) lv;
		}
		if (o instanceof Float || o instanceof Double) {
			return (int) ((Number) o).doubleValue();
		}
		if (o instanceof BigDecimal bd) {
			return bd.intValue();
		}
		if (o instanceof BigInteger bi) {
			long lv = bi.longValue();
			if (lv > Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			}
			if (lv < Integer.MIN_VALUE) {
				return Integer.MIN_VALUE;
			}
			return (int) lv;
		}
		if (o instanceof Boolean b) {
			return b ? 1 : 0;
		}
		if (o instanceof String s) {
			String t = s.trim();
			long L = LeadingNumberParser.parseAsLong(t);
			return (int) Math.min(Integer.MAX_VALUE, Math.max(Integer.MIN_VALUE, L));
		}
		String t = String.valueOf(o).trim();
		long L = LeadingNumberParser.parseAsLong(t);
		return (int) Math.min(Integer.MAX_VALUE, Math.max(Integer.MIN_VALUE, L));
	}
}
