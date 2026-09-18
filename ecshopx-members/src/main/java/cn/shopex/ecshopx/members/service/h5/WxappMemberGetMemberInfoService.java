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

package cn.shopex.ecshopx.members.service.h5;

import cn.shopex.ecshopx.common.members.port.WxappMemberEmployeePurchaseFlagsPort;
import cn.shopex.ecshopx.common.members.port.WxappMemberInfoKaquanPort;
import cn.shopex.ecshopx.common.members.port.WxappMemberPointRuleAndBalancePort;
import cn.shopex.ecshopx.common.members.port.WxappMemberPopularizeFlagsPort;
import cn.shopex.ecshopx.common.members.port.WxappMemberSalespersonListPort;
import cn.shopex.ecshopx.common.members.port.WxappMemberSelfDeliveryStaffListPort;
import cn.shopex.ecshopx.common.members.port.WxappMemberSpecificCrowdDiscountPort;
import cn.shopex.ecshopx.common.members.port.WxappMemberWebUrlSettingPort;
import cn.shopex.ecshopx.common.members.port.WxappMemberWechatUserRowPort;
import cn.shopex.ecshopx.common.port.shuyun.ShuyunOpenPlatformMemberEnhanceMergePort;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.companys.service.setting.RechargeSettingRedisService;
import cn.shopex.ecshopx.members.integration.admin.AdminMemberGetInfoConfigRequestFieldsPort;
import cn.shopex.ecshopx.members.integration.admin.AdminMemberGetInfoDepositTotalPort;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.members.service.h5.support.WxappMemberH5DataMasking;
import cn.shopex.ecshopx.members.service.h5.support.WxappMemberH5QueryParamEmpty;
import cn.shopex.ecshopx.members.service.stats.MemberTotalConsumptionReadService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappMemberGetMemberInfoService {

	private final MemberAccountService memberAccountService;
	private final AdminMemberGetInfoConfigRequestFieldsPort adminMemberGetInfoConfigRequestFieldsPort;
	private final AdminMemberGetInfoDepositTotalPort adminMemberGetInfoDepositTotalPort;
	private final WxappMemberEmployeePurchaseFlagsPort wxappMemberEmployeePurchaseFlagsPort;
	private final WxappMemberInfoKaquanPort wxappMemberInfoKaquanPort;
	private final MemberTotalConsumptionReadService memberTotalConsumptionReadService;
	private final WxappMemberWechatUserRowPort wxappMemberWechatUserRowPort;
	private final WxappMemberPointRuleAndBalancePort wxappMemberPointRuleAndBalancePort;
	private final WxappMemberSalespersonListPort wxappMemberSalespersonListPort;
	private final WxappMemberSelfDeliveryStaffListPort wxappMemberSelfDeliveryStaffListPort;
	private final WxappMemberPopularizeFlagsPort wxappMemberPopularizeFlagsPort;
	private final WxappMemberSpecificCrowdDiscountPort wxappMemberSpecificCrowdDiscountPort;
	private final WxappMemberWebUrlSettingPort wxappMemberWebUrlSettingPort;
	private final RechargeSettingRedisService rechargeSettingRedisService;
	private final LangueProperties langueProperties;
	private final ShuyunOpenPlatformMemberEnhanceMergePort openPlatformMemberEnhanceMergePort;
	private final boolean oemShuyun;

	public WxappMemberGetMemberInfoService(
			MemberAccountService memberAccountService,
			AdminMemberGetInfoConfigRequestFieldsPort adminMemberGetInfoConfigRequestFieldsPort,
			AdminMemberGetInfoDepositTotalPort adminMemberGetInfoDepositTotalPort,
			WxappMemberEmployeePurchaseFlagsPort wxappMemberEmployeePurchaseFlagsPort,
			WxappMemberInfoKaquanPort wxappMemberInfoKaquanPort,
			MemberTotalConsumptionReadService memberTotalConsumptionReadService,
			WxappMemberWechatUserRowPort wxappMemberWechatUserRowPort,
			WxappMemberPointRuleAndBalancePort wxappMemberPointRuleAndBalancePort,
			WxappMemberSalespersonListPort wxappMemberSalespersonListPort,
			WxappMemberSelfDeliveryStaffListPort wxappMemberSelfDeliveryStaffListPort,
			WxappMemberPopularizeFlagsPort wxappMemberPopularizeFlagsPort,
			WxappMemberSpecificCrowdDiscountPort wxappMemberSpecificCrowdDiscountPort,
			WxappMemberWebUrlSettingPort wxappMemberWebUrlSettingPort,
			RechargeSettingRedisService rechargeSettingRedisService,
			LangueProperties langueProperties,
			ShuyunOpenPlatformMemberEnhanceMergePort openPlatformMemberEnhanceMergePort,
			@Value("${common.oem-shuyun:false}") boolean oemShuyun) {
		this.memberAccountService = memberAccountService;
		this.adminMemberGetInfoConfigRequestFieldsPort = adminMemberGetInfoConfigRequestFieldsPort;
		this.adminMemberGetInfoDepositTotalPort = adminMemberGetInfoDepositTotalPort;
		this.wxappMemberEmployeePurchaseFlagsPort = wxappMemberEmployeePurchaseFlagsPort;
		this.wxappMemberInfoKaquanPort = wxappMemberInfoKaquanPort;
		this.memberTotalConsumptionReadService = memberTotalConsumptionReadService;
		this.wxappMemberWechatUserRowPort = wxappMemberWechatUserRowPort;
		this.wxappMemberPointRuleAndBalancePort = wxappMemberPointRuleAndBalancePort;
		this.wxappMemberSalespersonListPort = wxappMemberSalespersonListPort;
		this.wxappMemberSelfDeliveryStaffListPort = wxappMemberSelfDeliveryStaffListPort;
		this.wxappMemberPopularizeFlagsPort = wxappMemberPopularizeFlagsPort;
		this.wxappMemberSpecificCrowdDiscountPort = wxappMemberSpecificCrowdDiscountPort;
		this.wxappMemberWebUrlSettingPort = wxappMemberWebUrlSettingPort;
		this.rechargeSettingRedisService = rechargeSettingRedisService;
		this.langueProperties = langueProperties;
		this.openPlatformMemberEnhanceMergePort = openPlatformMemberEnhanceMergePort;
		this.oemShuyun = oemShuyun;
	}

	public Map<String, Object> getMemberInfo(
			long companyId,
			long authUserId,
			Map<String, Object> claims,
			String activityIdRaw,
			String inviteCodeRaw,
			HttpServletRequest request) {
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		Map<String, Object> cardInfo = wxappMemberInfoKaquanPort.loadMemberCard(companyId);

		String mobilePlainForOuterScope = "";
		boolean memberTruthy = false;
		LinkedHashMap<String, Object> memberInfo = null;
		String plainMobileForStaff = "";

		if (authUserId > 0L) {
			Map<String, Object> raw = memberAccountService.getMemberInfo(authUserId, companyId);
			memberInfo = raw == null ? null : new LinkedHashMap<>(raw);
			if (memberInfo != null) {
				memberInfo.remove("region_mobile");
			}
			memberTruthy = memberInfo != null && !memberInfo.isEmpty();
			if (memberTruthy) {
				mobilePlainForOuterScope =
						String.valueOf(memberInfo.getOrDefault("mobile", "")).trim();
				plainMobileForStaff = mobilePlainForOuterScope;
				adminMemberGetInfoConfigRequestFieldsPort.enrichMemberInfo(companyId, memberInfo, request);
				// D8：数云 enhance 合并资料（失败不影响本地展示）；对齐 PHP Front getMemberInfo
				openPlatformMemberEnhanceMergePort.mergeEnhanceIntoMemberInfo(
						companyId, authUserId, memberInfo);
				wxappMemberEmployeePurchaseFlagsPort.applyEmployeeFlag(companyId, authUserId, memberInfo);
				if (WxappMemberH5QueryParamEmpty.isActivityIdTruthy(activityIdRaw)) {
					long aid = WxappMemberH5QueryParamEmpty.parseActivityIdAsLong(activityIdRaw);
					wxappMemberEmployeePurchaseFlagsPort.applyActivityRelativeFlags(
							companyId, aid, authUserId, memberInfo);
				} else if (WxappMemberH5QueryParamEmpty.isInviteCodeTruthy(inviteCodeRaw)) {
					wxappMemberEmployeePurchaseFlagsPort.applyInviteRelativeFlags(
							companyId, authUserId, memberInfo, inviteCodeRaw.trim());
				}
				String acceptLanguage = RequestLangTag.current(langueProperties);
				wxappMemberInfoKaquanPort.applyGradeAndNext(companyId, memberInfo, acceptLanguage);
				memberInfo.put(
						"totalConsumption",
						memberTotalConsumptionReadService.getTotalConsumption(authUserId));
				Map<String, Object> wechatRow = wxappMemberWechatUserRowPort.loadByUserId(companyId, authUserId);
				if (wechatRow.get("nickname") != null
						&& StringUtils.hasText(String.valueOf(wechatRow.get("nickname")).trim())) {
					memberInfo.put("nickname", wechatRow.get("nickname"));
				}
				memberInfo.put("open_id", Objects.toString(claims.getOrDefault("open_id", ""), ""));
				WxappMemberH5DataMasking.applyMemberInfoMobileBirthdayAddressSex(memberInfo, oemShuyun);
				if (claims.get("headimgurl") != null
						&& StringUtils.hasText(String.valueOf(claims.get("headimgurl")).trim())) {
					memberInfo.put("avatar", claims.get("headimgurl"));
				}
				if (claims.get("nickname") != null
						&& StringUtils.hasText(String.valueOf(claims.get("nickname")).trim())) {
					memberInfo.put("nickname", claims.get("nickname"));
				}
				Object uname = memberInfo.get("username");
				if (uname == null || !StringUtils.hasText(String.valueOf(uname).trim())) {
					Object nick = wechatRow.get("nickname");
					memberInfo.put("username", nick != null ? String.valueOf(nick) : "");
				}
				Object av = memberInfo.get("avatar");
				if (av == null || !StringUtils.hasText(String.valueOf(av).trim())) {
					Object head = wechatRow.get("headimgurl");
					memberInfo.put("avatar", head != null ? String.valueOf(head) : "");
				}
				List<Map<String, Object>> salesPersonList =
						wxappMemberSalespersonListPort.listForMember(companyId, authUserId, 100);
				List<Map<String, Object>> deliveryRows =
						wxappMemberSelfDeliveryStaffListPort.listByPlainMobile(companyId, plainMobileForStaff);
				List<Map<String, Object>> deliveryList = deliveryRows == null ? List.of() : deliveryRows;
				LinkedHashMap<String, Object> deliveryStaffWrapper = new LinkedHashMap<>();
				deliveryStaffWrapper.put("total_count", deliveryList.size());
				deliveryStaffWrapper.put("list", deliveryList);
				result.put("memberInfo", memberInfo);
				result.put("salesPersonList", salesPersonList == null ? List.of() : salesPersonList);
				result.put("deliveryStaffList", deliveryStaffWrapper);
			}

			int userDiscountCount =
					(int)
							Math.min(
									Integer.MAX_VALUE,
									wxappMemberInfoKaquanPort.countUserDiscountWithStatusOne(companyId, authUserId));
			long fen = adminMemberGetInfoDepositTotalPort.readTotalFen(companyId, authUserId);
			int depositJson;
			if (fen < 0L) {
				depositJson = 0;
			} else if (fen > Integer.MAX_VALUE) {
				depositJson = Integer.MAX_VALUE;
			} else {
				depositJson = (int) fen;
			}
			int coupon =
					(int)
							Math.min(
									Integer.MAX_VALUE,
									wxappMemberInfoKaquanPort.countCouponStyleUserDiscount(companyId, authUserId));
			result.put("userDiscountCount", userDiscountCount);
			result.put("deposit", Integer.valueOf(depositJson));
			result.put("coupon", coupon);
		}

		result.put("point_open_status", Boolean.FALSE);
		Map<String, Object> rule = wxappMemberPointRuleAndBalancePort.loadRule(companyId);
		if ("true".equals(String.valueOf(rule.get("isOpenMemberPoint")).trim())) {
			result.put("point_open_status", Boolean.TRUE);
			if (authUserId > 0L) {
				Map<String, Object> display =
						wxappMemberPointRuleAndBalancePort.loadWxappPointDisplay(
								companyId, authUserId, mobilePlainForOuterScope);
				result.put("point", extractPointScalar(display.get("point")));
				if (display.containsKey("frozen_point")) {
					result.put("frozen_point", extractPointScalar(display.get("frozen_point")));
				}
			} else {
				result.put("point", 0L);
			}
		} else {
			result.put("point", 0L);
		}

		result.put(
				"cardInfo",
				cardInfo == null || cardInfo.isEmpty() ? Collections.emptyMap() : cardInfo);
		wxappMemberInfoKaquanPort.putVipGrade(companyId, authUserId, result);
		wxappMemberPopularizeFlagsPort.apply(companyId, authUserId, claims, result);
		wxappMemberSpecificCrowdDiscountPort.apply(companyId, authUserId, result);
		wxappMemberWebUrlSettingPort.applyClasshourArranged(companyId, result);
		Map<String, Object> rechargeOpen = rechargeSettingRedisService.handle(companyId, Collections.emptyMap());
		result.put("is_recharge_status", rechargeOpen.get("recharge_status"));
		return result;
	}

	private static long extractPointScalar(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw == null) {
			return 0L;
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

}
