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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityItems;
import cn.shopex.ecshopx.employeepurchase.domain.Cart;
import cn.shopex.ecshopx.employeepurchase.domain.MemberActivityItemsAggregate;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityItemsMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.CartMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.MemberActivityItemsAggregateMapper;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseItemLimitValidator.ItemAggregate;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseItemLimitValidator.ItemLimit;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseItemLimitValidator.LimitLine;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** 加购 / 改数量 / 立即购买时校验每人限购与限额。对齐 PHP CartService。 */
@Service
public class EmployeePurchaseCartItemLimitService {

	private final CartMapper cartMapper;
	private final ActivityItemsMapper activityItemsMapper;
	private final MemberActivityItemsAggregateMapper memberActivityItemsAggregateMapper;

	public EmployeePurchaseCartItemLimitService(
			CartMapper cartMapper,
			ActivityItemsMapper activityItemsMapper,
			MemberActivityItemsAggregateMapper memberActivityItemsAggregateMapper) {
		this.cartMapper = cartMapper;
		this.activityItemsMapper = activityItemsMapper;
		this.memberActivityItemsAggregateMapper = memberActivityItemsAggregateMapper;
	}

	public void assertFastBuyIntent(
			long companyId, long enterpriseId, long activityId, long userId, long itemId, int num) {
		ActivityItems activityItem =
				activityItemsMapper.selectOne(
						new LambdaQueryWrapper<ActivityItems>()
								.eq(ActivityItems::getCompanyId, companyId)
								.eq(ActivityItems::getActivityId, activityId)
								.eq(ActivityItems::getItemId, itemId)
								.last("LIMIT 1"));
		if (activityItem == null || !isOnShelf(activityItem)) {
			throw new ResourceException(EmployeePurchaseItemLimitValidator.MSG_NOT_IN_ACTIVITY);
		}
		int price = activityItem.getActivityPrice() == null ? 0 : activityItem.getActivityPrice();
		List<LimitLine> lines = List.of(new LimitLine(itemId, num, price * num));
		EmployeePurchaseItemLimitValidator.assertWithPreloadedData(
				Map.of(itemId, toItemLimit(activityItem)),
				loadAggregates(companyId, enterpriseId, activityId, userId, List.of(itemId)),
				lines);
	}

	public void assertDbCartIntent(
			long companyId,
			long enterpriseId,
			long activityId,
			long userId,
			long targetItemId,
			int targetFinalNum,
			boolean targetWillBeChecked) {
		if (targetFinalNum <= 0 || !targetWillBeChecked) {
			return;
		}
		List<Cart> rows =
				cartMapper.selectList(
						new LambdaQueryWrapper<Cart>()
								.eq(Cart::getCompanyId, companyId)
								.eq(Cart::getEnterpriseId, enterpriseId)
								.eq(Cart::getActivityId, activityId)
								.eq(Cart::getUserId, userId)
								.orderByAsc(Cart::getCartId));
		if (rows == null) {
			rows = List.of();
		}
		List<RawLine> rawLines = new ArrayList<>();
		boolean hasCheckedTarget = false;
		for (Cart row : rows) {
			if (!Boolean.TRUE.equals(row.getIsChecked())) {
				continue;
			}
			long iid = row.getItemId() == null ? 0L : row.getItemId();
			if (iid <= 0L) {
				continue;
			}
			int num = iid == targetItemId ? targetFinalNum : (row.getNum() == null ? 0 : row.getNum().intValue());
			if (iid == targetItemId) {
				hasCheckedTarget = true;
			}
			if (num <= 0) {
				continue;
			}
			long cartId = row.getCartId() == null ? 0L : row.getCartId();
			rawLines.add(new RawLine(iid, num, cartId));
		}
		if (!hasCheckedTarget) {
			rawLines.add(new RawLine(targetItemId, targetFinalNum, Long.MAX_VALUE));
		}
		rawLines.sort((a, b) -> Long.compare(a.cartId(), b.cartId()));
		List<Long> itemIds = new ArrayList<>();
		for (RawLine row : rawLines) {
			if (!itemIds.contains(row.itemId())) {
				itemIds.add(row.itemId());
			}
		}
		Map<Long, ActivityItems> activityItemByItemId =
				loadActivityItems(companyId, activityId, itemIds);
		Map<Long, ItemLimit> limits = new LinkedHashMap<>();
		List<LimitLine> lines = new ArrayList<>();
		for (RawLine row : rawLines) {
			ActivityItems ai = activityItemByItemId.get(row.itemId());
			if (ai == null || !isOnShelf(ai)) {
				throw new ResourceException(EmployeePurchaseItemLimitValidator.MSG_NOT_IN_ACTIVITY);
			}
			int price = ai.getActivityPrice() == null ? 0 : ai.getActivityPrice();
			limits.put(row.itemId(), toItemLimit(ai));
			lines.add(new LimitLine(row.itemId(), row.num(), price * row.num()));
		}
		EmployeePurchaseItemLimitValidator.assertWithPreloadedData(
				limits,
				loadAggregates(companyId, enterpriseId, activityId, userId, itemIds),
				lines);
	}

	private Map<Long, ActivityItems> loadActivityItems(long companyId, long activityId, List<Long> itemIds) {
		LinkedHashMap<Long, ActivityItems> out = new LinkedHashMap<>();
		if (itemIds == null || itemIds.isEmpty()) {
			return out;
		}
		List<ActivityItems> rows =
				activityItemsMapper.selectList(
						new LambdaQueryWrapper<ActivityItems>()
								.eq(ActivityItems::getCompanyId, companyId)
								.eq(ActivityItems::getActivityId, activityId)
								.in(ActivityItems::getItemId, itemIds));
		if (rows == null) {
			return out;
		}
		for (ActivityItems ai : rows) {
			if (ai.getItemId() != null) {
				out.put(ai.getItemId(), ai);
			}
		}
		return out;
	}

	private Map<Long, ItemAggregate> loadAggregates(
			long companyId, long enterpriseId, long activityId, long userId, List<Long> itemIds) {
		LinkedHashMap<Long, ItemAggregate> out = new LinkedHashMap<>();
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
			if (agg.getItemId() == null) {
				continue;
			}
			out.put(
					agg.getItemId(),
					new ItemAggregate(
							agg.getAggregateNum() == null ? 0 : agg.getAggregateNum(),
							agg.getAggregateFee() == null ? 0 : agg.getAggregateFee()));
		}
		return out;
	}

	public static boolean isOnShelf(ActivityItems activityItem) {
		if (activityItem == null) {
			return false;
		}
		Integer shelf = activityItem.getShelfStatus();
		return shelf == null || shelf == 1;
	}

	public static ItemLimit toItemLimit(ActivityItems activityItem) {
		return new ItemLimit(
				activityItem.getLimitNum() == null ? 0 : activityItem.getLimitNum(),
				activityItem.getLimitFee() == null ? 0 : activityItem.getLimitFee());
	}

	private record RawLine(long itemId, int num, long cartId) {}
}
