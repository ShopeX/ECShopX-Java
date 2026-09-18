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
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardParamNormalize;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpenapiThirdApiV1CouponUpdateUserCardService {

	private static final String MSG_CARD_INVALID = "该优惠券已失效";
	private static final String MSG_UPDATE_FAILED = "修改失败，请检查优惠券是否存在";

	private final DiscountCardsMapper discountCardsMapper;
	private final UserDiscountMapper userDiscountMapper;

	public OpenapiThirdApiV1CouponUpdateUserCardService(
			DiscountCardsMapper discountCardsMapper, UserDiscountMapper userDiscountMapper) {
		this.discountCardsMapper = discountCardsMapper;
		this.userDiscountMapper = userDiscountMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> executeOpenapiUpdateUserCard(
			long companyId,
			String templateCode,
			String cardRuleCode,
			String userDiscountCode,
			String startTimeRaw,
			String endTimeRaw) {
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
			throw new ResourceException(MSG_CARD_INVALID);
		}
		long cardId = cardEntity.getCardId();

		UserDiscount userCard =
				userDiscountMapper.selectOne(
						new LambdaQueryWrapper<UserDiscount>()
								.eq(UserDiscount::getCompanyId, companyId)
								.eq(UserDiscount::getCardId, cardId)
								.eq(UserDiscount::getCode, userDiscountCode)
								.last("LIMIT 1"));
		if (userCard == null) {
			throw new ResourceException(MSG_UPDATE_FAILED);
		}

		int beginDate = DiscountCardParamNormalize.parseIntFlexible(startTimeRaw, 0);
		int endDate = DiscountCardParamNormalize.parseIntFlexible(endTimeRaw, 0);

		userDiscountMapper.update(
				null,
				new LambdaUpdateWrapper<UserDiscount>()
						.eq(UserDiscount::getId, userCard.getId())
						.set(UserDiscount::getBeginDate, beginDate)
						.set(UserDiscount::getEndDate, endDate));

		return Map.of("status", true);
	}
}
