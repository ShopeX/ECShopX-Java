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

import cn.shopex.ecshopx.common.dispatch.InvoiceRedJobDispatchPublisher;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoopInvoiceRedJobDispatchPublisher implements InvoiceRedJobDispatchPublisher {

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public void publish(Map<String, Object> aftersalesSnapshot) {
		int n = callCount.incrementAndGet();
		Object bn = aftersalesSnapshot == null ? null : aftersalesSnapshot.get("aftersales_bn");
		Object oid = aftersalesSnapshot == null ? null : aftersalesSnapshot.get("order_id");
		Object cid = aftersalesSnapshot == null ? null : aftersalesSnapshot.get("company_id");
		log.info("[cron-mock][invoice-red-job] called#{}, company_id={}, order_id={}, aftersales_bn={}", n, cid, oid, bn);
	}
}
