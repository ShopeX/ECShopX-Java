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
import cn.shopex.ecshopx.orders.domain.Cart;
import cn.shopex.ecshopx.orders.mapper.CartMapper;
import cn.shopex.ecshopx.orders.port.WxappCartAddPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappUpdateCartNumService {

	private final CartMapper cartMapper;
	private final WxappCartAddPort wxappCartAddPort;

	public WxappUpdateCartNumService(CartMapper cartMapper, WxappCartAddPort wxappCartAddPort) {
		this.cartMapper = cartMapper;
		this.wxappCartAddPort = wxappCartAddPort;
	}

	public Object updateCartNum(long companyId, long authUserId, Map<String, Object> merged) {
		long cartId = requireTruthyCartId(merged.get("cart_id"));
		long buyUserId = longVal(merged.get("buy_user_id"));
		long effectiveUserId = (buyUserId != 0L) ? buyUserId : authUserId;

		LambdaQueryWrapper<Cart> w =
				new LambdaQueryWrapper<Cart>()
						.eq(Cart::getCartId, cartId)
						.eq(Cart::getCompanyId, companyId)
						.eq(Cart::getUserId, effectiveUserId);
		Cart row = cartMapper.selectOne(w);
		if (row == null) {
			throw new ResourceException("数据错误");
		}

		int num = resolveMergedNumForUpdate(merged);
		if (num <= 0) {
			cartMapper.deleteById(cartId);
			Map<String, Object> deleted = new LinkedHashMap<>();
			deleted.put("status", Boolean.TRUE);
			return deleted;
		}

		Map<String, Object> params = buildAddParamsFromRow(row, merged, cartId, companyId, effectiveUserId);
		Object raw = wxappCartAddPort.addCartDirect(params);

		long responseCartId = cartId;
		if (raw instanceof Map<?, ?> m) {
			if (m.containsKey("status") && m.get("status") instanceof Boolean) {
				return raw;
			}
			Object cid = m.get("cart_id");
			if (cid instanceof Number n) {
				responseCartId = n.longValue();
			}
		}
		Cart refreshed = cartMapper.selectById(responseCartId);
		if (refreshed == null) {
			throw new ResourceException("数据错误");
		}
		return OrdersCartColumnNamesMapSupport.toColumnNamesMapWxappNoPoint(refreshed);
	}

	/**
	 * 与 {@code WxappH5CartAddService.parseRequiredNumStrict} 及 {@link #buildAddParamsFromRow} 对 num 的约定一致：
	 * 无 {@code num} 键视为 0；有键则严格解析整数。
	 */
	private static int resolveMergedNumForUpdate(Map<String, Object> merged) {
		if (!merged.containsKey("num")) {
			return 0;
		}
		return parseRequiredNumStrict(merged.get("num"));
	}

	/** 与加购路径 {@code WxappH5CartAddService.parseRequiredNumStrict} 等价。 */
	private static int parseRequiredNumStrict(Object raw) {
		if (raw == null) {
			throw new BadRequestException("提交购物车数据有误");
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(raw.toString().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数非法");
		}
	}

	private static Map<String, Object> buildAddParamsFromRow(
			Cart row, Map<String, Object> merged, long cartId, long companyId, long effectiveUserId) {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", companyId);
		params.put("user_id", effectiveUserId);
		Long promoter = row.getPromoterUserId();
		params.put("promoter_user_id", promoter != null ? promoter : 0L);
		params.put("item_id", row.getItemId());
		params.put("shop_type", row.getShopType());
		params.put("shop_id", row.getShopId() != null ? row.getShopId() : 0L);
		String activityType = row.getActivityType();
		params.put("activity_type", activityType != null ? activityType : "");
		Long aid = row.getActivityId();
		params.put("activity_id", aid != null ? aid : 0L);
		String itemsId = row.getItemsId();
		if (itemsId != null) {
			params.put("items_id", itemsId);
		}
		String wxa = row.getWxaAppid();
		params.put("wxa_appid", wxa != null ? wxa : "");
		params.put("wxapp_appid", wxa != null ? wxa : "");
		params.put("cart_id", cartId);
		if (!merged.containsKey("num")) {
			params.put("num", 0);
		} else {
			params.put("num", merged.get("num"));
		}
		params.put("isAccumulate", Boolean.FALSE);
		if (!merged.containsKey("isShopScreen") || merged.get("isShopScreen") == null) {
			params.put("isShopScreen", 0);
		} else {
			params.put("isShopScreen", merged.get("isShopScreen"));
		}
		return params;
	}

	private static long requireTruthyCartId(Object raw) {
		if (raw == null) {
			throw new ResourceException("参数错误");
		}
		if (raw instanceof Boolean b && Boolean.FALSE.equals(b)) {
			throw new ResourceException("参数错误");
		}
		if (raw instanceof Number n) {
			if (n.doubleValue() == 0.0) {
				throw new ResourceException("参数错误");
			}
			long v = n.longValue();
			if (v <= 0L) {
				throw new ResourceException("参数错误");
			}
			return v;
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			throw new ResourceException("参数错误");
		}
		try {
			long v = Long.parseLong(s);
			if (v <= 0L) {
				throw new ResourceException("参数错误");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new ResourceException("参数错误");
		}
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
