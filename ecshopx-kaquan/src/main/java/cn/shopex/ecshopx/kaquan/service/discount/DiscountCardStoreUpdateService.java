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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiscountCardStoreUpdateService {

	private static final long INT_MAX_AS_LONG = 2147483647L;

	private final DiscountCardsMapper discountCardsMapper;
	private final UserDiscountMapper userDiscountMapper;

	public DiscountCardStoreUpdateService(
			DiscountCardsMapper discountCardsMapper,
			UserDiscountMapper userDiscountMapper) {
		this.discountCardsMapper = discountCardsMapper;
		this.userDiscountMapper = userDiscountMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void updateStore(Map<String, Object> mergedParams, long companyId) {
		Long cardId = parseCardId(mergedParams.get("card_id"));
		if (cardId == null) {
			return;
		}
		DiscountCards row = discountCardsMapper.selectOne(new LambdaQueryWrapper<DiscountCards>()
				.eq(DiscountCards::getCardId, cardId)
				.eq(DiscountCards::getCompanyId, companyId));
		if (row == null) {
			return;
		}
		String typeRaw = mergedParams.get("type") == null ? "" : mergedParams.get("type").toString().trim();
		if (!"increase".equals(typeRaw) && !"reduce".equals(typeRaw)) {
			throw new BadRequestException("type 仅支持 increase 或 reduce");
		}
		long oldstore = row.getQuantity() == null ? 0L : row.getQuantity().longValue();
		long getNum = userDiscountMapper.selectCount(new LambdaQueryWrapper<UserDiscount>()
				.eq(UserDiscount::getCardId, cardId)
				.eq(UserDiscount::getCompanyId, companyId));
		long store = parseAdjustStoreAsLong(mergedParams.get("quantity"));
		store = Math.abs(store);

		long lastNum;
		if ("reduce".equals(typeRaw)) {
			if (oldstore - getNum - store <= 0L) {
				lastNum = getNum;
			} else {
				lastNum = oldstore - store;
			}
		} else {
			lastNum = oldstore + store;
		}
		if (lastNum > INT_MAX_AS_LONG) {
			throw new ResourceException(KaquanDiscountCardMessages.STOCK_FIELD_EXCEEDS_MAX);
		}
		row.setQuantity((int) lastNum);
		discountCardsMapper.updateById(row);
	}

	private static Long parseCardId(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number) {
			return ((Number) raw).longValue();
		}
		String s = raw.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * 解析调整量：全程 long，不因超过 int 范围而拒绝；仅 {@link Long#MIN_VALUE} 无法安全 {@link Math#abs(long)} 时拒绝。
	 */
	private static long parseAdjustStoreAsLong(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof String str) {
			String t = str.trim();
			if (t.isEmpty()) {
				return 0L;
			}
			try {
				long lv = Long.parseLong(t);
				if (lv == Long.MIN_VALUE) {
					throw new BadRequestException("修改库存出错,请输入大于0的整数");
				}
				return lv;
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		if (raw instanceof Number n) {
			long lv = n.longValue();
			if (lv == Long.MIN_VALUE) {
				throw new BadRequestException("修改库存出错,请输入大于0的整数");
			}
			return lv;
		}
		String t = raw.toString().trim();
		if (t.isEmpty()) {
			return 0L;
		}
		try {
			long lv = Long.parseLong(t);
			if (lv == Long.MIN_VALUE) {
				throw new BadRequestException("修改库存出错,请输入大于0的整数");
			}
			return lv;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
