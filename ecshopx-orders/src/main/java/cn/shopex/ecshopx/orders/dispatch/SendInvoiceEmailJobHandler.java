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
import cn.shopex.ecshopx.orders.service.invoice.SendInvoiceEmailJobService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SendInvoiceEmailJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(SendInvoiceEmailJobHandler.class);

	private final SendInvoiceEmailJobService sendInvoiceEmailJobService;

	public SendInvoiceEmailJobHandler(SendInvoiceEmailJobService sendInvoiceEmailJobService) {
		this.sendInvoiceEmailJobService = sendInvoiceEmailJobService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		if (payload == null) {
			return;
		}
		String email = readString(payload.get("email"));
		if (!StringUtils.hasText(email)) {
			log.warn("send-invoice-email job skipped: missing email");
			return;
		}
		String invoiceFileUrl = readString(payload.get("invoice_file_url"));
		if (!StringUtils.hasText(invoiceFileUrl)) {
			log.warn("send-invoice-email job skipped: missing invoice_file_url");
			return;
		}
		Long companyId = parseLong(payload.get("company_id"));
		if (companyId == null || companyId <= 0) {
			log.warn("send-invoice-email job skipped: invalid company_id");
			return;
		}
		String subjectOrNull = readOptionalSubject(payload.get("subject"));
		sendInvoiceEmailJobService.execute(email.trim(), invoiceFileUrl.trim(), companyId, subjectOrNull);
	}

	private static String readString(Object o) {
		if (o == null) {
			return null;
		}
		return String.valueOf(o);
	}

	private static String readOptionalSubject(Object o) {
		String s = readString(o);
		if (s == null || s.isBlank()) {
			return null;
		}
		return s.trim();
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
