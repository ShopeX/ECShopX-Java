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

package cn.shopex.ecshopx.thirdparty.service.dmcrm;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OrderDeliveryDmCrmOnNormalOrderDeliveryProcessor {

	private final DmCrmSettingReadPort dmCrmSettingReadPort;
	private final DmCrmNormalOrderDeliverySyncPort dmCrmNormalOrderDeliverySyncPort;

	public OrderDeliveryDmCrmOnNormalOrderDeliveryProcessor(
			DmCrmSettingReadPort dmCrmSettingReadPort,
			DmCrmNormalOrderDeliverySyncPort dmCrmNormalOrderDeliverySyncPort) {
		this.dmCrmSettingReadPort = dmCrmSettingReadPort;
		this.dmCrmNormalOrderDeliverySyncPort = dmCrmNormalOrderDeliverySyncPort;
	}

	public void handle(Map<String, Object> busPayload) {
		if (busPayload == null || busPayload.isEmpty()) {
			return;
		}
		long companyId = longVal(busPayload.get("company_id"));
		Object orderIdRaw = busPayload.get("order_id");
		if (companyId <= 0L || orderIdRaw == null) {
			return;
		}
		long orderId = longVal(orderIdRaw);
		if (orderId <= 0L) {
			return;
		}
		if (!dmCrmSettingReadPort.isPointIntegrationOpen(companyId)) {
			return;
		}
		dmCrmNormalOrderDeliverySyncPort.syncNormalOrderDelivery(companyId, orderId, busPayload);
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
