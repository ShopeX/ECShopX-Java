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

package cn.shopex.ecshopx.kaquan.openapi.thirdapi.v1;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.openapi.OpenapiLegacyZeroCodeFailException;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import cn.shopex.ecshopx.kaquan.domain.UserDiscountCardAggRow;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardKaquanDetailForConsumeService;
import cn.shopex.ecshopx.kaquan.util.UserDiscountUseConditionLegacyCodec;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV1CouponUserGetCardService {

	private static final String MSG_ALREADY_RECEIVED_THIRD_PARTY = "您已经领取该卡券";
	private static final String MSG_FAILED_TO_RECEIVE_THIRD_PARTY = "领取该卡券失败";
	private static final String SOURCE_TYPE_THIRD_PARTY = "第三方发放";

	private final MemberAccountService memberAccountService;
	private final DiscountCardsMapper discountCardsMapper;
	private final DiscountCardKaquanDetailForConsumeService discountCardKaquanDetailForConsumeService;
	private final UserDiscountMapper userDiscountMapper;

	public OpenapiThirdApiV1CouponUserGetCardService(
			MemberAccountService memberAccountService,
			DiscountCardsMapper discountCardsMapper,
			DiscountCardKaquanDetailForConsumeService discountCardKaquanDetailForConsumeService,
			UserDiscountMapper userDiscountMapper) {
		this.memberAccountService = memberAccountService;
		this.discountCardsMapper = discountCardsMapper;
		this.discountCardKaquanDetailForConsumeService = discountCardKaquanDetailForConsumeService;
		this.userDiscountMapper = userDiscountMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> executeOpenapiUserGetCard(
			long companyId,
			String templateCode,
			String cardRuleCode,
			String userDiscountCode,
			long startTimeEpoch,
			long endTimeEpoch,
			String outerCrmUserid,
			String mobileUserId) {
		long userId = resolveUserId(companyId, outerCrmUserid, mobileUserId);
		Map<String, Object> cardInfo = loadAndNormalizeCardInfo(
				companyId, templateCode, cardRuleCode, startTimeEpoch, endTimeEpoch);
		insertThirdPartyUserDiscount(companyId, userId, userDiscountCode, cardInfo);
		return Map.of("status", true);
	}

	private long resolveUserId(long companyId, String outerCrmUserid, String mobileUserId) {
		if (StringUtils.hasText(outerCrmUserid)) {
			Members member = memberAccountService.findMemberByCompanyAndThirdData(companyId, outerCrmUserid);
			if (member == null) {
				throw new ResourceException("用户不存在");
			}
			return member.getUserId();
		}
		if (StringUtils.hasText(mobileUserId)) {
			Members member = memberAccountService.findMemberByCompanyAndMobile(companyId, mobileUserId);
			if (member == null) {
				throw new ResourceException("用户不存在");
			}
			return member.getUserId();
		}
		return 0L;
	}

	private Map<String, Object> loadAndNormalizeCardInfo(
			long companyId,
			String templateCode,
			String cardRuleCode,
			long startTimeEpoch,
			long endTimeEpoch) {
		String ruleCode = cardRuleCode == null ? "" : cardRuleCode;
		DiscountCards cardEntity =
				discountCardsMapper.selectOne(
						new LambdaQueryWrapper<DiscountCards>()
								.eq(DiscountCards::getCompanyId, companyId)
								.eq(DiscountCards::getCardCode, templateCode)
								.eq(DiscountCards::getCardRuleCode, ruleCode)
								.orderByDesc(DiscountCards::getUpdated)
								.last("LIMIT 1"));
		if (cardEntity == null) {
			throw new ResourceException("该优惠券已失效");
		}

		long cardId = cardEntity.getCardId();
		if (endTimeEpoch > 0L && endTimeEpoch <= System.currentTimeMillis() / 1000L) {
			throw new ResourceException("领取优惠券失败，优惠券已过期");
		}

		int getNum = numForCard(userDiscountMapper.countIssuedGroupByCardId(companyId, List.of(cardId)), cardId);
		int quantity = cardEntity.getQuantity() == null ? 0 : cardEntity.getQuantity();
		if (quantity <= getNum) {
			throw new ResourceException("领取的优惠券失败，库存不足了");
		}

		Map<String, Object> cardInfo =
				discountCardKaquanDetailForConsumeService.loadDetail(companyId, cardId);
		normalizeCardInfoForThirdApiGrant(cardInfo, startTimeEpoch, endTimeEpoch);
		return cardInfo;
	}

	private void normalizeCardInfoForThirdApiGrant(
			Map<String, Object> cardInfo, long startTimeEpoch, long endTimeEpoch) {
		int discount = intFrom(cardInfo.get("discount"), 0);
		int leastCost = intFrom(cardInfo.get("least_cost"), 0);
		int reduceCost = intFrom(cardInfo.get("reduce_cost"), 0);
		int mostCost = intFrom(cardInfo.get("most_cost"), 0);
		if (mostCost <= 0) {
			mostCost = 99999900;
		}
		cardInfo.put("discount", discount);
		cardInfo.put("least_cost", leastCost);
		cardInfo.put("reduce_cost", reduceCost);
		cardInfo.put("most_cost", mostCost);

		Map<String, Object> useCondition =
				UserDiscountUseConditionLegacyCodec.buildFromCardInfo(cardInfo, leastCost);
		cardInfo.put("use_condition", UserDiscountUseConditionLegacyCodec.serialize(useCondition));

		if ("true".equals(String.valueOf(cardInfo.get("use_all_shops")))) {
			cardInfo.put("rel_shops_ids", "all");
			cardInfo.put("distributor_id", "all");
		} else {
			Object shopIdsRaw = cardInfo.get("rel_shops_ids");
			List<?> shopIds = shopIdsRaw instanceof List<?> list ? list : List.of();
			cardInfo.put(
					"rel_shops_ids",
					!shopIds.isEmpty()
							? "," + shopIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ","
							: "all");

			Object distIdsRaw = cardInfo.get("rel_distributor_ids");
			List<?> distributorIds = distIdsRaw instanceof List<?> list ? list : List.of();
			cardInfo.put(
					"distributor_id",
					!distributorIds.isEmpty()
							? "," + distributorIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ","
							: "all");
		}

		Object relItemIdsRaw = cardInfo.get("rel_item_ids");
		if (relItemIdsRaw instanceof List<?> relItemIds && !relItemIds.isEmpty()) {
			cardInfo.put(
					"rel_item_ids",
					"," + relItemIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ",");
		} else {
			cardInfo.put("rel_item_ids", "all");
		}

		cardInfo.put("begin_date", (int) Math.min(startTimeEpoch, Integer.MAX_VALUE));
		cardInfo.put("end_date", (int) Math.min(endTimeEpoch, Integer.MAX_VALUE));
	}

	private void insertThirdPartyUserDiscount(
			long companyId, long userId, String code, Map<String, Object> cardInfo) {
		UserDiscount existing =
				userDiscountMapper.selectOne(
						new LambdaQueryWrapper<UserDiscount>()
								.eq(UserDiscount::getCode, code)
								.eq(UserDiscount::getCardId, longFrom(cardInfo.get("card_id")))
								.last("LIMIT 1"));
		if (existing != null) {
			Long existingUserId = existing.getUserId();
			if (existingUserId != null && existingUserId == userId) {
				throw new OpenapiLegacyZeroCodeFailException(MSG_ALREADY_RECEIVED_THIRD_PARTY);
			}
			throw new OpenapiLegacyZeroCodeFailException(MSG_FAILED_TO_RECEIVE_THIRD_PARTY);
		}

		UserDiscount row = new UserDiscount();
		row.setCompanyId(companyId);
		row.setUserId(userId);
		row.setCardId(longFrom(cardInfo.get("card_id")));
		row.setCode(code);
		row.setStatus(1);
		row.setSourceType(SOURCE_TYPE_THIRD_PARTY);
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
		row.setApplyScope(str(cardInfo.get("apply_scope")));
		userDiscountMapper.insert(row);
	}

	private static int numForCard(List<UserDiscountCardAggRow> rows, long cardId) {
		if (rows == null) {
			return 0;
		}
		for (UserDiscountCardAggRow row : rows) {
			if (row.getCardId() != null && row.getCardId() == cardId) {
				return row.getNum() == null ? 0 : row.getNum().intValue();
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
