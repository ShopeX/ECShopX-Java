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

package cn.shopex.ecshopx.goods.service.cart.wxapp;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.port.WxappCartAddPort;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappCartAddPortImpl implements WxappCartAddPort {

	private final WxappH5CartAddService h5CartAddService;
	private final WxappH5CartListService h5CartListService;
	private final WxappH5CartPlusBuyResetService plusBuyResetService;

	public WxappCartAddPortImpl(
			WxappH5CartAddService h5CartAddService,
			WxappH5CartListService h5CartListService,
			WxappH5CartPlusBuyResetService plusBuyResetService) {
		this.h5CartAddService = h5CartAddService;
		this.h5CartListService = h5CartListService;
		this.plusBuyResetService = plusBuyResetService;
	}

	@Override
	public Object addCartDirect(Map<String, Object> params) {
		return h5CartAddService.addCart(params);
	}

	@Override
	public Object addCart(Map<String, Object> params) {
		Object addResult = h5CartAddService.addCart(params);
		if (!CartResultTruthiness.isTruthy(addResult)) {
			return addResult;
		}
		long companyId = longVal(params.get("company_id"));
		long userId = longVal(params.get("user_id"));
		long shopIdFilter = parseShopIdFilter(params.get("shop_id"));
		String cartType = stringOrDefault(params.get("cart_type"), "cart");
		Object shopTypeRaw = params.get("shop_type");
		if (shopTypeRaw == null || !StringUtils.hasText(shopTypeRaw.toString().trim())) {
			throw new ResourceException("提交的购物车数据有误");
		}
		String shopType = shopTypeRaw.toString().trim();
		int iscrossborder = intVal(params.get("iscrossborder"), 0);
		int isShopScreen = intVal(params.get("isShopScreen"), 0);
		Map<String, Object> listResult =
				h5CartListService.getCartList(companyId, userId, shopIdFilter, cartType, shopType, false, iscrossborder, isShopScreen);
		plusBuyResetService.resetPlusBuyCart(companyId, userId, listResult);
		return listResult;
	}

	private static long parseShopIdFilter(Object raw) {
		if (raw == null) {
			return 0L;
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s) || "0".equals(s) || "undefined".equals(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String stringOrDefault(Object v, String def) {
		if (v == null) {
			return def;
		}
		String s = v.toString().trim();
		return StringUtils.hasText(s) ? s : def;
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

	private static int intVal(Object v, int def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
