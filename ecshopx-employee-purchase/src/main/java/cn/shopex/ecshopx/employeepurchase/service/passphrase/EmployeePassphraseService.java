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
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityPassphraseEnterprise;
import cn.shopex.ecshopx.employeepurchase.domain.Employees;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityEnterpriseParticipateUserMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeesMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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

	public EmployeePassphraseService(
			ActivityPassphraseService activityPassphraseService,
			PassphraseVerifiedRedisService passphraseVerifiedRedisService,
			PassphraseParticipateQuotaRedisService passphraseParticipateQuotaRedisService,
			ActivityEnterpriseBehaviorLogService activityEnterpriseBehaviorLogService,
			ActivityEnterpriseParticipateUserMapper activityEnterpriseParticipateUserMapper,
			EmployeesMapper employeesMapper) {
		this.activityPassphraseService = activityPassphraseService;
		this.passphraseVerifiedRedisService = passphraseVerifiedRedisService;
		this.passphraseParticipateQuotaRedisService = passphraseParticipateQuotaRedisService;
		this.activityEnterpriseBehaviorLogService = activityEnterpriseBehaviorLogService;
		this.activityEnterpriseParticipateUserMapper = activityEnterpriseParticipateUserMapper;
		this.employeesMapper = employeesMapper;
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
		Employees existing =
				employeesMapper.selectOne(
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
		if (existing != null) {
			return;
		}

		if (isParticipateUser(companyId, activityId, enterpriseId, userId)) {
			insertEmployeeIfAbsent(companyId, enterpriseId, userId, memberMobile);
			return;
		}

		boolean quotaConsumed = false;
		try {
			consumeQuotaOrThrow(companyId, activityId, enterpriseId);
			quotaConsumed = true;
			insertEmployeeIfAbsent(companyId, enterpriseId, userId, memberMobile);
			int now = (int) (System.currentTimeMillis() / 1000L);
			activityEnterpriseParticipateUserMapper.insertIgnore(
					companyId, activityId, enterpriseId, userId, now);
			activityEnterpriseBehaviorLogService.recordBind(
					companyId, activityId, enterpriseId, userId, PassphraseConstants.BIND_CHANNEL_PASSPHRASE);
		} catch (RuntimeException ex) {
			if (quotaConsumed) {
				passphraseParticipateQuotaRedisService.releaseOneSlot(companyId, activityId, enterpriseId);
			}
			throw ex;
		}
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
			long companyId, long enterpriseId, long userId, String memberMobile) {
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
		String mobile = StringUtils.hasText(memberMobile) ? memberMobile.trim() : "";
		entity.setMemberMobile(mobile);
		entity.setName(mobile);
		entity.setMobile(mobile);
		entity.setDistributorId(0);
		entity.setOperatorId(0);
		entity.setCreated(now);
		entity.setUpdated(now);
		entity.setDisabled(false);
		employeesMapper.insert(entity);
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
