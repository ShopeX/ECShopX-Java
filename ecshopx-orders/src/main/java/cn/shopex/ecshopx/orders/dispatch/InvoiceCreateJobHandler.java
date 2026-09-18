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

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.orders.service.invoice.InvoiceCreateJobService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class InvoiceCreateJobHandler implements DispatchHandler {

	private final InvoiceCreateJobService invoiceCreateJobService;

	public InvoiceCreateJobHandler(InvoiceCreateJobService invoiceCreateJobService) {
		this.invoiceCreateJobService = invoiceCreateJobService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		if (payload == null) {
			return;
		}
		Long invoiceId = parseLong(payload.get("invoice_id"));
		if (invoiceId == null || invoiceId <= 0) {
			return;
		}
		invoiceCreateJobService.execute(payload);
	}

	private static Long parseLong(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(o).trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
