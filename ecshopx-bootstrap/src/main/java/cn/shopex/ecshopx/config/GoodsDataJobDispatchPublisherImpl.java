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

import cn.shopex.ecshopx.common.dispatch.DatacubeDispatchJobNames;
import cn.shopex.ecshopx.datacube.service.goodsdata.AdminGoodsDataFilter;
import cn.shopex.ecshopx.datacube.service.goodsdata.GoodsDataJobEnqueuePort;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GoodsDataJobDispatchPublisherImpl implements GoodsDataJobEnqueuePort {

	private final DispatchFacade dispatchFacade;

	public GoodsDataJobDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void enqueue(AdminGoodsDataFilter filter) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", filter.companyId());
		payload.put("date_start", filter.dateStart());
		payload.put("date_end", filter.dateEnd());
		payload.put("order_class_restrict_to_value", filter.orderClassRestrictToValue());
		payload.put("order_class_value", filter.orderClassValue());
		payload.put("act_ids", new ArrayList<>(filter.actIdsForIn()));
		Long merchantId = filter.merchantIdOrNull();
		if (merchantId != null) {
			payload.put("merchant_id", merchantId);
		}
		payload.put("operator_id", filter.operatorId());

		dispatchFacade.dispatchJob(
				DatacubeDispatchJobNames.GOODS_DATA_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));
	}
}
