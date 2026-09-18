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

import cn.shopex.ecshopx.common.dispatch.HfpayDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchRegistry;
import cn.shopex.ecshopx.dispatch.ListenerDispatchOptions;
import cn.shopex.ecshopx.hfpay.dispatch.HfpayTradeRecordHfpayProfitSharingDispatchListener;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Order(99)
public class HfpayTradeRecordHfpayProfitSharingDispatchListenerRegistrationConfig {

	private final DispatchRegistry dispatchRegistry;
	private final HfpayTradeRecordHfpayProfitSharingDispatchListener hfpayTradeRecordHfpayProfitSharingDispatchListener;

	public HfpayTradeRecordHfpayProfitSharingDispatchListenerRegistrationConfig(
			DispatchRegistry dispatchRegistry,
			HfpayTradeRecordHfpayProfitSharingDispatchListener hfpayTradeRecordHfpayProfitSharingDispatchListener) {
		this.dispatchRegistry = dispatchRegistry;
		this.hfpayTradeRecordHfpayProfitSharingDispatchListener = hfpayTradeRecordHfpayProfitSharingDispatchListener;
	}

	@PostConstruct
	public void registerHfpayTradeRecordHfpayProfitSharingListener() {
		dispatchRegistry.registerEventListener(
				HfpayDispatchEventNames.EVENT_HFPAY_PROFIT_SHARING_TRADE_RECORD,
				HfpayDispatchEventNames.LISTENER_HFPAY_TRADE_RECORD_PROFIT_SHARING,
				ListenerDispatchOptions.syncDefaults(),
				hfpayTradeRecordHfpayProfitSharingDispatchListener);
	}
}
