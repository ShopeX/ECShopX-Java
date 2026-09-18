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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * test-cron 下用 {@code CronTestMockConfig} 委托覆盖 {@code HfPayOrderApplyIdGenerator}；阶段 4 grep {@code [cron-mock][hfpay-order-id]}。
 */
@Slf4j
public class NoopHfPayOrderApplyIdGenerator {

	private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;
	private static final int ORDER_WIDTH = 9;

	private final AtomicInteger callCount = new AtomicInteger();

	public String nextOrderId() {
		int n = callCount.incrementAndGet();
		String day = LocalDate.now().format(DAY);
		String id = day + String.format("%0" + ORDER_WIDTH + "d", n);
		log.info(
				"[cron-mock][hfpay-order-id] called#{}, args={}",
				n,
				Arrays.toString(new Object[] {id}));
		return id;
	}

	public String nextApplyId() {
		int n = callCount.incrementAndGet();
		String day = LocalDate.now().format(DAY);
		String id = day + String.format("%08d", n);
		log.info(
				"[cron-mock][hfpay-order-id] called#{}, args={}",
				n,
				Arrays.toString(new Object[] {id}));
		return id;
	}
}
