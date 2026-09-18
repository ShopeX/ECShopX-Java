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

package cn.shopex.ecshopx.reservation.service;

import cn.shopex.ecshopx.common.dispatch.ReservationSendSmsNoticeDispatchPublisher;
import cn.shopex.ecshopx.reservation.domain.ReservationSetting;
import cn.shopex.ecshopx.reservation.port.ReservationSmsOpenTemplatePort;
import cn.shopex.ecshopx.reservation.support.ReservationSmsQueueDelayFormatter;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReservationAsyncNotifier {

	private final ReservationSmsOpenTemplatePort reservationSmsOpenTemplatePort;

	private final ReservationSendSmsNoticeDispatchPublisher reservationSendSmsNoticeDispatchPublisher;

	private final Clock clock;

	public ReservationAsyncNotifier(
			ReservationSmsOpenTemplatePort reservationSmsOpenTemplatePort,
			ReservationSendSmsNoticeDispatchPublisher reservationSendSmsNoticeDispatchPublisher,
			@Autowired(required = false) Clock clock) {
		this.reservationSmsOpenTemplatePort = reservationSmsOpenTemplatePort;
		this.reservationSendSmsNoticeDispatchPublisher = reservationSendSmsNoticeDispatchPublisher;
		this.clock = clock != null ? clock : Clock.systemUTC();
	}

	public void afterReservationCreated(long companyId, Map<String, Object> recordSnapshot, ReservationSetting setting) {
		Map<String, Object> t1 = reservationSmsOpenTemplatePort.getOpenTemplateInfo(companyId, "reservation_notice");
		if (t1 != null && !t1.isEmpty()) {
			reservationSendSmsNoticeDispatchPublisher.publishReservationNoticeSms(companyId, recordSnapshot);
		} else {
			return;
		}
		Map<String, Object> t2 = reservationSmsOpenTemplatePort.getOpenTemplateInfo(companyId, "gotoShop_notice");
		if (t2 == null || t2.isEmpty() || setting == null) {
			return;
		}
		int smsDelayHours = resolveSmsDelayHours(setting);
		long toShopTime = epochSecondsFromSnapshot(recordSnapshot.get("to_shop_time"));
		long endTime = toShopTime - smsDelayHours * 3600L;
		long rawDelay = endTime - clock.instant().getEpochSecond();
		long d = ReservationSmsQueueDelayFormatter.formatQueueDelaySeconds(rawDelay);
		if (d > 0) {
			reservationSendSmsNoticeDispatchPublisher.publishGotoShopNoticeSmsDelayed(
					companyId, recordSnapshot, Duration.ofSeconds(d));
		}
	}

	private static int resolveSmsDelayHours(ReservationSetting setting) {
		String s = setting.getSmsDelay();
		if (s == null || s.isBlank()) {
			return 1;
		}
		try {
			return Integer.parseInt(s.trim());
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private static long epochSecondsFromSnapshot(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}
}
