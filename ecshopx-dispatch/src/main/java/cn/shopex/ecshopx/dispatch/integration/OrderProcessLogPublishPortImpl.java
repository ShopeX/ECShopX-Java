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

package cn.shopex.ecshopx.dispatch.integration;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class OrderProcessLogPublishPortImpl implements OrderProcessLogPublishPort {

	private final DispatchFacade dispatchFacade;

	public OrderProcessLogPublishPortImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void publish(Map<String, Object> entities) {
		if (entities == null || entities.isEmpty()) {
			return;
		}
		Map<String, Object> payload = new LinkedHashMap<>(entities);
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(
					new TransactionSynchronization() {
						@Override
						public void afterCommit() {
							dispatchFacade.publishEvent(
									OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG,
									payload,
									DispatchOptions.oplQueuedAfterCommit());
						}
					});
		} else {
			dispatchFacade.publishEvent(
					OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG, payload, DispatchOptions.oplQueuedAfterCommit());
		}
	}
}
