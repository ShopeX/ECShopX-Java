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
import cn.shopex.ecshopx.datacube.service.goodsdata.GoodsDailyStatLineKey;
import cn.shopex.ecshopx.datacube.service.goodsdata.GoodsStatisticJobEnqueuePort;
import cn.shopex.ecshopx.datacube.service.goodsdata.GoodsStatisticsBatch;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test-cron")
public class GoodsStatisticJobDispatchPublisherImpl implements GoodsStatisticJobEnqueuePort {

	private final DispatchFacade dispatchFacade;

	public GoodsStatisticJobDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void enqueue(GoodsStatisticsBatch batch, LocalDate countDate) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("count_date", countDate != null ? countDate.toString() : null);
		payload.put("lines", toLineMaps(batch));
		dispatch(payload);
	}

	@Override
	public void enqueueEmployeePurchase(GoodsStatisticsBatch batch, LocalDate countDate, long actId) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("count_date", countDate != null ? countDate.toString() : null);
		payload.put("lines", toLineMaps(batch));
		payload.put("order_class", "employee_purchase");
		payload.put("act_id", actId);
		dispatch(payload);
	}

	private void dispatch(Map<String, Object> payload) {
		dispatchFacade.dispatchJob(
				DatacubeDispatchJobNames.GOODS_STATISTIC_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));
	}

	private static List<Map<String, Object>> toLineMaps(GoodsStatisticsBatch batch) {
		List<Map<String, Object>> lines = new ArrayList<>();
		if (batch == null || batch.lines() == null) {
			return lines;
		}
		for (GoodsDailyStatLineKey key : batch.lines()) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("order_id", key.getOrderId());
			row.put("line_id", key.getLineId());
			lines.add(row);
		}
		return lines;
	}
}
