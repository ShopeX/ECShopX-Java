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
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.goods.dispatch.ItemStoreUpdatedEventBusPublisher;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ItemStoreUpdatedEventBusPublisherImpl implements ItemStoreUpdatedEventBusPublisher {

	private final DispatchFacade dispatchFacade;

	public ItemStoreUpdatedEventBusPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void publish(long itemId, int store, long distributorId) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("item_id", itemId);
		payload.put("store", store);
		payload.put("distributor_id", distributorId);
		dispatchFacade.publishEvent(GoodsDispatchEventNames.EVENT_ITEM_STORE_UPDATE, payload, DispatchOptions.eventDefaults());
	}
}
