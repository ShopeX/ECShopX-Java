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

import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2FailException;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.members.service.admin.MemberUserCardCodeAllocateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberUpdateCardCodeGradeService {

	private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3456789][0-9]{9}$");

	private final MembersMapper membersMapper;
	private final MemberAccountService memberAccountService;
	private final MemberUserCardCodeAllocateService memberUserCardCodeAllocateService;

	public OpenapiThirdApiV2MemberUpdateCardCodeGradeService(
			MembersMapper membersMapper,
			MemberAccountService memberAccountService,
			MemberUserCardCodeAllocateService memberUserCardCodeAllocateService) {
		this.membersMapper = membersMapper;
		this.memberAccountService = memberAccountService;
		this.memberUserCardCodeAllocateService = memberUserCardCodeAllocateService;
	}

	@Transactional(rollbackFor = Exception.class)
	public void executeOpenapiUpdateCardCodeAndGrade(long companyId, Map<String, Object> mergedRaw) {
		try {
			if (companyId <= 0L) {
				throw missingParams("缺少必要参数");
			}

			Map<String, Object> merged = mergedRaw == null ? Map.of() : mergedRaw;
			String mobilePlain = validateMobile(merged);
			validatePatchFieldTypes(merged);

			LinkedHashMap<String, Object> actualUpdateData = new LinkedHashMap<>();
			if (merged.containsKey("user_card_code")) {
				String resolved =
						resolveUserCardCodeForUpdate(
								companyId, stringValue(merged.get("user_card_code")), mobilePlain);
				actualUpdateData.put("user_card_code", resolved);
			}
			if (merged.containsKey("grade_id")) {
				long gradeId = resolveGradeIdForUpdate(companyId, merged.get("grade_id"));
				actualUpdateData.put("grade_id", String.valueOf(gradeId));
			}
			if (actualUpdateData.isEmpty()) {
				return;
			}

			Members oldMember = memberAccountService.findMemberByCompanyAndMobile(companyId, mobilePlain);
			if (oldMember == null || oldMember.getUserId() == null) {
				throw v2Fail(OpenapiErrorCode.MEMBER_NOT_FOUND, "会员找不到");
			}

			long nowSec = System.currentTimeMillis() / 1000L;
			LambdaUpdateWrapper<Members> uw = new LambdaUpdateWrapper<>();
			uw.eq(Members::getCompanyId, companyId).eq(Members::getUserId, oldMember.getUserId());
			if (actualUpdateData.containsKey("user_card_code")) {
				uw.set(Members::getUserCardCode, actualUpdateData.get("user_card_code"));
			}
			if (actualUpdateData.containsKey("grade_id")) {
				uw.set(Members::getGradeId, Long.parseLong((String) actualUpdateData.get("grade_id")));
			}
			uw.set(Members::getUpdated, nowSec);
			int affected = membersMapper.update(null, uw);
			if (affected == 0) {
				throw v2Fail(OpenapiErrorCode.MEMBER_NOT_FOUND, "会员找不到");
			}
		} catch (OpenapiMemberV2FailException e) {
			throw e;
		} catch (Exception e) {
			throw v2Fail(OpenapiErrorCode.SERVICE_ERROR, "系统错误");
		}
	}

	private String validateMobile(Map<String, Object> mergedRaw) {
		Object raw = mergedRaw.get("mobile");
		if (raw == null) {
			throw missingParams("手机号必填");
		}
		String mobile = stringValue(raw);
		if (!StringUtils.hasText(mobile) || !StringUtils.hasText(mobile.trim())) {
			throw missingParams("手机号必填");
		}
		mobile = mobile.trim();
		if (!MOBILE_PATTERN.matcher(mobile).matches()) {
			throw missingParams("手机号填写错误");
		}
		return mobile;
	}

	private void validatePatchFieldTypes(Map<String, Object> mergedRaw) {
		if (mergedRaw.containsKey("user_card_code")) {
			Object raw = mergedRaw.get("user_card_code");
			if (raw != null && !(raw instanceof String)) {
				throw missingParams("会员卡号参数错误");
			}
		}
		if (mergedRaw.containsKey("grade_id")) {
			Object raw = mergedRaw.get("grade_id");
			if (!isNumeric(raw)) {
				throw missingParams("等级ID参数错误");
			}
		}
	}

	private String resolveUserCardCodeForUpdate(long companyId, String rawCardCode, String mobilePlain) {
		String userCardCode = StringUtils.hasText(rawCardCode) ? rawCardCode.trim() : "";
		if (!StringUtils.hasText(userCardCode)) {
			userCardCode = memberUserCardCodeAllocateService.allocateCode();
		}
		Members occupant =
				membersMapper.selectOne(
						new LambdaQueryWrapper<Members>()
								.eq(Members::getCompanyId, companyId)
								.eq(Members::getUserCardCode, userCardCode)
								.last("LIMIT 1"));
		if (occupant != null && occupant.getUserId() != null) {
			String occupantMobile = resolvePlainMobile(occupant);
			if (!mobilePlain.equals(occupantMobile)) {
				throw v2Fail(OpenapiErrorCode.MEMBER_CARD_EXIST, "会员卡号已存在");
			}
		}
		return userCardCode;
	}

	private long resolveGradeIdForUpdate(long companyId, Object rawGradeId) {
		long gradeId = parseNumericLong(rawGradeId);
		Long found = membersMapper.selectGradeIdIfExists(String.valueOf(companyId), gradeId);
		if (found == null) {
			throw v2Fail(OpenapiErrorCode.MEMBER_GRADE_NOT_FOUND, "会员等级找不到");
		}
		return found;
	}

	private static String resolvePlainMobile(Members member) {
		if (StringUtils.hasText(member.getRegionMobile())) {
			return member.getRegionMobile();
		}
		if (StringUtils.hasText(member.getMobile())) {
			return member.getMobile();
		}
		return "";
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

	private static String stringValue(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof String s) {
			return s;
		}
		return String.valueOf(raw);
	}

	private static OpenapiMemberV2FailException missingParams(String message) {
		return new OpenapiMemberV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}

	private static OpenapiMemberV2FailException v2Fail(String code, String message) {
		return new OpenapiMemberV2FailException(code, message);
	}
}
