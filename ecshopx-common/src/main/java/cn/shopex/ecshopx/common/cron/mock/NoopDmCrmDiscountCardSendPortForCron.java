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

import cn.shopex.ecshopx.common.cron.DmCrmDiscountCardSendCronPort;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/** plan §4 {@code [cron-mock][dm-crm-coupon]}；bean-alias {@code dm-crm-coupon}。 */
@Slf4j
public class NoopDmCrmDiscountCardSendPortForCron implements DmCrmDiscountCardSendCronPort {

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public boolean isOpen(long companyId) {
		return false;
	}

	@Override
	public String sendCoupon(long companyId, long userId, long cardId, String dmCardId, String mobile) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][dm-crm-coupon] called#{}, args={}",
				n,
				Arrays.toString(new Object[] {companyId, userId, cardId, dmCardId, mobile}));
		return "";
	}
}
