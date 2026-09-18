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

package cn.shopex.ecshopx.systemlink.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.systemlink.service.ome.TradeFinishSendOmeBusService;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TradeFinishSendOmeDispatchListener implements DispatchListener {

	private final TradeFinishSendOmeBusService tradeFinishSendOmeBusService;

	public TradeFinishSendOmeDispatchListener(TradeFinishSendOmeBusService tradeFinishSendOmeBusService) {
		this.tradeFinishSendOmeBusService = tradeFinishSendOmeBusService;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		if (payload == null || payload.isEmpty()) {
			return;
		}
		if (parsePositiveLong(first(payload, "company_id", "companyId")) == null) {
			return;
		}
		if (!StringUtils.hasText(stringifyOrderId(first(payload, "order_id", "orderId")))) {
			return;
		}
		tradeFinishSendOmeBusService.handleTradeFinishRow(payload);
	}

	private static Object first(Map<String, Object> m, String a, String b) {
		Object x = m.get(a);
		if (x != null) {
			return x;
		}
		return m.get(b);
	}

	private static String stringifyOrderId(Object o) {
		if (o == null) {
			return "";
		}
		return o.toString().trim();
	}

	private static Long parsePositiveLong(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : null;
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
