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

package cn.shopex.ecshopx.popularize.integration;

import cn.shopex.ecshopx.common.port.aftersales.AftersalesBrokeragePort;
import cn.shopex.ecshopx.popularize.service.BrokerageAftersalesClawbackService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component("aftersalesBrokeragePortImpl")
public class AftersalesBrokeragePortImpl implements AftersalesBrokeragePort {

	private final BrokerageAftersalesClawbackService brokerageAftersalesClawbackService;

	public AftersalesBrokeragePortImpl(BrokerageAftersalesClawbackService brokerageAftersalesClawbackService) {
		this.brokerageAftersalesClawbackService = brokerageAftersalesClawbackService;
	}

	@Override
	public void brokerageByAftersales(Map<String, Object> params) {
		if (params == null || params.isEmpty()) {
			return;
		}
		long companyId = longVal(params.get("company_id"));
		long orderId = longVal(params.get("order_id"));
		long itemId = longVal(params.get("item_id"));
		int num = intVal(params.get("num"));
		brokerageAftersalesClawbackService.brokerageByAftersales(companyId, orderId, itemId, num);
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int intVal(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
