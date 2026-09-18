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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.members.admin.AdminMemberBatchDiscountCardInventoryPort;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.domain.UserDiscountCardAggRow;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service("adminMemberBatchDiscountCardInventoryPortImpl")
public class AdminMemberBatchDiscountCardInventoryPortImpl implements AdminMemberBatchDiscountCardInventoryPort {

	private final DiscountCardsMapper discountCardsMapper;
	private final UserDiscountMapper userDiscountMapper;

	public AdminMemberBatchDiscountCardInventoryPortImpl(
			DiscountCardsMapper discountCardsMapper, UserDiscountMapper userDiscountMapper) {
		this.discountCardsMapper = discountCardsMapper;
		this.userDiscountMapper = userDiscountMapper;
	}

	@Override
	public void assertCouponCardsNotFullyIssued(long companyId, List<Long> cardIds) {
		if (cardIds == null || cardIds.isEmpty()) {
			return;
		}
		List<DiscountCards> cards =
				discountCardsMapper.selectList(
						new LambdaQueryWrapper<DiscountCards>()
								.eq(DiscountCards::getCompanyId, companyId)
								.in(DiscountCards::getCardId, cardIds));
		Map<Long, Integer> quantityById = new HashMap<>();
		for (DiscountCards c : cards) {
			int q = c.getQuantity() == null ? 0 : c.getQuantity();
			quantityById.put(c.getCardId(), q);
		}
		List<UserDiscountCardAggRow> issued = userDiscountMapper.countIssuedGroupByCardId(companyId, cardIds);
		Map<Long, Long> getNumById = new HashMap<>();
		for (UserDiscountCardAggRow r : issued) {
			if (r.getCardId() != null && r.getNum() != null) {
				getNumById.put(r.getCardId(), r.getNum());
			}
		}
		for (Long cardId : cardIds) {
			int quantity = quantityById.getOrDefault(cardId, 0);
			long getNum = getNumById.getOrDefault(cardId, 0L);
			if (getNum >= quantity) {
				throw new ResourceException("优惠券已发放完");
			}
		}
	}
}
