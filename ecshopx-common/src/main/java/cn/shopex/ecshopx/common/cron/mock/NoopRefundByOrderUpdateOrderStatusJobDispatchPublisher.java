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

import cn.shopex.ecshopx.common.dispatch.RefundByOrderUpdateOrderStatusJobDispatchPublisher;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoopRefundByOrderUpdateOrderStatusJobDispatchPublisher
		implements RefundByOrderUpdateOrderStatusJobDispatchPublisher {

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public void publish(long orderId, long companyId, String orderType) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][refund-by-order-update-order-status-job] called#{} orderId={} companyId={} orderType={}",
				n,
				orderId,
				companyId,
				orderType);
	}
}
