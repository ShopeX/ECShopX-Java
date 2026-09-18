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

package cn.shopex.ecshopx.orders.repository;

import cn.shopex.ecshopx.orders.domain.OrderItemsRelPointAccess;
import cn.shopex.ecshopx.orders.mapper.OrderItemsRelPointAccessMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class OrderItemsRelPointAccessReadRepository {

	private final OrderItemsRelPointAccessMapper mapper;

	public OrderItemsRelPointAccessReadRepository(OrderItemsRelPointAccessMapper mapper) {
		this.mapper = mapper;
	}

	public Map<Long, Long> mapPointByItemId(long companyId, Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return Collections.emptyMap();
		}
		LambdaQueryWrapper<OrderItemsRelPointAccess> w = new LambdaQueryWrapper<>();
		w.eq(OrderItemsRelPointAccess::getCompanyId, companyId).in(OrderItemsRelPointAccess::getItemId, itemIds);
		Map<Long, Long> out = new HashMap<>();
		for (OrderItemsRelPointAccess row : mapper.selectList(w)) {
			if (row.getItemId() != null && row.getPoint() != null) {
				out.put(row.getItemId(), row.getPoint());
			}
		}
		return out;
	}
}
