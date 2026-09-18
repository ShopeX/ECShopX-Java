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
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseActivityDataService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PassphraseBehaviorReportService {

	private final ActivitiesMapper activitiesMapper;
	private final EmployeePurchaseActivityDataService employeePurchaseActivityDataService;
	private final ActivityPassphraseService activityPassphraseService;
	private final ActivityEnterpriseBehaviorLogService activityEnterpriseBehaviorLogService;
	private final PassphraseVerifiedRedisService passphraseVerifiedRedisService;

	public PassphraseBehaviorReportService(
			ActivitiesMapper activitiesMapper,
			EmployeePurchaseActivityDataService employeePurchaseActivityDataService,
			ActivityPassphraseService activityPassphraseService,
			ActivityEnterpriseBehaviorLogService activityEnterpriseBehaviorLogService,
			PassphraseVerifiedRedisService passphraseVerifiedRedisService) {
		this.activitiesMapper = activitiesMapper;
		this.employeePurchaseActivityDataService = employeePurchaseActivityDataService;
		this.activityPassphraseService = activityPassphraseService;
		this.activityEnterpriseBehaviorLogService = activityEnterpriseBehaviorLogService;
		this.passphraseVerifiedRedisService = passphraseVerifiedRedisService;
	}

	public Map<String, Object> report(Map<String, Object> input) {
		String behaviorType = stringVal(input.get("behavior_type"));
		if (!PassphraseConstants.BEHAVIOR_SCAN.equals(behaviorType)
				&& !PassphraseConstants.BEHAVIOR_PASSPHRASE_VERIFY.equals(behaviorType)) {
			throw new ResourceException("behavior_type 须为 scan 或 passphrase_verify");
		}

		long companyId = longVal(input.get("company_id"), 0L);
		if (companyId <= 0L) {
			throw new ResourceException("公司ID必填");
		}
		long activityId = longVal(input.get("activity_id"), 0L);
		long enterpriseId = longVal(input.get("enterprise_id"), 0L);
		Long userId = nullableUserId(input.get("user_id"));
		String visitorKey = stringVal(input.get("visitor_key"));

		Activities activity = loadActivityOrThrow(companyId, activityId);
		employeePurchaseActivityDataService.assertActivityExistsAndEnterpriseParticipates(
				companyId, activityId, enterpriseId);

		if (PassphraseConstants.BEHAVIOR_SCAN.equals(behaviorType)) {
			long logId =
					activityEnterpriseBehaviorLogService.recordScan(
							companyId, activityId, enterpriseId, userId, visitorKey);
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("behavior_type", behaviorType);
			out.put("status", Boolean.TRUE);
			out.put("log_id", logId);
			return out;
		}

		String code = extractPassphraseCode(input);
		if (!StringUtils.hasText(code)) {
			throw new ResourceException("口令必填");
		}

		boolean matched =
				activityPassphraseService.isActivityEnterprisePassphraseMatch(
						activity, companyId, activityId, enterpriseId, code);
		long logId =
				activityEnterpriseBehaviorLogService.recordPassphraseVerify(
						companyId, activityId, enterpriseId, userId, visitorKey, matched);

		if (matched && userId != null && userId > 0L) {
			long end =
					activity.getEmployeeEndTime() == null
							? System.currentTimeMillis() / 1000L
							: activity.getEmployeeEndTime().longValue();
			passphraseVerifiedRedisService.markVerified(companyId, activityId, enterpriseId, userId, end);
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("behavior_type", behaviorType);
		out.put("verified", matched);
		out.put("log_id", logId);
		return out;
	}

	private Activities loadActivityOrThrow(long companyId, long activityId) {
		Activities activity =
				activitiesMapper.selectOne(
						Wrappers.<Activities>lambdaQuery()
								.eq(Activities::getCompanyId, companyId)
								.eq(Activities::getId, activityId)
								.last("LIMIT 1"));
		if (activity == null) {
			throw new ResourceException("活动不存在");
		}
		return activity;
	}

	private static String extractPassphraseCode(Map<String, Object> input) {
		String code = stringVal(input.get("passphrase_code"));
		if (StringUtils.hasText(code)) {
			return code.trim();
		}
		return stringVal(input.get("code")).trim();
	}

	private static Long nullableUserId(Object raw) {
		long v = longVal(raw, 0L);
		return v > 0L ? v : null;
	}

	private static long longVal(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}
}
