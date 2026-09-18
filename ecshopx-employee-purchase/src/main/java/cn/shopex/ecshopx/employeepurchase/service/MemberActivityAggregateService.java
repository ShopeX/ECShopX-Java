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
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityPassphraseEnterprise;
import cn.shopex.ecshopx.employeepurchase.domain.Employees;
import cn.shopex.ecshopx.employeepurchase.domain.MemberActivityAggregate;
import cn.shopex.ecshopx.employeepurchase.domain.Relatives;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.MemberActivityAggregateMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.RelativesMapper;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.ActivityPassphraseService;
import cn.shopex.ecshopx.employeepurchase.support.PurchaseModeSupport;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/** 对齐 PHP EmployeePurchaseBundle/Services/MemberActivityAggregateService。 */
@Service
public class MemberActivityAggregateService {

	private final ActivitiesMapper activitiesMapper;
	private final EmployeesMapper employeesMapper;
	private final RelativesMapper relativesMapper;
	private final MemberActivityAggregateMapper memberActivityAggregateMapper;
	private final ActivityPassphraseService activityPassphraseService;
	private final EmployeePurchaseAggregateRedisLockService aggregateRedisLockService;
	private final EnterpriseConfigService enterpriseConfigService;

	public MemberActivityAggregateService(
			ActivitiesMapper activitiesMapper,
			EmployeesMapper employeesMapper,
			RelativesMapper relativesMapper,
			MemberActivityAggregateMapper memberActivityAggregateMapper,
			ActivityPassphraseService activityPassphraseService,
			EmployeePurchaseAggregateRedisLockService aggregateRedisLockService,
			EnterpriseConfigService enterpriseConfigService) {
		this.activitiesMapper = activitiesMapper;
		this.employeesMapper = employeesMapper;
		this.relativesMapper = relativesMapper;
		this.memberActivityAggregateMapper = memberActivityAggregateMapper;
		this.activityPassphraseService = activityPassphraseService;
		this.aggregateRedisLockService = aggregateRedisLockService;
		this.enterpriseConfigService = enterpriseConfigService;
	}

	public void addAggregateFee(long companyId, long enterpriseId, long activityId, long userId, int fee) {
		if (fee <= 0) {
			return;
		}
		Activities activity = loadActivityOrThrow(companyId, activityId);
		assertEnterpriseParticipates(activity, enterpriseId);

		if (activityPassphraseService.isPassphraseEnabled(activity)) {
			String key = aggregateFeeLockKeyPassphrase(companyId, enterpriseId, activityId, userId);
			aggregateRedisLockService.withLock(
					key,
					() -> addPassphraseAggregateFee(companyId, enterpriseId, activityId, userId, fee, activity));
			return;
		}

		Employees employee = findActiveEmployee(companyId, enterpriseId, userId);
		long lockUserId;
		if (employee != null) {
			lockUserId = employee.getUserId();
		} else {
			Relatives relative = findActiveRelative(companyId, enterpriseId, activityId, userId);
			if (relative == null) {
				throw new ResourceException("既不是员工也不是亲友");
			}
			lockUserId = relative.getEmployeeUserId();
		}

		String key =
				Boolean.TRUE.equals(activity.getIfShareLimitfee())
						? aggregateFeeLockKey(companyId, enterpriseId, activityId, lockUserId)
						: aggregateFeeLockKey(companyId, enterpriseId, activityId, userId);
		aggregateRedisLockService.withLock(
				key,
				() -> addNormalAggregateFee(companyId, enterpriseId, activityId, userId, fee, activity, employee));
	}

	public void minusAggregateFee(long companyId, long enterpriseId, long activityId, long userId, int fee) {
		if (fee <= 0) {
			return;
		}
		// 与 addAggregateFee 同一把锁（含共享额度时的员工维度），避免下单/取消并发交错
		String key = resolveAggregateFeeLockKey(companyId, enterpriseId, activityId, userId);
		aggregateRedisLockService.withLock(
				key,
				() -> {
					MemberActivityAggregate aggregateInfo =
							memberActivityAggregateMapper.selectOne(
									Wrappers.<MemberActivityAggregate>lambdaQuery()
											.eq(MemberActivityAggregate::getCompanyId, companyId)
											.eq(MemberActivityAggregate::getEnterpriseId, enterpriseId)
											.eq(MemberActivityAggregate::getActivityId, activityId)
											.eq(MemberActivityAggregate::getUserId, userId)
											.last("LIMIT 1"));
					int prev =
							aggregateInfo == null || aggregateInfo.getAggregateFee() == null
									? 0
									: aggregateInfo.getAggregateFee();
					if (aggregateInfo == null || prev < fee) {
						throw new ResourceException("额度返还失败");
					}
					int now = (int) (System.currentTimeMillis() / 1000L);
					MemberActivityAggregate update = new MemberActivityAggregate();
					update.setId(aggregateInfo.getId());
					update.setAggregateFee(prev - fee);
					update.setUpdated(now);
					memberActivityAggregateMapper.updateById(update);
				});
	}

