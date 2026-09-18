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

package cn.shopex.ecshopx.promotions.service.bargain;

import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.promotions.domain.BargainPromotions;
import cn.shopex.ecshopx.promotions.domain.UserBargains;
import cn.shopex.ecshopx.promotions.mapper.BargainPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.UserBargainsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class BargainHelpPayTradeFinishActivityService {

	private final NormalOrdersMapper normalOrdersMapper;
	private final UserBargainsMapper userBargainsMapper;
	private final BargainPromotionsMapper bargainPromotionsMapper;

	public BargainHelpPayTradeFinishActivityService(
			NormalOrdersMapper normalOrdersMapper,
			UserBargainsMapper userBargainsMapper,
			BargainPromotionsMapper bargainPromotionsMapper) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.userBargainsMapper = userBargainsMapper;
		this.bargainPromotionsMapper = bargainPromotionsMapper;
	}

	/**
	 * Handles trade-finish payloads for bargain help-pay flows: updates user bargain row as ordered and
	 * increments promotion {@code order_num} when the normal order belongs to bargain activity.
	 */
	@Transactional
	public void onTradeFinishTradeRow(Map<String, Object> tradeRowSnakeCase) {
		if (tradeRowSnakeCase == null || tradeRowSnakeCase.isEmpty()) {
			return;
		}

		String tradeSourceType = stringify(tradeRowSnakeCase.get("trade_source_type"));
		if (!StringUtils.hasText(tradeSourceType) || !"bargain".equalsIgnoreCase(tradeSourceType.trim())) {
			return;
		}

		Long companyId = parseLongFlexible(tradeRowSnakeCase.get("company_id"));
		Long orderId = parseLongFlexible(tradeRowSnakeCase.get("order_id"));
		if (companyId == null || orderId == null) {
			return;
		}

		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId));
		if (order == null) {
			return;
		}

		String orderClass = stringify(order.getOrderClass());
		if (!StringUtils.hasText(orderClass) || !"bargain".equalsIgnoreCase(orderClass.trim())) {
			return;
		}

		Long bargainId = order.getActId();
		Long userId = order.getUserId();
		if (bargainId == null || bargainId == 0L || userId == null || userId == 0L) {
			return;
		}

		int nowSec = Math.toIntExact(Instant.now().getEpochSecond());

		UpdateWrapper<UserBargains> userBargainUw = new UpdateWrapper<>();
		userBargainUw
				.eq("bargain_id", bargainId)
				.eq("user_id", userId)
				.and(
						w ->
								w.eq("is_ordered", Boolean.FALSE).or().isNull("is_ordered"))
				.set("is_ordered", Boolean.TRUE)
				.set("updated", nowSec);
		userBargainsMapper.update(null, userBargainUw);

		UpdateWrapper<BargainPromotions> promotionUw = new UpdateWrapper<>();
		promotionUw
				.eq("bargain_id", bargainId)
				.setSql("order_num = IFNULL(order_num, 0) + 1")
				.set("updated", nowSec);
		bargainPromotionsMapper.update(null, promotionUw);
	}

	private static String stringify(Object raw) {
		return raw == null ? "" : String.valueOf(raw).trim();
	}

	private static Long parseLongFlexible(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
