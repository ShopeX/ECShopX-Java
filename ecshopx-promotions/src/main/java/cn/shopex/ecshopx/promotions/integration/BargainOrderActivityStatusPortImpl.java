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

package cn.shopex.ecshopx.promotions.integration;

import cn.shopex.ecshopx.common.port.promotions.BargainOrderActivityStatusPort;
import cn.shopex.ecshopx.promotions.domain.BargainPromotions;
import cn.shopex.ecshopx.promotions.domain.UserBargains;
import cn.shopex.ecshopx.promotions.mapper.BargainPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.UserBargainsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * PHP {@code BargainNormalOrderService::changeOrderActivityStatus}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BargainOrderActivityStatusPortImpl implements BargainOrderActivityStatusPort {

	private final UserBargainsMapper userBargainsMapper;
	private final BargainPromotionsMapper bargainPromotionsMapper;

	@Override
	public void changeOrderActivityStatus(long userId, long bargainId, int state) {
		if (userId <= 0L || bargainId <= 0L) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		if (state != 0) {
			userBargainsMapper.update(
					null,
					new LambdaUpdateWrapper<UserBargains>()
							.eq(UserBargains::getBargainId, bargainId)
							.eq(UserBargains::getUserId, userId)
							.eq(UserBargains::getIsOrdered, false)
							.set(UserBargains::getIsOrdered, true)
							.set(UserBargains::getUpdated, now));
		} else {
			userBargainsMapper.update(
					null,
					new LambdaUpdateWrapper<UserBargains>()
							.eq(UserBargains::getBargainId, bargainId)
							.eq(UserBargains::getUserId, userId)
							.eq(UserBargains::getIsOrdered, true)
							.set(UserBargains::getIsOrdered, false)
							.set(UserBargains::getUpdated, now));
		}
		BargainPromotions promo =
				bargainPromotionsMapper.selectOne(
						new LambdaQueryWrapper<BargainPromotions>()
								.eq(BargainPromotions::getBargainId, bargainId)
								.last("LIMIT 1"));
		if (promo == null) {
			log.warn("changeOrderActivityStatus: bargain missing bargainId={}", bargainId);
			return;
		}
		int orderNum = promo.getOrderNum() == null ? 0 : promo.getOrderNum();
		int next = state != 0 ? orderNum + 1 : Math.max(0, orderNum - 1);
		bargainPromotionsMapper.update(
				null,
				new LambdaUpdateWrapper<BargainPromotions>()
						.eq(BargainPromotions::getBargainId, bargainId)
						.set(BargainPromotions::getOrderNum, next)
						.set(BargainPromotions::getUpdated, now));
	}
}
