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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class OfflinePaymentPayFeeSupport {

	private OfflinePaymentPayFeeSupport() {}

	static long resolveUploadPayFee(Object payFeeRaw, long orderTotalFee) {
		if (payFeeRaw == null) {
			return orderTotalFee;
		}
		String raw = String.valueOf(payFeeRaw).trim();
		if (raw.isEmpty()) {
			return orderTotalFee;
		}
		long payFee;
		try {
			payFee = new BigDecimal(raw).setScale(0, RoundingMode.HALF_UP).longValue();
		} catch (Exception e) {
			return orderTotalFee;
		}
		if (payFee <= 0L) {
			return orderTotalFee;
		}
		if (orderTotalFee > 0L && payFee != orderTotalFee) {
			return orderTotalFee;
		}
		return payFee;
	}

	static long parseTotalFeeToLong(String raw) {
		if (raw == null || raw.isBlank()) {
			return 0L;
		}
		try {
			return new BigDecimal(raw.trim()).setScale(0, RoundingMode.HALF_UP).longValue();
		} catch (Exception e) {
			return 0L;
		}
	}
}
