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

package cn.shopex.ecshopx.common.cron.mock;

import cn.shopex.ecshopx.common.cron.payment.AdapayPaymentConfirmHttpGateway;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoopAdapayPaymentConfirmHttpGateway implements AdapayPaymentConfirmHttpGateway {

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public Map<String, Object> call(Map<String, Object> body) {
		int n = callCount.incrementAndGet();
		log.info("[cron-mock][adapay-auto-payment-confirm-http] called#{}, args={}", n, Arrays.toString(new Object[] {body}));
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", "succeeded");
		data.put("id", "noop-pc-id");
		return Map.of("data", data);
	}
}
