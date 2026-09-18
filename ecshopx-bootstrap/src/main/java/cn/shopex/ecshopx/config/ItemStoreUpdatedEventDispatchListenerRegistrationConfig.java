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

import cn.shopex.ecshopx.ali.dispatch.ItemStoreUpdateSendAliTemplateDispatchListener;
import cn.shopex.ecshopx.common.dispatch.GoodsDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchRegistry;
import cn.shopex.ecshopx.dispatch.ListenerDispatchOptions;
import cn.shopex.ecshopx.members.listener.ItemStoreUpdatedGoodsArrivalDispatchListener;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Order(75)
public class ItemStoreUpdatedEventDispatchListenerRegistrationConfig {

	private final DispatchRegistry dispatchRegistry;
	private final ItemStoreUpdatedGoodsArrivalDispatchListener itemStoreUpdatedGoodsArrivalDispatchListener;
	private final ItemStoreUpdateSendAliTemplateDispatchListener itemStoreUpdateSendAliTemplateDispatchListener;

	public ItemStoreUpdatedEventDispatchListenerRegistrationConfig(
			DispatchRegistry dispatchRegistry,
			ItemStoreUpdatedGoodsArrivalDispatchListener itemStoreUpdatedGoodsArrivalDispatchListener,
			ItemStoreUpdateSendAliTemplateDispatchListener itemStoreUpdateSendAliTemplateDispatchListener) {
		this.dispatchRegistry = dispatchRegistry;
		this.itemStoreUpdatedGoodsArrivalDispatchListener = itemStoreUpdatedGoodsArrivalDispatchListener;
		this.itemStoreUpdateSendAliTemplateDispatchListener = itemStoreUpdateSendAliTemplateDispatchListener;
	}

	@PostConstruct
	public void registerItemStoreUpdatedEventListeners() {
		dispatchRegistry.registerEventListener(
				GoodsDispatchEventNames.EVENT_ITEM_STORE_UPDATE,
				"listener:goods.listeners.SendTemplateMsgListener",
				ListenerDispatchOptions.syncDefaults(),
				itemStoreUpdateSendAliTemplateDispatchListener);
		dispatchRegistry.registerEventListener(
				GoodsDispatchEventNames.EVENT_ITEM_STORE_UPDATE,
				"listener:members.listeners.ItemStoreUpdatedGoodsArrival",
				ListenerDispatchOptions.syncDefaults(),
				itemStoreUpdatedGoodsArrivalDispatchListener);
	}
}
