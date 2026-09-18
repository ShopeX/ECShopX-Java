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

import cn.shopex.ecshopx.common.dispatch.GoodsBundleDispatchJobNames;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import cn.shopex.ecshopx.goods.dispatch.UploadItemsToJushuitanJobDispatchPublisher;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class UploadItemsToJushuitanJobDispatchPublisherImpl implements UploadItemsToJushuitanJobDispatchPublisher {

	private final DispatchFacade dispatchFacade;

	public UploadItemsToJushuitanJobDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void enqueueUploadItemsToJushuitan(
			long companyId, List<Long> itemIds, long distributorId, String itemType) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("company_id", companyId);
		map.put("item_ids", itemIds);
		map.put("distributor_id", distributorId);
		map.put("item_type", itemType != null ? itemType : "normal");

		dispatchFacade.dispatchJob(
				GoodsBundleDispatchJobNames.UPLOAD_ITEMS_TO_JUSHUITAN,
				map,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));
	}
}
