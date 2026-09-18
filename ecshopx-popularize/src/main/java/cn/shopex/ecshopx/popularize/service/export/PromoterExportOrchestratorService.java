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

package cn.shopex.ecshopx.popularize.service.export;

import cn.shopex.ecshopx.popularize.dispatch.PopularizePromoterExportFileJobDispatchPublisher;
import org.springframework.stereotype.Service;

@Service
public class PromoterExportOrchestratorService {

	private final PopularizePromoterExportFileJobDispatchPublisher popularizePromoterExportFileJobDispatchPublisher;

	public PromoterExportOrchestratorService(
			PopularizePromoterExportFileJobDispatchPublisher popularizePromoterExportFileJobDispatchPublisher) {
		this.popularizePromoterExportFileJobDispatchPublisher = popularizePromoterExportFileJobDispatchPublisher;
	}

	public void exportPromoterList(PromoterExportJobContext ctx) {
		popularizePromoterExportFileJobDispatchPublisher.enqueuePopularizePromoterExport(ctx);
	}

	public void exportPopularizeOrder(PopularizeOrderExportJobContext ctx) {
		popularizePromoterExportFileJobDispatchPublisher.enqueuePopularizeOrderExport(ctx);
	}

	public void exportPopularizeStatic(PopularizeStaticExportJobContext ctx) {
		popularizePromoterExportFileJobDispatchPublisher.enqueuePopularizeStaticExport(ctx);
	}
}
