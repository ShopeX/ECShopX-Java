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

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.orders.service.refund.OfflinePayTradeFinishCanceledNormalOrderRefundService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OrdersTradeFinishOfflinePayCanceledNormalOrderRefundDispatchListener implements DispatchListener {

	private final OfflinePayTradeFinishCanceledNormalOrderRefundService delegate;

	public OrdersTradeFinishOfflinePayCanceledNormalOrderRefundDispatchListener(
			OfflinePayTradeFinishCanceledNormalOrderRefundService delegate) {
		this.delegate = delegate;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		if (payload == null) {
			return;
		}
		String tradeState = stringify(payload.get("trade_state"));
		if (!"SUCCESS".equals(tradeState)) {
			return;
		}
		delegate.executeIfApplicable(payload);
	}

	private static String stringify(Object raw) {
		return raw == null ? "" : String.valueOf(raw).trim();
	}
}
