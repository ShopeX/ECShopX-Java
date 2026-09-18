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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityItems;
import cn.shopex.ecshopx.employeepurchase.domain.Cart;
import cn.shopex.ecshopx.employeepurchase.domain.Employees;
import cn.shopex.ecshopx.employeepurchase.domain.Relatives;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityItemsMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.CartMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.RelativesMapper;
import cn.shopex.ecshopx.goods.service.items.EmployeePurchaseItemsSkuDetailService;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.ActivityPassphraseService;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.EmployeePassphraseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmployeePurchaseCartAddService {

	private final CartMapper cartMapper;
	private final ActivitiesMapper activitiesMapper;
	private final EmployeesMapper employeesMapper;
	private final RelativesMapper relativesMapper;
	private final ActivityItemsMapper activityItemsMapper;
	private final DistributorMapper distributorMapper;
	private final EmployeePurchaseItemsSkuDetailService employeePurchaseItemsSkuDetailService;
	private final EmployeePurchaseFastBuyRedisService employeePurchaseFastBuyRedisService;
	private final ActivityPassphraseService activityPassphraseService;
	private final EmployeePassphraseService employeePassphraseService;
	private final EmployeePurchaseCartItemLimitService employeePurchaseCartItemLimitService;

	public EmployeePurchaseCartAddService(
			CartMapper cartMapper,
			ActivitiesMapper activitiesMapper,
			EmployeesMapper employeesMapper,
			RelativesMapper relativesMapper,
			ActivityItemsMapper activityItemsMapper,
			DistributorMapper distributorMapper,
			EmployeePurchaseItemsSkuDetailService employeePurchaseItemsSkuDetailService,
			EmployeePurchaseFastBuyRedisService employeePurchaseFastBuyRedisService,
			ActivityPassphraseService activityPassphraseService,
			EmployeePassphraseService employeePassphraseService,
			EmployeePurchaseCartItemLimitService employeePurchaseCartItemLimitService) {
		this.cartMapper = cartMapper;
		this.activitiesMapper = activitiesMapper;
		this.employeesMapper = employeesMapper;
		this.relativesMapper = relativesMapper;
		this.activityItemsMapper = activityItemsMapper;
		this.distributorMapper = distributorMapper;
		this.employeePurchaseItemsSkuDetailService = employeePurchaseItemsSkuDetailService;
		this.employeePurchaseFastBuyRedisService = employeePurchaseFastBuyRedisService;
		this.activityPassphraseService = activityPassphraseService;
		this.employeePassphraseService = employeePassphraseService;
		this.employeePurchaseCartItemLimitService = employeePurchaseCartItemLimitService;
	}

	public Object addCartData(
			long companyId,
			long userId,
			Map<String, Object> filter,
			Map<String, Object> params,
			boolean isAccumulate) {
		Map<String, Object> mergedForValidation = new LinkedHashMap<>(filter);
		mergedForValidation.putAll(params);
		checkAddCartParams(mergedForValidation);
		resolveItemIdFromCartIfNeeded(companyId, userId, filter, params);
		checkAddCartItems(companyId, filter, params);

		Object rawCartType = params.get("cart_type");
		String cartTypeStr = rawCartType == null ? "" : rawCartType.toString();
		boolean fastbuy = "fastbuy".equals(cartTypeStr);
		if (fastbuy) {
			params.put("is_checked", Boolean.TRUE);
			Map<String, Object> mergedParams = new LinkedHashMap<>(filter);
			mergedParams.putAll(params);
			long enterpriseId = toLongStrict(filter.get("enterprise_id"));
			long activityId = toLongStrict(filter.get("activity_id"));
			long itemId = toLongStrict(filter.get("item_id"));
			employeePurchaseCartItemLimitService.assertFastBuyIntent(
					companyId, enterpriseId, activityId, userId, itemId, (int) toLongParam(params.get("num")));
			return employeePurchaseFastBuyRedisService.setFastBuyCart(
					companyId, enterpriseId, activityId, userId, mergedParams);
		}

		long filterItemId = toLongStrict(filter.get("item_id"));
		long reqNum = toLongParam(params.get("num"));
		LambdaQueryWrapper<Cart> cartRowWrapper = cartMatchWrapper(companyId, userId, filter, filterItemId);
		Cart cartRow = cartMapper.selectOne(cartRowWrapper);

		if (cartRow == null && reqNum <= 0L) {
			throw new ResourceException("加入购物车的数据有误");
		}
		if (cartRow != null && reqNum <= 0L) {
			cartMapper.delete(cartRowWrapper);
			return Collections.emptyList();
		}
		if (cartRow != null) {
			long existingNum = cartRow.getNum() == null ? 0L : cartRow.getNum();
			long newNum = isAccumulate ? reqNum + existingNum : reqNum;
			boolean willCheck =
					params.containsKey("is_checked")
							? parseBooleanLoose(params.get("is_checked"), true)
							: Boolean.TRUE.equals(cartRow.getIsChecked());
			employeePurchaseCartItemLimitService.assertDbCartIntent(
					companyId,
					toLongStrict(filter.get("enterprise_id")),
					toLongStrict(filter.get("activity_id")),
					userId,
					filterItemId,
					(int) newNum,
					willCheck);
			cartRow.setNum(newNum);
			Object checked = params.get("is_checked");
			if (checked instanceof Boolean b) {
				cartRow.setIsChecked(b);
			} else {
				cartRow.setIsChecked(parseBooleanLoose(checked, Boolean.TRUE));
			}
			copyShopFieldsFromParams(cartRow, params);
			int updated = cartMapper.updateById(cartRow);
			if (updated == 0) {
				throw new ResourceException("未查询到更新数据");
			}
			Cart refreshed = cartMapper.selectById(cartRow.getCartId());
			if (refreshed == null) {
				throw new ResourceException("未查询到更新数据");
			}
			return toResponseRow(refreshed);
		}
		if (reqNum > 0L) {
			boolean willCheckNew =
					params.containsKey("is_checked")
							? parseBooleanLoose(params.get("is_checked"), true)
							: true;
			employeePurchaseCartItemLimitService.assertDbCartIntent(
					companyId,
					toLongStrict(filter.get("enterprise_id")),
					toLongStrict(filter.get("activity_id")),
					userId,
					filterItemId,
					(int) reqNum,
					willCheckNew);
			Cart insert = new Cart();
			insert.setCompanyId(companyId);
			insert.setUserId(userId);
			insert.setEnterpriseId(toLongStrict(filter.get("enterprise_id")));
			insert.setActivityId(toLongStrict(filter.get("activity_id")));
			insert.setItemId(filterItemId);
			insert.setNum(reqNum);
			Object checked = params.get("is_checked");
			if (checked instanceof Boolean b) {
				insert.setIsChecked(b);
			} else {
				insert.setIsChecked(parseBooleanLoose(checked, Boolean.TRUE));
			}
			copyShopFieldsFromParams(insert, params);
			Object st = params.get("shop_type");
			insert.setShopType(st == null || !StringUtils.hasText(st.toString()) ? "distributor" : st.toString());
			Object sid = params.get("shop_id");
			insert.setShopId(sid instanceof Number ? ((Number) sid).longValue() : parseLongDefault(sid, 0L));
			cartMapper.insert(insert);
			Cart saved = cartMapper.selectById(insert.getCartId());
			if (saved == null) {
				throw new ResourceException("加入购物车的数据有误");
			}
			return toResponseRow(saved);
		}
		throw new ResourceException("加入购物车的数据有误");
	}

	public Map<String, Object> updateCartData(
			long companyId,
			long userId,
			Map<String, Object> filter,
			Map<String, Object> params) {
		Map<String, Object> mergedForValidation = new LinkedHashMap<>(filter);
		mergedForValidation.putAll(params);
		checkAddCartParams(mergedForValidation);

		Cart cartRow = findCartForUpdate(companyId, userId, filter);

		long reqNum = toLongParam(params.get("num"));
		if (cartRow == null || reqNum <= 0L) {
			throw new ResourceException("更新购物车的数据有误");
		}

		filter.put("item_id", cartRow.getItemId());

		checkAddCartItems(companyId, filter, params);

		boolean willCheck =
				params.containsKey("is_checked")
						? parseBooleanLoose(params.get("is_checked"), true)
						: Boolean.TRUE.equals(cartRow.getIsChecked());
		employeePurchaseCartItemLimitService.assertDbCartIntent(
				companyId,
				toLongStrict(filter.get("enterprise_id")),
				toLongStrict(filter.get("activity_id")),
				userId,
				cartRow.getItemId() == null ? 0L : cartRow.getItemId(),
				(int) reqNum,
				willCheck);

		cartRow.setNum(reqNum);
		Object checked = params.get("is_checked");
		if (checked instanceof Boolean b) {
			cartRow.setIsChecked(b);
		} else {
			cartRow.setIsChecked(parseBooleanLoose(checked, Boolean.TRUE));
		}
		copyShopFieldsFromParams(cartRow, params);

		int updated = cartMapper.updateById(cartRow);
		if (updated == 0) {
			throw new ResourceException("未查询到更新数据");
		}
		Cart refreshed = cartMapper.selectById(cartRow.getCartId());
		if (refreshed == null) {
			throw new ResourceException("未查询到更新数据");
		}
		return toResponseRow(refreshed);
	}

	private Cart findCartForUpdate(long companyId, long userId, Map<String, Object> filter) {
		LambdaQueryWrapper<Cart> w = new LambdaQueryWrapper<>();
		w.eq(Cart::getCompanyId, companyId)
				.eq(Cart::getUserId, userId)
				.eq(Cart::getEnterpriseId, toLongStrict(filter.get("enterprise_id")))
				.eq(Cart::getActivityId, toLongStrict(filter.get("activity_id")));
		Long cartIdEq = parseValidPositiveLong(filter.get("cart_id"));
		if (cartIdEq != null) {
			w.eq(Cart::getCartId, cartIdEq);
		}
		Long itemIdEq = parseValidPositiveLong(filter.get("item_id"));
		if (itemIdEq != null) {
			w.eq(Cart::getItemId, itemIdEq);
		}
		w.last("LIMIT 1");
		return cartMapper.selectOne(w);
	}

	private static void copyShopFieldsFromParams(Cart cartRow, Map<String, Object> params) {
		Object st = params.get("shop_type");
		if (st != null && StringUtils.hasText(st.toString())) {
			cartRow.setShopType(st.toString());
		}
		Object sid = params.get("shop_id");
		if (sid != null) {
			cartRow.setShopId(sid instanceof Number ? ((Number) sid).longValue() : parseLongDefault(sid, 0L));
		}
	}

	private static LambdaQueryWrapper<Cart> cartMatchWrapper(
			long companyId, long userId, Map<String, Object> filter, long itemId) {
		LambdaQueryWrapper<Cart> w = new LambdaQueryWrapper<>();
		w.eq(Cart::getCompanyId, companyId)
				.eq(Cart::getUserId, userId)
				.eq(Cart::getEnterpriseId, toLongStrict(filter.get("enterprise_id")))
				.eq(Cart::getActivityId, toLongStrict(filter.get("activity_id")))
				.eq(Cart::getItemId, itemId)
				.last("LIMIT 1");
		return w;
	}

	private void checkAddCartParams(Map<String, Object> merged) {
		requirePositiveLongField(merged.get("company_id"), "公司ID必填");
		requirePositiveLongResource(merged.get("enterprise_id"), "企业ID必填");
		requirePositiveLongField(merged.get("activity_id"), "活动ID必填");
		requirePositiveLongField(merged.get("user_id"), "用户ID必填");

		boolean itemOk = isValidPositiveLongKey(merged, "item_id");
		boolean cartOk = isValidPositiveLongKey(merged, "cart_id");
		if (!itemOk && !cartOk) {
			throw new ResourceException("商品ID必填");
		}

		long shopId = parseLongDefault(merged.get("shop_id"), 0L);
		Object shopTypeObj = merged.get("shop_type");
		String shopType = shopTypeObj == null ? "" : shopTypeObj.toString();
		if (shopId != 0L && "distributor".equals(shopType)) {
			Distributor d = distributorMapper.selectById(shopId);
			if (d == null || !"true".equals(d.getIsValid())) {
				throw new ResourceException("当前店铺已失效");
			}
		}
	}

	private Map<String, Object> checkAddCartItems(
			long companyId, Map<String, Object> filter, Map<String, Object> params) {
		long activityId = toLongStrict(filter.get("activity_id"));
		Activities activity =
				activitiesMapper.selectOne(
						new LambdaQueryWrapper<Activities>()
								.eq(Activities::getCompanyId, companyId)
								.eq(Activities::getId, activityId));
		if (activity == null) {
			throw new ResourceException("活动不存在");
		}
		String status = activity.getStatus() == null ? "" : activity.getStatus();
		if ("cancel".equals(status)) {
			throw new ResourceException("活动已取消");
		}
		if ("pending".equals(status)) {
			throw new ResourceException("活动已暂停");
		}
		if ("over".equals(status)) {
			throw new ResourceException("活动已结束");
		}

		long filterEnterpriseId = toLongStrict(filter.get("enterprise_id"));
		if (!parseEnterpriseIds(activity.getEnterpriseId()).contains(filterEnterpriseId)) {
			throw new ResourceException("企业不参与该活动");
		}

		long filterUserId = toLongStrict(filter.get("user_id"));
		if (activityPassphraseService.isPassphraseEnabled(activity)) {
			employeePassphraseService.ensurePassphraseEmployeeFromVerifiedActivity(
					activity, companyId, activityId, filterEnterpriseId, filterUserId, "");
		}

		long now = Instant.now().getEpochSecond();

		Employees employee =
				employeesMapper.selectOne(
						new LambdaQueryWrapper<Employees>()
								.eq(Employees::getCompanyId, companyId)
								.eq(Employees::getEnterpriseId, filterEnterpriseId)
								.eq(Employees::getUserId, filterUserId)
								.eq(Employees::getDisabled, false)
								.last("LIMIT 1"));
		if (employee != null) {
			long begin =
					activity.getEmployeeBeginTime() == null
							? 0L
							: activity.getEmployeeBeginTime().longValue();
			long end =
					activity.getEmployeeEndTime() == null
							? 0L
							: activity.getEmployeeEndTime().longValue();
			if (begin > now || end < now) {
				throw new ResourceException("非员工购买时段");
			}
		} else {
			if (!Boolean.TRUE.equals(activity.getIfRelativeJoin())) {
				throw new ResourceException("不是员工，无权购买");
			}
			Relatives relative =
					relativesMapper.selectOne(
							new LambdaQueryWrapper<Relatives>()
									.eq(Relatives::getCompanyId, companyId)
									.eq(Relatives::getEnterpriseId, filterEnterpriseId)
									.eq(Relatives::getActivityId, activityId)
									.eq(Relatives::getUserId, filterUserId)
									.eq(Relatives::getDisabled, false)
									.last("LIMIT 1"));
			if (relative == null) {
				throw new ResourceException("既不是员工也不是亲友，无权购买");
			}
			long rBegin =
					activity.getRelativeBeginTime() == null
							? 0L
							: activity.getRelativeBeginTime().longValue();
			long rEnd =
					activity.getRelativeEndTime() == null
							? 0L
							: activity.getRelativeEndTime().longValue();
			if (rBegin > now || rEnd < now) {
				throw new ResourceException("非亲友购买时段");
			}
		}

		long filterItemId = toLongStrict(filter.get("item_id"));
		Map<String, Object> itemInfo =
				employeePurchaseItemsSkuDetailService.loadSkuDetailMap(companyId, filterItemId);

		Object rowCompany = itemInfo.get("company_id");
		long itemCompanyId = rowCompany instanceof Number ? ((Number) rowCompany).longValue() : 0L;
		if (itemCompanyId != companyId) {
			throw new ResourceException("无效商品");
		}
		Object approve = itemInfo.get("approve_status");
		if (!"onsale".equals(approve == null ? null : approve.toString())) {
			throw new ResourceException("无效商品");
		}

		Object md = itemInfo.get("medicine_data");
		if (md instanceof Map<?, ?> medMap) {
			Object mx = medMap.get("max_num");
			int maxNum = mx instanceof Number ? ((Number) mx).intValue() : 0;
			long reqNum = toLongParam(params.get("num"));
			if (maxNum > 0 && reqNum > maxNum) {
				throw new ResourceException("超出药品单次最大可购买数量,最大可购买:" + maxNum + "个");
			}
		}

		ActivityItems activityItem =
				activityItemsMapper.selectOne(
						new LambdaQueryWrapper<ActivityItems>()
								.eq(ActivityItems::getCompanyId, companyId)
								.eq(ActivityItems::getActivityId, activityId)
								.eq(ActivityItems::getItemId, filterItemId));
		if (activityItem == null || !EmployeePurchaseCartItemLimitService.isOnShelf(activityItem)) {
			throw new ResourceException("商品未参加内购活动");
		}

		long reqNum = toLongParam(params.get("num"));
		if (Boolean.TRUE.equals(activity.getIfShareStore())) {
			int store = 0;
			Object st = itemInfo.get("store");
			if (st instanceof Number) {
				store = ((Number) st).intValue();
			}
			if (store < reqNum) {
				throw new ResourceException("活动库存不足");
			}
		} else {
			int actStore =
					activityItem.getActivityStore() == null ? 0 : activityItem.getActivityStore();
			if (actStore < reqNum) {
				throw new ResourceException("活动库存不足");
			}
		}

		return params;
	}

	private void resolveItemIdFromCartIfNeeded(
			long companyId, long userId, Map<String, Object> filter, Map<String, Object> params) {
		if (isValidPositiveLongKey(filter, "item_id")) {
			return;
		}
		Long cartId = firstValidCartId(filter, params);
		if (cartId == null) {
			throw new ResourceException("商品ID必填");
		}
		LambdaQueryWrapper<Cart> w = new LambdaQueryWrapper<>();
		w.eq(Cart::getCompanyId, companyId)
				.eq(Cart::getUserId, userId)
				.eq(Cart::getEnterpriseId, toLongStrict(filter.get("enterprise_id")))
				.eq(Cart::getActivityId, toLongStrict(filter.get("activity_id")))
				.eq(Cart::getCartId, cartId);
		Cart cart = cartMapper.selectOne(w);
		if (cart == null) {
			throw new ResourceException("加入购物车的数据有误");
		}
		Long itemId = cart.getItemId();
		if (itemId == null || itemId <= 0L) {
			throw new ResourceException("加入购物车的数据有误");
		}
		filter.put("item_id", itemId);
	}

	private static Long firstValidCartId(Map<String, Object> filter, Map<String, Object> params) {
		Long fromFilter = parseValidPositiveLong(filter.get("cart_id"));
		if (fromFilter != null) {
			return fromFilter;
		}
		return parseValidPositiveLong(params.get("cart_id"));
	}

	private static Long parseValidPositiveLong(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			long x = n.longValue();
			return x > 0L ? x : null;
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			long x = Long.parseLong(s);
			return x > 0L ? x : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static boolean isValidPositiveLongKey(Map<String, Object> map, String key) {
		return parseValidPositiveLong(map.get(key)) != null;
	}

	private static void requirePositiveLongField(Object raw, String errMsg) {
		if (raw == null) {
			throw new BadRequestException(errMsg);
		}
		if (raw instanceof Number n) {
			if (n.longValue() <= 0L) {
				throw new BadRequestException(errMsg);
			}
			return;
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException(errMsg);
		}
		try {
			if (Long.parseLong(s) <= 0L) {
				throw new BadRequestException(errMsg);
			}
		} catch (NumberFormatException e) {
			throw new BadRequestException(errMsg);
		}
	}

	private static void requirePositiveLongResource(Object raw, String errMsg) {
		if (raw == null) {
			throw new ResourceException(errMsg);
		}
		if (raw instanceof Number n) {
			if (n.longValue() <= 0L) {
				throw new ResourceException(errMsg);
			}
			return;
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new ResourceException(errMsg);
		}
		try {
			if (Long.parseLong(s) <= 0L) {
				throw new ResourceException(errMsg);
			}
		} catch (NumberFormatException e) {
			throw new ResourceException(errMsg);
		}
	}

	private static long toLongStrict(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(v.toString().trim());
	}

	private static long parseLongDefault(Object v, long def) {
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

	private static long toLongParam(Object n) {
		if (n == null) {
			return 0L;
		}
		if (n instanceof Number num) {
			return num.longValue();
		}
		try {
			return Long.parseLong(n.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Set<Long> parseEnterpriseIds(String csv) {
		Set<Long> out = new HashSet<>();
		if (csv == null || csv.isEmpty()) {
			return out;
		}
		for (String part : csv.split(",")) {
			String t = part.trim();
			if (t.isEmpty()) {
				continue;
			}
			try {
				out.add(Long.parseLong(t));
			} catch (NumberFormatException ignored) {
				// skip invalid token
			}
		}
		return out;
	}

	private static boolean parseBooleanLoose(Object v, boolean defaultTrue) {
		if (v == null) {
			return defaultTrue;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		String s = v.toString().trim();
		String lower = s.toLowerCase(Locale.ROOT);
		if ("false".equals(lower) || "0".equals(lower) || "no".equals(lower) || "off".equals(lower) || s.isEmpty()) {
			return false;
		}
		if ("true".equals(lower) || "1".equals(lower) || "yes".equals(lower) || "on".equals(lower)) {
			return true;
		}
		return true;
	}

	private static Map<String, Object> toResponseRow(Cart c) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("cart_id", c.getCartId());
		m.put("company_id", c.getCompanyId());
		m.put("enterprise_id", c.getEnterpriseId());
		m.put("activity_id", c.getActivityId());
		m.put("user_id", c.getUserId());
		m.put("shop_type", c.getShopType() == null ? "distributor" : c.getShopType());
		m.put("shop_id", c.getShopId() == null ? 0L : c.getShopId());
		m.put("item_id", c.getItemId());
		m.put("num", c.getNum() == null ? 0L : c.getNum());
		m.put("is_checked", Boolean.TRUE.equals(c.getIsChecked()));
		return m;
	}
}
