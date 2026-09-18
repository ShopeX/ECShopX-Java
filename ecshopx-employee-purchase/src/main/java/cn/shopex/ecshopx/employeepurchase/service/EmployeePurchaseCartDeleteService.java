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
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class EmployeePurchaseCartDeleteService {

	private final CartMapper cartMapper;

	public EmployeePurchaseCartDeleteService(CartMapper cartMapper) {
		this.cartMapper = cartMapper;
	}

	public void deleteByFilter(
			long companyId,
			long userId,
			long enterpriseId,
			long activityId,
			Optional<Long> cartId,
			Optional<Long> itemId) {
		LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
		wrapper
				.eq(Cart::getCompanyId, companyId)
				.eq(Cart::getUserId, userId)
				.eq(Cart::getEnterpriseId, enterpriseId)
				.eq(Cart::getActivityId, activityId);
		cartId.ifPresent(id -> wrapper.eq(Cart::getCartId, id));
		itemId.ifPresent(id -> wrapper.eq(Cart::getItemId, id));
		cartMapper.delete(wrapper);
	}

	public void deleteByItemIds(
			long companyId,
			long userId,
			long enterpriseId,
			long activityId,
			List<Long> itemIds) {
		if (CollectionUtils.isEmpty(itemIds)) {
			return;
		}
		LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
		wrapper
				.eq(Cart::getCompanyId, companyId)
				.eq(Cart::getUserId, userId)
				.eq(Cart::getEnterpriseId, enterpriseId)
				.eq(Cart::getActivityId, activityId)
				.in(Cart::getItemId, itemIds);
		cartMapper.delete(wrapper);
	}
}
