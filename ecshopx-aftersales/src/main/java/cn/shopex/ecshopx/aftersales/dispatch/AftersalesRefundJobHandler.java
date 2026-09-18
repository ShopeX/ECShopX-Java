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

package cn.shopex.ecshopx.aftersales.dispatch;

import cn.shopex.ecshopx.aftersales.service.AftersalesRefundJobService;
import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesRefundQueueMessage;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AftersalesRefundJobHandler implements DispatchHandler {

	private final AftersalesRefundJobService aftersalesRefundJobService;

	public AftersalesRefundJobHandler(AftersalesRefundJobService aftersalesRefundJobService) {
		this.aftersalesRefundJobService = aftersalesRefundJobService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long refundBn = extractLong(payload.get("refund_bn"));
		long companyId = extractLong(payload.get("company_id"));
		Long orderId = extractNullableLong(payload.get("order_id"));
		aftersalesRefundJobService.handle(new AftersalesRefundQueueMessage(refundBn, companyId, orderId));
	}

	private static long extractLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}

	private static Long extractNullableLong(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof String s && s.isBlank()) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}
}
