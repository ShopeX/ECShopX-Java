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

package cn.shopex.ecshopx.orders.listener;

import cn.shopex.ecshopx.orders.event.JushuitanTradeAftersalesSyncSpringEvent;
import cn.shopex.ecshopx.orders.event.WdtErpTradeAfterSaleSyncSpringEvent;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AftersalesRefundErpSyncEventListener {

	private static final Logger log =
			LoggerFactory.getLogger(AftersalesRefundErpSyncEventListener.class);

	@EventListener
	public void onJushuitanTradeAftersalesSync(JushuitanTradeAftersalesSyncSpringEvent event) {
		Map<String, Object> p = event.getPayload();
		log.info(
				"JushuitanTradeAftersalesSyncSpringEvent order_id={} aftersales_bn={} aftersales_status={}",
				p.get("order_id"),
				p.get("aftersales_bn"),
				p.get("aftersales_status"));
	}

	@EventListener
	public void onWdtErpTradeAfterSaleSync(WdtErpTradeAfterSaleSyncSpringEvent event) {
		Map<String, Object> p = event.getPayload();
		log.info(
				"WdtErpTradeAfterSaleSyncSpringEvent order_id={} aftersales_bn={} aftersales_status={}",
				p.get("order_id"),
				p.get("aftersales_bn"),
				p.get("aftersales_status"));
	}
}
