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

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.orders.dispatch.InvoiceExportFileJobPayloadSupport;
import cn.shopex.ecshopx.orders.dispatch.OrderListExportFileJobPayloadSupport;
import cn.shopex.ecshopx.orders.dispatch.RightsExportFileJobPayloadSupport;
import cn.shopex.ecshopx.orders.service.invoice.export.InvoiceExportFileJobHandler;
import cn.shopex.ecshopx.orders.service.orderexport.OrderExportFileJobHandler;
import cn.shopex.ecshopx.orders.service.rights.export.RightsExportFileJobHandler;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OrderListExportFileJobDispatchHandler implements DispatchHandler {

	private final OrderExportFileJobHandler orderExportFileJobHandler;
	private final InvoiceExportFileJobHandler invoiceExportFileJobHandler;
	private final RightsExportFileJobHandler rightsExportFileJobHandler;

	public OrderListExportFileJobDispatchHandler(
			OrderExportFileJobHandler orderExportFileJobHandler,
			InvoiceExportFileJobHandler invoiceExportFileJobHandler,
			RightsExportFileJobHandler rightsExportFileJobHandler) {
		this.orderExportFileJobHandler = orderExportFileJobHandler;
		this.invoiceExportFileJobHandler = invoiceExportFileJobHandler;
		this.rightsExportFileJobHandler = rightsExportFileJobHandler;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		String type = payload.get("type") == null ? "" : String.valueOf(payload.get("type")).trim();
		if ("invoice".equals(type)) {
			invoiceExportFileJobHandler.run(InvoiceExportFileJobPayloadSupport.contextFromPayload(payload));
		} else if ("right".equals(type)) {
			rightsExportFileJobHandler.run(RightsExportFileJobPayloadSupport.contextFromPayload(payload));
		} else {
			orderExportFileJobHandler.run(OrderListExportFileJobPayloadSupport.contextFromPayload(payload));
		}
	}
}
