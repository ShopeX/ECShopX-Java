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

package cn.shopex.ecshopx.kaquan.integration.order;

import cn.shopex.ecshopx.common.port.order.OrderCancelUserDiscountRestorePort;
import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import cn.shopex.ecshopx.kaquan.domain.UserDiscountLogs;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountLogsMapper;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 订单取消后回退优惠券（kaquan_user_discount 状态改回 1=未使用，kaquan_user_discount_logs 插入 callback 记录）。
 */
@Service
public class OrderCancelUserDiscountRestorePortImpl implements OrderCancelUserDiscountRestorePort {

	private final UserDiscountMapper userDiscountMapper;
	private final UserDiscountLogsMapper userDiscountLogsMapper;

	public OrderCancelUserDiscountRestorePortImpl(
			UserDiscountMapper userDiscountMapper,
			UserDiscountLogsMapper userDiscountLogsMapper) {
		this.userDiscountMapper = userDiscountMapper;
		this.userDiscountLogsMapper = userDiscountLogsMapper;
	}

	@Override
	public void callbackUserCard(String couponCode, long orderId) {
		if (!StringUtils.hasText(couponCode)) {
			return;
		}
		UserDiscount discount =
				userDiscountMapper.selectOne(
						new LambdaQueryWrapper<UserDiscount>()
								.eq(UserDiscount::getCode, couponCode.trim())
								.last("LIMIT 1"));
		if (discount == null) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		userDiscountMapper.update(
				null,
				new LambdaUpdateWrapper<UserDiscount>()
						.eq(UserDiscount::getId, discount.getId())
						.set(UserDiscount::getStatus, 1));
		UserDiscountLogs log = new UserDiscountLogs();
		log.setUserId(discount.getUserId());
		log.setCompanyId(discount.getCompanyId());
		log.setCardId(discount.getCardId());
		log.setCode(couponCode.trim());
		log.setCardType(discount.getCardType());
		log.setUsedTime(now);
		log.setUsedStatus("callback");
		log.setUsedOrder(String.valueOf(orderId));
		userDiscountLogsMapper.insert(log);
	}
}
