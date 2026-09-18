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

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.distribution.service.HandleValidCartValidShopIdsService;
import cn.shopex.ecshopx.employeepurchase.domain.Cart;
import cn.shopex.ecshopx.employeepurchase.mapper.CartMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class EmployeePurchaseCartHandleValidCartService {

	private static final ObjectMapper OM = new ObjectMapper();

	private static final String[] PHP_CART_LINE_STRING_KEYS = {
		"cart_id",
		"company_id",
		"enterprise_id",
		"activity_id",
		"user_id",
		"item_id",
		"goods_id",
		"num",
		"price",
		"sale_price",
		"market_price",
		"type",
		"taxation_num",
		"taxstrategy_id",
		"origincountry_id",
		"is_medicine",
		"start_num"
	};

	private final CartMapper cartMapper;
	private final HandleValidCartValidShopIdsService handleValidCartValidShopIdsService;

	public EmployeePurchaseCartHandleValidCartService(
			CartMapper cartMapper, HandleValidCartValidShopIdsService handleValidCartValidShopIdsService) {
		this.cartMapper = cartMapper;
		this.handleValidCartValidShopIdsService = handleValidCartValidShopIdsService;
	}

	public Map<String, Object> handle(
			long companyId,
			long userId,
			List<Map<String, Object>> cartList,
			Map<Long, Map<String, Object>> itemListByItemId) {
		List<Long> distinctShopIds = new ArrayList<>();
		LinkedHashSet<Long> seen = new LinkedHashSet<>();
		for (Map<String, Object> row : cartList) {
			Long sid = toLongObject(row.get("shop_id"));
			if (sid != null && seen.add(sid)) {
				distinctShopIds.add(sid);
			}
		}
		List<Long> validShopIds =
				handleValidCartValidShopIdsService.resolveValidShopIdsAfterDistributorAndMerchantRules(
						companyId, distinctShopIds);

		List<Map<String, Object>> validCart = new ArrayList<>();
		List<Map<String, Object>> invalidCart = new ArrayList<>();

		for (Map<String, Object> cartdata : cartList) {
			long itemId = toLong(cartdata.get("item_id"));
			Map<String, Object> itemRow = itemListByItemId.get(itemId);
			if (itemRow == null) {
				invalidCart.add(cartdata);
				continue;
			}
			Long shopId = toLongObject(cartdata.get("shop_id"));
			if (shopId != null && shopId != 0L && !validShopIds.contains(shopId)) {
				invalidCart.add(cartdata);
				continue;
			}
			if (!singleItemOk(cartdata, itemRow)) {
				invalidCart.add(cartdata);
				continue;
			}

			long store = toLong(itemRow.get("store"));
			long cartNum = toLong(cartdata.get("num"));
			if (cartNum > store) {
				// 库存不足：仅内存按剩余库存参与计价，不回写购物车
				cartdata.put("num", store);
				cartNum = store;
			}
			cartdata.put("sale_price", itemRow.get("sale_price"));

			if (cartNum <= 0) {
				if (store <= 0) {
					invalidCart.add(cartdata);
					continue;
				}
				cartdata.put("num", 1L);
				long cid = toLong(cartdata.get("cart_id"));
				if (cid > 0L) {
					Cart entity = cartMapper.selectById(cid);
					if (entity != null) {
						entity.setNum(1L);
						cartMapper.updateById(entity);
					}
				}
				cartNum = 1L;
			}

			int type = toInt(itemRow.get("type"));
			if (type == 1) {
				continue;
			}

			String itemPic = firstPicForCart(itemRow);

			cartdata.put("is_last_price", false);
			cartdata.put("price", itemRow.get("price"));
			cartdata.put("discount_fee", 0);
			long price = toLong(itemRow.get("price"));
			cartdata.put("total_fee", price * cartNum);
			cartdata.put("store", store);
			cartdata.put("market_price", itemRow.get("market_price"));
			cartdata.put("brief", itemRow.get("brief"));
			cartdata.put("item_type", itemRow.get("item_type"));
			cartdata.put("approve_status", itemRow.get("approve_status"));
			Object itemName = itemRow.get("item_name");
			cartdata.put("item_name", itemName != null ? itemName : "");
			cartdata.put("pics", itemPic);
			cartdata.put("item_spec_desc", nzString(itemRow.get("item_spec_desc")));
			cartdata.put("parent_id", 0);
			long gid = toLong(itemRow.get("goods_id"));
			cartdata.put("goods_id", gid > 0L ? gid : itemId);
			cartdata.put("user_id", userId);
			cartdata.put("item_category", itemRow.get("item_category"));

			cartdata.put("type", itemRow.get("type") != null ? itemRow.get("type") : 0);
			cartdata.put(
					"crossborder_tax_rate",
					itemRow.get("crossborder_tax_rate") != null ? itemRow.get("crossborder_tax_rate") : "0");
			cartdata.put(
					"taxstrategy_id", itemRow.get("taxstrategy_id") != null ? itemRow.get("taxstrategy_id") : 0L);
			cartdata.put("taxation_num", itemRow.get("taxation_num") != null ? itemRow.get("taxation_num") : 0);
			Object oc = itemRow.get("origincountry_id");
			cartdata.put("origincountry_id", oc != null ? oc : "");

			int isMedicine = toInt(itemRow.get("is_medicine"));
			cartdata.put("is_medicine", isMedicine);
			if (isMedicine == 1) {
				@SuppressWarnings("unchecked")
				Map<String, Object> md = (Map<String, Object>) itemRow.get("medicine_data");
				Object presc = md != null ? md.get("is_prescription") : null;
				if (presc == null) {
					presc = itemRow.get("is_prescription");
				}
				cartdata.put("is_prescription", presc != null ? presc : 0);
			}
			cartdata.put("start_num", itemRow.get("start_num") != null ? itemRow.get("start_num") : 0);
			cartdata.put("origincountry_name", "");
			cartdata.put("origincountry_img_url", "");

			if (cartNum > 0) {
				Map<String, Object> line = new LinkedHashMap<>(cartdata);
				applyPhpCartLineFieldTypes(line);
				validCart.add(line);
			}
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("valid_cart", validCart);
		data.put("invalid_cart", invalidCart);
		return data;
	}

	private static boolean singleItemOk(Map<String, Object> cartdata, Map<String, Object> itemRow) {
		String approve = String.valueOf(itemRow.getOrDefault("approve_status", ""));
		if (!"onsale".equals(approve) && !"offline_sale".equals(approve)) {
			return false;
		}
		String shopType = String.valueOf(cartdata.getOrDefault("shop_type", ""));
		String specialType = String.valueOf(itemRow.getOrDefault("special_type", "normal"));
		if ("drug".equals(shopType) && !"drug".equals(specialType)) {
			return false;
		}
		if (!"drug".equals(shopType) && "drug".equals(specialType)) {
			return false;
		}
		if (isGiftTruthy(itemRow.get("is_gift"))) {
			return false;
		}
		return true;
	}

	private static boolean isGiftTruthy(Object v) {
		if (Boolean.TRUE.equals(v)) {
			return true;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		return "true".equalsIgnoreCase(String.valueOf(v));
	}

	private static String firstPicForCart(Map<String, Object> itemRow) {
		Object pics = itemRow.get("pics");
		if (pics instanceof Collection<?> coll && !coll.isEmpty()) {
			Object first = pics instanceof List<?> list ? list.get(0) : coll.iterator().next();
			return first != null ? String.valueOf(first) : "";
		}
		if (pics instanceof String s) {
			return firstPicFromString(s);
		}
		return pics == null ? "" : String.valueOf(pics);
	}

	private static void applyPhpCartLineFieldTypes(Map<String, Object> line) {
		for (String key : PHP_CART_LINE_STRING_KEYS) {
			if (!line.containsKey(key)) {
				continue;
			}
			Object v = line.get(key);
			if (v == null) {
				line.put(key, "");
				continue;
			}
			if (!(v instanceof String)) {
				line.put(key, String.valueOf(v));
			}
		}
		Object mp = line.get("market_price");
		if (mp == null || (mp instanceof Number n && n.longValue() == 0L) || "0".equals(String.valueOf(mp).trim())) {
			line.put("market_price", "0");
		}
	}

	private static String firstPicFromString(String s) {
		s = s.trim();
		if (s.startsWith("[")) {
			try {
				List<?> arr = OM.readValue(s, new TypeReference<List<?>>() {});
				if (arr != null && !arr.isEmpty()) {
					return String.valueOf(arr.get(0));
				}
			} catch (Exception ignored) {
			}
			String inner = s.substring(1, s.endsWith("]") ? s.length() - 1 : s.length()).trim();
			if (inner.isEmpty()) {
				return "";
			}
			String[] parts = inner.split(",");
			if (parts.length > 0) {
				String p0 = parts[0].trim();
				if ((p0.startsWith("\"") && p0.endsWith("\"")) || (p0.startsWith("'") && p0.endsWith("'"))) {
					return p0.substring(1, p0.length() - 1);
				}
				return p0;
			}
		}
		return s;
	}

	private static String nzString(Object o) {
		return o == null ? "" : o.toString();
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o == null) {
			return 0L;
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Long toLongObject(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int toInt(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		if (o == null) {
			return 0;
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
