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
import cn.shopex.ecshopx.dispatch.DispatchRegistry;
import cn.shopex.ecshopx.orders.dispatch.SendInvoiceEmailJobHandler;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SendInvoiceEmailJobRegistrationConfig {

	private final DispatchRegistry dispatchRegistry;
	private final SendInvoiceEmailJobHandler sendInvoiceEmailJobHandler;

	public SendInvoiceEmailJobRegistrationConfig(
			DispatchRegistry dispatchRegistry, SendInvoiceEmailJobHandler sendInvoiceEmailJobHandler) {
		this.dispatchRegistry = dispatchRegistry;
		this.sendInvoiceEmailJobHandler = sendInvoiceEmailJobHandler;
	}

	@PostConstruct
	public void registerSendInvoiceEmailJob() {
		dispatchRegistry.registerJob(OrdersDispatchJobNames.SEND_INVOICE_EMAIL_JOB, sendInvoiceEmailJobHandler);
	}
}
