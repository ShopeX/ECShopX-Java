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

package cn.shopex.ecshopx.promotions.service.schedule;

import cn.shopex.ecshopx.common.cron.PromotionScheduleFireSceneSmsOutPort;
import cn.shopex.ecshopx.common.goods.port.AdminManualRightsAddByItemPort;
import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountReceiveCardService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeUserVipGradeGetService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 与 {@code ScheduleFirePromotionsActivity::handle} 中逐会员路径对齐，执行商品权益与优惠券及可选短信（失败按条吞并打 debug 级别日志）。
 */
@Service
public class PromotionActivityScheduleFireActionService {

	private static final Logger log = LoggerFactory.getLogger(PromotionActivityScheduleFireActionService.class);

	private final AdminManualRightsAddByItemPort adminManualRightsAddByItemPort;
	private final UserDiscountReceiveCardService userDiscountReceiveCardService;
	private final VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService;
	private final MemberAccountService memberAccountService;
	private final PromotionScheduleFireSceneSmsOutPort promotionScheduleFireSceneSmsOutPort;
	private final MembershipSchedulePromotionActivitySupport schedulePromotionActivitySupport;

	public PromotionActivityScheduleFireActionService(
			AdminManualRightsAddByItemPort adminManualRightsAddByItemPort,
			UserDiscountReceiveCardService userDiscountReceiveCardService,
			VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService,
			MemberAccountService memberAccountService,
			PromotionScheduleFireSceneSmsOutPort promotionScheduleFireSceneSmsOutPort,
			MembershipSchedulePromotionActivitySupport schedulePromotionActivitySupport) {
		this.adminManualRightsAddByItemPort = adminManualRightsAddByItemPort;
		this.userDiscountReceiveCardService = userDiscountReceiveCardService;
		this.vipGradeUserVipGradeGetService = vipGradeUserVipGradeGetService;
		this.memberAccountService = memberAccountService;
		this.promotionScheduleFireSceneSmsOutPort = promotionScheduleFireSceneSmsOutPort;
		this.schedulePromotionActivitySupport = schedulePromotionActivitySupport;
	}

