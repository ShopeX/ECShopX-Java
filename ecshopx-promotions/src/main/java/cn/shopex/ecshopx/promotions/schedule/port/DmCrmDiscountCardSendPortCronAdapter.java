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

package cn.shopex.ecshopx.promotions.schedule.port;

import cn.shopex.ecshopx.common.cron.DmCrmDiscountCardSendCronPort;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmDiscountCardSendPort;

/** test-cron：将 common 窄 Port 适配为 third-party 的 {@link DmCrmDiscountCardSendPort}。 */
public final class DmCrmDiscountCardSendPortCronAdapter implements DmCrmDiscountCardSendPort {

	private final DmCrmDiscountCardSendCronPort cronPort;

	public DmCrmDiscountCardSendPortCronAdapter(DmCrmDiscountCardSendCronPort cronPort) {
		this.cronPort = cronPort;
	}

	@Override
	public boolean isOpen(long companyId) {
		return cronPort.isOpen(companyId);
	}

	@Override
	public String sendCoupon(long companyId, long userId, long cardId, String dmCardId, String mobile) {
		return cronPort.sendCoupon(companyId, userId, cardId, dmCardId, mobile);
	}
}
