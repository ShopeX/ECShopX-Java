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

import cn.shopex.ecshopx.common.dispatch.CancelActivityOrdersJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.CommunityDispatchJobNames;
import cn.shopex.ecshopx.common.port.orders.CommunityActivityCancelOrderRow;
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
public class CancelActivityOrdersJobDispatchPublisherImpl implements CancelActivityOrdersJobDispatchPublisher {

	private final DispatchFacade dispatchFacade;

	public CancelActivityOrdersJobDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void publishBatch(List<CommunityActivityCancelOrderRow> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		List<Map<String, Object>> rowMaps = new ArrayList<>(rows.size());
		for (CommunityActivityCancelOrderRow r : rows) {
			rowMaps.add(toPayloadRow(r));
		}
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("rows", rowMaps);
		dispatchFacade.dispatchJob(
				CommunityDispatchJobNames.CANCEL_ACTIVITY_ORDERS_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));
	}

	private static Map<String, Object> toPayloadRow(CommunityActivityCancelOrderRow r) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", r.getCompanyId());
		m.put("order_id", r.getOrderId());
		m.put("cancel_reason", r.getCancelReason() == null ? "" : r.getCancelReason());
		m.put("user_id", r.getUserId());
		m.put("mobile", r.getMobile() == null ? "" : r.getMobile());
		m.put("cancel_from", r.getCancelFrom() == null ? "system" : r.getCancelFrom());
		if (r.getChiefId() != null && r.getChiefId() > 0L) {
			m.put("chief_id", r.getChiefId());
		}
		return m;
	}
}
