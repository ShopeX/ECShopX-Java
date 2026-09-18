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

package cn.shopex.ecshopx.orders.integration.notify;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service("orderDeliverySuccessNotifyWechat")
@ConditionalOnProperty(
		name = "ecshopx.orders.delivery-success-notify.enabled",
		havingValue = "true")
public class OrderDeliverySuccessNotifyWechatService implements OrderDeliverySuccessNotifyPort {

	private static final Logger log = LoggerFactory.getLogger(OrderDeliverySuccessNotifyWechatService.class);

	@Override
	public void sendDeliverySuccessNotice(Map<String, Object> context) {
		log.warn(
				"delivery success notify (wechat) not fully wired; context keys={}",
				context == null ? null : context.keySet());
	}
}
