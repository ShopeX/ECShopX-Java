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

import cn.shopex.ecshopx.crossborder.service.CrossBorderSetInfoQueryService;
import cn.shopex.ecshopx.orders.domain.Cart;
import cn.shopex.ecshopx.orders.mapper.CartMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappH5DistributorCartListService {

	private final CartMapper cartMapper;
	private final WxappH5CartListService wxappH5CartListService;
	private final CrossBorderSetInfoQueryService crossBorderSetInfoQueryService;
	private final WxappH5DistributorCartListPostProcessor wxappH5DistributorCartListPostProcessor;

	public WxappH5DistributorCartListService(
			CartMapper cartMapper,
			WxappH5CartListService wxappH5CartListService,
			CrossBorderSetInfoQueryService crossBorderSetInfoQueryService,
			WxappH5DistributorCartListPostProcessor wxappH5DistributorCartListPostProcessor) {
		this.cartMapper = cartMapper;
		this.wxappH5CartListService = wxappH5CartListService;
		this.crossBorderSetInfoQueryService = crossBorderSetInfoQueryService;
		this.wxappH5DistributorCartListPostProcessor = wxappH5DistributorCartListPostProcessor;
	}

	public Map<String, Object> getDistributorCartList(
			long companyId,
			long authUserId,
			long effectiveUserId,
			Map<String, Object> merged) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("valid_cart", new ArrayList<>());
		result.put("invalid_cart", new ArrayList<>());

		String cartType = normalizeCartType(merged.get("cart_type"));
		String shopType = normalizeShopType(merged.get("shop_type"));
		int iscrossborder = intParamOr(merged.get("iscrossborder"), 0, 0);
		int isShopScreen = intParamOr(merged.get("isShopScreen"), 0, 0);
		long isNostores = intParamOr(merged.get("isNostores"), 2, 0);
		long shopId = resolveShopIdFilter(merged);
		String userDevice = merged.get("user_device") == null ? null : merged.get("user_device").toString();

		if ("offline".equals(cartType)) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> items = (List<Map<String, Object>>) merged.get("items");
			if (items == null || items.isEmpty()) {
				return result;
			}
			Map<String, Object> inputData = buildInputData(effectiveUserId, merged);
			Map<String, Object> oneShop =
					wxappH5CartListService.getCartList(
							companyId,
							authUserId,
							shopId,
							"offline",
							shopType,
							false,
							iscrossborder,
							isShopScreen,
							userDevice,
							items,
							inputData);
			mergeShopResponse(result, oneShop);
			putCrossborderShow(companyId, result);
			wxappH5DistributorCartListPostProcessor.apply(result);
			return result;
		}

		LambdaQueryWrapper<Cart> w = buildCartListWrapper(companyId, effectiveUserId, shopType, shopId, isShopScreen, isNostores);
		long totalCount = cartMapper.selectCount(w);
		if (totalCount == 0L) {
			return result;
		}
		List<Cart> cartList = cartMapper.selectList(w);
		List<Long> shopIds;
		if (shopId > 0L) {
			shopIds = List.of(shopId);
		} else {
			shopIds =
					cartList.stream()
							.map(Cart::getShopId)
							.filter(Objects::nonNull)
							.distinct()
							.collect(Collectors.toList());
		}
		Map<String, Object> inputData = buildInputData(effectiveUserId, merged);
		for (Long sid : shopIds) {
			Map<String, Object> oneShop =
					wxappH5CartListService.getCartList(
							companyId,
							effectiveUserId,
							sid,
							cartType,
							shopType,
							false,
							iscrossborder,
							isShopScreen,
							userDevice,
							Collections.emptyList(),
							inputData);
			mergeShopResponse(result, oneShop);
		}
		putCrossborderShow(companyId, result);
		wxappH5DistributorCartListPostProcessor.apply(result);
		return result;
	}

	private static Map<String, Object> buildInputData(long effectiveUserId, Map<String, Object> merged) {
		Map<String, Object> inputData = new LinkedHashMap<>();
		inputData.put("user_id", effectiveUserId);
		long promoter = longVal(merged.get("promoter_user_id"));
		if (promoter != 0L) {
			inputData.put("promoter_user_id", promoter);
		}
		return inputData;
	}

	private static void mergeShopResponse(Map<String, Object> aggregate, Map<String, Object> oneShop) {
		stripPerShopAggregationKeys(oneShop);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> validAgg = (List<Map<String, Object>>) aggregate.get("valid_cart");
		Object vc = oneShop.get("valid_cart");
		if (vc instanceof List<?> blocks && !blocks.isEmpty() && blocks.get(0) instanceof Map<?, ?> firstBlock) {
			validAgg.add(copyShopBlock(firstBlock));
		}
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> invAgg = (List<Map<String, Object>>) aggregate.get("invalid_cart");
		Object inv = oneShop.get("invalid_cart");
		if (inv instanceof List<?> lines) {
			for (Object line : lines) {
				if (line instanceof Map<?, ?> lm) {
					Map<String, Object> copy = new LinkedHashMap<>();
					for (Map.Entry<?, ?> e : lm.entrySet()) {
						copy.put(String.valueOf(e.getKey()), e.getValue());
					}
					invAgg.add(copy);
				}
			}
		}
	}

	private static void stripPerShopAggregationKeys(Map<String, Object> oneShop) {
		oneShop.remove("cart_total_goods_fee");
		oneShop.remove("total_fee");
		oneShop.remove("discount_fee");
	}

	private static Map<String, Object> copyShopBlock(Map<?, ?> src) {
		Map<String, Object> b = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : src.entrySet()) {
			String k = String.valueOf(e.getKey());
			if ("list".equals(k) && e.getValue() instanceof List<?> lst) {
				List<Map<String, Object>> nl = new ArrayList<>();
				for (Object o : lst) {
					if (o instanceof Map<?, ?> m) {
						Map<String, Object> line = new LinkedHashMap<>();
						for (Map.Entry<?, ?> le : m.entrySet()) {
							line.put(String.valueOf(le.getKey()), le.getValue());
						}
						nl.add(line);
					}
				}
				b.put("list", nl);
			} else {
				b.put(k, e.getValue());
			}
		}
		return b;
	}

	private void putCrossborderShow(long companyId, Map<String, Object> result) {
		var opt = crossBorderSetInfoQueryService.findByCompanyId(companyId);
		if (opt.isEmpty() || opt.get().getCrossborderShow() == null) {
			result.put("crossborder_show", 0);
		} else {
			result.put("crossborder_show", opt.get().getCrossborderShow());
		}
	}

	private static LambdaQueryWrapper<Cart> buildCartListWrapper(
			long companyId,
			long effectiveUserId,
			String shopType,
			long shopId,
			int isShopScreen,
			long isNostores) {
		LambdaQueryWrapper<Cart> w = new LambdaQueryWrapper<>();
		w.eq(Cart::getCompanyId, companyId).eq(Cart::getUserId, effectiveUserId).eq(Cart::getShopType, shopType);
		boolean shopScreenOrSid = (isShopScreen == 1) || (shopId > 0L);
		if (shopScreenOrSid) {
			w.eq(Cart::getShopId, shopId);
		} else if (isNostores == 1L) {
			w.eq(Cart::getShopId, 0L);
		}
		return w;
	}

	private static long resolveShopIdFilter(Map<String, Object> merged) {
		Object primary = merged.get("shop_id");
		if (isUnsetShopIdPrimary(primary)) {
			primary = merged.get("distributor_id");
		}
		return normalizeShopIdFilterForCart(primary);
	}

	private static boolean isUnsetShopIdPrimary(Object o) {
		if (o == null) {
			return true;
		}
		if (o instanceof Boolean b) {
			return !b;
		}
		if (o instanceof Number n) {
			return n.longValue() == 0L;
		}
		if (o instanceof String s) {
			return s.trim().isEmpty();
		}
		return String.valueOf(o).trim().isEmpty();
	}

	private static long normalizeShopIdFilterForCart(Object raw) {
		if (raw == null) {
			return 0L;
		}
		String t = raw.toString().trim();
		if (!StringUtils.hasText(t) || "undefined".equals(t)) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String normalizeCartType(Object raw) {
		if (raw == null) {
			return "cart";
		}
		String t = raw.toString().trim();
		return t.isEmpty() ? "cart" : t;
	}

	private static String normalizeShopType(Object raw) {
		if (raw == null) {
			return "distributor";
		}
		String t = raw.toString().trim();
		return t.isEmpty() ? "distributor" : t;
	}

	private static int intParamOr(Object v, int whenMissing, int whenBad) {
		if (v == null) {
			return whenMissing;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return whenBad;
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
