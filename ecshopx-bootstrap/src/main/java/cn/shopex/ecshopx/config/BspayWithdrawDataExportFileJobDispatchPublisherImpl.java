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

import cn.shopex.ecshopx.bspay.dispatch.BspayWithdrawDataExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.bspay.dispatch.BspayWithdrawDataExportFileJobTypes;
import cn.shopex.ecshopx.bspay.service.export.BspayWithdrawDataExportContext;
import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Component;

@Component
public class BspayWithdrawDataExportFileJobDispatchPublisherImpl
		implements BspayWithdrawDataExportFileJobDispatchPublisher {

	private final DispatchFacade dispatchFacade;

	public BspayWithdrawDataExportFileJobDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void enqueueBspayWithdrawDataExport(BspayWithdrawDataExportContext ctx) {
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", BspayWithdrawDataExportFileJobTypes.TYPE_BSPAY_WITHDRAW);
		payload.put("company_id", ctx.companyId());
		payload.put("operator_id", ctx.jwtOperatorId());
		payload.put("filter", new LinkedHashMap<>(ctx.exportFilter()));
		dispatchFacade.dispatchJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_BSPAY_WITHDRAW_DATA,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));
	}
}
