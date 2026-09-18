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

package cn.shopex.ecshopx.members.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.kaquan.port.OpenapiMemberCardGradeByExternalIdPort;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2FailException;
import cn.shopex.ecshopx.common.promotions.port.FirePromotionsActivityDispatchPublisher;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.integration.kaquan.OpenapiMemberBasicInfoGradePort;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberUpdateGradeService {

	private final MembersMapper membersMapper;
	private final OpenapiMemberCardGradeByExternalIdPort memberCardGradeByExternalIdPort;
	private final OpenapiMemberBasicInfoGradePort openapiMemberBasicInfoGradePort;
	private final FirePromotionsActivityDispatchPublisher firePromotionsActivityDispatchPublisher;

	public OpenapiThirdApiV2MemberUpdateGradeService(
			MembersMapper membersMapper,
			OpenapiMemberCardGradeByExternalIdPort memberCardGradeByExternalIdPort,
			OpenapiMemberBasicInfoGradePort openapiMemberBasicInfoGradePort,
			FirePromotionsActivityDispatchPublisher firePromotionsActivityDispatchPublisher) {
		this.membersMapper = membersMapper;
		this.memberCardGradeByExternalIdPort = memberCardGradeByExternalIdPort;
		this.openapiMemberBasicInfoGradePort = openapiMemberBasicInfoGradePort;
		this.firePromotionsActivityDispatchPublisher = firePromotionsActivityDispatchPublisher;
	}

	@Transactional(rollbackFor = Exception.class)
	public void executeOpenapiUpdateGrade(long companyId, Map<String, Object> mergedRaw) {
		try {
			if (companyId <= 0L) {
				throw missingParams("缺少必要参数");
			}

			Map<String, Object> merged = mergedRaw == null ? Map.of() : mergedRaw;
			long userId = validatePlatAccountAsUserId(merged);
			String externalId = validateGradeId(merged);
			validateGradeLevel(merged);

			Map<String, Object> gradeRow =
					memberCardGradeByExternalIdPort.findByExternalId(companyId, externalId);
			if (gradeRow == null || gradeRow.get("grade_id") == null) {
				throw v2Fail(OpenapiErrorCode.MEMBER_GRADE_NOT_FOUND, "会员等级找不到");
			}
			long newGradeId = longValue(gradeRow.get("grade_id"));

			Members oldMember =
					membersMapper.selectOne(
							new LambdaQueryWrapper<Members>()
									.eq(Members::getCompanyId, companyId)
									.eq(Members::getUserId, userId)
									.last("LIMIT 1"));
			if (oldMember == null || oldMember.getUserId() == null) {
				throw v2Fail(OpenapiErrorCode.MEMBER_NOT_FOUND, "会员找不到");
			}
			long oldGradeId = oldMember.getGradeId() == null ? 0L : oldMember.getGradeId();

			long nowSec = System.currentTimeMillis() / 1000L;
			LambdaUpdateWrapper<Members> uw = new LambdaUpdateWrapper<>();
			uw.eq(Members::getCompanyId, companyId).eq(Members::getUserId, userId);
			uw.set(Members::getGradeId, newGradeId);
			uw.set(Members::getUpdated, nowSec);
			int affected = membersMapper.update(null, uw);
			if (affected == 0) {
				throw v2Fail(OpenapiErrorCode.MEMBER_NOT_FOUND, "会员找不到");
			}

			Members newMember =
					membersMapper.selectOne(
							new LambdaQueryWrapper<Members>()
									.eq(Members::getCompanyId, companyId)
									.eq(Members::getUserId, userId)
									.last("LIMIT 1"));
			maybeDispatchMemberUpgradeAfterCommit(companyId, newMember, oldGradeId, newGradeId);
		} catch (OpenapiMemberV2FailException e) {
			throw e;
		} catch (Exception e) {
			throw v2Fail(OpenapiErrorCode.SERVICE_ERROR, "系统错误");
		}
	}

	private long validatePlatAccountAsUserId(Map<String, Object> merged) {
		Object raw = merged.get("plat_account");
		if (raw == null) {
			throw missingParams("商派会员Id必填");
		}
		if (!isNumeric(raw)) {
			throw missingParams("商派会员Id必填");
		}
		String text = String.valueOf(raw).trim();
		if (!StringUtils.hasText(text)) {
			throw missingParams("商派会员Id必填");
		}
		return parseNumericLong(raw);
	}

	private String validateGradeId(Map<String, Object> merged) {
		Object raw = merged.get("grade_id");
		if (raw == null) {
			throw missingParams("等级ID参数必填");
		}
		if (!isNumeric(raw)) {
			throw missingParams("等级ID参数必填");
		}
		String externalId = normalizeExternalId(raw);
		if (!StringUtils.hasText(externalId)) {
			throw missingParams("等级ID参数必填");
		}
		return externalId;
	}

	private void validateGradeLevel(Map<String, Object> merged) {
		Object raw = merged.get("grade_level");
		if (raw == null) {
			throw missingParams("等级参数必填");
		}
		if (!(raw instanceof String)) {
			throw missingParams("等级参数必填");
		}
		if (!StringUtils.hasText(((String) raw).trim())) {
			throw missingParams("等级参数必填");
		}
	}

	private void maybeDispatchMemberUpgradeAfterCommit(
			long companyId, Members newMember, long oldGradeId, long newGradeId) {
		Map<String, Object> newGrade =
				openapiMemberBasicInfoGradePort.getGradeByGradeIdForOpenapi(companyId, newGradeId);
		Map<String, Object> oldGrade =
				openapiMemberBasicInfoGradePort.getGradeByGradeIdForOpenapi(companyId, oldGradeId);

		int newGradeLevel =
				phpIntTotalConsumption(newGrade == null ? null : newGrade.get("promotion_condition"));
		int oldGradeLevel =
				phpIntTotalConsumption(oldGrade == null ? null : oldGrade.get("promotion_condition"));

		if (newGradeLevel > oldGradeLevel && newGradeId > oldGradeId) {
			LinkedHashMap<String, Object> activityMemberInfo = new LinkedHashMap<>();
			activityMemberInfo.put("grade_id", newGradeId);
			activityMemberInfo.put("user_id", newMember.getUserId());
			activityMemberInfo.put("mobile", resolvePlainMobileForJob(newMember));
			activityMemberInfo.put("grade_name", newGrade != null ? str(newGrade.get("grade_name")) : "");
			scheduleMemberUpgradeJobAfterCommit(companyId, activityMemberInfo);
		}
	}

	private void scheduleMemberUpgradeJobAfterCommit(long companyId, Map<String, Object> activityMemberInfo) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			firePromotionsActivityDispatchPublisher.publish(companyId, activityMemberInfo, "member_upgrade");
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						firePromotionsActivityDispatchPublisher.publish(
								companyId, activityMemberInfo, "member_upgrade");
					}
				});
	}

	private static int phpIntTotalConsumption(Object promotionCondition) {
		if (!(promotionCondition instanceof Map<?, ?> pc)) {
			return 0;
		}
		Object tc = pc.get("total_consumption");
		if (tc == null) {
			return 0;
		}
		if (tc instanceof Number n) {
			return (int) n.doubleValue();
		}
		try {
			double d = Double.parseDouble(tc.toString().trim());
			return (int) d;
		} catch (Exception e) {
			return 0;
		}
	}

	private static String resolvePlainMobileForJob(Members member) {
		if (StringUtils.hasText(member.getRegionMobile())) {
			return member.getRegionMobile();
		}
		if (StringUtils.hasText(member.getMobile())) {
			return LegacyFixedMobileEncrypt.fixedDecryptLegacyPayload(member.getMobile());
		}
		return "";
	}

	private static String normalizeExternalId(Object raw) {
		return String.valueOf(raw).trim();
	}

	private static boolean isNumeric(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Number num) {
			return Double.isFinite(num.doubleValue());
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return false;
			}
			try {
				double d = Double.parseDouble(t);
				return Double.isFinite(d);
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return false;
	}

	private static long parseNumericLong(Object raw) {
		if (raw instanceof Number num) {
			return num.longValue();
		}
		double d = Double.parseDouble(String.valueOf(raw).trim());
		return (long) d;
	}

	private static long longValue(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}

	private static OpenapiMemberV2FailException missingParams(String message) {
		return new OpenapiMemberV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}

	private static OpenapiMemberV2FailException v2Fail(String code, String message) {
		return new OpenapiMemberV2FailException(code, message);
	}
}
