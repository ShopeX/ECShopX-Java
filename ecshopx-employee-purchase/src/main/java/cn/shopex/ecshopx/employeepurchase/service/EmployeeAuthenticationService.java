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
import cn.shopex.ecshopx.employeepurchase.domain.Employees;
import cn.shopex.ecshopx.employeepurchase.domain.Enterprises;
import cn.shopex.ecshopx.employeepurchase.domain.Relatives;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterprisesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.RelativesMapper;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.ActivityEnterpriseBehaviorLogService;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.PassphraseConstants;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EmployeeAuthenticationService {

	private final EnterprisesMapper enterprisesMapper;
	private final EmployeesMapper employeesMapper;
	private final RelativesMapper relativesMapper;
	private final ActivitiesMapper activitiesMapper;
	private final ActivityEnterpriseBehaviorLogService activityEnterpriseBehaviorLogService;

	public EmployeeAuthenticationService(
			EnterprisesMapper enterprisesMapper,
			EmployeesMapper employeesMapper,
			RelativesMapper relativesMapper,
			ActivitiesMapper activitiesMapper,
			ActivityEnterpriseBehaviorLogService activityEnterpriseBehaviorLogService) {
		this.enterprisesMapper = enterprisesMapper;
		this.employeesMapper = employeesMapper;
		this.relativesMapper = relativesMapper;
		this.activitiesMapper = activitiesMapper;
		this.activityEnterpriseBehaviorLogService = activityEnterpriseBehaviorLogService;
	}

	@Transactional(rollbackFor = Exception.class)
	public void authenticate(Map<String, Object> params) {
		try {
			doAuthenticate(params);
		} catch (ResourceException | BadRequestException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(e.getMessage());
		}
	}

	private void doAuthenticate(Map<String, Object> params) {
		long companyId = longFromParam(params.get("company_id"));
		long userId = longFromParam(params.get("user_id"));
		String memberMobile = Objects.requireNonNullElse(params.get("member_mobile"), "").toString();
		long enterpriseId = longFromParam(params.get("enterprise_id"));

		String requestAuthType = params.get("auth_type").toString().trim();

		Enterprises enterprise = enterprisesMapper.selectOne(
				Wrappers.<Enterprises>lambdaQuery()
						.eq(Enterprises::getCompanyId, companyId)
						.eq(Enterprises::getId, enterpriseId)
						.eq(Enterprises::getAuthType, requestAuthType)
						.last("LIMIT 1"));
		if (enterprise == null) {
			throw new ResourceException("企业不存在");
		}

		Long bound = employeesMapper.selectCount(
				Wrappers.<Employees>lambdaQuery()
						.eq(Employees::getEnterpriseId, enterpriseId)
						.eq(Employees::getUserId, userId));
		if (bound != null && bound > 0L) {
			throw new ResourceException("已经是该企业员工，不需要重复绑定");
		}

		int now = (int) (System.currentTimeMillis() / 1000);

		if (Boolean.FALSE.equals(enterprise.getIsEmployeeCheckEnabled())) {
			Employees entity = new Employees();
			entity.setCompanyId(companyId);
			entity.setEnterpriseId(enterpriseId);
			entity.setUserId(userId);
			entity.setMemberMobile(memberMobile);
			Integer dist = enterprise.getDistributorId();
			entity.setDistributorId(dist == null ? 0 : dist);
			entity.setOperatorId(0);
			entity.setCreated(now);
			entity.setUpdated(now);
			entity.setDisabled(false);

			String enterpriseAuth = enterprise.getAuthType() == null ? "" : enterprise.getAuthType();
			if (Objects.equals(enterpriseAuth, "email")) {
				String decoded = urlDecodeUtf8(params.get("email").toString());
				entity.setName(decoded);
				entity.setEmail(decoded);
			} else {
				entity.setName(memberMobile);
				entity.setMobile(memberMobile);
			}
			employeesMapper.insert(entity);
		} else {
			long employeeRowId = parsePositiveLong(params.get("employee_id"));
			Employees employee = employeesMapper.selectOne(
					Wrappers.<Employees>lambdaQuery()
							.eq(Employees::getCompanyId, companyId)
							.eq(Employees::getEnterpriseId, enterpriseId)
							.eq(Employees::getId, employeeRowId)
							.last("LIMIT 1"));
			if (employee == null) {
				throw new ResourceException("企业员工验证失败");
			}
			if (employee.getUserId() != null && employee.getUserId() > 0L) {
				throw new ResourceException("企业员工已绑定其他用户");
			}
			employeesMapper.update(
					null,
					Wrappers.<Employees>lambdaUpdate()
							.set(Employees::getUserId, userId)
							.set(Employees::getMemberMobile, memberMobile)
							.set(Employees::getUpdated, now)
							.eq(Employees::getCompanyId, companyId)
							.eq(Employees::getEnterpriseId, enterpriseId)
							.eq(Employees::getId, employeeRowId));
		}

		relativesMapper.update(
				null,
				Wrappers.<Relatives>lambdaUpdate()
						.set(Relatives::getDisabled, true)
						.eq(Relatives::getCompanyId, companyId)
						.eq(Relatives::getUserId, userId)
						.eq(Relatives::getEnterpriseId, enterpriseId));

		tryWriteEmployeeBindBehaviorLog(params, companyId, userId, enterpriseId, requestAuthType);
	}

	private void tryWriteEmployeeBindBehaviorLog(
			Map<String, Object> params,
			long companyId,
			long userId,
			long enterpriseId,
			String authType) {
		Long activityId = readOptionalPositiveLong(params.get("activity_id"));
		if (activityId == null || activityId <= 0L) {
			return;
		}
		Long count =
				activitiesMapper.selectCount(
						Wrappers.<cn.shopex.ecshopx.employeepurchase.domain.Activities>lambdaQuery()
								.eq(cn.shopex.ecshopx.employeepurchase.domain.Activities::getCompanyId, companyId)
								.eq(cn.shopex.ecshopx.employeepurchase.domain.Activities::getId, activityId));
		if (count == null || count <= 0L) {
			return;
		}
		String bindChannel = authType == null ? "" : authType.trim();
		if (bindChannel.isEmpty()) {
			bindChannel = PassphraseConstants.BIND_CHANNEL_PASSPHRASE;
		}
		activityEnterpriseBehaviorLogService.recordBind(
				companyId, activityId, enterpriseId, userId, bindChannel);
	}

	private static Long readOptionalPositiveLong(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : null;
		}
		try {
			long v = Long.parseLong(raw.toString().trim());
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long longFromParam(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long parsePositiveLong(Object raw) {
		if (raw == null) {
			throw new ResourceException("企业员工验证失败");
		}
		if (raw instanceof Boolean b) {
			if (Boolean.TRUE.equals(b)) {
				return 1L;
			}
			throw new ResourceException("企业员工验证失败");
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			if (v <= 0L) {
				throw new ResourceException("企业员工验证失败");
			}
			return v;
		}
		if (raw instanceof String s) {
			String trimmed = s.trim();
			if (!StringUtils.hasText(trimmed)) {
				throw new ResourceException("企业员工验证失败");
			}
			try {
				long v = Long.parseLong(trimmed);
				if (v <= 0L) {
					throw new ResourceException("企业员工验证失败");
				}
				return v;
			} catch (NumberFormatException e) {
				throw new ResourceException("企业员工验证失败");
			}
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			throw new ResourceException("企业员工验证失败");
		}
		try {
			long v = Long.parseLong(s);
			if (v <= 0L) {
				throw new ResourceException("企业员工验证失败");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new ResourceException("企业员工验证失败");
		}
	}

	private static String urlDecodeUtf8(String s) {
		try {
			return URLDecoder.decode(s, StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {
			throw new BadRequestException("请填写正确的邮箱");
		}
	}
}
