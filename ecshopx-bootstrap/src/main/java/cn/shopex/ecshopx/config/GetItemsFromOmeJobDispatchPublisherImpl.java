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
import cn.shopex.ecshopx.goods.dispatch.GetItemsFromOmeJobDispatchPublisher;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GetItemsFromOmeJobDispatchPublisherImpl implements GetItemsFromOmeJobDispatchPublisher {

	private final DispatchFacade dispatchFacade;

	public GetItemsFromOmeJobDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void enqueueGetItemsFromOme(
			long companyId, int page, long endLastmodifyUnixSeconds, String goodsBn) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("company_id", companyId);
		map.put("page", page);
		map.put("end_lastmodify_unix", endLastmodifyUnixSeconds);
		map.put("goods_bn", goodsBn != null ? goodsBn : "");

		dispatchFacade.dispatchJob(
				GoodsBundleDispatchJobNames.GET_ITEMS_FROM_OME,
				map,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));
	}
}
