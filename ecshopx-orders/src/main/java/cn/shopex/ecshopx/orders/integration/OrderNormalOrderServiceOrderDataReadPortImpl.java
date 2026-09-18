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

package cn.shopex.ecshopx.orders.integration;

import cn.shopex.ecshopx.common.port.order.OrderNormalOrderServiceOrderDataReadPort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.normal.NormalOrdersServiceOrderDataAssembler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class OrderNormalOrderServiceOrderDataReadPortImpl implements OrderNormalOrderServiceOrderDataReadPort {

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler;

	public OrderNormalOrderServiceOrderDataReadPortImpl(
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersServiceOrderDataAssembler = normalOrdersServiceOrderDataAssembler;
	}

	@Override
	public Optional<Map<String, Object>> getServiceOrderData(long companyId, long orderId) {
		NormalOrders o =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (o == null) {
			return Optional.empty();
		}
		return Optional.of(normalOrdersServiceOrderDataAssembler.toServiceOrderData(o));
	}
}
