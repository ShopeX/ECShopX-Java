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

package cn.shopex.ecshopx.orders.service.offline.export;

import cn.shopex.ecshopx.orders.domain.OfflinePayment;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OfflinePaymentExportQuerySupport {

	private OfflinePaymentExportQuerySupport() {}

	public static LambdaQueryWrapper<OfflinePayment> toWrapper(LinkedHashMap<String, Object> filter) {
		LambdaQueryWrapper<OfflinePayment> w = new LambdaQueryWrapper<>();
		for (Map.Entry<String, Object> e : filter.entrySet()) {
			String key = e.getKey();
			Object val = e.getValue();
			if (val == null) {
				continue;
			}
			switch (key) {
				case "create_time|gte" -> w.ge(OfflinePayment::getCreateTime, toIntEpoch(val));
				case "create_time|lte" -> w.le(OfflinePayment::getCreateTime, toIntEpoch(val));
				case "order_id" -> applyOrderIdEq(w, val);
				case "company_id" -> w.eq(OfflinePayment::getCompanyId, toLong(val));
				case "user_id" -> w.eq(OfflinePayment::getUserId, toLong(val));
				case "check_status" -> w.eq(OfflinePayment::getCheckStatus, toInt(val));
				case "pay_sn" -> w.eq(OfflinePayment::getPaySn, String.valueOf(val).trim());
				case "pay_account_no" -> w.eq(OfflinePayment::getPayAccountNo, String.valueOf(val).trim());
				case "pay_account_bank" -> w.eq(OfflinePayment::getPayAccountBank, String.valueOf(val).trim());
				case "bank_account_name" -> w.eq(OfflinePayment::getBankAccountName, String.valueOf(val).trim());
				default -> {
					// ignore unknown keys
				}
			}
		}
		return w;
	}

	private static void applyOrderIdEq(LambdaQueryWrapper<OfflinePayment> w, Object val) {
		String s = String.valueOf(val).trim();
		if (s.isEmpty()) {
			return;
		}
		try {
			w.eq(OfflinePayment::getOrderId, Long.parseLong(s));
		} catch (NumberFormatException ignored) {
			// align with repository: non-numeric order_id string adds no condition
		}
	}

	private static int toIntEpoch(Object val) {
		if (val instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(val).trim());
	}

	private static long toLong(Object val) {
		if (val instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(val).trim());
	}

	private static int toInt(Object val) {
		if (val instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(val).trim());
	}
}
