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

package cn.shopex.ecshopx.orders.dispatch;

import cn.shopex.ecshopx.common.dispatch.OrdersTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import java.util.Map;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class OrdersTradeFinishAfterCommitDispatchPublisher implements OrdersTradeFinishDispatchPublisher {

	private final DispatchFacade dispatchFacade;
	private final String tradeFinishMessageName;

	public OrdersTradeFinishAfterCommitDispatchPublisher(
			DispatchFacade dispatchFacade, String tradeFinishMessageName) {
		this.dispatchFacade = dispatchFacade;
		this.tradeFinishMessageName = tradeFinishMessageName;
	}

	@Override
	public void publish(Map<String, Object> tradeRowPayload) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(
					new TransactionSynchronization() {
						@Override
						public void afterCommit() {
							dispatchFacade.publishEvent(
									tradeFinishMessageName, tradeRowPayload, DispatchOptions.eventDefaults());
						}
					});
		} else {
			dispatchFacade.publishEvent(tradeFinishMessageName, tradeRowPayload, DispatchOptions.eventDefaults());
		}
	}
}
