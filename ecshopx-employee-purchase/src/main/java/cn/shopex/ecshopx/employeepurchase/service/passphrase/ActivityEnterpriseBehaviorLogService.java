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
import cn.shopex.ecshopx.employeepurchase.domain.ActivityEnterpriseBehaviorLog;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityEnterpriseBehaviorLogMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ActivityEnterpriseBehaviorLogService {

	private final ActivityEnterpriseBehaviorLogMapper activityEnterpriseBehaviorLogMapper;
	private final ObjectMapper objectMapper;

	public ActivityEnterpriseBehaviorLogService(
			ActivityEnterpriseBehaviorLogMapper activityEnterpriseBehaviorLogMapper,
			ObjectMapper objectMapper) {
		this.activityEnterpriseBehaviorLogMapper = activityEnterpriseBehaviorLogMapper;
		this.objectMapper = objectMapper;
	}

	public long recordScan(
			long companyId,
			long activityId,
			long enterpriseId,
			Long userId,
			String visitorKey) {
		return insertLog(
				companyId,
				activityId,
				enterpriseId,
				userId,
				PassphraseConstants.BEHAVIOR_SCAN,
				null,
				visitorKey,
				null,
				null);
	}

	public long recordPassphraseVerify(
			long companyId,
			long activityId,
			long enterpriseId,
			Long userId,
			String visitorKey,
			boolean success) {
		return insertLog(
				companyId,
				activityId,
				enterpriseId,
				userId,
				PassphraseConstants.BEHAVIOR_PASSPHRASE_VERIFY,
				success ? PassphraseConstants.RESULT_SUCCESS : PassphraseConstants.RESULT_FAIL,
				visitorKey,
				null,
				null);
	}

	public long recordBind(
			long companyId,
			long activityId,
			long enterpriseId,
			long userId,
			String bindChannel) {
		Map<String, Object> extra = new LinkedHashMap<>();
		extra.put("bind_channel", bindChannel);
		return insertLog(
				companyId,
				activityId,
				enterpriseId,
				userId,
				PassphraseConstants.BEHAVIOR_BIND,
				null,
				null,
				null,
				extra);
	}

	public long recordOrder(long companyId, long activityId, long enterpriseId, long userId, long orderId) {
		Long existing =
				activityEnterpriseBehaviorLogMapper.selectCount(
						Wrappers.<ActivityEnterpriseBehaviorLog>lambdaQuery()
								.eq(ActivityEnterpriseBehaviorLog::getCompanyId, companyId)
								.eq(ActivityEnterpriseBehaviorLog::getActivityId, activityId)
								.eq(ActivityEnterpriseBehaviorLog::getEnterpriseId, enterpriseId)
								.eq(ActivityEnterpriseBehaviorLog::getUserId, userId)
								.eq(ActivityEnterpriseBehaviorLog::getBehaviorType, PassphraseConstants.BEHAVIOR_ORDER)
								.eq(ActivityEnterpriseBehaviorLog::getRefId, orderId));
		if (existing != null && existing > 0L) {
			return 0L;
		}
		return insertLog(
				companyId,
				activityId,
				enterpriseId,
				userId,
				PassphraseConstants.BEHAVIOR_ORDER,
				null,
				null,
				orderId,
				null);
	}

	private long insertLog(
			long companyId,
			long activityId,
			long enterpriseId,
			Long userId,
			String behaviorType,
			String resultStatus,
			String visitorKey,
			Long refId,
			Map<String, Object> extra) {
		if (PassphraseConstants.BEHAVIOR_PASSPHRASE_VERIFY.equals(behaviorType)) {
			if (!PassphraseConstants.RESULT_SUCCESS.equals(resultStatus)
					&& !PassphraseConstants.RESULT_FAIL.equals(resultStatus)) {
				throw new ResourceException("口令验证流水须指定 result_status 为 success 或 fail");
			}
		} else if (resultStatus != null) {
			throw new ResourceException("当前仅口令验证行为可写 result_status");
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		ActivityEnterpriseBehaviorLog log = new ActivityEnterpriseBehaviorLog();
		log.setCompanyId(companyId);
		log.setActivityId(activityId);
		log.setEnterpriseId(enterpriseId);
		log.setUserId(userId);
		log.setBehaviorType(behaviorType);
		log.setResultStatus(resultStatus);
		log.setVisitorKey(StringUtils.hasText(visitorKey) ? visitorKey.trim() : null);
		log.setRefId(refId);
		if (extra != null && !extra.isEmpty()) {
			try {
				log.setExtra(objectMapper.writeValueAsString(extra));
			} catch (JsonProcessingException e) {
				log.setExtra("{}");
			}
		}
		log.setCreated(now);
		activityEnterpriseBehaviorLogMapper.insert(log);
		return log.getId() == null ? 0L : log.getId();
	}
}