	/**
	 * 解析活动金额累计锁 key，须与 {@link #addAggregateFee} 一致。活动已删时回退为按 userId，保证取消仍能还额度。
	 */
	private String resolveAggregateFeeLockKey(
			long companyId, long enterpriseId, long activityId, long userId) {
		Activities activity =
				activitiesMapper.selectOne(
						Wrappers.<Activities>lambdaQuery()
								.eq(Activities::getCompanyId, companyId)
								.eq(Activities::getId, activityId)
								.last("LIMIT 1"));
		if (activity == null) {
			return aggregateFeeLockKey(companyId, enterpriseId, activityId, userId);
		}
		if (activityPassphraseService.isPassphraseEnabled(activity)) {
			return aggregateFeeLockKeyPassphrase(companyId, enterpriseId, activityId, userId);
		}
		if (Boolean.TRUE.equals(activity.getIfShareLimitfee())) {
			Employees employee = findActiveEmployee(companyId, enterpriseId, userId);
			long lockUserId;
			if (employee != null && employee.getUserId() != null) {
				lockUserId = employee.getUserId();
			} else {
				Relatives relative = findActiveRelative(companyId, enterpriseId, activityId, userId);
				if (relative != null && relative.getEmployeeUserId() != null) {
					lockUserId = relative.getEmployeeUserId();
				} else {
					lockUserId = userId;
				}
			}
			return aggregateFeeLockKey(companyId, enterpriseId, activityId, lockUserId);
		}
		return aggregateFeeLockKey(companyId, enterpriseId, activityId, userId);
	}

	private static String aggregateFeeLockKey(
			long companyId, long enterpriseId, long activityId, long lockUserId) {
		return "aggregateFee_" + companyId + "_" + enterpriseId + "_" + activityId + "_" + lockUserId;
	}

	private static String aggregateFeeLockKeyPassphrase(
			long companyId, long enterpriseId, long activityId, long userId) {
		return "aggregateFee_passphrase_"
				+ companyId
				+ "_"
				+ enterpriseId
				+ "_"
				+ activityId
				+ "_"
				+ userId;
	}

	private void addPassphraseAggregateFee(
			long companyId,
			long enterpriseId,
			long activityId,
			long userId,
			int fee,
			Activities activity) {
		int limitFen;
		if (PurchaseModeSupport.isNewContractActivity(activity)) {
			limitFen = enterpriseConfigService.requirePerCapitaLimitfee(companyId, activityId, enterpriseId);
		} else {
			ActivityPassphraseEnterprise cfg =
					activityPassphraseService.requireEnterpriseConfig(companyId, activityId, enterpriseId);
			limitFen = cfg == null || cfg.getPassphraseLimitfee() == null ? 0 : cfg.getPassphraseLimitfee();
		}
		long aggregateFee = sumAggregateFee(companyId, enterpriseId, activityId, List.of(userId));
		if (aggregateFee + fee > limitFen) {
			throw exceedLimitException(activity, "超过个人口令通道额度");
		}
		upsertAggregateFee(companyId, enterpriseId, activityId, userId, fee);
	}

	private void addNormalAggregateFee(
			long companyId,
			long enterpriseId,
			long activityId,
			long userId,
			int fee,
			Activities activity,
			Employees employee) {
		if (Boolean.TRUE.equals(activity.getIfShareLimitfee())) {
			long employeeUserId =
					employee != null
							? employee.getUserId()
							: findActiveRelative(companyId, enterpriseId, activityId, userId)
									.getEmployeeUserId();
			List<Long> userIds = listRelativeUserIdsForShare(companyId, enterpriseId, activityId, employeeUserId);
			Set<Long> distinct = new LinkedHashSet<>(userIds);
			distinct.add(employeeUserId);
			long aggregateFee = sumAggregateFee(companyId, enterpriseId, activityId, new ArrayList<>(distinct));
			int limitFee = resolveEmployeeLimitFee(activity, companyId, activityId, enterpriseId);
			if (aggregateFee + fee > limitFee) {
				throw exceedLimitException(activity, "超过共享额度");
			}
		} else {
			long aggregateFee = sumAggregateFee(companyId, enterpriseId, activityId, List.of(userId));
			if (employee != null) {
				int limitFee = resolveEmployeeLimitFee(activity, companyId, activityId, enterpriseId);
				if (aggregateFee + fee > limitFee) {
					throw exceedLimitException(activity, "超过员工额度");
				}
			} else {
				int limitFee = activity.getRelativeLimitfee() == null ? 0 : activity.getRelativeLimitfee();
				if (aggregateFee + fee > limitFee) {
					throw new ResourceException("超过家属额度");
				}
			}
		}
		upsertAggregateFee(companyId, enterpriseId, activityId, userId, fee);
	}

	private int resolveEmployeeLimitFee(
			Activities activity, long companyId, long activityId, long enterpriseId) {
		if (PurchaseModeSupport.isNewContractActivity(activity)) {
			return enterpriseConfigService.requirePerCapitaLimitfee(companyId, activityId, enterpriseId);
		}
		return activity.getEmployeeLimitfee() == null ? 0 : activity.getEmployeeLimitfee();
	}

