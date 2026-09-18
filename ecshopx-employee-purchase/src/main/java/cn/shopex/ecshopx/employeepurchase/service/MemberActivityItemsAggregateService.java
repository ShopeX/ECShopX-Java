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
import cn.shopex.ecshopx.employeepurchase.domain.MemberActivityItemsAggregate;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityItemsMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.MemberActivityItemsAggregateMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** 对齐 PHP EmployeePurchaseBundle/Services/MemberActivityItemsAggregateService。 */
@Service
public class MemberActivityItemsAggregateService {

	public static final String MSG_FEE = EmployeePurchaseItemLimitValidator.MSG_FEE;
	public static final String MSG_NUM = EmployeePurchaseItemLimitValidator.MSG_NUM;

	private final ActivityItemsMapper activityItemsMapper;
	private final MemberActivityItemsAggregateMapper memberActivityItemsAggregateMapper;
	private final EmployeePurchaseAggregateRedisLockService aggregateRedisLockService;

	public MemberActivityItemsAggregateService(
			ActivityItemsMapper activityItemsMapper,
			MemberActivityItemsAggregateMapper memberActivityItemsAggregateMapper,
			EmployeePurchaseAggregateRedisLockService aggregateRedisLockService) {
		this.activityItemsMapper = activityItemsMapper;
		this.memberActivityItemsAggregateMapper = memberActivityItemsAggregateMapper;
		this.aggregateRedisLockService = aggregateRedisLockService;
	}

	public void addItemAggregate(
			long companyId, long enterpriseId, long activityId, long userId, long itemId, int fee, int num) {
		if (fee <= 0 && num <= 0) {
			return;
		}
		ActivityItems activityItem =
				activityItemsMapper.selectOne(
						Wrappers.<ActivityItems>lambdaQuery()
								.eq(ActivityItems::getCompanyId, companyId)
								.eq(ActivityItems::getActivityId, activityId)
								.eq(ActivityItems::getItemId, itemId)
								.last("LIMIT 1"));
		if (activityItem == null) {
			throw new ResourceException("商品不参与活动");
		}

		// 扣/还共用同一把锁，避免同用户下单与取消并发交错改累计
		String key = itemAggregateLockKey(companyId, enterpriseId, activityId, userId);
		aggregateRedisLockService.withLock(
				key,
				() ->
						addItemAggregateUnderLock(
								companyId,
								enterpriseId,
								activityId,
								userId,
								itemId,
								fee,
								num,
								activityItem));
	}

	public void minusItemAggregate(
			long companyId, long enterpriseId, long activityId, long userId, long itemId, int fee, int num) {
		if (fee <= 0 && num <= 0) {
			return;
		}
		String key = itemAggregateLockKey(companyId, enterpriseId, activityId, userId);
		aggregateRedisLockService.withLock(
				key,
				() -> {
					MemberActivityItemsAggregate aggregateInfo =
							memberActivityItemsAggregateMapper.selectOne(
									Wrappers.<MemberActivityItemsAggregate>lambdaQuery()
											.eq(MemberActivityItemsAggregate::getCompanyId, companyId)
											.eq(MemberActivityItemsAggregate::getEnterpriseId, enterpriseId)
											.eq(MemberActivityItemsAggregate::getActivityId, activityId)
											.eq(MemberActivityItemsAggregate::getUserId, userId)
											.eq(MemberActivityItemsAggregate::getItemId, itemId)
											.last("LIMIT 1"));
					int prevFee =
							aggregateInfo == null || aggregateInfo.getAggregateFee() == null
									? 0
									: aggregateInfo.getAggregateFee();
					int prevNum =
							aggregateInfo == null || aggregateInfo.getAggregateNum() == null
									? 0
									: aggregateInfo.getAggregateNum();
					if (aggregateInfo == null || prevFee < fee || prevNum < num) {
						throw new ResourceException("商品限额返还失败");
					}
					int now = (int) (System.currentTimeMillis() / 1000L);
					MemberActivityItemsAggregate update = new MemberActivityItemsAggregate();
					update.setId(aggregateInfo.getId());
					update.setAggregateFee(prevFee - fee);
					update.setAggregateNum(prevNum - num);
					update.setUpdated(now);
					memberActivityItemsAggregateMapper.updateById(update);
				});
	}

	private void addItemAggregateUnderLock(
			long companyId,
			long enterpriseId,
			long activityId,
			long userId,
			long itemId,
			int fee,
			int num,
			ActivityItems activityItem) {
		int now = (int) (System.currentTimeMillis() / 1000L);

		MemberActivityItemsAggregate aggregateInfo =
				memberActivityItemsAggregateMapper.selectOne(
						Wrappers.<MemberActivityItemsAggregate>lambdaQuery()
								.eq(MemberActivityItemsAggregate::getCompanyId, companyId)
								.eq(MemberActivityItemsAggregate::getEnterpriseId, enterpriseId)
								.eq(MemberActivityItemsAggregate::getActivityId, activityId)
								.eq(MemberActivityItemsAggregate::getUserId, userId)
								.eq(MemberActivityItemsAggregate::getItemId, itemId)
								.last("LIMIT 1"));
		if (aggregateInfo == null) {
			EmployeePurchaseItemLimitValidator.assertWithPreloadedData(
					Map.of(itemId, EmployeePurchaseCartItemLimitService.toItemLimit(activityItem)),
					Map.of(),
					List.of(new EmployeePurchaseItemLimitValidator.LimitLine(itemId, num, fee)));
			MemberActivityItemsAggregate row = new MemberActivityItemsAggregate();
			row.setCompanyId(companyId);
			row.setEnterpriseId(enterpriseId);
			row.setActivityId(activityId);
			row.setUserId(userId);
			row.setItemId(itemId);
			row.setAggregateFee(fee);
			row.setAggregateNum(num);
			row.setCreated(now);
			row.setUpdated(now);
			memberActivityItemsAggregateMapper.insert(row);
			return;
		}

		int prevFee = aggregateInfo.getAggregateFee() == null ? 0 : aggregateInfo.getAggregateFee();
		int prevNum = aggregateInfo.getAggregateNum() == null ? 0 : aggregateInfo.getAggregateNum();
		int newFee = prevFee + fee;
		int newNum = prevNum + num;
		EmployeePurchaseItemLimitValidator.assertWithPreloadedData(
				Map.of(itemId, EmployeePurchaseCartItemLimitService.toItemLimit(activityItem)),
				Map.of(itemId, new EmployeePurchaseItemLimitValidator.ItemAggregate(prevNum, prevFee)),
				List.of(new EmployeePurchaseItemLimitValidator.LimitLine(itemId, num, fee)));

		MemberActivityItemsAggregate update = new MemberActivityItemsAggregate();
		update.setId(aggregateInfo.getId());
		update.setAggregateFee(newFee);
		update.setAggregateNum(newNum);
		update.setUpdated(now);
		memberActivityItemsAggregateMapper.updateById(update);
	}

	/** 商品累计扣减与返还必须同 key，否则下单/取消并发会交错读改写。 */
	private static String itemAggregateLockKey(
			long companyId, long enterpriseId, long activityId, long userId) {
		return "itemAggregate_" + companyId + "_" + enterpriseId + "_" + activityId + "_" + userId;
	}
}
