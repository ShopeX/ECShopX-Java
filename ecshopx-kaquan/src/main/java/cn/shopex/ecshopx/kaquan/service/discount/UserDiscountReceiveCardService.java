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

package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import cn.shopex.ecshopx.kaquan.domain.UserDiscountCardAggRow;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.mapper.RelItemsMapper;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeUserVipGradeGetService;
import cn.shopex.ecshopx.salesperson.service.SalespersonTaskCouponCompleteService;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmDiscountCardSendPort;
import cn.shopex.ecshopx.wechat.service.WxaTemplateMsgCouponNotifyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserDiscountReceiveCardService {

	private static final String CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

	private static final String SOURCE_FROM_MALL_LOCAL = "商城本地领取";

	private static final String SOURCE_FROM_TURNTABLE_WIN = "大转盘中奖领取";

	private final UserDiscountMapper userDiscountMapper;
	private final DiscountCardsMapper discountCardsMapper;
	private final RelItemsMapper relItemsMapper;
	private final MemberAccountService memberAccountService;
	private final VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService;
	private final SalespersonTaskCouponCompleteService salespersonTaskCouponCompleteService;
	private final DiscountCardsRowMapperService discountCardsRowMapperService;
	private final ObjectMapper objectMapper;
	private final DmCrmDiscountCardSendPort dmCrmDiscountCardSendPort;
	private final WxaTemplateMsgCouponNotifyService wxaTemplateMsgCouponNotifyService;
	private final SecureRandom secureRandom = new SecureRandom();

	public UserDiscountReceiveCardService(
			UserDiscountMapper userDiscountMapper,
			DiscountCardsMapper discountCardsMapper,
			RelItemsMapper relItemsMapper,
			MemberAccountService memberAccountService,
			VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService,
			SalespersonTaskCouponCompleteService salespersonTaskCouponCompleteService,
			DiscountCardsRowMapperService discountCardsRowMapperService,
			ObjectMapper objectMapper,
			DmCrmDiscountCardSendPort dmCrmDiscountCardSendPort,
			WxaTemplateMsgCouponNotifyService wxaTemplateMsgCouponNotifyService) {
		this.userDiscountMapper = userDiscountMapper;
		this.discountCardsMapper = discountCardsMapper;
		this.relItemsMapper = relItemsMapper;
		this.memberAccountService = memberAccountService;
		this.vipGradeUserVipGradeGetService = vipGradeUserVipGradeGetService;
		this.salespersonTaskCouponCompleteService = salespersonTaskCouponCompleteService;
		this.discountCardsRowMapperService = discountCardsRowMapperService;
		this.objectMapper = objectMapper;
		this.dmCrmDiscountCardSendPort = dmCrmDiscountCardSendPort;
		this.wxaTemplateMsgCouponNotifyService = wxaTemplateMsgCouponNotifyService;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> receiveCard(
			long companyId,
			long userId,
			String mobile,
			Long cardId,
			long salespersonId,
			String salespersonCode) {
		return receiveCard(companyId, userId, mobile, cardId, salespersonId, salespersonCode, SOURCE_FROM_MALL_LOCAL);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> receiveCard(
			long companyId,
			long userId,
			String mobile,
			Long cardId,
			long salespersonId,
			String salespersonCode,
			String sourceFrom) {
		return receiveCard(companyId, userId, mobile, cardId, salespersonId, salespersonCode, sourceFrom, null);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> receiveCard(
			long companyId,
			long userId,
			String mobile,
			Long cardId,
			long salespersonId,
			String salespersonCode,
			String sourceFrom,
			String getOuterStr) {
		String resolvedSource = normalizeSourceFrom(sourceFrom);
		if (StringUtils.hasText(getOuterStr) && existsTurntableGrant(companyId, userId, getOuterStr)) {
			Map<String, Object> status = new LinkedHashMap<>();
			status.put("code", "");
			status.put("idempotent", Boolean.TRUE);
			return status;
		}
		return doReceiveCard(
				companyId, userId, mobile, cardId, salespersonId, salespersonCode, resolvedSource, getOuterStr);
	}

	private String normalizeSourceFrom(String sourceFrom) {
		if (sourceFrom == null || !StringUtils.hasText(sourceFrom.trim())) {
			return SOURCE_FROM_MALL_LOCAL;
		}
		return sourceFrom.trim();
	}

	private Map<String, Object> doReceiveCard(
			long companyId,
			long userId,
			String mobile,
			Long cardId,
			long salespersonId,
			String salespersonCode,
			String resolvedSource,
			String getOuterStr) {
		if (companyId <= 0L) {
			throw new ResourceException(KaquanDiscountCardMessages.COUPON_RECORD_ADD_FAILED);
		}
		checkGuideCouponLimit(companyId, cardId, salespersonCode);
		if (cardId == null || cardId <= 0L) {
			throw new ResourceException(KaquanDiscountCardMessages.COUPON_INVALID);
		}

		DiscountCards entity = loadCardOrThrow(companyId, cardId);
		validateExpiryAndStock(companyId, cardId, entity);

		Map<String, Object> cardInfo =
				DiscountCardUserReceiveFieldNormalizer.normalize(
						discountCardsRowMapperService, objectMapper, entity, relItemsMapper);

		int kq = entity.getKqStatus() == null ? -1 : entity.getKqStatus();
		if (kq != DiscountNewGiftCardUpdateService.STATUS_NORMAL) {
			throw new ResourceException(KaquanDiscountCardMessages.COUPON_STATUS_ABNORMAL);
		}

		Map<String, Object> memberInfo = memberAccountService.getMemberInfo(userId, companyId);
		Map<String, Object> vipGrade = vipGradeUserVipGradeGetService.userVipGradeGet(companyId, userId, false);
		if (!"商城后台发放".equals(resolvedSource)) {
			validateMallLocalGrades(cardInfo, memberInfo, vipGrade);
		}

		String dmCardCode = "";
		if (shouldSendDamoCoupon(companyId, entity)) {
			String dmCardId = entity.getDmCardId() == null ? "" : entity.getDmCardId().trim();
			dmCardCode =
					dmCrmDiscountCardSendPort.sendCoupon(companyId, userId, cardId, dmCardId, mobile);
			if (dmCardCode == null) {
				dmCardCode = "";
			}
		}

		String userCode = allocUniqueCode(companyId);

		int userGetNumBefore =
				numForCard(
						userDiscountMapper.countIssuedGroupByCardIdForUser(companyId, userId, List.of(cardId)), cardId);
		int getLimit = intFrom(cardInfo.get("get_limit"), 0);
		boolean applyUserGetLimit =
				SOURCE_FROM_MALL_LOCAL.equals(resolvedSource) || SOURCE_FROM_TURNTABLE_WIN.equals(resolvedSource);
		if (applyUserGetLimit && userGetNumBefore >= getLimit) {
			throw new ResourceException(KaquanDiscountCardMessages.USER_EXCEED_COUPON_LIMIT);
		}

		insertUserDiscountRow(
				companyId,
				userId,
				salespersonId,
				salespersonCode,
				resolvedSource,
				userCode,
				dmCardCode,
				cardInfo,
				getOuterStr);

		salespersonTaskCouponCompleteService.completeGetCoupon(
				companyId, salespersonId, userId, "coupons_user", cardId);

		int quantity = entity.getQuantity() == null ? 0 : entity.getQuantity();
		int totalIssuedAfter =
				numForCard(userDiscountMapper.countIssuedGroupByCardId(companyId, List.of(cardId)), cardId);
		int userIssuedAfter =
				numForCard(
						userDiscountMapper.countIssuedGroupByCardIdForUser(companyId, userId, List.of(cardId)), cardId);

		int totalLastgetNum = Math.max(0, quantity - totalIssuedAfter);
		int lastgetNum = Math.max(0, getLimit - userIssuedAfter);

		Map<String, Object> status = new LinkedHashMap<>();
		status.put("code", userCode);
		status.put("dm_card_code", dmCardCode);
		status.put("total_lastget_num", totalLastgetNum);
		status.put("lastget_num", lastgetNum);

		Map<String, Object> cardSnapshot = new LinkedHashMap<>(cardInfo);
		cardSnapshot.put("code", userCode);
		cardSnapshot.put("dm_card_code", dmCardCode);
		cardSnapshot.put("status", 1);
		wxaTemplateMsgCouponNotifyService.sendReceiveCardSuccess(companyId, userId, cardSnapshot);
		return status;
	}

	private void checkGuideCouponLimit(long companyId, Long cardId, String salespersonCode) {
		if (!StringUtils.hasText(salespersonCode)) {
			return;
		}
		if (cardId == null || cardId <= 0L) {
			return;
		}
		DiscountCards card =
				discountCardsMapper.selectOne(
						new LambdaQueryWrapper<DiscountCards>()
								.eq(DiscountCards::getCompanyId, companyId)
								.eq(DiscountCards::getCardId, cardId)
								.last("LIMIT 1"));
		if (card == null) {
			return;
		}
		if (!"guide".equals(String.valueOf(card.getCouponType()))) {
			return;
		}
		int guideQty = card.getGuideIssueQuantity() == null ? 0 : card.getGuideIssueQuantity();
		if (guideQty <= 0) {
			return;
		}
		List<UserDiscountCardAggRow> rows =
				userDiscountMapper.countIssuedGroupByCardIdForSalesperson(
						companyId, salespersonCode.trim(), List.of(cardId));
		int receivedCount = numForCard(rows, cardId);
		if (receivedCount >= guideQty) {
			throw new ResourceException(KaquanDiscountCardMessages.GUIDE_COUPON_OVER_LIMIT);
		}
	}

	private DiscountCards loadCardOrThrow(long companyId, long cardId) {
		DiscountCards c =
				discountCardsMapper.selectOne(
						new LambdaQueryWrapper<DiscountCards>()
								.eq(DiscountCards::getCompanyId, companyId)
								.eq(DiscountCards::getCardId, cardId)
								.last("LIMIT 1"));
		if (c == null) {
			throw new ResourceException(KaquanDiscountCardMessages.COUPON_INVALID);
		}
		return c;
	}

	private void validateExpiryAndStock(long companyId, long cardId, DiscountCards entity) {
		long nowSec = System.currentTimeMillis() / 1000L;
		Integer end = entity.getEndDate();
		if (end != null && end > 0 && end <= nowSec) {
			throw new ResourceException(KaquanDiscountCardMessages.RECEIVE_COUPON_EXPIRED);
		}
		int quantity = entity.getQuantity() == null ? 0 : entity.getQuantity();
		int issued =
				numForCard(userDiscountMapper.countIssuedGroupByCardId(companyId, List.of(cardId)), cardId);
		if (quantity <= issued) {
			throw new ResourceException(KaquanDiscountCardMessages.RECEIVE_COUPON_OUT_OF_STOCK);
		}
	}

	private boolean shouldSendDamoCoupon(long companyId, DiscountCards entity) {
		if (!dmCrmDiscountCardSendPort.isOpen(companyId)) {
			return false;
		}
		String st = entity.getSourceType();
		return st != null && "dmcrm".equalsIgnoreCase(st.trim());
	}

	private void validateMallLocalGrades(
			Map<String, Object> cardInfo, Map<String, Object> memberInfo, Map<String, Object> vipGrade) {
		Object gradeIdsRaw = cardInfo.get("grade_ids");
		if (gradeIdsRaw != null && hasTextish(gradeIdsRaw)) {
			List<String> gradeIds = splitIdsFlexible(gradeIdsRaw);
			String memberGrade = String.valueOf(memberInfo.getOrDefault("grade_id", ""));
			if (!gradeIds.contains(memberGrade)) {
				throw new ResourceException(KaquanDiscountCardMessages.MEMBER_GRADE_NOT_MATCH);
			}
		}
		Object vipGradeIdsRaw = cardInfo.get("vip_grade_ids");
		if (vipGradeIdsRaw != null && hasTextish(vipGradeIdsRaw)) {
			if (!Boolean.TRUE.equals(vipGrade.get("is_open"))) {
				throw new ResourceException(KaquanDiscountCardMessages.VIP_GRADE_NOT_MATCH_NOT_OPEN);
			}
			List<String> vipGradeIds = splitIdsFlexible(vipGradeIdsRaw);
			String vg = String.valueOf(vipGrade.getOrDefault("vip_grade_id", ""));
			if (!vipGradeIds.contains(vg)) {
				throw new ResourceException(KaquanDiscountCardMessages.VIP_GRADE_NOT_MATCH);
			}
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

	private void insertUserDiscountRow(
			long companyId,
			long userId,
			long salespersonId,
			String salespersonCode,
			String sourceType,
			String code,
			String dmCardCode,
			Map<String, Object> cardInfo,
			String getOuterStr) {
		UserDiscount existing =
				userDiscountMapper.selectOne(
						new LambdaQueryWrapper<UserDiscount>()
								.eq(UserDiscount::getCode, code)
								.eq(UserDiscount::getCardId, longFrom(cardInfo.get("card_id")))
								.last("LIMIT 1"));
		if (existing != null) {
			if (existing.getUserId() != null && existing.getUserId() == userId) {
				throw new ResourceException(KaquanDiscountCardMessages.ALREADY_RECEIVED_COUPON);
			}
			throw new ResourceException(KaquanDiscountCardMessages.FAILED_TO_RECEIVE_COUPON);
		}
		UserDiscount row = new UserDiscount();
		row.setCompanyId(companyId);
		row.setUserId(userId);
		row.setCardId(longFrom(cardInfo.get("card_id")));
		row.setCode(code);
		row.setStatus(1);
		row.setSourceType(sourceType);
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
		row.setSalespersonId(salespersonId);
		row.setSalespersonCode(salespersonCode == null ? "" : salespersonCode.trim());
		row.setApplyScope(str(cardInfo.get("apply_scope")));
		if (StringUtils.hasText(dmCardCode)) {
			row.setDmCardCode(dmCardCode.trim());
		}
		if (StringUtils.hasText(getOuterStr)) {
			row.setGetOuterStr(getOuterStr.trim());
		}
		userDiscountMapper.insert(row);
	}

	private String allocUniqueCode(long companyId) {
		for (int attempt = 0; attempt < 20; attempt++) {
			String code = randomCode(12);
			long cnt =
					userDiscountMapper.selectCount(
							new LambdaQueryWrapper<UserDiscount>()
									.eq(UserDiscount::getCompanyId, companyId)
									.eq(UserDiscount::getCode, code));
			if (cnt == 0L) {
				return code;
			}
		}
		throw new ResourceException(KaquanDiscountCardMessages.DATA_CREATE_FAILED_RETRY);
	}

	private String randomCode(int len) {
		byte[] buf = new byte[len];
		secureRandom.nextBytes(buf);
		char[] out = new char[len];
		for (int i = 0; i < len; i++) {
			int idx = (buf[i] & 0xff) % CHARSET.length();
			out[i] = CHARSET.charAt(idx);
		}
		return new String(out);
	}

	public int countIssuedForCard(long companyId, long cardId) {
		return numForCard(userDiscountMapper.countIssuedGroupByCardId(companyId, List.of(cardId)), cardId);
	}

	public int countIssuedForCardAndUser(long companyId, long userId, long cardId) {
		return numForCard(
				userDiscountMapper.countIssuedGroupByCardIdForUser(companyId, userId, List.of(cardId)), cardId);
	}

	public boolean existsTurntableGrant(long companyId, long userId, String requestId) {
		if (!StringUtils.hasText(requestId)) {
			return false;
		}
		UserDiscount row =
				userDiscountMapper.selectOne(
						new LambdaQueryWrapper<UserDiscount>()
								.eq(UserDiscount::getCompanyId, companyId)
								.eq(UserDiscount::getUserId, userId)
								.eq(UserDiscount::getGetOuterStr, requestId.trim())
								.last("LIMIT 1"));
		return row != null;
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
}
