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

package cn.shopex.ecshopx.employeepurchase.integration;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.order.normal.OrderCheckoutEmployeePurchaseCartPort;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseCartDataListService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderCheckoutEmployeePurchaseCartPortImpl implements OrderCheckoutEmployeePurchaseCartPort {

	private final EmployeePurchaseCartDataListService employeePurchaseCartDataListService;
	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public OrderCheckoutEmployeePurchaseCartPortImpl(
			EmployeePurchaseCartDataListService employeePurchaseCartDataListService,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate stringRedisTemplate,
			ObjectMapper objectMapper) {
		this.employeePurchaseCartDataListService = employeePurchaseCartDataListService;
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void fillItemsFromEmployeePurchaseCart(NormalOrderCreateParams p, HttpServletRequest request) {
		Map<String, Object> pr = p.getParams();
		long companyId = longVal(pr.get("company_id"), 0L);
		long enterpriseId = longVal(pr.get("enterprise_id"), 0L);
		long activityId = longVal(pr.get("activity_id"), 0L);
		long userId = longVal(pr.get("user_id"), 0L);
		if (companyId <= 0L || enterpriseId <= 0L || activityId <= 0L || userId <= 0L) {
			throw new ResourceException("购物车为空");
		}

		String cartType = pr.get("cart_type") == null ? "cart" : pr.get("cart_type").toString().trim();
		Map<String, Object> cartData;
		if ("fastbuy".equals(cartType)) {
			cartData = loadFastBuyCartData(companyId, enterpriseId, activityId, userId);
		} else {
			cartData = employeePurchaseCartDataListService.getCartDataList(
					companyId, userId, enterpriseId, activityId);
		}
		List<Map<String, Object>> validCart = (List<Map<String, Object>>) cartData.get("valid_cart");
		if (validCart == null || validCart.isEmpty()) {
			throw new ResourceException("购物车为空");
		}
		Map<String, Object> cartlist = validCart.get(0);
		List<Map<String, Object>> list = (List<Map<String, Object>>) cartlist.get("list");
		if (list == null || list.isEmpty()) {
			throw new ResourceException("购物车为空");
		}

		List<Map<String, Object>> items = new ArrayList<>();
		for (Map<String, Object> cart : list) {
			if (!truthyChecked(cart.get("is_checked"))) {
				continue;
			}
			long num = longVal(cart.get("num"), 0L);
			if (num <= 0L) {
				continue;
			}
			Map<String, Object> line = new LinkedHashMap<>();
			line.put("item_id", longVal(cart.get("item_id"), 0L));
			line.put("price", longVal(cart.get("price"), 0L));
			line.put("num", (int) Math.min(num, Integer.MAX_VALUE));
			items.add(line);
		}
		if (items.isEmpty()) {
			throw new ResourceException("购物车为空");
		}
		pr.put("items", items);
		pr.put("_checkout_cart_meta", cartlist);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> loadFastBuyCartData(
			long companyId, long enterpriseId, long activityId, long userId) {
		String key =
				"employee_purchase_fastbuy:"
						+ sha1Hex(String.valueOf(companyId) + enterpriseId + activityId + userId);
		String raw = stringRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("valid_cart", List.of());
			empty.put("invalid_cart", List.of());
			return empty;
		}
		try {
			Map<String, Object> cartRow = objectMapper.readValue(raw, new TypeReference<>() {});
			cartRow.put("is_checked", true);
			Map<String, Object> cartlist = new LinkedHashMap<>();
			cartlist.put("list", List.of(cartRow));
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("valid_cart", List.of(cartlist));
			body.put("invalid_cart", List.of());
			return body;
		} catch (Exception e) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("valid_cart", List.of());
			empty.put("invalid_cart", List.of());
			return empty;
		}
	}

	private static boolean truthyChecked(Object v) {
		if (v == null) {
			return false;
		}
		if (Boolean.FALSE.equals(v)) {
			return false;
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		String s = v.toString().trim();
		return !s.isEmpty() && !"0".equals(s) && !"false".equalsIgnoreCase(s);
	}

	private static String sha1Hex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(digest.length * 2);
			for (byte b : digest) {
				sb.append(String.format(Locale.ROOT, "%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new ResourceException("系统环境不支持摘要算法，无法读取快购购物车");
		}
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
