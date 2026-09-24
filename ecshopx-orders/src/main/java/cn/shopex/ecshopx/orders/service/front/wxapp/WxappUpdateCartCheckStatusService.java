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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.orders.domain.Cart;
import cn.shopex.ecshopx.orders.mapper.CartMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class WxappUpdateCartCheckStatusService {

	private final CartMapper cartMapper;

	public WxappUpdateCartCheckStatusService(CartMapper cartMapper) {
		this.cartMapper = cartMapper;
	}

	public int updateCartCheckStatus(
			long companyId, long effectiveUserId, long cartId, boolean isCheckedForDb) {
		return updateCartCheckStatus(companyId, effectiveUserId, List.of(cartId), isCheckedForDb);
	}

	public int updateCartCheckStatus(
			long companyId, long effectiveUserId, List<Long> cartIds, boolean isCheckedForDb) {
		if (cartIds == null || cartIds.isEmpty()) {
			return 0;
		}
		LambdaUpdateWrapper<Cart> wrapper =
				new LambdaUpdateWrapper<Cart>()
						.in(Cart::getCartId, cartIds)
						.eq(Cart::getUserId, effectiveUserId)
						.eq(Cart::getCompanyId, companyId)
						.ne(Cart::getIsChecked, isCheckedForDb)
						.set(Cart::getIsChecked, isCheckedForDb);
		return cartMapper.update(null, wrapper);
	}
}
