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

package cn.shopex.ecshopx.popularize.dispatch;

import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import cn.shopex.ecshopx.popularize.service.export.PopularizeOrderExportJobContext;
import cn.shopex.ecshopx.popularize.service.export.PopularizeStaticExportJobContext;
import cn.shopex.ecshopx.popularize.service.export.PromoterExportJobContext;
import org.springframework.stereotype.Component;

@Component
public class PopularizePromoterExportFileJobDispatchPublisher {

	public static final String POPULARIZE_PROMOTER_EXPORT_JOB_QUEUE = "slow";

	private final DispatchFacade dispatchFacade;

	public PopularizePromoterExportFileJobDispatchPublisher(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	public void enqueuePopularizePromoterExport(PromoterExportJobContext ctx) {
		dispatchFacade.dispatchJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_POPULARIZE_PROMOTER_LIST,
				PopularizePromoterExportFileJobPayloadSupport.toPayload(ctx),
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						POPULARIZE_PROMOTER_EXPORT_JOB_QUEUE,
						null,
						RetryPolicy.platformDefault()));
	}

	public void enqueuePopularizeOrderExport(PopularizeOrderExportJobContext ctx) {
		dispatchFacade.dispatchJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_POPULARIZE_ORDER,
				PopularizeOrderExportFileJobPayloadSupport.toPayload(ctx),
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						POPULARIZE_PROMOTER_EXPORT_JOB_QUEUE,
						null,
						RetryPolicy.platformDefault()));
	}

	public void enqueuePopularizeStaticExport(PopularizeStaticExportJobContext ctx) {
		dispatchFacade.dispatchJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_POPULARIZE_STATIC,
				PopularizeStaticExportFileJobPayloadSupport.toPayload(ctx),
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						POPULARIZE_PROMOTER_EXPORT_JOB_QUEUE,
						null,
						RetryPolicy.platformDefault()));
	}
}
