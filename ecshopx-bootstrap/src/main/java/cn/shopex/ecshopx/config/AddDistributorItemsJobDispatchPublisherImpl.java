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

import cn.shopex.ecshopx.common.dispatch.AddDistributorItemsJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.DistributionDispatchJobNames;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test-cron")
public class AddDistributorItemsJobDispatchPublisherImpl implements AddDistributorItemsJobDispatchPublisher {

	private final DispatchFacade dispatchFacade;

	public AddDistributorItemsJobDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void enqueueFirstPage(
			long companyId,
			long distributorId,
			List<Long> itemIdsFilterOrNullForAll,
			boolean isCanSale,
			int pageSize) {
		dispatchFacade.dispatchJob(
				DistributionDispatchJobNames.ADD_DISTRIBUTOR_ITEMS_JOB,
				buildPayload(companyId, distributorId, itemIdsFilterOrNullForAll, isCanSale, 1, pageSize),
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));
	}

	@Override
	public void enqueuePage(
			long companyId,
			long distributorId,
			List<Long> itemIdsFilterOrNullForAll,
			boolean isCanSale,
			int page,
			int pageSize) {
		dispatchFacade.dispatchJob(
				DistributionDispatchJobNames.ADD_DISTRIBUTOR_ITEMS_JOB,
				buildPayload(companyId, distributorId, itemIdsFilterOrNullForAll, isCanSale, page, pageSize),
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));
	}

	private static Map<String, Object> buildPayload(
			long companyId,
			long distributorId,
			List<Long> itemIdsFilterOrNullForAll,
			boolean isCanSale,
			int page,
			int pageSize) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", companyId);
		m.put("distributor_id", distributorId);
		if (itemIdsFilterOrNullForAll == null) {
			m.put("item_ids", "_all");
		} else {
			m.put("item_ids", new ArrayList<>(itemIdsFilterOrNullForAll));
		}
		m.put("is_can_sale", isCanSale);
		m.put("page", page);
		m.put("pageSize", pageSize);
		return m;
	}
}
