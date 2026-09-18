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

package cn.shopex.ecshopx.kaquan.integration.members;

import cn.shopex.ecshopx.common.members.port.WxappMemberInfoKaquanPort;
import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountValidCountForMemberService;
import cn.shopex.ecshopx.kaquan.service.membercard.MemberCardGradeNextForWxappService;
import cn.shopex.ecshopx.kaquan.service.membercard.MemberCardGradeSingleRowQueryService;
import cn.shopex.ecshopx.kaquan.service.membercard.MemberCardSetService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeUserVipGradeGetService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service("wxappMemberInfoKaquanPortImpl")
public class WxappMemberInfoKaquanPortImpl implements WxappMemberInfoKaquanPort {

	private static final Pattern INTEGER_STRING = Pattern.compile("-?(0|[1-9]\\d*)");

	private final MemberCardSetService memberCardSetService;
	private final MemberCardGradeSingleRowQueryService memberCardGradeSingleRowQueryService;
	private final MemberCardGradeNextForWxappService memberCardGradeNextForWxappService;
	private final VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService;
	private final UserDiscountValidCountForMemberService userDiscountValidCountForMemberService;

	public WxappMemberInfoKaquanPortImpl(
			MemberCardSetService memberCardSetService,
			MemberCardGradeSingleRowQueryService memberCardGradeSingleRowQueryService,
			MemberCardGradeNextForWxappService memberCardGradeNextForWxappService,
			VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService,
			UserDiscountValidCountForMemberService userDiscountValidCountForMemberService) {
		this.memberCardSetService = memberCardSetService;
		this.memberCardGradeSingleRowQueryService = memberCardGradeSingleRowQueryService;
		this.memberCardGradeNextForWxappService = memberCardGradeNextForWxappService;
		this.vipGradeUserVipGradeGetService = vipGradeUserVipGradeGetService;
		this.userDiscountValidCountForMemberService = userDiscountValidCountForMemberService;
	}

	@Override
	public Map<String, Object> loadMemberCard(long companyId) {
		Map<String, Object> card = memberCardSetService.getMemberCard(companyId);
		if (card == null || card.isEmpty()) {
			return Collections.emptyMap();
		}
		return card;
	}

	@Override
	public void applyGradeAndNext(long companyId, Map<String, Object> memberInfo, String acceptLanguage) {
		OptionalLong resolved = resolveGradeIdForLoad(memberInfo.get("grade_id"));
		if (resolved.isEmpty()) {
			memberInfo.put("gradeInfo", null);
			memberInfo.put("nextGradeInfo", Collections.emptyList());
			return;
		}
		String lang =
				acceptLanguage != null && !acceptLanguage.isBlank() ? acceptLanguage.trim() : "zh-CN";
		Map<String, Object> loaded =
				memberCardGradeSingleRowQueryService.loadForAdmin(companyId, resolved.getAsLong(), lang);
		if (loaded == null || loaded.isEmpty()) {
			memberInfo.put("gradeInfo", null);
		} else {
			memberInfo.put("gradeInfo", new LinkedHashMap<>(loaded));
		}
		long gradeIdForNext = resolved.getAsLong();
		memberCardGradeNextForWxappService.putNextGradeInfo(companyId, gradeIdForNext, memberInfo);
	}

	@Override
	public void putVipGrade(long companyId, long userId, Map<String, Object> result) {
		Object vipObj = vipGradeUserVipGradeGetService.userVipGradeGet(companyId, userId, false);
		if (vipObj == null) {
			result.put("vipgrade", Collections.singletonMap("is_vip", Boolean.FALSE));
			return;
		}
		if (vipObj instanceof java.util.List<?>) {
			result.put("vipgrade", Collections.singletonMap("is_vip", Boolean.FALSE));
			return;
		}
		if (!(vipObj instanceof Map<?, ?> vipMap)) {
			result.put("vipgrade", Collections.singletonMap("is_vip", Boolean.FALSE));
			return;
		}
		if (vipMap.isEmpty()) {
			result.put("vipgrade", Collections.singletonMap("is_vip", Boolean.FALSE));
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> vip = (Map<String, Object>) vipMap;
		result.put("vipgrade", vip);
	}

	@Override
	public long countUserDiscountWithStatusOne(long companyId, long userId) {
		return userDiscountValidCountForMemberService.countStatusOneOnly(companyId, userId);
	}

	@Override
	public long countCouponStyleUserDiscount(long companyId, long userId) {
		return userDiscountValidCountForMemberService.countAllStatusesForMember(companyId, userId);
	}

	@Override
	public long countStatisticalValidUserDiscount(long companyId, long userId) {
		return userDiscountValidCountForMemberService.countValidForMember(companyId, userId);
	}

	private static OptionalLong resolveGradeIdForLoad(Object raw) {
		if (raw == null) {
			return OptionalLong.empty();
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty() || "0".equals(t)) {
				return OptionalLong.empty();
			}
			if (!INTEGER_STRING.matcher(t).matches()) {
				return OptionalLong.empty();
			}
			try {
				return OptionalLong.of(Long.parseLong(t));
			} catch (NumberFormatException e) {
				return OptionalLong.empty();
			}
		}
		if (raw instanceof Number n) {
			if (n.longValue() == 0L) {
				return OptionalLong.empty();
			}
			return OptionalLong.of(n.longValue());
		}
		return OptionalLong.empty();
	}
}
