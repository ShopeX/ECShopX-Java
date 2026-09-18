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

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchJobNames;
import cn.shopex.ecshopx.common.dispatch.SendInvoiceEmailJobDispatchPublisher;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Profile("!test-cron")
public class SendInvoiceEmailJobDispatchPublisherImpl implements SendInvoiceEmailJobDispatchPublisher {

	private final DispatchFacade dispatchFacade;

	public SendInvoiceEmailJobDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void publish(String email, String invoiceFileUrl, long companyId) {
		publish(email, invoiceFileUrl, companyId, null);
	}

	@Override
	public void publish(String email, String invoiceFileUrl, long companyId, String subjectOrNull) {
		Map<String, Object> payload = new LinkedHashMap<>(4);
		payload.put("email", email);
		payload.put("invoice_file_url", invoiceFileUrl);
		payload.put("company_id", companyId);
		if (StringUtils.hasText(subjectOrNull)) {
			payload.put("subject", subjectOrNull.trim());
		}
		dispatchFacade.dispatchJob(
				OrdersDispatchJobNames.SEND_INVOICE_EMAIL_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"default",
						null,
						RetryPolicy.platformDefault()));
	}
}
