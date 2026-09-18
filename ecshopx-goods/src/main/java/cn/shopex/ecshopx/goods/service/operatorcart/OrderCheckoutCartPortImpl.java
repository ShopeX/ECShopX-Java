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

package cn.shopex.ecshopx.goods.service.operatorcart;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.operatorcart.OperatorCartDataListFacade;
import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.order.normal.OrderCheckoutCartPort;
import cn.shopex.ecshopx.goods.service.cart.wxapp.CheckoutCartLogisticsSupplierStoreClampService;
import cn.shopex.ecshopx.goods.service.cart.wxapp.WxappH5CartListService;
import cn.shopex.ecshopx.goods.service.recommend.GoodsRecommendCheckoutAddService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderCheckoutCartPortImpl implements OrderCheckoutCartPort {

	private final OperatorCartDataListFacade operatorCartDataListFacade;

	private final WxappH5CartListService wxappH5CartListService;

	private final StringRedisTemplate stringRedisTemplate;

	private final ObjectMapper objectMapper;

	public OrderCheckoutCartPortImpl(
			OperatorCartDataListFacade operatorCartDataListFacade,
			WxappH5CartListService wxappH5CartListService,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate stringRedisTemplate,
			ObjectMapper objectMapper) {
		this.operatorCartDataListFacade = operatorCartDataListFacade;
		this.wxappH5CartListService = wxappH5CartListService;
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	@Override
	public void fillItemsFromOperatorCart(NormalOrderCreateParams p, HttpServletRequest request) {
		Map<String, Object> pr = p.getParams();
		long companyId = longVal(pr.get("company_id"), 0L);
		long operatorId = longVal(pr.get("operator_id"), 0L);
		long distributorId = longVal(pr.get("distributor_id"), 0L);
		long userId = longVal(pr.get("user_id"), 0L);
		Map<String, Object> cart =
				operatorCartDataListFacade.getCartDataList(companyId, operatorId, distributorId, userId, request, true);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> validCart = (List<Map<String, Object>>) cart.get("valid_cart");
		if (validCart == null || validCart.isEmpty()) {
			throw new ResourceException("购物车为空");
		}
		Map<String, Object> cartlist = validCart.get(0);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) cartlist.get("list");
		if (list == null || list.isEmpty()) {
			throw new ResourceException("购物车为空");
		}
		List<Map<String, Object>> items = new ArrayList<>();
		for (Map<String, Object> cartRow : list) {
			if (!Boolean.TRUE.equals(cartRow.get("is_checked"))) {
				continue;
			}
			long num = longVal(cartRow.get("num"), 0L);
			if (num <= 0L) {
				continue;
			}
			Map<String, Object> line = new LinkedHashMap<>();
			line.put("item_id", longVal(cartRow.get("item_id"), 0L));
			line.put("num", (int) Math.min(num, Integer.MAX_VALUE));
			line.put("activity_id", cartRow.get("activity_id"));
			line.put("activity_type", cartRow.get("activity_type") != null ? cartRow.get("activity_type") : "normal");
			line.put("items_id", cartRow.get("items_id") != null ? cartRow.get("items_id") : List.of());
			items.add(line);
		}
		if (items.isEmpty()) {
			throw new ResourceException("购物车为空");
		}
		pr.put("items", items);
		pr.put("_checkout_cart_meta", cartlist);
	}

	@Override
	public void fillItemsFromMemberDistributorCart(NormalOrderCreateParams p, HttpServletRequest request) {
		Map<String, Object> pr = p.getParams();
		long companyId = longVal(pr.get("company_id"), 0L);
		long userId = longVal(pr.get("user_id"), 0L);
		if (userId <= 0L) {
			throw new ResourceException("购物车为空");
		}
		long shopId = longVal(pr.get("distributor_id"), 0L);
		if (truthyFlag(pr.get("isNostores"))) {
			shopId = 0L;
		}
		String rawCartType = pr.get("cart_type") == null ? "" : pr.get("cart_type").toString().trim();
		List<Map<String, Object>> requestItemRows = extractMapList(pr.get("items"));
		String cartType;
		if (StringUtils.hasText(rawCartType)) {
			cartType = rawCartType;
		} else if (!requestItemRows.isEmpty()) {
			cartType = "offline";
		} else {
			cartType = "cart";
		}
		int iscrossborder = intVal(pr.get("iscrossborder"), 0);
		int isShopScreen = intVal(pr.get("isShopScreen"), 0);
		String userDevice = pr.get("user_device") == null ? null : pr.get("user_device").toString();
		List<Map<String, Object>> offlinePass = "offline".equals(cartType) ? requestItemRows : Collections.emptyList();
		Map<String, Object> cart = wxappH5CartListService.getCartList(
				companyId,
				userId,
				shopId,
				cartType,
				"distributor",
				true,
				iscrossborder,
				isShopScreen,
				userDevice,
				offlinePass,
				pr);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> validCart = (List<Map<String, Object>>) cart.get("valid_cart");
		if (validCart == null || validCart.isEmpty()) {
			if (applyStoreAdjustBlockIfNeeded(pr, cart, null)) {
				return;
			}
			throw new ResourceException("购物车商品为空");
		}
		Map<String, Object> cartlist = validCart.get(0);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) cartlist.get("list");
		if (list == null || list.isEmpty()) {
			if (applyStoreAdjustBlockIfNeeded(pr, cart, cartlist)) {
				return;
			}
			throw new ResourceException("购物车商品为空");
		}
		List<Map<String, Object>> items = new ArrayList<>();
		for (Map<String, Object> cartRow : list) {
			if (!Boolean.TRUE.equals(cartRow.get("is_checked"))) {
				continue;
			}
			long num = longVal(cartRow.get("num"), 0L);
			if (num <= 0L) {
				continue;
			}
			Map<String, Object> line = new LinkedHashMap<>();
			line.put("item_id", longVal(cartRow.get("item_id"), 0L));
			line.put("num", (int) Math.min(num, Integer.MAX_VALUE));
			line.put("activity_id", cartRow.get("activity_id"));
			line.put("activity_type", cartRow.get("activity_type") != null ? cartRow.get("activity_type") : "normal");
			line.put("items_id", cartRow.get("items_id") != null ? cartRow.get("items_id") : List.of());
			line.put("is_logistics", normalizeIsLogistics(cartRow.get("is_logistics")));
			if (cartRow.containsKey("is_medicine")) {
				line.put("is_medicine", cartRow.get("is_medicine"));
			}
			if (cartRow.containsKey("is_prescription")) {
				line.put("is_prescription", cartRow.get("is_prescription"));
			}
			items.add(line);
		}
		if (items.isEmpty()) {
			if (applyStoreAdjustBlockIfNeeded(pr, cart, cartlist)) {
				return;
			}
			throw new ResourceException("购物车商品为空");
		}
		if (Boolean.TRUE.equals(pr.get(GoodsRecommendCheckoutAddService.CHECKOUT_RECOMMEND_MERGE_REQUEST_ITEMS))) {
			mergeCheckoutRecommendRequestItems(items, requestItemRows);
		}
		pr.put("items", items);
		pr.put("_checkout_cart_meta", cartlist);
		attachStoreQuantityAdjustments(pr, cart);
	}

	static void mergeCheckoutRecommendRequestItems(
			List<Map<String, Object>> cartItems, List<Map<String, Object>> requestItemRows) {
		if (cartItems == null || requestItemRows == null || requestItemRows.isEmpty()) {
			return;
		}
		Set<Long> existingItemIds = new java.util.HashSet<>();
		for (Map<String, Object> line : cartItems) {
			long itemId = longVal(line.get("item_id"), 0L);
			if (itemId > 0L) {
				existingItemIds.add(itemId);
			}
		}
		for (Map<String, Object> req : requestItemRows) {
			long itemId = longVal(req.get("item_id"), 0L);
			if (itemId <= 0L || existingItemIds.contains(itemId)) {
				continue;
			}
			Map<String, Object> line = new LinkedHashMap<>();
			line.put("item_id", itemId);
			line.put("num", (int) Math.max(1L, Math.min(longVal(req.get("num"), 1L), Integer.MAX_VALUE)));
			line.put("activity_id", req.get("activity_id"));
			line.put("activity_type", req.get("activity_type") != null ? req.get("activity_type") : "normal");
			line.put("items_id", req.get("items_id") != null ? req.get("items_id") : List.of());
			line.put("is_logistics", normalizeIsLogistics(req.get("is_logistics")));
			if (req.containsKey("is_medicine")) {
				line.put("is_medicine", req.get("is_medicine"));
			}
			if (req.containsKey("is_prescription")) {
				line.put("is_prescription", req.get("is_prescription"));
			}
			cartItems.add(line);
			existingItemIds.add(itemId);
		}
	}

	@Override
	public void fillItemsFromGroupsFastBuyCart(NormalOrderCreateParams p, HttpServletRequest request) {
		Map<String, Object> pr = p.getParams();
		if (!extractMapList(pr.get("items")).isEmpty()) {
			return;
		}
		long companyId = longVal(pr.get("company_id"), 0L);
		long userId = longVal(pr.get("user_id"), 0L);
		if (companyId <= 0L || userId <= 0L) {
			return;
		}
		String key = "fastbuy:" + DigestUtils.sha1Hex(String.valueOf(companyId) + userId);
		String raw = stringRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			return;
		}
		try {
			Map<String, Object> fastBuy = objectMapper.readValue(raw, new TypeReference<>() {});
			long itemId = longVal(fastBuy.get("item_id"), 0L);
			long num = longVal(fastBuy.get("num"), 0L);
			if (itemId <= 0L || num <= 0L) {
				return;
			}
			Map<String, Object> line = new LinkedHashMap<>();
			line.put("item_id", itemId);
			line.put("num", (int) Math.min(num, Integer.MAX_VALUE));
			pr.put("items", List.of(line));
		} catch (Exception ignored) {
		}
	}

	@Override
	public void fillItemsFromMemberPointsmallCart(NormalOrderCreateParams p, HttpServletRequest request) {
		Map<String, Object> pr = p.getParams();
		long companyId = longVal(pr.get("company_id"), 0L);
		long userId = longVal(pr.get("user_id"), 0L);
		if (userId <= 0L) {
			throw new ResourceException("购物车为空");
		}
		String rawCartType = pr.get("cart_type") == null ? "" : pr.get("cart_type").toString().trim();
		List<Map<String, Object>> requestItemRows = extractMapList(pr.get("items"));
		String cartType;
		if (StringUtils.hasText(rawCartType)) {
			cartType = rawCartType;
		} else if (!requestItemRows.isEmpty()) {
			cartType = "offline";
		} else {
			cartType = "cart";
		}
		int iscrossborder = intVal(pr.get("iscrossborder"), 0);
		int isShopScreen = intVal(pr.get("isShopScreen"), 0);
		String userDevice = pr.get("user_device") == null ? null : pr.get("user_device").toString();
		List<Map<String, Object>> offlinePass = "offline".equals(cartType) ? requestItemRows : Collections.emptyList();
		Map<String, Object> cart =
				wxappH5CartListService.getCartList(
						companyId,
						userId,
						0L,
						cartType,
						"pointsmall",
						true,
						iscrossborder,
						isShopScreen,
						userDevice,
						offlinePass,
						pr);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> validCart = (List<Map<String, Object>>) cart.get("valid_cart");
		if (validCart == null || validCart.isEmpty()) {
			throw new ResourceException("购物车为空");
		}
		Map<String, Object> cartlist = validCart.get(0);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) cartlist.get("list");
		if (list == null || list.isEmpty()) {
			throw new ResourceException("购物车为空");
		}
		List<Map<String, Object>> items = new ArrayList<>();
		for (Map<String, Object> cartRow : list) {
			if (!Boolean.TRUE.equals(cartRow.get("is_checked"))) {
				continue;
			}
			long num = longVal(cartRow.get("num"), 0L);
			if (num <= 0L) {
				continue;
			}
			Map<String, Object> line = new LinkedHashMap<>();
			line.put("item_id", longVal(cartRow.get("item_id"), 0L));
			line.put("num", (int) Math.min(num, Integer.MAX_VALUE));
			line.put("activity_id", cartRow.get("activity_id"));
			line.put("activity_type", cartRow.get("activity_type") != null ? cartRow.get("activity_type") : "normal");
			line.put("items_id", cartRow.get("items_id") != null ? cartRow.get("items_id") : List.of());
			items.add(line);
		}
		if (items.isEmpty()) {
			throw new ResourceException("购物车为空");
		}
		pr.put("items", items);
		pr.put("_checkout_cart_meta", cartlist);
		attachStoreQuantityAdjustments(pr, cart);
	}

	/**
	 * 当前配送方式下勾选商品经库存夹紧后全部不可结算时：不抛空车，写入阻断标志供结算页弹窗并引导切换配送。
	 *
	 * @return true 表示已按阻断态写回 params，调用方应直接返回
	 */
	@SuppressWarnings("unchecked")
	private static boolean applyStoreAdjustBlockIfNeeded(
			Map<String, Object> pr, Map<String, Object> cart, Map<String, Object> cartlist) {
		if (pr == null || cart == null) {
			return false;
		}
		Object adjRaw = cart.get("store_quantity_adjustments");
		if (!(adjRaw instanceof List<?> adjList) || adjList.isEmpty()) {
			return false;
		}
		List<Map<String, Object>> adjustments = new ArrayList<>();
		for (Object el : adjList) {
			if (el instanceof Map<?, ?> m) {
				Map<String, Object> row = new LinkedHashMap<>();
				for (Map.Entry<?, ?> e : m.entrySet()) {
					row.put(String.valueOf(e.getKey()), e.getValue());
				}
				adjustments.add(row);
			}
		}
		if (adjustments.isEmpty()) {
			return false;
		}
		String receiptType = pr.get("receipt_type") == null ? "" : pr.get("receipt_type").toString().trim();
		String suggested = suggestAlternateReceiptType(receiptType);
		pr.put("items", List.of());
		pr.put("store_adjust_block_checkout", Boolean.TRUE);
		pr.put("store_quantity_adjustments", adjustments);
		if (!suggested.isEmpty()) {
			pr.put("suggested_receipt_type", suggested);
		}
		pr.put(
				"store_quantity_adjust_tip",
				CheckoutCartLogisticsSupplierStoreClampService.buildStoreQuantityAdjustTip(
						adjustments, true, suggested));
		if (cartlist != null) {
			pr.put("_checkout_cart_meta", cartlist);
		}
		return true;
	}

	private static String suggestAlternateReceiptType(String current) {
		if ("logistics".equals(current)) {
			return "ziti";
		}
		if ("ziti".equals(current) || "merchant".equals(current)) {
			return "logistics";
		}
		return "";
	}

	private static void attachStoreQuantityAdjustments(Map<String, Object> pr, Map<String, Object> cart) {
		if (pr == null || cart == null) {
			return;
		}
		Object adj = cart.get("store_quantity_adjustments");
		if (adj instanceof List<?> list && !list.isEmpty()) {
			pr.put("store_quantity_adjustments", adj);
			List<Map<String, Object>> normalized = new ArrayList<>();
			for (Object el : list) {
				if (el instanceof Map<?, ?> m) {
					Map<String, Object> row = new LinkedHashMap<>();
					for (Map.Entry<?, ?> e : m.entrySet()) {
						row.put(String.valueOf(e.getKey()), e.getValue());
					}
					normalized.add(row);
				}
			}
			if (!normalized.isEmpty()) {
				pr.put(
						"store_quantity_adjust_tip",
						CheckoutCartLogisticsSupplierStoreClampService.buildStoreQuantityAdjustTip(
								normalized, false, ""));
			}
		}
	}

	private static boolean normalizeIsLogistics(Object v) {
		if (v == null) {
			return false;
		}
		if (Boolean.TRUE.equals(v)) {
			return true;
		}
		if (v instanceof String s && "true".equalsIgnoreCase(s.trim())) {
			return true;
		}
		return false;
	}

	private static boolean truthyFlag(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = v.toString().trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s);
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

	private static List<Map<String, Object>> extractMapList(Object raw) {
		if (!(raw instanceof List<?> lst)) {
			return Collections.emptyList();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object el : lst) {
			if (!(el instanceof Map<?, ?> m)) {
				continue;
			}
			Map<String, Object> row = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				row.put(String.valueOf(e.getKey()), e.getValue());
			}
			out.add(row);
		}
		return out;
	}

	private static long longVal(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
