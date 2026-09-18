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
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DiscountCardStandardUpdatePrecheckService {

	private static final String DATE_TYPE_FIX_TIME_RANGE = "DATE_TYPE_FIX_TIME_RANGE";

	private final DiscountCardsMapper discountCardsMapper;

	public DiscountCardStandardUpdatePrecheckService(DiscountCardsMapper discountCardsMapper) {
		this.discountCardsMapper = discountCardsMapper;
	}

	public DiscountCards loadCardOrThrow(Map<String, Object> postdata, long companyId) {
		long cardId = DiscountCardParamNormalize.longFromObject(postdata.get("card_id"), 0L);
		if (cardId <= 0) {
			throw new ResourceException(KaquanDiscountCardMessages.COUPON_INVALID);
		}
		DiscountCards row = discountCardsMapper.selectOne(new LambdaQueryWrapper<DiscountCards>()
				.eq(DiscountCards::getCardId, cardId)
				.eq(DiscountCards::getCompanyId, companyId));
		if (row == null) {
			throw new ResourceException(KaquanDiscountCardMessages.COUPON_INVALID);
		}
		return row;
	}

	public void assertFixTimeRangeNotShrinking(Map<String, Object> postdata, DiscountCards existing) {
		String incomingType = DiscountCardParamNormalize.stringVal(postdata.get("date_type"));
		if (!DATE_TYPE_FIX_TIME_RANGE.equals(incomingType)) {
			return;
		}
		if (!incomingType.equals(DiscountCardParamNormalize.stringVal(existing.getDateType()))) {
			return;
		}
		long newBegin = DiscountCardParamNormalize.longFromObject(postdata.get("begin_time"), 0L);
		long newEnd = DiscountCardParamNormalize.longFromObject(postdata.get("end_time"), 0L);
		int oldBegin = existing.getBeginDate() != null ? existing.getBeginDate() : 0;
		int oldEnd = existing.getEndDate() != null ? existing.getEndDate() : 0;
		if (newBegin > oldBegin) {
			throw new ResourceException(KaquanDiscountCardMessages.UPDATE_CARD_START_TIME_ERROR);
		}
		if (newEnd < oldEnd) {
			throw new ResourceException(KaquanDiscountCardMessages.UPDATE_CARD_END_TIME_ERROR);
		}
	}
}
