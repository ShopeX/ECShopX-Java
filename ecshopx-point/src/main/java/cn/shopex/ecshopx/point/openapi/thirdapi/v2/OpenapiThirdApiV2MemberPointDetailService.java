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

package cn.shopex.ecshopx.point.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2FailException;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.point.service.PointMemberBalanceReadService;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberPointDetailService {

	private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3456789][0-9]{9}$");

	private final MemberAccountService memberAccountService;
	private final PointMemberBalanceReadService pointMemberBalanceReadService;

	public OpenapiThirdApiV2MemberPointDetailService(
			MemberAccountService memberAccountService,
			PointMemberBalanceReadService pointMemberBalanceReadService) {
		this.memberAccountService = memberAccountService;
		this.pointMemberBalanceReadService = pointMemberBalanceReadService;
	}

	public Map<String, Object> executeOpenapiDetail(long companyId, String mobileRaw) {
		String mobilePlain = validateMobile(mobileRaw);

		Members member = memberAccountService.findMemberByCompanyAndMobile(companyId, mobilePlain);
		long userId = member != null && member.getUserId() != null ? member.getUserId() : 0L;

		long point = 0L;
		if (userId > 0L) {
			point = pointMemberBalanceReadService.getPointBalance(companyId, userId);
		}

		return Map.of("point", (int) point);
	}

	private String validateMobile(String mobileRaw) {
		if (mobileRaw == null) {
			throw missingParams("手机号必填");
		}
		if (!StringUtils.hasText(mobileRaw) || !StringUtils.hasText(mobileRaw.trim())) {
			throw missingParams("手机号必填");
		}
		String mobile = mobileRaw.trim();
		if (!MOBILE_PATTERN.matcher(mobile).matches()) {
			throw missingParams("手机号填写错误");
		}
		return mobile;
	}

	private static OpenapiMemberV2FailException missingParams(String message) {
		return new OpenapiMemberV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}
}
