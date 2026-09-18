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

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityItems;
import cn.shopex.ecshopx.employeepurchase.domain.Cart;
import cn.shopex.ecshopx.employeepurchase.domain.MemberActivityItemsAggregate;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityItemsMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.CartMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.MemberActivityItemsAggregateMapper;
import cn.shopex.ecshopx.employeepurchase.support.PurchaseModeSupport;
import cn.shopex.ecshopx.goods.service.items.EmployeePurchaseItemsSkuListService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class EmployeePurchaseCartDataListService {

	private final ActivitiesMapper activitiesMapper;
	private final CartMapper cartMapper;
	private final ActivityItemsMapper activityItemsMapper;
	private final DistributorMapper distributorMapper;
	private final EmployeePurchaseItemsSkuListService employeePurchaseItemsSkuListService;
	private final EmployeePurchaseCartHandleValidCartService employeePurchaseCartHandleValidCartService;
	private final MemberActivityItemsAggregateMapper memberActivityItemsAggregateMapper;

	public EmployeePurchaseCartDataListService(
			ActivitiesMapper activitiesMapper,
			CartMapper cartMapper,
			ActivityItemsMapper activityItemsMapper,
			DistributorMapper distributorMapper,
			EmployeePurchaseItemsSkuListService employeePurchaseItemsSkuListService,
			EmployeePurchaseCartHandleValidCartService employeePurchaseCartHandleValidCartService,
			MemberActivityItemsAggregateMapper memberActivityItemsAggregateMapper) {
		this.activitiesMapper = activitiesMapper;
		this.cartMapper = cartMapper;
		this.activityItemsMapper = activityItemsMapper;
		this.distributorMapper = distributorMapper;
		this.employeePurchaseItemsSkuListService = employeePurchaseItemsSkuListService;
		this.employeePurchaseCartHandleValidCartService = employeePurchaseCartHandleValidCartService;
		this.memberActivityItemsAggregateMapper = memberActivityItemsAggregateMapper;
	}

	public Map<String, Object> getCartDataList(long companyId, long userId, long enterpriseId, long activityId) {
		LambdaQueryWrapper<Activities> aw = new LambdaQueryWrapper<>();
		aw.eq(Activities::getCompanyId, companyId).eq(Activities::getId, activityId);
		Activities activity = activitiesMapper.selectOne(aw);
		if (activity == null) {
			return emptyBody();
		}
		String st = activity.getStatus() != null ? activity.getStatus() : "";
		if ("cancel".equals(st) || "pending".equals(st) || "over".equals(st)) {
			return emptyBody();
		}

		LambdaQueryWrapper<Cart> cw = new LambdaQueryWrapper<>();
		cw.eq(Cart::getCompanyId, companyId)
				.eq(Cart::getUserId, userId)
				.eq(Cart::getEnterpriseId, enterpriseId)
				.eq(Cart::getActivityId, activityId)
				.orderByAsc(Cart::getCartId);
		List<Cart> dbCarts = cartMapper.selectList(cw);
		if (dbCarts == null || dbCarts.isEmpty()) {
			return emptyBody();
		}

		LinkedHashMap<Long, Map<String, Object>> cartByCartId = new LinkedHashMap<>();
		for (Cart c : dbCarts) {
			Map<String, Object> m = cartEntityToMap(c);
			m.put("shop_id", 0L);
			m.put("shop_type", "employee_purchase");
			cartByCartId.put(c.getCartId(), m);
		}

		List<Long> itemIds = new ArrayList<>();
		for (Map<String, Object> row : cartByCartId.values()) {
			long iid = toLong(row.get("item_id"));
			if (iid > 0L) {
				itemIds.add(iid);
			}
		}
		Map<String, Object> skuPack = employeePurchaseItemsSkuListService.loadSkuItemsList(companyId, itemIds);
		int totalCount = toInt(skuPack.get("total_count"));
		if (totalCount <= 0) {
			return emptyBody();
		}
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> skuList = (List<Map<String, Object>>) skuPack.get("list");
		if (skuList == null) {
			return emptyBody();
		}
		Map<Long, Map<String, Object>> itemListByItemId = new LinkedHashMap<>();
		for (Map<String, Object> row : skuList) {
			long iid = toLong(row.get("item_id"));
			if (iid > 0L) {
				itemListByItemId.put(iid, row);
			}
		}

		LambdaQueryWrapper<ActivityItems> aiw = new LambdaQueryWrapper<>();
		aiw.eq(ActivityItems::getCompanyId, companyId)
				.eq(ActivityItems::getActivityId, activityId)
				.in(ActivityItems::getItemId, itemIds);
		List<ActivityItems> activityItemRows = activityItemsMapper.selectList(aiw);
		Map<Long, ActivityItems> activityItemByItemId = new LinkedHashMap<>();
		for (ActivityItems ai : activityItemRows) {
			if (ai.getItemId() != null) {
				activityItemByItemId.put(ai.getItemId(), ai);
			}
		}

		Boolean ifShareStore = activity.getIfShareStore();
		for (Map<String, Object> item : itemListByItemId.values()) {
			long iid = toLong(item.get("item_id"));
			ActivityItems ai = activityItemByItemId.get(iid);
			Object origPrice = item.get("price");
			if (ai != null) {
				item.put("sale_price", origPrice);
				if (ai.getActivityPrice() != null) {
					item.put("price", ai.getActivityPrice());
				}
				if (!Boolean.TRUE.equals(ifShareStore) && ai.getActivityStore() != null) {
					item.put("store", ai.getActivityStore());
				}
			} else {
				item.put("sale_price", origPrice);
				item.put("store", 0);
			}
		}

		Iterator<Map.Entry<Long, Map<String, Object>>> it = cartByCartId.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Long, Map<String, Object>> e = it.next();
			Map<String, Object> cartRow = e.getValue();
			long iid = toLong(cartRow.get("item_id"));
			if (!itemListByItemId.containsKey(iid)) {
				it.remove();
				Long cartId = e.getKey();
				if (cartId != null) {
					cartMapper.deleteById(cartId);
				}
			} else {
				Map<String, Object> item = itemListByItemId.get(iid);
				cartRow.put("price", item.get("price"));
				cartRow.put("sale_price", item.get("sale_price"));
				cartRow.put("item_name", item.get("item_name"));
				cartRow.put("pics", firstPicString(item.get("pics")));
			}
		}

		if (cartByCartId.isEmpty()) {
			return emptyBody();
		}

		List<Map<String, Object>> cartList = new ArrayList<>(cartByCartId.values());
		Map<String, Object> handled =
				employeePurchaseCartHandleValidCartService.handle(companyId, userId, cartList, itemListByItemId);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> validCartRows = (List<Map<String, Object>>) handled.get("valid_cart");
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> invalidCartRows = (List<Map<String, Object>>) handled.get("invalid_cart");

		long cartTotalPrice = 0L;
		long cartTotalNum = 0L;
		long cartTotalCount = 0L;
		for (Map<String, Object> cart : validCartRows) {
			boolean checked = truthyChecked(cart.get("is_checked"));
			cart.put("is_checked", checked);
			if (checked) {
				long price = toLong(cart.get("price"));
				long num = toLong(cart.get("num"));
				cartTotalPrice += price * num;
				cartTotalNum += num;
				cartTotalCount += 1;
			}
		}

		String shopName = "";
		Integer distId = activity.getDistributorId();
		if (distId != null && distId > 0) {
			Distributor d = distributorMapper.selectById(distId.longValue());
			if (d != null && d.getName() != null) {
				shopName = d.getName();
			}
		}

		Map<String, Object> validCart = new LinkedHashMap<>();
		validCart.put("shop_id", distId != null ? distId.longValue() : 0L);
		validCart.put("shop_name", shopName);
		validCart.put("is_ziti", false);
		validCart.put("is_delivery", true);
		validCart.put("item_fee", cartTotalPrice);
		validCart.put("cart_total_price", cartTotalPrice);
		validCart.put("cart_total_num", cartTotalNum);
		validCart.put("cart_total_count", cartTotalCount);
		validCart.put("total_fee", cartTotalPrice);
		validCart.put("list", validCartRows);
		attachItemLimitFields(
				validCartRows, activityItemByItemId, loadItemAggregates(companyId, enterpriseId, activityId, userId, itemIds));

		if (invalidCartRows != null && !invalidCartRows.isEmpty()) {
			List<Long> invalidIds = new ArrayList<>();
			for (Map<String, Object> row : invalidCartRows) {
				long cid = toLong(row.get("cart_id"));
				if (cid > 0L) {
					invalidIds.add(cid);
				}
			}
			if (!invalidIds.isEmpty()) {
				LambdaUpdateWrapper<Cart> uw = new LambdaUpdateWrapper<>();
				uw.set(Cart::getIsChecked, false)
						.in(Cart::getCartId, invalidIds)
						.eq(Cart::getCompanyId, companyId)
						.eq(Cart::getUserId, userId)
						.eq(Cart::getEnterpriseId, enterpriseId)
						.eq(Cart::getActivityId, activityId);
				cartMapper.update(null, uw);
			}
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("invalid_cart", invalidCartRows != null ? invalidCartRows : List.of());
		body.put("valid_cart", List.of(validCart));
		body.put("purchase_mode", activity.getPurchaseMode());
		body.put("purchase_mode_desc", PurchaseModeSupport.desc(activity.getPurchaseMode()));
		return body;
	}

	private Map<Long, MemberActivityItemsAggregate> loadItemAggregates(
			long companyId, long enterpriseId, long activityId, long userId, List<Long> itemIds) {
		LinkedHashMap<Long, MemberActivityItemsAggregate> out = new LinkedHashMap<>();
		if (itemIds == null || itemIds.isEmpty()) {
			return out;
		}
		List<MemberActivityItemsAggregate> rows =
				memberActivityItemsAggregateMapper.selectList(
						new LambdaQueryWrapper<MemberActivityItemsAggregate>()
								.eq(MemberActivityItemsAggregate::getCompanyId, companyId)
								.eq(MemberActivityItemsAggregate::getEnterpriseId, enterpriseId)
								.eq(MemberActivityItemsAggregate::getUserId, userId)
								.eq(MemberActivityItemsAggregate::getActivityId, activityId)
								.in(MemberActivityItemsAggregate::getItemId, itemIds));
		if (rows == null) {
			return out;
		}
		for (MemberActivityItemsAggregate agg : rows) {
			if (agg.getItemId() != null) {
				out.put(agg.getItemId(), agg);
			}
		}
		return out;
	}

	static void attachItemLimitFields(
			List<Map<String, Object>> rows,
			Map<Long, ActivityItems> activityItemByItemId,
			Map<Long, MemberActivityItemsAggregate> aggregateByItemId) {
		if (rows == null) {
			return;
		}
		for (Map<String, Object> row : rows) {
			long iid = toLong(row.get("item_id"));
			if (iid <= 0L) {
				continue;
			}
			ActivityItems act = activityItemByItemId == null ? null : activityItemByItemId.get(iid);
			MemberActivityItemsAggregate agg = aggregateByItemId == null ? null : aggregateByItemId.get(iid);
			row.put("limit_num", act == null || act.getLimitNum() == null ? 0 : act.getLimitNum());
			row.put("limit_fee", act == null || act.getLimitFee() == null ? 0 : act.getLimitFee());
			row.put("aggregate_num", agg == null || agg.getAggregateNum() == null ? 0 : agg.getAggregateNum());
			row.put("aggregate_fee", agg == null || agg.getAggregateFee() == null ? 0 : agg.getAggregateFee());
		}
	}

	private static Map<String, Object> emptyBody() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("invalid_cart", List.of());
		m.put("valid_cart", List.of());
		m.put("purchase_mode", null);
		return m;
	}

	private static Map<String, Object> cartEntityToMap(Cart c) {
		Map<String, Object> m = new LinkedHashMap<>();
		if (c.getCartId() != null) {
			m.put("cart_id", c.getCartId());
		}
		if (c.getCompanyId() != null) {
			m.put("company_id", c.getCompanyId());
		}
		if (c.getEnterpriseId() != null) {
			m.put("enterprise_id", c.getEnterpriseId());
		}
		if (c.getActivityId() != null) {
			m.put("activity_id", c.getActivityId());
		}
		if (c.getUserId() != null) {
			m.put("user_id", c.getUserId());
		}
		if (c.getShopType() != null) {
			m.put("shop_type", c.getShopType());
		}
		if (c.getShopId() != null) {
			m.put("shop_id", c.getShopId());
		}
		if (c.getItemId() != null) {
			m.put("item_id", c.getItemId());
		}
		if (c.getNum() != null) {
			m.put("num", c.getNum());
		}
		if (c.getIsChecked() != null) {
			m.put("is_checked", c.getIsChecked());
		}
		return m;
	}

	private static String firstPicString(Object picsRaw) {
		if (picsRaw == null) {
			return "";
		}
		if (picsRaw instanceof String s) {
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
		return picsRaw.toString();
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
