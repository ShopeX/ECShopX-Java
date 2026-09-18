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

package cn.shopex.ecshopx.goods.integration.orders;

import cn.shopex.ecshopx.common.orders.port.AdminOrderDetailItemsSkuSpecPort;
import cn.shopex.ecshopx.goods.service.items.OrderDetailItemsRealSkuSpecApplier;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminOrderDetailItemsSkuSpecPortImpl implements AdminOrderDetailItemsSkuSpecPort {

	private final OrderDetailItemsRealSkuSpecApplier orderDetailItemsRealSkuSpecApplier;

	public AdminOrderDetailItemsSkuSpecPortImpl(OrderDetailItemsRealSkuSpecApplier orderDetailItemsRealSkuSpecApplier) {
		this.orderDetailItemsRealSkuSpecApplier = orderDetailItemsRealSkuSpecApplier;
	}

	@Override
	public void applyRealSkuSpec(long companyId, List<Map<String, Object>> orderItems) {
		orderDetailItemsRealSkuSpecApplier.apply(companyId, orderItems);
	}
}
