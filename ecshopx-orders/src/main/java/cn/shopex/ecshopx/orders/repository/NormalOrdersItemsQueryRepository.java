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

import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class NormalOrdersItemsQueryRepository {

	private final NormalOrdersItemsMapper mapper;

	public NormalOrdersItemsQueryRepository(NormalOrdersItemsMapper mapper) {
		this.mapper = mapper;
	}

	public List<NormalOrdersItems> listByCompanyUserOrder(long companyId, long userId, long orderId) {
		LambdaQueryWrapper<NormalOrdersItems> w = new LambdaQueryWrapper<>();
		w.eq(NormalOrdersItems::getCompanyId, companyId).eq(NormalOrdersItems::getUserId, userId)
				.eq(NormalOrdersItems::getOrderId, orderId)
				.select(NormalOrdersItems::getItemId, NormalOrdersItems::getNum)
				.orderByAsc(NormalOrdersItems::getId);
		return mapper.selectList(w);
	}
}