	private static ResourceException exceedLimitException(Activities activity, String cashMessage) {
		if (PurchaseModeSupport.isPrepaidPoint(activity)) {
			return new ResourceException("预充点数不足");
		}
		return new ResourceException(cashMessage);
	}

	private void upsertAggregateFee(
			long companyId, long enterpriseId, long activityId, long userId, int fee) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		MemberActivityAggregate aggregateInfo =
				memberActivityAggregateMapper.selectOne(
						Wrappers.<MemberActivityAggregate>lambdaQuery()
								.eq(MemberActivityAggregate::getCompanyId, companyId)
								.eq(MemberActivityAggregate::getEnterpriseId, enterpriseId)
								.eq(MemberActivityAggregate::getActivityId, activityId)
								.eq(MemberActivityAggregate::getUserId, userId)
								.last("LIMIT 1"));
		if (aggregateInfo == null) {
			MemberActivityAggregate row = new MemberActivityAggregate();
			row.setCompanyId(companyId);
			row.setEnterpriseId(enterpriseId);
			row.setActivityId(activityId);
			row.setUserId(userId);
			row.setAggregateFee(fee);
			row.setCreated(now);
			row.setUpdated(now);
			memberActivityAggregateMapper.insert(row);
			return;
		}
		int prev = aggregateInfo.getAggregateFee() == null ? 0 : aggregateInfo.getAggregateFee();
		MemberActivityAggregate update = new MemberActivityAggregate();
		update.setId(aggregateInfo.getId());
		update.setAggregateFee(prev + fee);
		update.setUpdated(now);
		memberActivityAggregateMapper.updateById(update);
	}

	private long sumAggregateFee(long companyId, long enterpriseId, long activityId, List<Long> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return 0L;
		}
		Long v =
				memberActivityAggregateMapper.sumAggregateFeeByUserIds(
						companyId, enterpriseId, activityId, userIds);
		return v == null ? 0L : v.longValue();
	}

	private Activities loadActivityOrThrow(long companyId, long activityId) {
		Activities row =
				activitiesMapper.selectOne(
						Wrappers.<Activities>lambdaQuery()
								.eq(Activities::getCompanyId, companyId)
								.eq(Activities::getId, activityId)
								.last("LIMIT 1"));
		if (row == null) {
			throw new ResourceException("活动不存在");
		}
		return row;
	}

	private void assertEnterpriseParticipates(Activities activity, long enterpriseId) {
		String csv = activity.getEnterpriseId();
		if (csv == null || csv.isBlank()) {
			throw new ResourceException("企业不参与该活动");
		}
		for (String part : csv.split(",")) {
			String p = part.trim();
			if (p.isEmpty()) {
				continue;
			}
			try {
				if (Long.parseLong(p) == enterpriseId) {
					return;
				}
			} catch (NumberFormatException ignored) {
			}
		}
		throw new ResourceException("企业不参与该活动");
	}

	private Employees findActiveEmployee(long companyId, long enterpriseId, long userId) {
		return employeesMapper.selectOne(
				Wrappers.<Employees>lambdaQuery()
						.eq(Employees::getCompanyId, companyId)
						.eq(Employees::getEnterpriseId, enterpriseId)
						.eq(Employees::getUserId, userId)
						.and(
								w ->
										w.eq(Employees::getDisabled, false)
												.or()
												.isNull(Employees::getDisabled))
						.last("LIMIT 1"));
	}

	private Relatives findActiveRelative(long companyId, long enterpriseId, long activityId, long userId) {
		return relativesMapper.selectOne(
				Wrappers.<Relatives>lambdaQuery()
						.eq(Relatives::getCompanyId, companyId)
						.eq(Relatives::getEnterpriseId, enterpriseId)
						.eq(Relatives::getActivityId, activityId)
						.eq(Relatives::getUserId, userId)
						.and(
								w ->
										w.eq(Relatives::getDisabled, false)
												.or()
												.isNull(Relatives::getDisabled))
						.last("LIMIT 1"));
	}

	private List<Long> listRelativeUserIdsForShare(
			long companyId, long enterpriseId, long activityId, long employeeUserId) {
		List<Relatives> rows =
				relativesMapper.selectList(
						Wrappers.<Relatives>lambdaQuery()
								.eq(Relatives::getCompanyId, companyId)
								.eq(Relatives::getEnterpriseId, enterpriseId)
								.eq(Relatives::getActivityId, activityId)
								.eq(Relatives::getEmployeeUserId, employeeUserId)
								.and(
										w ->
												w.eq(Relatives::getDisabled, false)
														.or()
														.isNull(Relatives::getDisabled)));
		List<Long> out = new ArrayList<>();
		for (Relatives r : rows) {
			if (r.getUserId() != null) {
				out.add(r.getUserId());
			}
		}
		return out;
	}
}
