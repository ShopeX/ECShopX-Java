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
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import cn.shopex.ecshopx.kaquan.domain.UserDiscountCardAggRow;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.mapper.RelItemsMapper;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardUserReceiveFieldNormalizer;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardsRowMapperService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountNewGiftCardUpdateService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeUserVipGradeGetService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.salesperson.service.SalespersonTaskCouponCompleteService;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmDiscountCardSendPort;
import cn.shopex.ecshopx.wechat.service.WxaTemplateMsgCouponNotifyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2DiscountCardUserGetCardService {

	private static final String SOURCE_MALL_LOCAL = "商城本地领取";

	private static final String SOURCE_TURNTABLE = "大转盘中奖领取";

	private static final String SOURCE_DM_BATCH = "达摩CRM批量投放事件";

	private static final String SOURCE_DM_RECEIVE = "达摩CRM领取事件";

	private final MemberAccountService memberAccountService;
	private final DiscountCardsMapper discountCardsMapper;
	private final RelItemsMapper relItemsMapper;
	private final DiscountCardsRowMapperService discountCardsRowMapperService;
	private final UserDiscountMapper userDiscountMapper;
	private final VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService;
	private final DmCrmDiscountCardSendPort dmCrmDiscountCardSendPort;
	private final SalespersonTaskCouponCompleteService salespersonTaskCouponCompleteService;
	private final WxaTemplateMsgCouponNotifyService wxaTemplateMsgCouponNotifyService;
	private final OpenapiDiscountCardV2SendPresentationService presentationService;
	private final ObjectMapper objectMapper;

	public OpenapiThirdApiV2DiscountCardUserGetCardService(
			MemberAccountService memberAccountService,
			DiscountCardsMapper discountCardsMapper,
			RelItemsMapper relItemsMapper,
			DiscountCardsRowMapperService discountCardsRowMapperService,
			UserDiscountMapper userDiscountMapper,
			VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService,
			DmCrmDiscountCardSendPort dmCrmDiscountCardSendPort,
			SalespersonTaskCouponCompleteService salespersonTaskCouponCompleteService,
			WxaTemplateMsgCouponNotifyService wxaTemplateMsgCouponNotifyService,
			OpenapiDiscountCardV2SendPresentationService presentationService,
			ObjectMapper objectMapper) {
		this.memberAccountService = memberAccountService;
		this.discountCardsMapper = discountCardsMapper;
		this.relItemsMapper = relItemsMapper;
		this.discountCardsRowMapperService = discountCardsRowMapperService;
		this.userDiscountMapper = userDiscountMapper;
		this.vipGradeUserVipGradeGetService = vipGradeUserVipGradeGetService;
		this.dmCrmDiscountCardSendPort = dmCrmDiscountCardSendPort;
		this.salespersonTaskCouponCompleteService = salespersonTaskCouponCompleteService;
		this.wxaTemplateMsgCouponNotifyService = wxaTemplateMsgCouponNotifyService;
		this.presentationService = presentationService;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> execute(
			long companyId,
			String platAccount,
			String cardIdRaw,
			String sourceType,
			String activityName) {
		long userId = parseUserId(platAccount);
		String mobile = memberAccountService.resolveMemberMobileForH5Context(userId, companyId);
		if (!StringUtils.hasText(mobile)) {
			throw fail(OpenapiDiscountCardV2SendPhpMessages.MSG_MEMBER_NOT_FOUND);
		}
		String activity = activityName == null ? "" : activityName;
		Map<String, Object> inner =
				doUserGetCard(companyId, userId, mobile, cardIdRaw, sourceType, activity);
		return presentationService.toOpenApiData(inner);
	}

	private Map<String, Object> doUserGetCard(
			long companyId,
			long userId,
			String mobile,
			String cardIdRaw,
			String sourceType,
			String activityName) {
		if (companyId <= 0L) {
			throw fail(OpenapiDiscountCardV2SendPhpMessages.MSG_COUPON_RECORD_ADD_FAILED);
		}

		long cardId = parseCardId(cardIdRaw);

		DiscountCards entity =
				discountCardsMapper.selectOne(
						new LambdaQueryWrapper<DiscountCards>()
								.eq(DiscountCards::getCompanyId, companyId)
								.eq(DiscountCards::getCardId, cardId)
								.last("LIMIT 1"));
		if (entity == null) {
			throw fail(OpenapiDiscountCardV2SendPhpMessages.MSG_COUPON_NOT_EXIST);
		}

		long nowSec = System.currentTimeMillis() / 1000L;
		Integer end = entity.getEndDate();
		if (end != null && end > 0 && end <= nowSec) {
			throw fail(OpenapiDiscountCardV2SendPhpMessages.MSG_COUPON_EXPIRED);
		}

		int issued = countIssuedForCard(companyId, cardId);
		int quantity = entity.getQuantity() == null ? 0 : entity.getQuantity();
		if (quantity <= issued) {
			throw fail(OpenapiDiscountCardV2SendPhpMessages.MSG_COUPON_OUT_OF_STOCK);
		}

		Map<String, Object> cardInfo =
				DiscountCardUserReceiveFieldNormalizer.normalize(
						discountCardsRowMapperService, objectMapper, entity, relItemsMapper);

		String dmCardCode = "";
		if (shouldSendDamoCoupon(companyId, sourceType, entity)) {
			String dmCardId = entity.getDmCardId() == null ? "" : entity.getDmCardId().trim();
			dmCardCode = dmCrmDiscountCardSendPort.sendCoupon(companyId, userId, cardId, dmCardId, mobile);
			if (dmCardCode == null) {
				dmCardCode = "";
			}
		}

		allocNumericCode(companyId);

		int kq = entity.getKqStatus() == null ? -1 : entity.getKqStatus();
		if (kq != DiscountNewGiftCardUpdateService.STATUS_NORMAL) {
			throw fail(OpenapiDiscountCardV2SendPhpMessages.MSG_STATUS_ABNORMAL);
		}

		if (SOURCE_MALL_LOCAL.equals(sourceType)) {
			validateMallLocalGradesOrVip(companyId, userId, cardInfo);
		}

		int userGetNum = countIssuedForUser(companyId, userId, cardId);
		int getLimit = entity.getGetLimit() == null ? 0 : entity.getGetLimit();
		boolean applyUserGetLimit =
				SOURCE_MALL_LOCAL.equals(sourceType) || SOURCE_TURNTABLE.equals(sourceType);
		if (applyUserGetLimit && userGetNum >= getLimit) {
			throw fail(OpenapiDiscountCardV2SendPhpMessages.MSG_ALREADY_RECEIVED_LIMIT);
		}

		String userCode = allocNumericCode(companyId);

		insertUserDiscountRow(
				companyId, userId, sourceType, activityName, userCode, dmCardCode, cardInfo);

		salespersonTaskCouponCompleteService.completeGetCoupon(
				companyId, 0L, userId, "coupons_user", cardId);

		Map<String, Object> cardSnapshot = new LinkedHashMap<>(cardInfo);
		cardSnapshot.put("code", userCode);
		cardSnapshot.put("dm_card_code", dmCardCode);
		cardSnapshot.put("status", 1);
		wxaTemplateMsgCouponNotifyService.sendReceiveCardSuccess(companyId, userId, cardSnapshot);

		int totalIssuedAfter = countIssuedForCard(companyId, cardId);
		int userIssuedAfter = countIssuedForUser(companyId, userId, cardId);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("code", userCode);
		result.put("dm_card_code", dmCardCode);
		result.put("total_lastget_num", Math.max(0, quantity - totalIssuedAfter));
		result.put("lastget_num", Math.max(0, getLimit - userIssuedAfter));
		return result;
	}

	private void validateMallLocalGradesOrVip(long companyId, long userId, Map<String, Object> cardInfo) {
		boolean gradeReceive = true;
		Object gradeIdsRaw = cardInfo.get("grade_ids");
		if (gradeIdsRaw != null && hasTextish(gradeIdsRaw)) {
			List<String> gradeIds = splitIdsFlexible(gradeIdsRaw);
			Map<String, Object> memberInfo = memberAccountService.getMemberInfo(userId, companyId);
			String memberGrade = String.valueOf(memberInfo.getOrDefault("grade_id", ""));
			if (!gradeIds.contains(memberGrade)) {
				gradeReceive = false;
			}
		}

		boolean vipGradeReceive = true;
		Object vipGradeIdsRaw = cardInfo.get("vip_grade_ids");
		if (!gradeReceive && vipGradeIdsRaw != null && hasTextish(vipGradeIdsRaw)) {
			Map<String, Object> vipGrade =
					vipGradeUserVipGradeGetService.userVipGradeGet(companyId, userId, false);
			if (!Boolean.TRUE.equals(vipGrade.get("is_open"))) {
				vipGradeReceive = false;
			} else {
				List<String> vipGradeIds = splitIdsFlexible(vipGradeIdsRaw);
				String vg = String.valueOf(vipGrade.getOrDefault("vip_grade_id", ""));
				if (!vipGradeIds.contains(vg)) {
					vipGradeReceive = false;
				}
			}
		}

		if (!gradeReceive && !vipGradeReceive) {
			throw fail(OpenapiDiscountCardV2SendPhpMessages.MSG_NOT_RECEIVABLE);
		}
	}

	private boolean shouldSendDamoCoupon(long companyId, String sourceType, DiscountCards entity) {
		if (Set.of(SOURCE_DM_BATCH, SOURCE_DM_RECEIVE).contains(sourceType)) {
			return false;
		}
		if (!dmCrmDiscountCardSendPort.isOpen(companyId)) {
			return false;
		}
		String st = entity.getSourceType();
		return st != null && "dmcrm".equalsIgnoreCase(st.trim());
	}

	private void insertUserDiscountRow(
			long companyId,
			long userId,
			String sourceType,
			String activityName,
			String code,
			String dmCardCode,
			Map<String, Object> cardInfo) {
		UserDiscount existing =
				userDiscountMapper.selectOne(
						new LambdaQueryWrapper<UserDiscount>()
								.eq(UserDiscount::getCode, code)
								.eq(UserDiscount::getCardId, longFrom(cardInfo.get("card_id")))
								.last("LIMIT 1"));
		if (existing != null) {
			if (existing.getUserId() != null && existing.getUserId() == userId) {
				throw fail(OpenapiDiscountCardV2SendPhpMessages.MSG_ALREADY_RECEIVED_SAME_USER);
			}
			throw fail(OpenapiDiscountCardV2SendPhpMessages.MSG_FAILED_TO_RECEIVE);
		}

		UserDiscount row = new UserDiscount();
		row.setCompanyId(companyId);
		row.setUserId(userId);
		row.setCardId(longFrom(cardInfo.get("card_id")));
		row.setCode(code);
		row.setStatus(1);
		row.setSourceType(sourceType);
		row.setActivityName(activityName == null ? "" : activityName);
		row.setUseScenes(str(cardInfo.get("use_scenes")));
		row.setTitle(str(cardInfo.get("title")));
		row.setColor(str(cardInfo.get("color")));
		row.setDiscount(intFrom(cardInfo.get("discount"), 0));
		row.setCardType(str(cardInfo.get("card_type")));
		row.setLeastCost(intFrom(cardInfo.get("least_cost"), 0));
		row.setReduceCost(intFrom(cardInfo.get("reduce_cost"), 0));
		row.setUseCondition(str(cardInfo.get("use_condition")));
		row.setRelShopsIds(str(cardInfo.get("rel_shops_ids")));
		row.setUseBound(intFrom(cardInfo.get("use_bound"), 0));
		row.setRelItemIds(str(cardInfo.get("rel_item_ids")));
		row.setRelDistributorIds(str(cardInfo.get("distributor_id")));
		row.setBeginDate(intFrom(cardInfo.get("begin_date"), 0));
		row.setEndDate(intFrom(cardInfo.get("end_date"), 0));
		row.setUsePlatform(strOrDefault(cardInfo.get("use_platform"), "store"));
		row.setMostCost(intFrom(cardInfo.get("most_cost"), 99999900));
		row.setGetDate((int) Math.min(System.currentTimeMillis() / 1000L, Integer.MAX_VALUE));
		row.setSalespersonId(0L);
		row.setSalespersonCode("");
		row.setApplyScope(str(cardInfo.get("apply_scope")));
		if (StringUtils.hasText(dmCardCode)) {
			row.setDmCardCode(dmCardCode.trim());
		}
		userDiscountMapper.insert(row);
	}

	private String allocNumericCode(long companyId) {
		for (int attempt = 0; attempt < 20; attempt++) {
			StringBuilder sb = new StringBuilder(12);
			for (int i = 0; i < 12; i++) {
				sb.append(ThreadLocalRandom.current().nextInt(10));
			}
			String code = sb.toString();
			long cnt =
					userDiscountMapper.selectCount(
							new LambdaQueryWrapper<UserDiscount>()
									.eq(UserDiscount::getCompanyId, companyId)
									.eq(UserDiscount::getCode, code));
			if (cnt == 0L) {
				return code;
			}
		}
		throw fail(OpenapiDiscountCardV2SendPhpMessages.MSG_FAILED_TO_RECEIVE);
	}

	private int countIssuedForCard(long companyId, long cardId) {
		return numForCard(userDiscountMapper.countIssuedGroupByCardId(companyId, List.of(cardId)), cardId);
	}

	private int countIssuedForUser(long companyId, long userId, long cardId) {
		return numForCard(
				userDiscountMapper.countIssuedGroupByCardIdForUser(companyId, userId, List.of(cardId)), cardId);
	}

	private static int numForCard(List<UserDiscountCardAggRow> rows, long cardId) {
		if (rows == null) {
			return 0;
		}
		for (UserDiscountCardAggRow r : rows) {
			if (r.getCardId() != null && r.getCardId() == cardId && r.getNum() != null) {
				return r.getNum().intValue();
			}
		}
		return 0;
	}

	private static long parseUserId(String platAccount) {
		try {
			long userId = Long.parseLong(platAccount.trim());
			if (userId <= 0L) {
				throw fail(OpenapiDiscountCardV2SendPhpMessages.MSG_MEMBER_NOT_FOUND);
			}
			return userId;
		} catch (NumberFormatException e) {
			throw fail(OpenapiDiscountCardV2SendPhpMessages.MSG_MEMBER_NOT_FOUND);
		}
	}

	private static long parseCardId(String cardIdRaw) {
		try {
			long cardId = Long.parseLong(cardIdRaw.trim());
			if (cardId <= 0L) {
				throw fail(OpenapiDiscountCardV2SendPhpMessages.MSG_COUPON_NOT_EXIST);
			}
			return cardId;
		} catch (NumberFormatException e) {
			throw fail(OpenapiDiscountCardV2SendPhpMessages.MSG_COUPON_NOT_EXIST);
		}
	}

	private static boolean hasTextish(Object raw) {
		if (raw instanceof List<?> list) {
			return !list.isEmpty();
		}
		return StringUtils.hasText(String.valueOf(raw).trim());
	}

	private static List<String> splitIdsFlexible(Object raw) {
		if (raw instanceof List<?> list) {
			List<String> out = new ArrayList<>();
			for (Object o : list) {
				if (o == null) {
					continue;
				}
				String t = o.toString().trim();
				if (StringUtils.hasText(t)) {
					out.add(t);
				}
			}
			return out;
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return List.of();
		}
		s = s.replaceAll("^,+|,+$/", "");
		List<String> out = new ArrayList<>();
		for (String p : s.split(",")) {
			String t = p.trim();
			if (StringUtils.hasText(t)) {
				out.add(t);
			}
		}
		return out;
	}

	private static long longFrom(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int intFrom(Object raw, int def) {
		if (raw == null) {
			return def;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(raw.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static String strOrDefault(Object o, String d) {
		String s = str(o);
		return s.isEmpty() ? d : s;
	}

	private static OpenapiKaquanV2FailException fail(String message) {
		return new OpenapiKaquanV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}
}
