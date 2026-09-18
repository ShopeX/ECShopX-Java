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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class InternalSaleEligibilityService {

	private final ActivitiesMapper activitiesMapper;
	private final EmployeesMapper employeesMapper;
	private final RelativesMapper relativesMapper;

	public InternalSaleEligibilityService(
			ActivitiesMapper activitiesMapper,
			EmployeesMapper employeesMapper,
			RelativesMapper relativesMapper) {
		this.activitiesMapper = activitiesMapper;
		this.employeesMapper = employeesMapper;
		this.relativesMapper = relativesMapper;
	}

	public Map<String, Object> getInternalSaleEligibility(
			long companyId, long userId, long activityId, long enterpriseId) {
		if (activityId <= 0L) {
			throw new ResourceException("活动ID必填");
		}
		if (enterpriseId <= 0L) {
			throw new ResourceException("企业ID必填");
		}

		Activities activity =
				activitiesMapper.selectOne(
						new LambdaQueryWrapper<Activities>()
								.eq(Activities::getCompanyId, companyId)
								.eq(Activities::getId, activityId)
								.last("LIMIT 1"));
		if (activity == null) {
			throw new ResourceException("活动不存在");
		}
		List<Long> enterpriseIds = parseEnterpriseIdCsv(activity.getEnterpriseId());
		if (!enterpriseIds.contains(enterpriseId)) {
			throw new ResourceException("企业不参与该活动");
		}

		String eligibleAs = "none";
		if (isActiveEmployee(companyId, enterpriseId, userId)) {
			eligibleAs = "employee";
		} else if (isActiveRelative(companyId, enterpriseId, activityId, userId)) {
			eligibleAs = "relative";
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("internal_sale_eligible", "none".equals(eligibleAs) ? 0 : 1);
		out.put("eligible_as", eligibleAs);
		return out;
	}

	private boolean isActiveEmployee(long companyId, long enterpriseId, long userId) {
		Employees row =
				employeesMapper.selectOne(
						new LambdaQueryWrapper<Employees>()
								.eq(Employees::getCompanyId, companyId)
								.eq(Employees::getEnterpriseId, enterpriseId)
								.eq(Employees::getUserId, userId)
								.eq(Employees::getDisabled, false)
								.last("LIMIT 1"));
		return row != null;
	}

	private boolean isActiveRelative(long companyId, long enterpriseId, long activityId, long userId) {
		Relatives row =
				relativesMapper.selectOne(
						new LambdaQueryWrapper<Relatives>()
								.eq(Relatives::getCompanyId, companyId)
								.eq(Relatives::getEnterpriseId, enterpriseId)
								.eq(Relatives::getActivityId, activityId)
								.eq(Relatives::getUserId, userId)
								.eq(Relatives::getDisabled, false)
								.last("LIMIT 1"));
		return row != null;
	}

	private static List<Long> parseEnterpriseIdCsv(String csv) {
		if (!StringUtils.hasText(csv)) {
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
