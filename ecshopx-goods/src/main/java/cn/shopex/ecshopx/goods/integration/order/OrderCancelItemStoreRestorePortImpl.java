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

package cn.shopex.ecshopx.goods.integration.order;

import cn.shopex.ecshopx.common.inventory.ItemInventoryLineContext;
import cn.shopex.ecshopx.common.port.order.OrderCancelItemStoreRestorePort;
import cn.shopex.ecshopx.goods.service.items.ItemInventoryOrchestratorService;
import org.springframework.stereotype.Service;

/** 订单取消后回补商品库存（按 receipt_type / supplier_id / is_total_store 路由到对应库存池）。 */
@Service
public class OrderCancelItemStoreRestorePortImpl implements OrderCancelItemStoreRestorePort {

	private final ItemInventoryOrchestratorService itemInventoryOrchestratorService;

	public OrderCancelItemStoreRestorePortImpl(ItemInventoryOrchestratorService itemInventoryOrchestratorService) {
		this.itemInventoryOrchestratorService = itemInventoryOrchestratorService;
	}

	@Override
	public void restoreStore(ItemInventoryLineContext ctx, int restoreQty) {
		itemInventoryOrchestratorService.restoreItemStore(ctx, restoreQty);
	}
}
