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

package cn.shopex.ecshopx.config;

import cn.shopex.ecshopx.common.dispatch.GoodsDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchRegistry;
import cn.shopex.ecshopx.dispatch.ListenerDispatchOptions;
import cn.shopex.ecshopx.goods.dispatch.ItemsApproveStatusSyncDispatchListener;
import cn.shopex.ecshopx.thirdparty.dispatch.ItemBatchEditStatusPushMarketingCenterDispatchListener;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ItemBatchEditStatusEventDispatchListenerRegistrationConfig {

	private final DispatchRegistry dispatchRegistry;
	private final ItemBatchEditStatusPushMarketingCenterDispatchListener
			itemBatchEditStatusPushMarketingCenterDispatchListener;
	private final ItemsApproveStatusSyncDispatchListener itemsApproveStatusSyncDispatchListener;

	public ItemBatchEditStatusEventDispatchListenerRegistrationConfig(
			DispatchRegistry dispatchRegistry,
			ItemBatchEditStatusPushMarketingCenterDispatchListener itemBatchEditStatusPushMarketingCenterDispatchListener,
			ItemsApproveStatusSyncDispatchListener itemsApproveStatusSyncDispatchListener) {
		this.dispatchRegistry = dispatchRegistry;
		this.itemBatchEditStatusPushMarketingCenterDispatchListener =
				itemBatchEditStatusPushMarketingCenterDispatchListener;
		this.itemsApproveStatusSyncDispatchListener = itemsApproveStatusSyncDispatchListener;
	}

	@PostConstruct
	public void registerItemBatchEditStatusEventListeners() {
		dispatchRegistry.registerEventListener(
				GoodsDispatchEventNames.EVENT_ITEM_BATCH_EDIT_STATUS,
				"listener:thirdparty.item_batch_edit_status_push_marketing_center",
				ListenerDispatchOptions.asyncDefaults(),
				itemBatchEditStatusPushMarketingCenterDispatchListener);
		dispatchRegistry.registerEventListener(
				GoodsDispatchEventNames.EVENT_ITEM_BATCH_EDIT_STATUS,
				"listener:goods.items_approve_status_sync",
				ListenerDispatchOptions.asyncDefaults(),
				itemsApproveStatusSyncDispatchListener);
	}
}
