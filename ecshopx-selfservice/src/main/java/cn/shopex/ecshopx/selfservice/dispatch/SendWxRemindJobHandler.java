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

package cn.shopex.ecshopx.selfservice.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.common.port.selfservice.SendRegistrationWxRemindQueueMessage;
import cn.shopex.ecshopx.selfservice.integration.SendRegistrationWxRemindJobHandler;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SendWxRemindJobHandler implements DispatchHandler {

	private final SendRegistrationWxRemindJobHandler sendRegistrationWxRemindJobHandler;

	public SendWxRemindJobHandler(SendRegistrationWxRemindJobHandler sendRegistrationWxRemindJobHandler) {
		this.sendRegistrationWxRemindJobHandler = sendRegistrationWxRemindJobHandler;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long activityId = extractLong(payload.get("activity_id"));
		sendRegistrationWxRemindJobHandler.handle(new SendRegistrationWxRemindQueueMessage(activityId));
	}

	private static long extractLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}
}
