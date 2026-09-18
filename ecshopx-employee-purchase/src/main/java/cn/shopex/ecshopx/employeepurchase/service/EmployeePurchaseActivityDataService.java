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
import cn.shopex.ecshopx.employeepurchase.domain.Relatives;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.MemberActivityAggregateMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.RelativesMapper;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.ActivityPassphraseService;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.EmployeePassphraseService;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.PassphraseVerifiedRedisService;
import cn.shopex.ecshopx.employeepurchase.support.PurchaseModeSupport;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class EmployeePurchaseActivityDataService {

	private final ActivitiesMapper activitiesMapper;
	private final EmployeesMapper employeesMapper;
	private final RelativesMapper relativesMapper;
	private final MemberActivityAggregateMapper memberActivityAggregateMapper;
	private final ActivityPassphraseService activityPassphraseService;
	private final PassphraseVerifiedRedisService passphraseVerifiedRedisService;
	private final EmployeePassphraseService employeePassphraseService;
	private final EnterpriseConfigService enterpriseConfigService;

	public EmployeePurchaseActivityDataService(
			ActivitiesMapper activitiesMapper,
			EmployeesMapper employeesMapper,
			RelativesMapper relativesMapper,
			MemberActivityAggregateMapper memberActivityAggregateMapper,
			ActivityPassphraseService activityPassphraseService,
			PassphraseVerifiedRedisService passphraseVerifiedRedisService,
			EmployeePassphraseService employeePassphraseService,
			EnterpriseConfigService enterpriseConfigService) {
		this.activitiesMapper = activitiesMapper;
		this.employeesMapper = employeesMapper;
		this.relativesMapper = relativesMapper;
		this.memberActivityAggregateMapper = memberActivityAggregateMapper;
		this.activityPassphraseService = activityPassphraseService;
		this.passphraseVerifiedRedisService = passphraseVerifiedRedisService;
		this.employeePassphraseService = employeePassphraseService;
		this.enterpriseConfigService = enterpriseConfigService;
	}

	public void assertActivityExistsAndEnterpriseParticipates(long companyId, long activityId, long enterpriseId) {
		requireActivityWithEnterpriseOrThrow(companyId, activityId, enterpriseId);
	}

	public Activities requireActivityWithEnterpriseOrThrow(long companyId, long activityId, long enterpriseId) {
		Activities activity = loadActivityOrThrow(companyId, activityId);
		if (!enterpriseParticipates(activity, enterpriseId)) {
			throw new ResourceException("企业不参与该活动");
		}
		return activity;
	}

	public List<Long> listEnterpriseIdsFromCsv(String csv) {
		return new ArrayList<>(parseEnterpriseIdCsv(csv));
	}

	public Map<String, Object> getAggregateFeeForInvitee(
			long companyId, long enterpriseId, long activityId, long targetUserId) {
		Activities activity = loadActivityOrThrow(companyId, activityId);
		if (activityPassphraseService.isPassphraseEnabled(activity)) {
			int limitFee = resolvePassphraseLimitFee(activity, companyId, activityId, enterpriseId);
			return getPassphraseAggregateFee(activity, companyId, enterpriseId, activityId, targetUserId, limitFee);
		}
		return computeAggregateFee(activity, companyId, enterpriseId, activityId, targetUserId);
	}

	public Map<String, Object> getPassphraseAggregateFee(
			Activities activity,
			long companyId,
			long enterpriseId,
			long activityId,
			long userId,
			int passphraseLimitFee) {
		long aggregateFee = sumAggregateFee(companyId, enterpriseId, activityId, List.of(userId));
		int leftFee = passphraseLimitFee - safeIntFromAggregate(aggregateFee);
		return feeTriple(passphraseLimitFee, aggregateFee, leftFee);
	}

	public Map<String, Object> buildActivityData(
			long companyId, long userId, long activityId, long enterpriseId, String memberMobile) {
		Activities activity = loadActivityOrThrow(companyId, activityId);
		if (!enterpriseParticipates(activity, enterpriseId)) {
			throw new ResourceException("企业不参与该活动");
		}

		String mobile = memberMobile == null ? "" : memberMobile;
		employeePassphraseService.ensurePassphraseEmployeeFromVerifiedActivity(
				activity, companyId, activityId, enterpriseId, userId, mobile);

		Map<String, Object> feePart;
		if (activityPassphraseService.isPassphraseEnabled(activity)) {
			int limitFee = resolvePassphraseLimitFee(activity, companyId, activityId, enterpriseId);
			feePart = getPassphraseAggregateFee(activity, companyId, enterpriseId, activityId, userId, limitFee);
		} else {
			feePart = computeAggregateFee(activity, companyId, enterpriseId, activityId, userId);
		}

		LinkedHashMap<String, Object> result = new LinkedHashMap<>(feePart);
		result.put("activity_id", activity.getId());
		result.put("name", activity.getName());
		result.put("title", activity.getTitle());
		result.put("pic", activity.getPic());
		result.put("share_pic", activity.getSharePic());
		result.put("purchase_mode", activity.getPurchaseMode());
		result.put("purchase_mode_desc", PurchaseModeSupport.desc(activity.getPurchaseMode()));
		result.put(
				"if_relative_join",
				PurchaseModeSupport.isPrepaidPoint(activity)
						? 0
						: (Boolean.TRUE.equals(activity.getIfRelativeJoin()) ? 1 : 0));
		result.put("invite_limit", activity.getInviteLimit() != null ? activity.getInviteLimit() : 0);
		result.put("relative_begin_time", activity.getRelativeBeginTime());
		result.put("relative_end_time", activity.getRelativeEndTime());

		Map<String, Object> passphrasePart =
				activityPassphraseService.getPassphraseClientSummary(activity, companyId, activityId, enterpriseId);
		result.putAll(passphrasePart);
		int verified =
				passphraseVerifiedRedisService.isVerified(companyId, activityId, enterpriseId, userId) ? 1 : 0;
		result.put("passphrase_user_verified", verified);

		Employees employee2 = findActiveEmployee(companyId, enterpriseId, userId);
		result.put("is_employee", 0);
		result.put("is_relative", 0);

		Relatives relative2 = null;
		if (employee2 != null) {
			result.put("is_employee", 1);
			result.put("enterprise_id", employee2.getEnterpriseId());
			result.put(
					"invited_num",
					countInvitedRelatives(companyId, enterpriseId, activityId, userId));
		} else {
			relative2 = findActiveRelative(companyId, enterpriseId, activityId, userId);
			if (relative2 != null) {
				result.put("is_relative", 1);
				result.put("enterprise_id", relative2.getEnterpriseId());
			}
		}

		return result;
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

	private boolean enterpriseParticipates(Activities activity, long enterpriseId) {
		List<Long> ids = parseEnterpriseIdCsv(activity.getEnterpriseId());
		for (Long id : ids) {
			if (id != null && id.longValue() == enterpriseId) {
				return true;
			}
		}
		return false;
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

	public boolean isActiveRelative(long companyId, long enterpriseId, long activityId, long userId) {
		return findActiveRelative(companyId, enterpriseId, activityId, userId) != null;
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

	private Map<String, Object> computeAggregateFee(
			Activities activity,
			long companyId,
			long enterpriseId,
			long activityId,
			long userId) {
		Objects.requireNonNull(activity, "activity");
		activity = loadActivityOrThrow(companyId, activityId);
		if (!enterpriseParticipates(activity, enterpriseId)) {
			throw new ResourceException("企业不参与该活动");
		}

		Employees employee = findActiveEmployee(companyId, enterpriseId, userId);
		if (employee != null) {
			long employeeUserId = employee.getUserId();
			if (Boolean.TRUE.equals(activity.getIfShareLimitfee())) {
				List<Long> userIds = listRelativeUserIdsForShare(companyId, enterpriseId, activityId, employeeUserId);
				Set<Long> distinct = new LinkedHashSet<>(userIds);
				distinct.add(employeeUserId);
				List<Long> sumIds = new ArrayList<>(distinct);
				long aggregateFee = sumAggregateFee(companyId, enterpriseId, activityId, sumIds);
				int limitFee = resolveEmployeeLimitFee(activity, companyId, activityId, enterpriseId);
				int leftFee = limitFee - safeIntFromAggregate(aggregateFee);
				return feeTriple(limitFee, aggregateFee, leftFee);
			}
			long aggregateFee = sumAggregateFee(companyId, enterpriseId, activityId, List.of(userId));
			int limitFee = resolveEmployeeLimitFee(activity, companyId, activityId, enterpriseId);
			int leftFee = limitFee - safeIntFromAggregate(aggregateFee);
			return feeTriple(limitFee, aggregateFee, leftFee);
		}

		Relatives relative = findActiveRelative(companyId, enterpriseId, activityId, userId);
		if (relative != null) {
			long employeeUserId = relative.getEmployeeUserId();
			if (Boolean.TRUE.equals(activity.getIfShareLimitfee())) {
				List<Long> userIds = listRelativeUserIdsForShare(companyId, enterpriseId, activityId, employeeUserId);
				Set<Long> distinct = new LinkedHashSet<>(userIds);
				distinct.add(employeeUserId);
				List<Long> sumIds = new ArrayList<>(distinct);
				long aggregateFee = sumAggregateFee(companyId, enterpriseId, activityId, sumIds);
				int limitFee = resolveEmployeeLimitFee(activity, companyId, activityId, enterpriseId);
				int leftFee = limitFee - safeIntFromAggregate(aggregateFee);
				return feeTriple(limitFee, aggregateFee, leftFee);
			}
			long aggregateFee = sumAggregateFee(companyId, enterpriseId, activityId, List.of(userId));
			int limitFee = activity.getRelativeLimitfee() == null ? 0 : activity.getRelativeLimitfee();
			int leftFee = limitFee - safeIntFromAggregate(aggregateFee);
			return feeTriple(limitFee, aggregateFee, leftFee);
		}

		return feeTriple(0, 0L, 0);
	}

	private int resolveEmployeeLimitFee(
			Activities activity, long companyId, long activityId, long enterpriseId) {
		if (PurchaseModeSupport.isNewContractActivity(activity)) {
			return enterpriseConfigService.requirePerCapitaLimitfee(companyId, activityId, enterpriseId);
		}
		return activity.getEmployeeLimitfee() == null ? 0 : activity.getEmployeeLimitfee();
	}

	private int resolvePassphraseLimitFee(
			Activities activity, long companyId, long activityId, long enterpriseId) {
		if (PurchaseModeSupport.isNewContractActivity(activity)) {
			return enterpriseConfigService.requirePerCapitaLimitfee(companyId, activityId, enterpriseId);
		}
		ActivityPassphraseEnterprise cfg =
				activityPassphraseService.requireEnterpriseConfig(companyId, activityId, enterpriseId);
		return cfg == null || cfg.getPassphraseLimitfee() == null ? 0 : cfg.getPassphraseLimitfee();
	}

	private static Map<String, Object> feeTriple(int limitFee, long aggregateFee, int leftFee) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("limit_fee", limitFee);
		m.put("aggregate_fee", aggregateFee);
		m.put("left_fee", leftFee);
		return m;
	}

	private static int safeIntFromAggregate(long aggregateFee) {
		if (aggregateFee <= 0L) {
			return 0;
		}
		if (aggregateFee > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		return (int) aggregateFee;
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

	private int countInvitedRelatives(
			long companyId, long enterpriseId, long activityId, long employeeUserId) {
		Long c =
				relativesMapper.selectCount(
						Wrappers.<Relatives>lambdaQuery()
								.eq(Relatives::getEmployeeUserId, employeeUserId)
								.eq(Relatives::getActivityId, activityId)
								.eq(Relatives::getCompanyId, companyId)
								.eq(Relatives::getEnterpriseId, enterpriseId)
								.and(
										w ->
												w.eq(Relatives::getDisabled, false)
														.or()
														.isNull(Relatives::getDisabled)));
		return c == null ? 0 : c.intValue();
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

	private static List<Long> parseEnterpriseIdCsv(String csv) {
		if (csv == null || csv.isBlank()) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (String part : csv.split(",")) {
			String p = part.trim();
			if (p.isEmpty()) {
				continue;
			}
			try {
				out.add(Long.parseLong(p));
			} catch (NumberFormatException ignored) {
			}
		}
		return out;
	}
}
