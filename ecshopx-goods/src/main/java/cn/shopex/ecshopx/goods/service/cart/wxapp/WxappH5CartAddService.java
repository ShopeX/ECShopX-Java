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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.Cart;
import cn.shopex.ecshopx.orders.mapper.CartMapper;
import cn.shopex.ecshopx.orders.service.front.wxapp.OrdersCartColumnNamesMapSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappH5CartAddService {

	private static final Logger log = LoggerFactory.getLogger(WxappH5CartAddService.class);
	private static final ObjectMapper OM = new ObjectMapper();

	private final WxappH5CartAddSupport wxappH5CartAddSupport;
	private final CartMapper cartMapper;
	private final StringRedisTemplate stringRedisTemplate;
	public WxappH5CartAddService(
			WxappH5CartAddSupport wxappH5CartAddSupport,
			CartMapper cartMapper,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate stringRedisTemplate) {
		this.wxappH5CartAddSupport = wxappH5CartAddSupport;
		this.cartMapper = cartMapper;
		this.stringRedisTemplate = stringRedisTemplate;
	}

	public Object addCart(Map<String, Object> params) {
		validateAddCartParams(params);
		if (hasTopLevelItemId(params)) {
			applyCartTypeDerivationForSingleItem(params);
			return addSingleItemFlow(params);
		}
		if (hasBatchItems(params)) {
			return addBatchItemsFlow(params);
		}
		throw new ResourceException("提交的商品数据错误");
	}

	private void validateAddCartParams(Map<String, Object> params) {
		String shopTypeRaw = stringVal(params.get("shop_type"));
		if (!StringUtils.hasText(shopTypeRaw)) {
			throw new ResourceException("提交的购物车数据有误");
		}
		String cartType = normalizedCartTypeString(params);
		if ("drug".equals(shopTypeRaw) && "fastbuy".equals(cartType)) {
			throw new ResourceException("该商品不可购买");
		}
		Object itemsObj = params.get("items");
		if (itemsObj instanceof List<?> items) {
			for (Object o : items) {
				if (!(o instanceof Map<?, ?> m)) {
					throw new BadRequestException("提交购物车数据有误");
				}
				parseRequiredPositiveItemId(m.get("item_id"));
				parseRequiredNumStrict(m.get("num"));
			}
		}
	}

	private Object addBatchItemsFlow(Map<String, Object> params) {
		String ct = normalizedCartTypeString(params);
		if (!"fastbuy".equals(ct)) {
			params.put("cart_type", "cart");
		}
		long userId = longVal(params.get("user_id"));
		if (userId <= 0L) {
			log.info("wxapp h5 addCart batch skip persist: userId<=0");
			return Boolean.TRUE;
		}
		List<?> rawItems = (List<?>) params.get("items");
		List<Object> chunk = new ArrayList<>();
		for (Object el : rawItems) {
			if (!(el instanceof Map<?, ?> one)) {
				throw new BadRequestException("提交购物车数据有误");
			}
			LinkedHashMap<String, Object> slice = new LinkedHashMap<>(params);
			slice.put("item_id", one.get("item_id"));
			slice.put("num", one.get("num"));
			if (one.get("company_id") != null) {
				slice.put("company_id", one.get("company_id"));
			}
			chunk.add(addSingleItemFlow(slice));
		}
		return chunk;
	}

	private Object addSingleItemFlow(Map<String, Object> params) {
		long userId = longVal(params.get("user_id"));
		if (userId <= 0L) {
			log.info("wxapp h5 addCart skip persist: userId<=0 companyId={} itemId={}", params.get("company_id"), params.get("item_id"));
			return Boolean.TRUE;
		}
		ensureShopTypeSupported(stringVal(params.get("shop_type")));
		long itemId = parseRequiredPositiveItemId(params.get("item_id"));
		int num = parseRequiredNumStrict(params.get("num"));
		Cart existing = findCartRowForAdd(params, itemId);
		if (num <= 0 && existing != null) {
			cartMapper.deleteById(existing.getCartId());
			Map<String, Object> status = new LinkedHashMap<>();
			status.put("status", Boolean.TRUE);
			return status;
		}
		if (num <= 0) {
			throw new BadRequestException("提交购物车数据有误");
		}
		Map<String, Object> cartInfo = cartRowToInfoMap(existing);
		Map<String, Object> formatted = wxappH5CartAddSupport.formatAddCartData(params, cartInfo);
		String shopType = stringVal(params.get("shop_type"));
		wxappH5CartAddSupport.checkItemParamsForShopType(shopType, params);
		wxappH5CartAddSupport.checkCartInfoPromotionForShopType(shopType, formatted, true);
		if ("fastbuy".equals(normalizedCartTypeString(params))) {
			return setFastBuyCart(formatted, params);
		}
		return persistCart(formatted, params, existing);
	}

	private void applyCartTypeDerivationForSingleItem(Map<String, Object> params) {
		String ct = normalizedCartTypeString(params);
		if (!"fastbuy".equals(ct)) {
			params.put("cart_type", "cart");
		}
	}

	private static boolean hasTopLevelItemId(Map<String, Object> params) {
		Object raw = params.get("item_id");
		if (raw == null) {
			return false;
		}
		long v = parseLongStrict(raw);
		return v > 0L;
	}

	private static boolean hasBatchItems(Map<String, Object> params) {
		Object raw = params.get("items");
		if (!(raw instanceof List<?> list) || list.isEmpty()) {
			return false;
		}
		return !hasTopLevelItemId(params);
	}

	private void ensureShopTypeSupported(String shopType) {
		if ("distributor".equals(shopType) || "drug".equals(shopType) || "pointsmall".equals(shopType)) {
			return;
		}
		if ("community".equals(shopType) || "shop".equals(shopType) || "mall".equals(shopType)) {
			throw new ResourceException("购物车商品类型不能混合");
		}
		throw new ResourceException("购物车商品类型不能混合");
	}

	private Cart findCartRowForAdd(Map<String, Object> params, long itemId) {
		long companyId = longVal(params.get("company_id"));
		long userId = longVal(params.get("user_id"));
		String shopType = stringVal(params.get("shop_type"));
		long shopId = longVal(params.get("shop_id"));
		String activityType = stringVal(params.get("activity_type"));
		long activityId = longVal(params.get("activity_id"));
		String wxappAppid = stringVal(params.get("wxapp_appid"));
		LambdaQueryWrapper<Cart> w = new LambdaQueryWrapper<>();
		w.eq(Cart::getCompanyId, companyId)
				.eq(Cart::getUserId, userId)
				.eq(Cart::getItemId, itemId)
				.eq(Cart::getShopType, shopType)
				.eq(Cart::getShopId, shopId)
				.eq(Cart::getActivityType, activityType);
		if (activityId <= 0L) {
			w.isNull(Cart::getActivityId);
		} else {
			w.eq(Cart::getActivityId, activityId);
		}
		if ("package".equals(activityType)) {
			String itemsIdStr = OrdersCartColumnNamesMapSupport.joinItemsId(params.get("items_id"));
			w.eq(Cart::getItemsId, itemsIdStr);
		} else {
			w.and(q -> q.isNull(Cart::getItemsId).or().eq(Cart::getItemsId, "").or().eq(Cart::getItemsId, "0"));
		}
		if (StringUtils.hasText(wxappAppid)) {
			w.eq(Cart::getWxaAppid, wxappAppid);
		}
		w.last("LIMIT 1");
		return cartMapper.selectOne(w);
	}

	private static Map<String, Object> cartRowToInfoMap(Cart c) {
		Map<String, Object> m = new LinkedHashMap<>();
		if (c == null) {
			return m;
		}
		if (c.getCartId() != null) {
			m.put("cart_id", c.getCartId());
		}
		if (c.getNum() != null) {
			m.put("num", c.getNum());
		}
		if (c.getMarketingType() != null) {
			m.put("marketing_type", c.getMarketingType());
		}
		if (c.getMarketingId() != null) {
			m.put("marketing_id", c.getMarketingId());
		}
		return m;
	}

	private Map<String, Object> persistCart(Map<String, Object> formatted, Map<String, Object> params, Cart existing) {
		int newNum = intVal(formatted.get("num"));
		int now = (int) (System.currentTimeMillis() / 1000L);
		if (existing != null) {
			existing.setNum(newNum);
			existing.setItemName(stringVal(formatted.get("item_name")));
			existing.setPics(stringVal(formatted.get("pics")));
			existing.setPrice(intVal(formatted.get("price")));
			String activityType = stringVal(formatted.get("activity_type"));
			if (StringUtils.hasText(activityType)) {
				existing.setActivityType(activityType);
			}
			if (isTruthyLong(formatted.get("activity_id"))) {
				existing.setActivityId(longVal(formatted.get("activity_id")));
			}
			String marketingType = stringVal(formatted.get("marketing_type"));
			if (StringUtils.hasText(marketingType)) {
				existing.setMarketingType(marketingType);
			}
			if (isTruthyLong(formatted.get("marketing_id"))) {
				existing.setMarketingId(longVal(formatted.get("marketing_id")));
			}
			String itemsId = stringVal(formatted.get("items_id"));
			if (StringUtils.hasText(itemsId)) {
				existing.setItemsId(itemsId);
			}
			String wxapp = stringVal(formatted.get("wxapp_appid"));
			if (StringUtils.hasText(wxapp)) {
				existing.setWxaAppid(wxapp);
			}
			existing.setSourceType("normal");
			existing.setUpdated(now);
			int rows = cartMapper.updateById(existing);
			if (rows <= 0) {
				throw new ResourceException("未查询到更新数据");
			}
			Cart refreshed = cartMapper.selectById(existing.getCartId());
			return cartRowToResponse(refreshed);
		}
		Cart insert = new Cart();
		insert.setCompanyId(longVal(params.get("company_id")));
		insert.setUserId(longVal(params.get("user_id")));
		long promoterUserId = longVal(params.get("promoter_user_id"));
		if (promoterUserId > 0L) {
			insert.setPromoterUserId(promoterUserId);
		}
		insert.setShopType(stringVal(params.get("shop_type")));
		insert.setShopId(longVal(params.get("shop_id")));
		String activityType = stringVal(formatted.get("activity_type"));
		if (StringUtils.hasText(activityType)) {
			insert.setActivityType(activityType);
		}
		if (isTruthyLong(formatted.get("activity_id"))) {
			insert.setActivityId(longVal(formatted.get("activity_id")));
		}
		insert.setItemType("normal");
		insert.setItemId(longVal(formatted.get("item_id")));
		String itemsId = stringVal(formatted.get("items_id"));
		if (StringUtils.hasText(itemsId)) {
			insert.setItemsId(itemsId);
		}
		insert.setItemName(stringVal(formatted.get("item_name")));
		insert.setPics(stringVal(formatted.get("pics")));
		insert.setNum(newNum);
		insert.setPrice(intVal(formatted.get("price")));
		String wxapp = stringVal(formatted.get("wxapp_appid"));
		if (StringUtils.hasText(wxapp)) {
			insert.setWxaAppid(wxapp);
		}
		insert.setIsChecked(true);
		insert.setIsPlusBuy(false);
		insert.setSourceType("normal");
		insert.setCreated(now);
		insert.setUpdated(now);
		cartMapper.insert(insert);
		clearUnsetOptionalCartFields(insert.getCartId(), formatted);
		Cart saved = cartMapper.selectById(insert.getCartId());
		return cartRowToResponse(saved);
	}

	private void clearUnsetOptionalCartFields(Long cartId, Map<String, Object> formatted) {
		if (cartId == null) {
			return;
		}
		LambdaUpdateWrapper<Cart> uw = new LambdaUpdateWrapper<>();
		uw.eq(Cart::getCartId, cartId);
		boolean touched = false;
		if (!isTruthyLong(formatted.get("activity_id"))) {
			uw.set(Cart::getActivityId, null);
			touched = true;
		}
		if (!StringUtils.hasText(stringVal(formatted.get("marketing_type")))) {
			uw.set(Cart::getMarketingType, null);
			touched = true;
		}
		if (!isTruthyLong(formatted.get("marketing_id"))) {
			uw.set(Cart::getMarketingId, null);
			touched = true;
		}
		if (!StringUtils.hasText(stringVal(formatted.get("items_id")))) {
			uw.set(Cart::getItemsId, null);
			touched = true;
		}
		if (!StringUtils.hasText(stringVal(formatted.get("wxapp_appid")))) {
			uw.set(Cart::getWxaAppid, null);
			touched = true;
		}
		if (touched) {
			cartMapper.update(null, uw);
		}
	}

	private static Map<String, Object> cartRowToResponse(Cart c) {
		Map<String, Object> m = new LinkedHashMap<>();
		if (c != null && c.getCartId() != null) {
			m.put("cart_id", c.getCartId());
		}
		return m;
	}

	private Map<String, Object> setFastBuyCart(Map<String, Object> formatted, Map<String, Object> params) {
		try {
			formatted.put("is_checked", Boolean.TRUE);
			formatted.put("cart_id", 0L);
			long companyId = longVal(params.get("company_id"));
			long userId = longVal(params.get("user_id"));
			String key = "fastbuy:" + DigestUtils.sha1Hex(String.valueOf(companyId) + userId);
			String json = OM.writeValueAsString(formatted);
			stringRedisTemplate.opsForValue().set(key, json, Duration.ofSeconds(600));
			return formatted;
		} catch (Exception e) {
			throw new ResourceException("购物车数据序列化失败");
		}
	}

	private static boolean isTruthyLong(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Number n) {
			return n.longValue() > 0L;
		}
		try {
			return Long.parseLong(raw.toString().trim()) > 0L;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static String normalizedCartTypeString(Map<String, Object> params) {
		Object raw = params.get("cart_type");
		if (raw == null) {
			return "";
		}
		return raw.toString().trim();
	}

	private static long parseRequiredPositiveItemId(Object raw) {
		long v = parseLongStrict(raw);
		if (v <= 0L) {
			throw new BadRequestException("提交购物车数据有误");
		}
		return v;
	}

	private static long parseLongStrict(Object raw) {
		if (raw == null) {
			throw new BadRequestException("参数非法");
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数非法");
		}
	}

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

	private static int intVal(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
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
