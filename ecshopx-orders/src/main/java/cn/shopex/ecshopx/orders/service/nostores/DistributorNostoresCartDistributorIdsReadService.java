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

package cn.shopex.ecshopx.orders.service.nostores;

import cn.shopex.ecshopx.orders.domain.Cart;
import cn.shopex.ecshopx.orders.mapper.CartMapper;
import cn.shopex.ecshopx.orders.repository.nostores.NostoresScopedDistributorIdsJdbcRepository;
import cn.shopex.ecshopx.orders.service.nostores.dto.NostoresScopedDistributorFilter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorNostoresCartDistributorIdsReadService {

	private final NostoresScopedDistributorIdsJdbcRepository nostoresScopedDistributorIdsJdbcRepository;
	private final CartMapper cartMapper;
	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public DistributorNostoresCartDistributorIdsReadService(
			NostoresScopedDistributorIdsJdbcRepository nostoresScopedDistributorIdsJdbcRepository,
			CartMapper cartMapper,
			StringRedisTemplate stringRedisTemplate,
			ObjectMapper objectMapper) {
		this.nostoresScopedDistributorIdsJdbcRepository = nostoresScopedDistributorIdsJdbcRepository;
		this.cartMapper = cartMapper;
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public List<Long> listDistributorIdsByNostoresCart(
			long companyId,
			long userId,
			String cartType,
			String orderType,
			String seckillId,
			String seckillTicket,
			String iscrossborder,
			String bargainId) {
		return listDistributorIdsByNostoresCart(
				companyId,
				userId,
				NostoresScopedDistributorFilter.forCompanyZitiOnly(companyId),
				cartType,
				orderType,
				seckillId,
				seckillTicket,
				iscrossborder,
				bargainId,
				0L);
	}

	public List<Long> listDistributorIdsByNostoresCart(
			long companyId,
			long userId,
			NostoresScopedDistributorFilter nostoresScopedFilter,
			String cartType,
			String orderType,
			String seckillId,
			String seckillTicket,
			String iscrossborder,
			String bargainId) {
		return listDistributorIdsByNostoresCart(
				companyId,
				userId,
				nostoresScopedFilter,
				cartType,
				orderType,
				seckillId,
				seckillTicket,
				iscrossborder,
				bargainId,
				0L);
	}

	public List<Long> listDistributorIdsByNostoresCart(
			long companyId,
			long userId,
			NostoresScopedDistributorFilter nostoresScopedFilter,
			String cartType,
			String orderType,
			String seckillId,
			String seckillTicket,
			String iscrossborder,
			String bargainId,
			long cartDistributorShopId) {
		if (userId <= 0L) {
			return List.of();
		}
		if ("offline".equals(cartType)) {
			return List.of();
		}
		List<Long> candidates = nostoresScopedDistributorIdsJdbcRepository.listDistributorIdsByScope(nostoresScopedFilter);
		if (candidates.isEmpty()) {
			return List.of();
		}
		boolean normalOrder = "normal".equals(orderType);
		long shopIdFilterForNormal = cartDistributorShopId > 0L ? cartDistributorShopId : 0L;

		if (normalOrder) {
			List<Cart> cartRows =
					loadCheckedCartsForShop(
							companyId,
							userId,
							shopIdFilterForNormal,
							cartType,
							orderType,
							seckillId,
							seckillTicket,
							iscrossborder,
							bargainId);
			return mergeZitiWithCartSemantics(candidates, cartRows);
		}

		List<Long> out = new ArrayList<>();
		for (Long distId : candidates) {
			List<Cart> cartRows =
					loadCheckedCartsForShop(
							companyId,
							userId,
							distId,
							cartType,
							orderType,
							seckillId,
							seckillTicket,
							iscrossborder,
							bargainId);
			if (!cartRows.isEmpty()) {
				out.add(distId);
			}
		}
		return out;
	}

	private List<Cart> loadCheckedCartsForShop(
			long companyId,
			long userId,
			long shopIdFilter,
			String cartType,
			String orderType,
			String seckillId,
			String seckillTicket,
			String iscrossborder,
			String bargainId) {
		List<Cart> raw;
		if ("fastbuy".equals(cartType == null ? "cart" : cartType)) {
			raw = loadFastBuyCartRows(companyId, userId, shopIdFilter);
		} else {
			LambdaQueryWrapper<Cart> w =
					new LambdaQueryWrapper<Cart>()
							.eq(Cart::getCompanyId, companyId)
							.eq(Cart::getUserId, userId)
							.eq(Cart::getShopType, "distributor")
							.eq(Cart::getIsChecked, Boolean.TRUE)
							.eq(Cart::getShopId, shopIdFilter);
			raw = cartMapper.selectList(w);
		}
		List<Cart> out = new ArrayList<>();
		for (Cart c : raw) {
			if (matchesNostoresCartParams(
					c, cartType, orderType, seckillId, seckillTicket, iscrossborder, bargainId)) {
				out.add(c);
			}
		}
		return out;
	}

	private List<Cart> loadFastBuyCartRows(long companyId, long userId, long shopIdFilter) {
		String key = "fastbuy:" + DigestUtils.sha1Hex(String.valueOf(companyId) + userId);
		String raw = stringRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		String trim = raw.trim();
		if ("[]".equals(trim) || "null".equalsIgnoreCase(trim)) {
			return List.of();
		}
		try {
			Map<String, Object> line = objectMapper.readValue(trim, new TypeReference<Map<String, Object>>() {});
			if (line == null || line.isEmpty()) {
				return List.of();
			}
			long lineShop = longObj(line.get("shop_id"));
			if (shopIdFilter > 0L && lineShop != shopIdFilter) {
				return List.of();
			}
			if (shopIdFilter == 0L && lineShop != 0L) {
				// normal-order path filters shop_id=0 when cart_distributor_id unset
				return List.of();
			}
			String shopType = stringVal(line.get("shop_type"));
			if (StringUtils.hasText(shopType) && !"distributor".equals(shopType)) {
				return List.of();
			}
			Object checked = line.get("is_checked");
			if (checked instanceof Boolean b && !b) {
				return List.of();
			}
			if (checked != null && !(checked instanceof Boolean)) {
				String cs = String.valueOf(checked).trim();
				if ("false".equalsIgnoreCase(cs) || "0".equals(cs)) {
					return List.of();
				}
			}
			long itemId = longObj(line.get("item_id"));
			if (itemId <= 0L) {
				return List.of();
			}
			return List.of(cartFromFastBuyLineMap(line, companyId, userId));
		} catch (Exception e) {
			return List.of();
		}
	}

	private static Cart cartFromFastBuyLineMap(Map<String, Object> line, long companyId, long userId) {
		Cart c = new Cart();
		long lc = longObj(line.get("company_id"));
		c.setCompanyId(lc > 0L ? lc : companyId);
		long lu = longObj(line.get("user_id"));
		c.setUserId(lu > 0L ? lu : userId);
		c.setCartId(longObj(line.get("cart_id")));
		c.setShopId(longObj(line.get("shop_id")));
		String st = stringVal(line.get("shop_type"));
		c.setShopType(StringUtils.hasText(st) ? st : "distributor");
		c.setItemId(longObj(line.get("item_id")));
		int n = intQty(line.get("num"));
		c.setNum(n > 0 ? n : 1);
		String it = stringVal(line.get("item_type"));
		c.setItemType(StringUtils.hasText(it) ? it : "normal");
		c.setItemsId(stringVal(line.get("items_id")));
		c.setItemName(stringVal(line.get("item_name")));
		c.setPics(stringVal(line.get("pics")));
		c.setPrice(intQty(line.get("price")));
		c.setPoint(intQty(line.get("point")));
		c.setActivityId(longObj(line.get("activity_id")));
		c.setActivityType(stringVal(line.get("activity_type")));
		c.setMarketingId(longObjNullable(line.get("marketing_id")));
		c.setMarketingType(stringVal(line.get("marketing_type")));
		Object plus = line.get("is_plus_buy");
		if (plus instanceof Boolean b) {
			c.setIsPlusBuy(b);
		} else if (plus != null) {
			String ps = String.valueOf(plus).trim();
			c.setIsPlusBuy("1".equals(ps) || "true".equalsIgnoreCase(ps));
		} else {
			c.setIsPlusBuy(Boolean.FALSE);
		}
		c.setIsChecked(Boolean.TRUE);
		return c;
	}

	private static boolean matchesNostoresCartParams(
			Cart c,
			String cartType,
			String orderType,
			String seckillId,
			String seckillTicket,
			String iscrossborder,
			String bargainId) {
		String ct = cartType == null ? "cart" : cartType;
		if (!"cart".equals(ct) && !"fastbuy".equals(ct)) {
			return false;
		}
		if ("fastbuy".equals(ct) && Boolean.TRUE.equals(c.getIsPlusBuy())) {
			return false;
		}
		if ("normal".equals(orderType) && "services".equals(c.getItemType())) {
			return false;
		}
		Long seckillNumeric = parsePositiveLong(seckillId);
		boolean seckillTicketPresent = StringUtils.hasText(trimToEmpty(seckillTicket));
		if (seckillNumeric != null) {
			if (c.getActivityId() == null || c.getActivityId().longValue() != seckillNumeric.longValue()) {
				return false;
			}
			String at = c.getActivityType();
			if (at == null) {
				return false;
			}
			if (!"seckill".equals(at) && !"limitedTimeSale".equalsIgnoreCase(at)) {
				return false;
			}
		} else if (seckillTicketPresent) {
			String at = c.getActivityType();
			if (at == null) {
				return false;
			}
			if (!"seckill".equals(at) && !"limitedTimeSale".equalsIgnoreCase(at)) {
				return false;
			}
		} else {
			Long bargainNumeric = parsePositiveLong(bargainId);
			if (bargainNumeric != null) {
				if (c.getActivityId() == null || c.getActivityId().longValue() != bargainNumeric.longValue()) {
					return false;
				}
				if (!"bargain".equals(c.getActivityType())) {
					return false;
				}
			}
		}
		if (isCrossBorderActive(iscrossborder)) {
			return c.getMarketingId() != null
					|| (c.getMarketingType() != null && !c.getMarketingType().isBlank());
		}
		return true;
	}

	private static List<Long> mergeZitiWithCartSemantics(List<Long> ziti, List<Cart> cartRows) {
		if (cartRows.isEmpty()) {
			return List.of();
		}
		Set<Long> cartShopIds = new LinkedHashSet<>();
		boolean anyZero = false;
		for (Cart c : cartRows) {
			Long sid = c.getShopId();
			if (sid == null || sid == 0L) {
				anyZero = true;
			} else {
				cartShopIds.add(sid);
			}
		}
		List<Long> out = new ArrayList<>();
		if (anyZero) {
			out.addAll(ziti);
			return out;
		}
		for (Long id : ziti) {
			if (cartShopIds.contains(id)) {
				out.add(id);
			}
		}
		return out;
	}

	private static boolean isCrossBorderActive(String iscrossborder) {
		if (!StringUtils.hasText(iscrossborder)) {
			return false;
		}
		String t = iscrossborder.trim();
		return "1".equals(t) || "true".equalsIgnoreCase(t);
	}

	private static String trimToEmpty(String s) {
		return s == null ? "" : s.trim();
	}

	private static Long parsePositiveLong(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		String t = raw.trim();
		if ("0".equals(t)) {
			return null;
		}
		try {
			long v = Long.parseLong(t);
			return v > 0L ? Long.valueOf(v) : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long longObj(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Long longObjNullable(Object v) {
		long n = longObj(v);
		return n > 0L ? Long.valueOf(n) : null;
	}

	private static int intQty(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String stringVal(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof String s) {
			return s;
		}
		if (v instanceof List<?> || v instanceof Map<?, ?>) {
			return null;
		}
		return String.valueOf(v);
	}
}