	/** @param memberRow 含 user_id/grade_id/mobile 等，与列表查询行一致 */
	public void actionPromotionActivity(
			String activityType, Map<String, Object> activityInfo, Map<String, Object> memberRow) {
		long companyId = longOf(activityInfo.get("company_id"));
		long userId = longOf(memberRow.get("user_id"));
		String mobile = mobilePlain(companyId, userId, memberRow);

		List<ActionIndex> actionList = buildActionList(companyId, userId, memberRow);
		if (actionList.isEmpty()) {
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> discountConfig = (Map<String, Object>) activityInfo.get("discount_config");
		if (discountConfig == null) {
			return;
		}
		Object goodsMapOb = discountConfig.get("goods");
		Object couponMapOb = discountConfig.get("coupons");
		@SuppressWarnings("unchecked")
		Map<String, Object> goodsByIndex = goodsMapOb instanceof Map ? (Map<String, Object>) goodsMapOb : Map.of();
		@SuppressWarnings("unchecked")
		Map<String, Object> couponsByIndex = couponMapOb instanceof Map ? (Map<String, Object>) couponMapOb : Map.of();

		String smsIsOpen = str(activityInfo.get("sms_isopen"));
		for (ActionIndex ai : actionList) {
			String idx = ai.indexKey;
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> items = listOfItemMaps(goodsByIndex.get(idx));
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> coupons = listOfItemMaps(couponsByIndex.get(idx));
			if (!items.isEmpty()) {
				actionItemsPromotions(companyId, items, userId, mobile, schedulePromotionActivitySupport.getSourceFromStr(activityType));
			}
			if (!coupons.isEmpty()) {
				actionItemsCoupons(companyId, coupons, userId, mobile, schedulePromotionActivitySupport.getSourceFromStr(activityType));
			}
			if ("true".equalsIgnoreCase(smsIsOpen)) {
				try {
					promotionScheduleFireSceneSmsOutPort.sendTemplatedSceneSms(
							companyId,
							mobile,
							schedulePromotionActivitySupport.getSmsTmplName(activityType),
							Map.of());
				} catch (Exception e) {
					log.debug("活动促销短信发送失败：{} {}", e.getMessage(), e.toString());
				}
			}
		}
	}

	private void actionItemsPromotions(
			long companyId, List<Map<String, Object>> items, long userId, String mobile, String sourceFrom) {
		for (Map<String, Object> itemInfo : items) {
			if (itemInfo == null) {
				continue;
			}
			long itemId = longOf(itemInfo.get("id"));
			int count = (int) Math.max(1, longOf(itemInfo.getOrDefault("count", 1)));
			if (userId <= 0 || !StringUtils.hasText(mobile) || itemId <= 0) {
				continue;
			}
			for (int c = 0; c < count; c++) {
				try {
					adminManualRightsAddByItemId(itemId, userId, companyId, mobile, sourceFrom);
				} catch (Exception e) {
					log.debug("权益发放：{} => {}", sourceFrom, e.getMessage());
				}
			}
		}
	}

	private void actionItemsCoupons(
			long companyId, List<Map<String, Object>> coupons, long userId, String mobile, String sourceFrom) {
		for (Map<String, Object> couponRow : coupons) {
			if (couponRow == null) {
				continue;
			}
			long cardId = longOf(couponRow.get("id"));
			int count = (int) Math.max(1, longOf(couponRow.getOrDefault("count", 1)));
			if (cardId <= 0) {
				continue;
			}
			for (int i = 0; i < count; i++) {
				try {
					userDiscountReceiveCardService.receiveCard(companyId, userId, mobile, cardId, 0L, "", sourceFrom);
				} catch (Exception e) {
					log.debug("领券：{} => {}", sourceFrom, e.getMessage());
				}
			}
		}
	}

	private void adminManualRightsAddByItemId(
			long itemId, long userId, long companyId, String mobile, String sourceFrom) {
		adminManualRightsAddByItemPort.addRightsByItemId(itemId, userId, companyId, mobile, sourceFrom);
	}

	private String mobilePlain(long companyId, long userId, Map<String, Object> memberRow) {
		String fromAccount = memberAccountService.findMobileStored(companyId, userId);
		if (StringUtils.hasText(fromAccount)) {
			return fromAccount;
		}
		Object m = memberRow.get("mobile");
		return m == null ? "" : m.toString();
	}

	private List<ActionIndex> buildActionList(long companyId, long userId, Map<String, Object> memberRow) {
		List<ActionIndex> out = new ArrayList<>();
		Object g = memberRow.get("grade_id");
		if (g != null) {
			String idx = String.valueOf(((Number) g).longValue());
			out.add(new ActionIndex(idx, shallowCopyRow(memberRow)));
		}
		Map<String, Object> vip = vipGradeUserVipGradeGetService.userVipGradeGet(companyId, userId, false);
		Object isV = vip.get("is_vip");
		if (Boolean.TRUE.equals(isV)) {
			Object vt = vip.get("vip_type");
			if (vt == null) {
				vt = vip.get("lv_type");
			}
			if (vt != null) {
				Map<String, Object> merged = shallowCopyRow(memberRow);
				Object gn = vip.get("grade_name");
				if (gn != null) {
					merged.put("grade_name", gn);
				}
				merged.put("vip_grade_type", vt.toString());
				out.add(new ActionIndex(vt.toString(), merged));
			}
		}
		return out;
	}

	private static Map<String, Object> shallowCopyRow(Map<String, Object> m) {
		return new LinkedHashMap<>(m);
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> listOfItemMaps(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof List<?> list) {
			List<Map<String, Object>> out = new ArrayList<>();
			for (Object o : list) {
				if (o instanceof Map<?, ?>) {
					out.add((Map<String, Object>) o);
				}
			}
			return out;
		}
		return List.of();
	}

	private static long longOf(Object o) {
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

	private record ActionIndex(String indexKey, @SuppressWarnings("unused") Map<String, Object> memberView) {}
}
