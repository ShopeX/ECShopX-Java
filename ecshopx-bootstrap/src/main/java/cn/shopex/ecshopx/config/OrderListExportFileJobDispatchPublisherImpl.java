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

import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.common.dispatch.OrderListExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test-cron")
public class OrderListExportFileJobDispatchPublisherImpl implements OrderListExportFileJobDispatchPublisher {

	private final DispatchFacade dispatchFacade;

	public OrderListExportFileJobDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void enqueueOrderListExport(
			long companyId, long operatorId, String exportType, LinkedHashMap<String, Object> filter) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", exportType);
		payload.put("company_id", companyId);
		payload.put("operator_id", operatorId);
		payload.put("filter", new LinkedHashMap<>(filter));
		DispatchOptions options =
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault());
		dispatchFacade.dispatchJob(EspierDispatchJobNames.EXPORT_FILE_JOB_ORDER_LIST, payload, options);
	}

	@Override
	public void enqueueInvoiceExport(long companyId, long operatorId, LinkedHashMap<String, Object> filter) {
		enqueueOrderListExport(companyId, operatorId, "invoice", filter);
	}

	@Override
	public void enqueueRightsExport(long companyId, long operatorId, LinkedHashMap<String, Object> filter) {
		enqueueOrderListExport(companyId, operatorId, "right", filter);
	}
}
