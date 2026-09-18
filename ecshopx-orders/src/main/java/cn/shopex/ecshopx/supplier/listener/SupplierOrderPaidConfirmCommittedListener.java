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

package cn.shopex.ecshopx.supplier.listener;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.orders.event.PaySuccessSpringEvent;
import cn.shopex.ecshopx.supplier.event.SupplierOrderPaidConfirmCommittedSpringEvent;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SupplierOrderPaidConfirmCommittedListener {

	private final DispatchFacade dispatchFacade;
	private final ApplicationEventPublisher publisher;

	public SupplierOrderPaidConfirmCommittedListener(DispatchFacade dispatchFacade, ApplicationEventPublisher publisher) {
		this.dispatchFacade = dispatchFacade;
		this.publisher = publisher;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onSupplierOrderPaidConfirmCommitted(SupplierOrderPaidConfirmCommittedSpringEvent event) {
		Map<String, Object> logMap = new LinkedHashMap<>();
		logMap.put("order_id", event.getOrderId());
		logMap.put("company_id", event.getCompanyId());
		logMap.put("operator_type", "supplier");
		logMap.put("operator_id", event.getSupplierId());
		logMap.put("remarks", "确认收款");
		logMap.put("detail", "订单号：" + event.getOrderId() + " 确认收款");
		logMap.put("params", Collections.emptyMap());
		dispatchFacade.publishEvent(
				OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG, logMap, DispatchOptions.eventDefaults());

		Map<String, Object> oi = event.getOrderInfoMap();
		if (oi != null && !oi.isEmpty()) {
			publisher.publishEvent(new PaySuccessSpringEvent(this, oi));
		}
	}
}
