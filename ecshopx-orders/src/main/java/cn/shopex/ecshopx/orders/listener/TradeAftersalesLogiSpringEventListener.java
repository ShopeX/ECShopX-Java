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

import cn.shopex.ecshopx.orders.event.TradeAftersalesLogiSpringEvent;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TradeAftersalesLogiSpringEventListener {

	private static final Logger log = LoggerFactory.getLogger(TradeAftersalesLogiSpringEventListener.class);

	@EventListener
	public void onTradeAftersalesLogi(TradeAftersalesLogiSpringEvent event) {
		Map<String, Object> p = event.getPayload();
		log.info("TradeAftersalesLogiSpringEvent order_id={} aftersales_bn={}", p.get("order_id"), p.get("aftersales_bn"));
	}
}
