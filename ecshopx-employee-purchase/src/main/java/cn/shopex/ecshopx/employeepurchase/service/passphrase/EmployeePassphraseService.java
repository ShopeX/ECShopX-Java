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

package cn.shopex.ecshopx.employeepurchase.service.passphrase;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.point.PointMemberDmPointMemberInfoReadPort;
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityPassphraseEnterprise;
import cn.shopex.ecshopx.employeepurchase.domain.Employees;
import cn.shopex.ecshopx.employeepurchase.domain.Enterprises;
import cn.shopex.ecshopx.employeepurchase.domain.Relatives;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityEnterpriseParticipateUserMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterprisesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.RelativesMapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EmployeePassphraseService {

	private final ActivityPassphraseService activityPassphraseService;
	private final PassphraseVerifiedRedisService passphraseVerifiedRedisService;
	private final PassphraseParticipateQuotaRedisService passphraseParticipateQuotaRedisService;
	private final ActivityEnterpriseBehaviorLogService activityEnterpriseBehaviorLogService;
	private final ActivityEnterpriseParticipateUserMapper activityEnterpriseParticipateUserMapper;
	private final EmployeesMapper employeesMapper;
	private final EnterprisesMapper enterprisesMapper;
	private final RelativesMapper relativesMapper;
	private final PointMemberDmPointMemberInfoReadPort pointMemberInfoReadPort;

	public EmployeePassphraseService(
			ActivityPassphraseService activityPassphraseService,
			PassphraseVerifiedRedisService passphraseVerifiedRedisService,
			PassphraseParticipateQuotaRedisService passphraseParticipateQuotaRedisService,
			ActivityEnterpriseBehaviorLogService activityEnterpriseBehaviorLogService,
			ActivityEnterpriseParticipateUserMapper activityEnterpriseParticipateUserMapper,
			EmployeesMapper employeesMapper,
			EnterprisesMapper enterprisesMapper,
			RelativesMapper relativesMapper,
			PointMemberDmPointMemberInfoReadPort pointMemberInfoReadPort) {
		this.activityPassphraseService = activityPassphraseService;
		this.passphraseVerifiedRedisService = passphraseVerifiedRedisService;
		this.passphraseParticipateQuotaRedisService = passphraseParticipateQuotaRedisService;
		this.activityEnterpriseBehaviorLogService = activityEnterpriseBehaviorLogService;
		this.activityEnterpriseParticipateUserMapper = activityEnterpriseParticipateUserMapper;
		this.employeesMapper = employeesMapper;
		this.enterprisesMapper = enterprisesMapper;
		this.relativesMapper = relativesMapper;
		this.pointMemberInfoReadPort = pointMemberInfoReadPort;
	}

	public boolean isApplicable(Activities activity, long companyId, long activityId, long enterpriseId, long userId) {
		if (!activityPassphraseService.isPassphraseEnabled(activity)) {
			return false;
		}
		return passphraseVerifiedRedisService.isVerified(companyId, activityId, enterpriseId, userId);
	}

	/**
	 * 已验口令 + 开口令 → 占名额并自动建档。DB 失败时补偿 release Redis 名额。
	 */
	@Transactional(rollbackFor = Exception.class)
	public void ensurePassphraseEmployeeFromVerifiedActivity(
			Activities activity, long companyId, long activityId, long enterpriseId, long userId, String memberMobile) {
		if (!isApplicable(activity, companyId, activityId, enterpriseId, userId)) {
			return;
		}
		if (!activityPassphraseService.supportsPassphraseBypassWhitelist(
				activity, companyId, activityId, enterpriseId)) {
			return;
		}
		if (isActiveRelative(companyId, enterpriseId, activityId, userId)) {
			return;
		}
		PassphraseMemberProfile memberProfile = requirePassphraseMemberProfile(companyId, userId);
		boolean isActiveEmployee = findActiveEmployee(companyId, enterpriseId, userId) != null;

		Enterprises enterprise =
				enterprisesMapper.selectOne(
						Wrappers.<Enterprises>lambdaQuery()
								.eq(Enterprises::getCompanyId, companyId)
								.eq(Enterprises::getId, enterpriseId)
								.last("LIMIT 1"));
		if (enterprise == null) {
			return;
		}
		int distributorId =
				enterprise.getDistributorId() == null ? 0 : enterprise.getDistributorId();

		boolean alreadyParticipating =
				isParticipateUser(companyId, activityId, enterpriseId, userId);
		boolean quotaConsumed = false;
		if (!alreadyParticipating) {
			consumeQuotaOrThrow(companyId, activityId, enterpriseId);
			quotaConsumed = true;
		}

		if (isActiveEmployee) {
			if (quotaConsumed) {
				recordParticipateAndBind(companyId, activityId, enterpriseId, userId);
			}
			return;
		}

		if (alreadyParticipating) {
			insertEmployeeIfAbsent(companyId, enterpriseId, userId, memberProfile, distributorId);
			return;
		}

		try {
			insertEmployeeIfAbsent(companyId, enterpriseId, userId, memberProfile, distributorId);
			recordParticipateAndBind(companyId, activityId, enterpriseId, userId);
		} catch (RuntimeException ex) {
			if (quotaConsumed) {
				passphraseParticipateQuotaRedisService.releaseOneSlot(companyId, activityId, enterpriseId);
			}
			throw ex;
		}
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

	private void recordParticipateAndBind(
			long companyId, long activityId, long enterpriseId, long userId) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		activityEnterpriseParticipateUserMapper.insertIgnore(
				companyId, activityId, enterpriseId, userId, now);
		activityEnterpriseBehaviorLogService.recordBind(
				companyId, activityId, enterpriseId, userId, PassphraseConstants.BIND_CHANNEL_PASSPHRASE);
	}

	public void assertQuotaAvailableForOrder(long companyId, long activityId, long enterpriseId, long userId) {
		if (isParticipateUser(companyId, activityId, enterpriseId, userId)) {
			return;
		}
		int remaining =
				passphraseParticipateQuotaRedisService.readRemainingOrThrow(companyId, activityId, enterpriseId);
		if (remaining <= 0) {
			throwQuotaEmpty();
		}
	}

	public boolean isParticipateUser(long companyId, long activityId, long enterpriseId, long userId) {
		Long count =
				activityEnterpriseParticipateUserMapper.selectCount(
						Wrappers.lambdaQuery(
										cn.shopex.ecshopx.employeepurchase.domain.ActivityEnterpriseParticipateUser
												.class)
								.eq(
										cn.shopex.ecshopx.employeepurchase.domain.ActivityEnterpriseParticipateUser
												::getCompanyId,
										companyId)
								.eq(
										cn.shopex.ecshopx.employeepurchase.domain.ActivityEnterpriseParticipateUser
												::getActivityId,
										activityId)
								.eq(
										cn.shopex.ecshopx.employeepurchase.domain.ActivityEnterpriseParticipateUser
												::getEnterpriseId,
										enterpriseId)
								.eq(
										cn.shopex.ecshopx.employeepurchase.domain.ActivityEnterpriseParticipateUser
												::getUserId,
										userId));
		return count != null && count > 0L;
	}

	private void insertEmployeeIfAbsent(
			long companyId,
			long enterpriseId,
			long userId,
			PassphraseMemberProfile memberProfile,
			int distributorId) {
		Employees existing =
				employeesMapper.selectOne(
						Wrappers.<Employees>lambdaQuery()
								.eq(Employees::getCompanyId, companyId)
								.eq(Employees::getEnterpriseId, enterpriseId)
								.eq(Employees::getUserId, userId)
								.last("LIMIT 1"));
		if (existing != null) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		Employees entity = new Employees();
		entity.setCompanyId(companyId);
		entity.setEnterpriseId(enterpriseId);
		entity.setUserId(userId);
		entity.setMemberMobile(memberProfile.memberMobileStored());
		entity.setName(memberProfile.displayName());
		entity.setMobile(memberProfile.mobileStored());
		if (StringUtils.hasText(memberProfile.email())) {
			entity.setEmail(memberProfile.email());
		}
		entity.setDistributorId(distributorId);
		entity.setOperatorId(0);
		entity.setCreated(now);
		entity.setUpdated(now);
		entity.setDisabled(false);
		employeesMapper.insert(entity);
		disableRelativesForUser(companyId, enterpriseId, userId);
	}

	private PassphraseMemberProfile requirePassphraseMemberProfile(long companyId, long userId) {
		Map<String, Object> member = pointMemberInfoReadPort.getMemberInfo(userId, companyId);
		if (member == null || member.isEmpty()) {
			throw new ResourceException("会员信息不存在");
		}
		Object uid = member.get("user_id");
		if (uid == null || String.valueOf(uid).trim().isEmpty() || "0".equals(String.valueOf(uid).trim())) {
			throw new ResourceException("会员信息不存在");
		}
		return PassphraseMemberProfile.fromMemberMap(userId, member);
	}

	private record PassphraseMemberProfile(String memberMobile, String memberEmail, String mobileForName) {

		static PassphraseMemberProfile fromMemberMap(long userId, Map<String, Object> member) {
			String memberMobile = stringVal(member.get("mobile")).trim();
			String memberEmail = stringVal(member.get("email")).trim();
			String mobileForName = StringUtils.hasText(memberMobile) ? memberMobile : String.valueOf(userId);
			return new PassphraseMemberProfile(memberMobile, memberEmail, mobileForName);
		}

		String memberMobileStored() {
			if (StringUtils.hasText(memberMobile)) {
				return memberMobile;
			}
			if (StringUtils.hasText(memberEmail)) {
				return memberEmail;
			}
			return mobileForName;
		}

		String mobileStored() {
			return mobileForName;
		}

		String displayName() {
			String mob = mobileForName;
			String suffix = mob.length() <= 4 ? mob : mob.substring(mob.length() - 4);
			return "用户-" + suffix;
		}

		String email() {
			return memberEmail;
		}

		private static String stringVal(Object o) {
			return o == null ? "" : o.toString();
		}
	}

	private boolean isActiveRelative(
			long companyId, long enterpriseId, long activityId, long userId) {
		Relatives relative =
				relativesMapper.selectOne(
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
		return relative != null;
	}

	private void disableRelativesForUser(long companyId, long enterpriseId, long userId) {
		relativesMapper.update(
				null,
				new UpdateWrapper<Relatives>()
						.set("disabled", true)
						.eq("company_id", companyId)
						.eq("user_id", userId)
						.eq("enterprise_id", enterpriseId));
	}

	private void consumeQuotaOrThrow(long companyId, long activityId, long enterpriseId) {
		ActivityPassphraseEnterprise cfg =
				activityPassphraseService.requireEnterpriseConfig(companyId, activityId, enterpriseId);
		if (cfg == null) {
			throw new ResourceException("口令企业配置不存在");
		}
		PassphraseParticipateQuotaRedisService.TryConsumeResult result =
				passphraseParticipateQuotaRedisService.tryConsumeSlot(companyId, activityId, enterpriseId);
		if (result == PassphraseParticipateQuotaRedisService.TryConsumeResult.NOT_CONFIGURED) {
			passphraseParticipateQuotaRedisService.syncRemainingQuota(
					companyId, activityId, enterpriseId, cfg.getParticipateQuota());
			result = passphraseParticipateQuotaRedisService.tryConsumeSlot(companyId, activityId, enterpriseId);
		}
		if (result != PassphraseParticipateQuotaRedisService.TryConsumeResult.SUCCESS) {
			throwQuotaEmpty();
		}
	}

	private static void throwQuotaEmpty() {
		throw new ResourceException("该企业在本活动下的参与名额已满");
	}
}
