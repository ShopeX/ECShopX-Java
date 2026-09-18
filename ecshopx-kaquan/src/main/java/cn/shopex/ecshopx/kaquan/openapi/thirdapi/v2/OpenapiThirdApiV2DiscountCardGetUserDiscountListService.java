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
import cn.shopex.ecshopx.common.openapi.OpenapiKaquanV2FailException;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import cn.shopex.ecshopx.kaquan.service.discount.dto.OpenapiUserDiscountListFilterParams;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2DiscountCardGetUserDiscountListService {

	private final MemberAccountService memberAccountService;
	private final UserDiscountMapper userDiscountMapper;
	private final OpenapiDiscountCardV2UserDiscountListPresentationService presentationService;

	public OpenapiThirdApiV2DiscountCardGetUserDiscountListService(
			MemberAccountService memberAccountService,
			UserDiscountMapper userDiscountMapper,
			OpenapiDiscountCardV2UserDiscountListPresentationService presentationService) {
		this.memberAccountService = memberAccountService;
		this.userDiscountMapper = userDiscountMapper;
		this.presentationService = presentationService;
	}

	public Map<String, Object> execute(
			long companyId,
			String platAccount,
			String code,
			int page,
			int pageSize) {
		long userId = parseUserIdForMemberLookup(platAccount);

		String mobile = memberAccountService.resolvePlainMobileForMember(companyId, userId);
		if (!StringUtils.hasText(mobile)) {
			throw new OpenapiKaquanV2FailException(
					OpenapiErrorCode.MEMBER_NOT_FOUND,
					OpenapiDiscountCardV2UserDiscountListPhpMessages.MSG_MEMBER_NOT_FOUND);
		}

		OpenapiUserDiscountListFilterParams filter = new OpenapiUserDiscountListFilterParams();
		filter.setCompanyId(companyId);
		filter.setUserId(userId);
		if (StringUtils.hasText(code)) {
			filter.setCode(code.trim());
		}

		long totalCount = userDiscountMapper.countOpenapiUserDiscountListDistinct(filter);

		List<Map<String, Object>> list;
		if (totalCount <= 0L) {
			list = new ArrayList<>();
		} else {
			long offset = (long) (page - 1) * pageSize;
			list = userDiscountMapper.selectOpenapiUserDiscountListPage(filter, offset, pageSize);
		}

		Map<String, Object> result =
				OpenapiDiscountCardV2ListFormatSupport.formatListStruct(totalCount, list, page, pageSize);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> mutableList = (List<Map<String, Object>>) result.get("list");
		presentationService.apply(mutableList);
		return result;
	}

	private static long parseUserIdForMemberLookup(String platAccount) {
		try {
			return Long.parseLong(platAccount.trim());
		} catch (NumberFormatException e) {
			return -1L;
		}
	}
}
