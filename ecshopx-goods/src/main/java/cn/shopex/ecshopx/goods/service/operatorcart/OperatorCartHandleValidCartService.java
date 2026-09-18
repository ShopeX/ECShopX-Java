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
import cn.shopex.ecshopx.companys.domain.OperatorCart;
import cn.shopex.ecshopx.companys.mapper.OperatorCartMapper;
import cn.shopex.ecshopx.distribution.service.HandleValidCartValidShopIdsService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OperatorCartHandleValidCartService {

	private final OperatorCartMapper operatorCartMapper;
	private final HandleValidCartValidShopIdsService handleValidCartValidShopIdsService;

	public OperatorCartHandleValidCartService(OperatorCartMapper operatorCartMapper,
			HandleValidCartValidShopIdsService handleValidCartValidShopIdsService) {
		this.operatorCartMapper = operatorCartMapper;
		this.handleValidCartValidShopIdsService = handleValidCartValidShopIdsService;
	}

	public Map<String, Object> handle(long companyId, long userId, List<Map<String, Object>> cartList,
			Map<Long, Map<String, Object>> itemListByItemId) {
		for (Map<String, Object> row : cartList) {
			Object st = row.get("shop_type");
			if (st == null || !StringUtils.hasText(String.valueOf(st))) {
				row.put("shop_type", "shop_offline");
			}
		}

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
				updateOperatorCartNum(cartdata, 1L);
				cartNum = 1L;
			}

			int type = toInt(itemRow.get("type"));
			if (type == 1) {
				invalidCart.add(cartdata);
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
				validCart.add(new LinkedHashMap<>(cartdata));
			}
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("valid_cart", validCart);
		data.put("invalid_cart", invalidCart);
		return data;
	}

	private void updateOperatorCartNum(Map<String, Object> cartdata, long newNum) {
		LambdaUpdateWrapper<OperatorCart> uw = new LambdaUpdateWrapper<>();
		uw.eq(OperatorCart::getCompanyId, toLong(cartdata.get("company_id")));
		uw.eq(OperatorCart::getOperatorId, toLong(cartdata.get("operator_id")));
		uw.eq(OperatorCart::getDistributorId, toLong(cartdata.get("distributor_id")));
		uw.eq(OperatorCart::getItemId, toLong(cartdata.get("item_id")));
		Long cartId = toLongObject(cartdata.get("cart_id"));
		if (cartId != null && cartId > 0L) {
			uw.eq(OperatorCart::getCartId, cartId);
		}
		uw.set(OperatorCart::getNum, newNum);
		int n = operatorCartMapper.update(null, uw);
		if (n <= 0) {
			throw new ResourceException("未查询到更新数据");
		}
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
		Object spec = itemRow.get("spec_image_url");
		if (spec != null && StringUtils.hasText(spec.toString())) {
			return spec.toString().trim();
		}
		Object pics = itemRow.get("pics");
		if (pics == null) {
			return "";
		}
		if (pics instanceof String s) {
			return firstPicFromString(s);
		}
		return pics.toString();
	}

	private static String firstPicFromString(String s) {
		s = s.trim();
		if (s.startsWith("[") && s.endsWith("]")) {
			String inner = s.substring(1, s.length() - 1).trim();
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
