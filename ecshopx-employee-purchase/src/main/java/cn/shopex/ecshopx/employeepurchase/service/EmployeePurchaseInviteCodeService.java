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
import cn.shopex.ecshopx.employeepurchase.domain.Employees;
import cn.shopex.ecshopx.employeepurchase.domain.Relatives;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.RelativesMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

@Service
public class EmployeePurchaseInviteCodeService {

	private static final int INVITE_CODE_GEN_MAX_ATTEMPTS = 50_000;

	private final ActivitiesMapper activitiesMapper;
	private final EmployeesMapper employeesMapper;
	private final RelativesMapper relativesMapper;
	private final EmployeePurchaseInviteRedisService inviteRedisService;
	private final EmployeePurchaseInviteHashidsSupport hashidsSupport;

	public EmployeePurchaseInviteCodeService(
			ActivitiesMapper activitiesMapper,
			EmployeesMapper employeesMapper,
			RelativesMapper relativesMapper,
			EmployeePurchaseInviteRedisService inviteRedisService,
			EmployeePurchaseInviteHashidsSupport hashidsSupport) {
		this.activitiesMapper = activitiesMapper;
		this.employeesMapper = employeesMapper;
		this.relativesMapper = relativesMapper;
		this.inviteRedisService = inviteRedisService;
		this.hashidsSupport = hashidsSupport;
	}

	public String generateInviteCode(long companyId, long enterpriseId, long activityId, long userId) {
		Activities activity =
				activitiesMapper.selectOne(
						Wrappers.<Activities>lambdaQuery()
								.eq(Activities::getCompanyId, companyId)
								.eq(Activities::getId, activityId)
								.last("LIMIT 1"));
		if (activity == null) {
			throw new ResourceException("活动不存在");
		}

		List<Long> enterpriseIds = parseEnterpriseIdCsv(activity.getEnterpriseId());
		boolean participates = false;
		for (Long id : enterpriseIds) {
			if (id != null && id.longValue() == enterpriseId) {
				participates = true;
				break;
			}
		}
		if (!participates) {
			throw new ResourceException("企业不参与该活动");
		}

		if (!Boolean.TRUE.equals(activity.getIfRelativeJoin())) {
			throw new ResourceException("活动不可以邀请亲友");
		}

		Employees employee =
				employeesMapper.selectOne(
						Wrappers.<Employees>lambdaQuery()
								.eq(Employees::getCompanyId, companyId)
								.eq(Employees::getEnterpriseId, enterpriseId)
								.eq(Employees::getUserId, userId)
								.eq(Employees::getDisabled, false));
		if (employee == null) {
			throw new ResourceException("只有员工可以邀请");
		}

		Long inviteNumLong =
				relativesMapper.selectCount(
						Wrappers.<Relatives>lambdaQuery()
								.eq(Relatives::getCompanyId, companyId)
								.eq(Relatives::getEnterpriseId, enterpriseId)
								.eq(Relatives::getEmployeeUserId, userId)
								.eq(Relatives::getActivityId, activityId)
								.eq(Relatives::getDisabled, false));
		long inviteNum = inviteNumLong == null ? 0L : inviteNumLong.longValue();
		int limit = activity.getInviteLimit() == null ? 0 : activity.getInviteLimit();
		if (inviteNum >= limit) {
			throw new ResourceException("已达到邀请上限");
		}

		return genShareCode(companyId, enterpriseId, activityId, userId);
	}

	private String genShareCode(long companyId, long enterpriseId, long activityId, long userId) {
		String ticket = hashidsSupport.encodeEnterpriseActivityUser(enterpriseId, activityId, userId);
		for (int i = 0; i < INVITE_CODE_GEN_MAX_ATTEMPTS; i++) {
			String code = String.valueOf(ThreadLocalRandom.current().nextInt(1_000_000, 10_000_000));
			if (inviteRedisService.claimInviteCodeIfAbsent(companyId, code, ticket)) {
				return code;
			}
		}
		throw new ResourceException("邀请码生成失败，请重试");
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
