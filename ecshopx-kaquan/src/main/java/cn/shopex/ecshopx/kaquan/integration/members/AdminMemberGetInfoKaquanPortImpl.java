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

import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountValidCountForMemberService;
import cn.shopex.ecshopx.kaquan.service.membercard.MemberCardGradeSingleRowQueryService;
import cn.shopex.ecshopx.kaquan.service.membercard.MemberCardSetService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeUserVipGradeGetService;
import cn.shopex.ecshopx.members.integration.admin.AdminMemberGetInfoKaquanPort;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service("adminMemberGetInfoKaquanPortImpl")
public class AdminMemberGetInfoKaquanPortImpl implements AdminMemberGetInfoKaquanPort {

	private static final Pattern INTEGER_STRING = Pattern.compile("-?(0|[1-9]\\d*)");

	private final MemberCardSetService memberCardSetService;
	private final MemberCardGradeSingleRowQueryService memberCardGradeSingleRowQueryService;
	private final VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService;
	private final UserDiscountValidCountForMemberService userDiscountValidCountForMemberService;

	public AdminMemberGetInfoKaquanPortImpl(
			MemberCardSetService memberCardSetService,
			MemberCardGradeSingleRowQueryService memberCardGradeSingleRowQueryService,
			VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService,
			UserDiscountValidCountForMemberService userDiscountValidCountForMemberService) {
		this.memberCardSetService = memberCardSetService;
		this.memberCardGradeSingleRowQueryService = memberCardGradeSingleRowQueryService;
		this.vipGradeUserVipGradeGetService = vipGradeUserVipGradeGetService;
		this.userDiscountValidCountForMemberService = userDiscountValidCountForMemberService;
	}

	@Override
	public void applyMemberCardSnapshot(long companyId, Map<String, Object> result) {
		Map<String, Object> card = memberCardSetService.getMemberCard(companyId);
		if (card == null || card.isEmpty()) {
			result.put("cardInfo", Collections.emptyList());
		} else {
			result.put("cardInfo", card);
		}
	}

	@Override
	public void applyGradeInfo(long companyId, Map<String, Object> result) {
		OptionalLong resolved = resolveGradeIdForLoad(result.get("grade_id"));
		if (resolved.isEmpty()) {
			result.remove("_acceptLanguageForGrade");
			result.put("gradeInfo", null);
			return;
		}
		Object langAttr = result.get("_acceptLanguageForGrade");
		String lang =
				langAttr instanceof String s && !s.isBlank() ? s : "zh-CN";
		Map<String, Object> loaded =
				memberCardGradeSingleRowQueryService.loadForAdmin(companyId, resolved.getAsLong(), lang);
		result.remove("_acceptLanguageForGrade");
		if (loaded == null || loaded.isEmpty()) {
			result.put("gradeInfo", null);
		} else {
			result.put("gradeInfo", new LinkedHashMap<>(loaded));
		}
	}

	@Override
	public void applyVipGrade(long companyId, Map<String, Object> result) {
		Object uidObj = result.get("user_id");
		long uid = uidObj instanceof Number n ? n.longValue() : 0L;
		Object vipObj = vipGradeUserVipGradeGetService.userVipGradeGet(companyId, uid, false);
		if (vipObj == null) {
			result.put("vipgrade", Collections.singletonMap("is_vip", Boolean.FALSE));
			return;
		}
		if (vipObj instanceof List<?>) {
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
	public void applyCouponNum(long companyId, Map<String, Object> result) {
		Object uidObj = result.get("user_id");
		long uid = uidObj instanceof Number n ? n.longValue() : 0L;
		long rowCount = userDiscountValidCountForMemberService.countValidForMember(companyId, uid);
		int couponNum = (int) Math.min(rowCount, (long) Integer.MAX_VALUE);
		result.put("coupon_num", Integer.valueOf(couponNum));
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
