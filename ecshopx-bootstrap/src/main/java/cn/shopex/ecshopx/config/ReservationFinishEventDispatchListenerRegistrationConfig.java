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

import cn.shopex.ecshopx.common.dispatch.ReservationDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchRegistry;
import cn.shopex.ecshopx.dispatch.ListenerDispatchOptions;
import cn.shopex.ecshopx.reservation.dispatch.ReservationFinishSendWxaTemplateDispatchListener;
import cn.shopex.ecshopx.reservation.dispatch.ReservationFinishWorkShiftAddDispatchListener;
import cn.shopex.ecshopx.reservation.dispatch.ReservationRemindSendWxaTemplateDispatchListener;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Order(77)
public class ReservationFinishEventDispatchListenerRegistrationConfig {

	private final DispatchRegistry dispatchRegistry;
	private final ReservationFinishWorkShiftAddDispatchListener reservationFinishWorkShiftAddDispatchListener;
	private final ReservationFinishSendWxaTemplateDispatchListener reservationFinishSendWxaTemplateDispatchListener;
	private final ReservationRemindSendWxaTemplateDispatchListener reservationRemindSendWxaTemplateDispatchListener;

	public ReservationFinishEventDispatchListenerRegistrationConfig(
			DispatchRegistry dispatchRegistry,
			ReservationFinishWorkShiftAddDispatchListener reservationFinishWorkShiftAddDispatchListener,
			ReservationFinishSendWxaTemplateDispatchListener reservationFinishSendWxaTemplateDispatchListener,
			ReservationRemindSendWxaTemplateDispatchListener reservationRemindSendWxaTemplateDispatchListener) {
		this.dispatchRegistry = dispatchRegistry;
		this.reservationFinishWorkShiftAddDispatchListener = reservationFinishWorkShiftAddDispatchListener;
		this.reservationFinishSendWxaTemplateDispatchListener = reservationFinishSendWxaTemplateDispatchListener;
		this.reservationRemindSendWxaTemplateDispatchListener = reservationRemindSendWxaTemplateDispatchListener;
	}

	@PostConstruct
	public void registerReservationFinishEventListeners() {
		dispatchRegistry.registerEventListener(
				ReservationDispatchEventNames.EVENT_RESERVATION_FINISH,
				ReservationDispatchEventNames.LISTENER_RESERVATION_FINISH_WORK_SHIFT_ADD,
				ListenerDispatchOptions.syncDefaults(),
				reservationFinishWorkShiftAddDispatchListener);
		dispatchRegistry.registerEventListener(
				ReservationDispatchEventNames.EVENT_RESERVATION_FINISH,
				ReservationDispatchEventNames.LISTENER_RESERVATION_FINISH_SEND_WXA_TEMPLATE,
				ListenerDispatchOptions.syncDefaults(),
				reservationFinishSendWxaTemplateDispatchListener);
		dispatchRegistry.registerEventListener(
				ReservationDispatchEventNames.EVENT_RESERVATION_FINISH,
				ReservationDispatchEventNames.LISTENER_RESERVATION_REMIND_SEND_WXA_TEMPLATE,
				ListenerDispatchOptions.async("default", null),
				reservationRemindSendWxaTemplateDispatchListener);
	}
}
