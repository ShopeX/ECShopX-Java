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
import cn.shopex.ecshopx.orders.service.invoice.InvoiceRedQueryJobService;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class InvoiceRedQueryJobHandler implements DispatchHandler {

	private final InvoiceRedQueryJobService invoiceRedQueryJobService;

	public InvoiceRedQueryJobHandler(InvoiceRedQueryJobService invoiceRedQueryJobService) {
		this.invoiceRedQueryJobService = invoiceRedQueryJobService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		if (payload == null) {
			return;
		}
		Long invoiceId = parseLong(payload.get("id"));
		Long companyId = parseLong(payload.get("company_id"));
		String orderId = str(payload.get("order_id"));
		String redSerial = str(payload.get("red_confirm_serial_no"));
		if (invoiceId == null || invoiceId <= 0) {
			log.warn("[InvoiceRedQueryJobHandler][handle] missing id keys={}", payload.keySet());
			return;
		}
		if (companyId == null || companyId <= 0) {
			log.warn("[InvoiceRedQueryJobHandler][handle] missing company_id keys={}", payload.keySet());
			return;
		}
		if (!StringUtils.hasText(orderId)) {
			log.warn("[InvoiceRedQueryJobHandler][handle] missing order_id keys={}", payload.keySet());
			return;
		}
		if (!StringUtils.hasText(redSerial)) {
			log.warn("[InvoiceRedQueryJobHandler][handle] missing red_confirm_serial_no keys={}", payload.keySet());
			return;
		}
		invoiceRedQueryJobService.execute(payload);
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

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}
}
