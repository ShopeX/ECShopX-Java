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

import cn.shopex.ecshopx.common.dispatch.ReservationDispatchJobNames;
import cn.shopex.ecshopx.common.dispatch.ReservationExpireCancelDispatchPublisher;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import cn.shopex.ecshopx.reservation.support.ReservationSmsQueueDelayFormatter;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReservationExpireCancelDispatchPublisherImpl implements ReservationExpireCancelDispatchPublisher {

	private final DispatchFacade dispatchFacade;
	private final Clock clock;

	public ReservationExpireCancelDispatchPublisherImpl(
			DispatchFacade dispatchFacade,
			@Autowired(required = false) Clock clock) {
		this.dispatchFacade = dispatchFacade;
		this.clock = clock != null ? clock : Clock.systemUTC();
	}

	@Override
	public void publishExpireCancelAfterCreate(long companyId, Map<String, Object> reservationRecordSnapshot) {
		Object rawToShop = reservationRecordSnapshot.get("to_shop_time");
		long toShopEpoch =
				rawToShop instanceof Number
						? ((Number) rawToShop).longValue()
						: Long.parseLong(String.valueOf(rawToShop).trim());
		long nowEpoch = clock.instant().getEpochSecond();
		long rawSeconds = toShopEpoch + 7200L - nowEpoch;
		long d = ReservationSmsQueueDelayFormatter.formatQueueDelaySeconds(rawSeconds);
		Map<String, Object> payload = new LinkedHashMap<>(reservationRecordSnapshot);
		payload.put("company_id", companyId);
		dispatchFacade.dispatchJob(
				ReservationDispatchJobNames.RESERVATION_EXPIRE_CANCEL_DELAYED,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"default",
						Duration.ofSeconds(d),
						RetryPolicy.platformDefault()));
	}
}
