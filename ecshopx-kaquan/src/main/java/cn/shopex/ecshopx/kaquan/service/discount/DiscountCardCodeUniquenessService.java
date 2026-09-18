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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DiscountCardCodeUniquenessService {

	private final DiscountCardsMapper discountCardsMapper;

	public DiscountCardCodeUniquenessService(DiscountCardsMapper discountCardsMapper) {
		this.discountCardsMapper = discountCardsMapper;
	}

	public void assertCardCodeAvailable(long companyId, String cardCode, String cardRuleCode, Long excludeCardId) {
		LambdaQueryWrapper<DiscountCards> w = new LambdaQueryWrapper<>();
		w.eq(DiscountCards::getCompanyId, companyId).eq(DiscountCards::getCardCode, cardCode);
		String nr = cardRuleCode == null ? "" : cardRuleCode;
		for (DiscountCards row : discountCardsMapper.selectList(w)) {
			if (excludeCardId != null && excludeCardId.equals(row.getCardId())) {
				continue;
			}
			String er = row.getCardRuleCode() == null ? "" : row.getCardRuleCode();
			if (!StringUtils.hasText(nr)) {
				if (!StringUtils.hasText(er)) {
					throw new ResourceException(KaquanDiscountCardMessages.COUPON_RULE_ID_DUPLICATE);
				}
			} else if (nr.equals(er)) {
				throw new ResourceException(KaquanDiscountCardMessages.COUPON_RULE_ID_DUPLICATE);
			}
		}
	}
}
