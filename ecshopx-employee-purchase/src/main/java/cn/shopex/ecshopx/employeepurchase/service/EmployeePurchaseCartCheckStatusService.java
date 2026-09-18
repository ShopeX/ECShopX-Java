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

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.employeepurchase.domain.Cart;
import cn.shopex.ecshopx.employeepurchase.mapper.CartMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;

@Service
public class EmployeePurchaseCartCheckStatusService {

	private final CartMapper cartMapper;

	public EmployeePurchaseCartCheckStatusService(CartMapper cartMapper) {
		this.cartMapper = cartMapper;
	}

	public int updateCartCheckStatus(
			long companyId,
			long userId,
			long enterpriseId,
			long activityId,
			long cartId,
			boolean isChecked) {
		LambdaQueryWrapper<Cart> query = new LambdaQueryWrapper<>();
		query
				.eq(Cart::getCompanyId, companyId)
				.eq(Cart::getUserId, userId)
				.eq(Cart::getEnterpriseId, enterpriseId)
				.eq(Cart::getActivityId, activityId)
				.eq(Cart::getCartId, cartId)
				.select(Cart::getIsChecked)
				.last("LIMIT 1");
		Cart row = cartMapper.selectOne(query);
		if (row == null) {
			return 0;
		}
		boolean current = Boolean.TRUE.equals(row.getIsChecked());
		if (current == isChecked) {
			return 0;
		}
		LambdaUpdateWrapper<Cart> wrapper = new LambdaUpdateWrapper<>();
		wrapper
				.eq(Cart::getCompanyId, companyId)
				.eq(Cart::getUserId, userId)
				.eq(Cart::getEnterpriseId, enterpriseId)
				.eq(Cart::getActivityId, activityId)
				.eq(Cart::getCartId, cartId)
				.set(Cart::getIsChecked, isChecked);
		return cartMapper.update(null, wrapper);
	}
}
