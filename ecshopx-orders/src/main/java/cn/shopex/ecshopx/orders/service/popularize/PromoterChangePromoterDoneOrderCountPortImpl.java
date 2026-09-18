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

package cn.shopex.ecshopx.orders.service.popularize;

import cn.shopex.ecshopx.common.popularize.PromoterChangePromoterDoneOrderCountPort;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

@Service
public class PromoterChangePromoterDoneOrderCountPortImpl implements PromoterChangePromoterDoneOrderCountPort {

	private final OrderAssociationsMapper orderAssociationsMapper;

	public PromoterChangePromoterDoneOrderCountPortImpl(OrderAssociationsMapper orderAssociationsMapper) {
		this.orderAssociationsMapper = orderAssociationsMapper;
	}

	@Override
	public long countDoneOrdersForBuyer(long companyId, long userId) {
		Long n = orderAssociationsMapper.selectCount(new LambdaQueryWrapper<OrderAssociations>()
				.eq(OrderAssociations::getOrderStatus, "DONE")
				.eq(OrderAssociations::getUserId, userId)
				.eq(OrderAssociations::getCompanyId, companyId));
		return n == null ? 0L : n;
	}
}
