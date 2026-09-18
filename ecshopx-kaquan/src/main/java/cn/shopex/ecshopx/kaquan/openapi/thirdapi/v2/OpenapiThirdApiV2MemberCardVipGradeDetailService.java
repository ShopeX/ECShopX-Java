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
import cn.shopex.ecshopx.kaquan.domain.VipGrade;
import cn.shopex.ecshopx.kaquan.domain.VipGradeRelUser;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeMapper;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeRelUserMapper;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberCardVipGradeDetailService {

	private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3456789][0-9]{9}$");

	private final MemberAccountService memberAccountService;
	private final VipGradeRelUserMapper vipGradeRelUserMapper;
	private final VipGradeMapper vipGradeMapper;
	private final ObjectMapper objectMapper;

	public OpenapiThirdApiV2MemberCardVipGradeDetailService(
			MemberAccountService memberAccountService,
			VipGradeRelUserMapper vipGradeRelUserMapper,
			VipGradeMapper vipGradeMapper,
			ObjectMapper objectMapper) {
		this.memberAccountService = memberAccountService;
		this.vipGradeRelUserMapper = vipGradeRelUserMapper;
		this.vipGradeMapper = vipGradeMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> executeOpenapiDetail(long companyId, String mobileRaw) {
		String mobilePlain = validateMobile(mobileRaw);

		Members member = memberAccountService.findMemberByCompanyAndMobile(companyId, mobilePlain);
		if (member == null) {
			throw new OpenapiMemberV2FailException(OpenapiErrorCode.MEMBER_NOT_FOUND, "会员找不到");
		}
		Long userId = member.getUserId();
		if (userId == null) {
			throw new OpenapiMemberV2FailException(OpenapiErrorCode.MEMBER_NOT_FOUND, "会员找不到");
		}

		VipGradeRelUser rel = vipGradeRelUserMapper.selectOne(
				new LambdaQueryWrapper<VipGradeRelUser>()
						.eq(VipGradeRelUser::getCompanyId, (int) companyId)
						.eq(VipGradeRelUser::getUserId, userId)
						.select(
								VipGradeRelUser::getVipGradeId,
								VipGradeRelUser::getVipType,
								VipGradeRelUser::getEndDate));

		VipGrade entity = null;
		if (rel != null && rel.getVipGradeId() != null) {
			entity = vipGradeMapper.selectOne(
					new LambdaQueryWrapper<VipGrade>()
							.eq(VipGrade::getCompanyId, (int) companyId)
							.eq(VipGrade::getVipGradeId, rel.getVipGradeId())
							.select(
									VipGrade::getVipGradeId,
									VipGrade::getLvType,
									VipGrade::getGradeName,
									VipGrade::getGuideTitle,
									VipGrade::getIsDefault,
									VipGrade::getIsDisabled,
									VipGrade::getBackgroundPicUrl,
									VipGrade::getPriceList,
									VipGrade::getPrivileges,
									VipGrade::getDescription,
									VipGrade::getExternalId,
									VipGrade::getCreated,
									VipGrade::getUpdated));
		}

		Map<String, Object> out =
				OpenapiMemberCardVipGradeOpenApiFormatSupport.formatOpenApiVipGradeRow(entity, objectMapper);
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
