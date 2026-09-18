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

package cn.shopex.ecshopx.orders.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.orders.service.refund.RefundByOrderUpdateOrderStatusJobService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RefundByOrderUpdateOrderStatusJobHandler implements DispatchHandler {

	private final RefundByOrderUpdateOrderStatusJobService refundByOrderUpdateOrderStatusJobService;

	public RefundByOrderUpdateOrderStatusJobHandler(
			RefundByOrderUpdateOrderStatusJobService refundByOrderUpdateOrderStatusJobService) {
		this.refundByOrderUpdateOrderStatusJobService = refundByOrderUpdateOrderStatusJobService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		if (payload == null) {
			return;
		}
		Long orderId = parseLong(payload.get("order_id"));
		if (orderId == null || orderId <= 0) {
			return;
		}
		Long companyId = parseLong(payload.get("company_id"));
		if (companyId == null || companyId <= 0) {
			return;
		}
		String orderType = str(payload.get("order_type"));
		refundByOrderUpdateOrderStatusJobService.execute(orderId, companyId, orderType);
	}

	private static Long parseLong(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(o).trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}
}
