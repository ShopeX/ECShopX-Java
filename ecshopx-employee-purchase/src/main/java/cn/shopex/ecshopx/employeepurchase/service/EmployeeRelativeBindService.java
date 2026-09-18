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
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.domain.Employees;
import cn.shopex.ecshopx.employeepurchase.domain.Relatives;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.RelativesMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;

@Service
public class EmployeeRelativeBindService {

	private final EmployeePurchaseInviteRedisService inviteRedisService;
	private final EmployeePurchaseInviteHashidsSupport hashidsSupport;
	private final ActivitiesMapper activitiesMapper;
	private final EmployeesMapper employeesMapper;
	private final RelativesMapper relativesMapper;

	public EmployeeRelativeBindService(
			EmployeePurchaseInviteRedisService inviteRedisService,
			EmployeePurchaseInviteHashidsSupport hashidsSupport,
			ActivitiesMapper activitiesMapper,
			EmployeesMapper employeesMapper,
			RelativesMapper relativesMapper) {
		this.inviteRedisService = inviteRedisService;
		this.hashidsSupport = hashidsSupport;
		this.activitiesMapper = activitiesMapper;
		this.employeesMapper = employeesMapper;
		this.relativesMapper = relativesMapper;
	}

	public void executeBindWithInviteLock(long companyId, long userId, String memberMobile, String inviteCode) {
		lockInviteCodeOnly(companyId, inviteCode);
		try {
			bindRelativeAndConsumeInvite(companyId, userId, memberMobile, inviteCode);
		} catch (ResourceException e) {
			inviteRedisService.unlockInviteCode(companyId, inviteCode);
			throw e;
		} catch (BadRequestException e) {
			inviteRedisService.unlockInviteCode(companyId, inviteCode);
			throw e;
		} catch (RuntimeException e) {
			inviteRedisService.unlockInviteCode(companyId, inviteCode);
			throw new ResourceException(e.getMessage());
		}
	}

	public void lockInviteCodeOnly(long companyId, String inviteCode) {
		inviteRedisService.lockInviteCode(companyId, inviteCode);
	}

	public void bindRelativeAndConsumeInvite(long companyId, long userId, String memberMobile, String inviteCode) {
		bindRelativeAfterLock(companyId, userId, memberMobile, inviteCode);
		inviteRedisService.delInviteCode(companyId, inviteCode);
	}

	public void unlockInviteCodeOnly(long companyId, String inviteCode) {
		inviteRedisService.unlockInviteCode(companyId, inviteCode);
	}

	private void bindRelativeAfterLock(long companyId, long userId, String memberMobile, String inviteCode) {
		String ticket = inviteRedisService.getTicketFieldUnderscore(companyId, inviteCode);
		if (ticket == null || ticket.isEmpty()) {
			throw new ResourceException("分享链接已失效");
		}

		long[] decoded = hashidsSupport.decodeTicketOrEmpty(ticket);
		if (decoded.length < 3) {
			throw new ResourceException("分享链接已失效");
		}
		long enterpriseId = decoded[0];
		long activityId = decoded[1];
		long employeeUserId = decoded[2];
		if (enterpriseId <= 0L || activityId <= 0L || employeeUserId <= 0L) {
			throw new ResourceException("分享链接已失效");
		}

		Activities activity = activitiesMapper.selectOne(
				Wrappers.<Activities>lambdaQuery()
						.eq(Activities::getCompanyId, companyId)
						.eq(Activities::getId, activityId));
		if (activity == null) {
			throw new ResourceException("活动不存在");
		}

		Employees invitor = employeesMapper.selectOne(
				Wrappers.<Employees>lambdaQuery()
						.eq(Employees::getCompanyId, companyId)
						.eq(Employees::getEnterpriseId, enterpriseId)
						.eq(Employees::getUserId, employeeUserId)
						.eq(Employees::getDisabled, false));
		if (invitor == null) {
			throw new ResourceException("分享链接已失效");
		}

		Employees currentAsEmployee = employeesMapper.selectOne(
				Wrappers.<Employees>lambdaQuery()
						.eq(Employees::getCompanyId, companyId)
						.eq(Employees::getEnterpriseId, enterpriseId)
						.eq(Employees::getUserId, userId)
						.eq(Employees::getDisabled, false));
		if (currentAsEmployee != null) {
			throw new ResourceException("已经是员工，不可以绑定");
		}

		Relatives existing = relativesMapper.selectOne(
				Wrappers.<Relatives>lambdaQuery()
						.eq(Relatives::getCompanyId, companyId)
						.eq(Relatives::getEnterpriseId, enterpriseId)
						.eq(Relatives::getActivityId, activityId)
						.eq(Relatives::getUserId, userId)
						.eq(Relatives::getDisabled, false));
		if (existing != null) {
			throw new ResourceException("已经是亲友，不需要重复绑定");
		}

		long inviteNum = relativesMapper.selectCount(
				Wrappers.<Relatives>lambdaQuery()
						.eq(Relatives::getCompanyId, companyId)
						.eq(Relatives::getEnterpriseId, enterpriseId)
						.eq(Relatives::getEmployeeUserId, employeeUserId)
						.eq(Relatives::getActivityId, activityId)
						.eq(Relatives::getDisabled, false));
		int limit = activity.getInviteLimit() == null ? 0 : activity.getInviteLimit();
		if (inviteNum >= limit) {
			throw new ResourceException("已达到邀请上限");
		}

		Relatives entity = new Relatives();
		entity.setCompanyId(companyId);
		entity.setDistributorId(invitor.getDistributorId() == null ? 0 : invitor.getDistributorId());
		entity.setEnterpriseId(enterpriseId);
		entity.setActivityId(activityId);
		entity.setEmployeeId(invitor.getId());
		entity.setEmployeeUserId(invitor.getUserId());
		entity.setUserId(userId);
		entity.setMemberMobile(memberMobile);
		entity.setCreated((int) (System.currentTimeMillis() / 1000));
		entity.setDisabled(false);

		int rows = relativesMapper.insert(entity);
		if (rows != 1) {
			throw new ResourceException("绑定成为亲友失败");
		}
	}
}
