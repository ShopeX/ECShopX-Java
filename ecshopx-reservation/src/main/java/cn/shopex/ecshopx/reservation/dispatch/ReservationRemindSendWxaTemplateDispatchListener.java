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

package cn.shopex.ecshopx.reservation.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Runs on the synchronous {@code DispatchOptions.eventDefaults()} path: {@code SyncDispatchDriver} invokes
 * {@link #onEvent} in registration order without reading {@code RegisteredListener.options()}. The
 * {@link cn.shopex.ecshopx.dispatch.ListenerDispatchOptions#async(String, java.time.Duration)} tuple is
 * registered so metadata matches the CSV listener name and stays aligned if the parent event is later
 * switched to async fan-out; it does not change current in-request thread semantics.
 */
@Component
public class ReservationRemindSendWxaTemplateDispatchListener implements DispatchListener {

	private final ReservationRemindSendWxaTemplateDispatchService reservationRemindSendWxaTemplateDispatchService;

	public ReservationRemindSendWxaTemplateDispatchListener(
			ReservationRemindSendWxaTemplateDispatchService reservationRemindSendWxaTemplateDispatchService) {
		this.reservationRemindSendWxaTemplateDispatchService = reservationRemindSendWxaTemplateDispatchService;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		reservationRemindSendWxaTemplateDispatchService.handle(payload);
	}
}
