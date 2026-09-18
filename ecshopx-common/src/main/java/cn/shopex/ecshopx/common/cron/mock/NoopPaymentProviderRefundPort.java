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

import cn.shopex.ecshopx.common.port.payment.PaymentProviderRefundPort;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * test-cron 下替换真实支付外呼；与 {@code trade-refund-finish-external} 等一同供阶段 4 快照。
 */
@Slf4j
public class NoopPaymentProviderRefundPort implements PaymentProviderRefundPort {

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public void executeProviderRefundTrigger(long companyId, long refundBn, String payType) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][payment-do-refund] called#{}, args=companyId={}, refundBn={}, payType={}",
				n,
				companyId,
				refundBn,
				payType);
	}
}
