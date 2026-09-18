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

package cn.shopex.ecshopx.common.dispatch;

public final class ReservationDispatchEventNames {

	public static final String EVENT_RESERVATION_FINISH =
			"event:260:ReservationBundle\\Events\\ReservationFinishEvent";

	public static final String LISTENER_RESERVATION_FINISH_WORK_SHIFT_ADD =
			"listener:ReservationBundle\\Listeners\\ReservationFinishWorkShiftAdd";

	public static final String LISTENER_RESERVATION_FINISH_SEND_WXA_TEMPLATE =
			"listener:ReservationBundle\\Listeners\\ReservationFinishSendWxaTemplate";

	public static final String LISTENER_RESERVATION_REMIND_SEND_WXA_TEMPLATE =
			"listener:ReservationBundle\\Listeners\\ReservationRemindSendWxaTemplate";

	private ReservationDispatchEventNames() {}
}
