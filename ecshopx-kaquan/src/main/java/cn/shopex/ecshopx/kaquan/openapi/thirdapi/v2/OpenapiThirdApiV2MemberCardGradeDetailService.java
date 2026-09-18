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

package cn.shopex.ecshopx.kaquan.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2FailException;
import cn.shopex.ecshopx.kaquan.domain.MemberCardGrade;
import cn.shopex.ecshopx.kaquan.mapper.MemberCardGradeMapper;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberCardGradeDetailService {

	private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3456789][0-9]{9}$");

	private final MemberAccountService memberAccountService;
	private final MemberCardGradeMapper memberCardGradeMapper;
	private final ObjectMapper objectMapper;

	public OpenapiThirdApiV2MemberCardGradeDetailService(
			MemberAccountService memberAccountService,
			MemberCardGradeMapper memberCardGradeMapper,
			ObjectMapper objectMapper) {
		this.memberAccountService = memberAccountService;
		this.memberCardGradeMapper = memberCardGradeMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> executeOpenapiDetail(long companyId, String mobileRaw) {
		String mobilePlain = validateMobile(mobileRaw);

		Members member = memberAccountService.findMemberByCompanyAndMobile(companyId, mobilePlain);
		if (member == null) {
			throw new OpenapiMemberV2FailException(OpenapiErrorCode.MEMBER_NOT_FOUND, "会员找不到");
		}

		long gradeId = member.getGradeId() != null ? member.getGradeId() : 0L;

		String companyIdStr = String.valueOf(companyId);
		MemberCardGrade entity = memberCardGradeMapper.selectOne(
				new LambdaQueryWrapper<MemberCardGrade>()
						.eq(MemberCardGrade::getCompanyId, companyIdStr)
						.eq(MemberCardGrade::getGradeId, gradeId)
						.select(
								MemberCardGrade::getGradeId,
								MemberCardGrade::getGradeName,
								MemberCardGrade::getDefaultGrade,
								MemberCardGrade::getBackgroundPicUrl,
								MemberCardGrade::getPrivileges,
								MemberCardGrade::getPromotionCondition,
								MemberCardGrade::getExternalId,
								MemberCardGrade::getCreated,
								MemberCardGrade::getUpdated));
		Map<String, Object> row = entity != null
				? OpenapiMemberCardGradeOpenApiFormatSupport.toOverlayRow(entity)
				: new LinkedHashMap<>();

		Map<String, Object> out =
				OpenapiMemberCardGradeOpenApiFormatSupport.formatOpenApiGradeRow(row, objectMapper);
		out.remove("is_default");
		return out;
	}

	private String validateMobile(String mobileRaw) {
		if (mobileRaw == null) {
			throw missingParams("缺少参数");
		}
		String trimmed = mobileRaw.trim();
		if (!StringUtils.hasText(trimmed) || !MOBILE_PATTERN.matcher(trimmed).matches()) {
			throw missingParams("缺少参数");
		}
		return trimmed;
	}

	private static OpenapiMemberV2FailException missingParams(String message) {
		return new OpenapiMemberV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}
}
